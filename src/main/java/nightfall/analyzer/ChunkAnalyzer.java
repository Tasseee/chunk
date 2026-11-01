package nightfall.analyzer;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import nightfall.core.Modules;

/** Bewertet einen geladenen Chunk inkrementell, damit ein kompletter Scan keinen Tick blockiert. */
public final class ChunkAnalyzer {
  static Set<Block> set(Block... b) { return new HashSet<>(Arrays.asList(b)); }
  static final Set<Block> LIGHT = set(Blocks.TORCH, Blocks.WALL_TORCH, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.END_ROD, Blocks.SEA_LANTERN);
  static final Set<Block> RS = set(Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER, Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON,
    Blocks.REPEATER, Blocks.COMPARATOR, Blocks.REDSTONE_LAMP, Blocks.REDSTONE_BLOCK, Blocks.TNT, Blocks.LEVER);
  static final Set<Block> ART = set(Blocks.GLASS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.SMOOTH_STONE, Blocks.POLISHED_ANDESITE,
    Blocks.POLISHED_DIORITE, Blocks.POLISHED_GRANITE, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.IRON_BLOCK,
    Blocks.GOLD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
    Blocks.IRON_DOOR, Blocks.WHITE_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.SCAFFOLDING);
  static final Set<Block> SPECIAL = set(Blocks.COBWEB, Blocks.MOSSY_COBBLESTONE, Blocks.BELL, Blocks.END_PORTAL_FRAME, Blocks.REINFORCED_DEEPSLATE,
    Blocks.SCULK_SHRIEKER, Blocks.NETHERITE_BLOCK, Blocks.BEACON);
  // Erze unter Y=0 werden nicht als Base-Indiz gewertet; sie dienen nur dazu,
  // spaetere serverseitige Blockdaten-Wiederherstellung (AntiESP) zu erkennen.
  static final Set<Block> UNDERGROUND_ORES = set(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
      Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
      Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
      Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
      Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
      Blocks.ANCIENT_DEBRIS);


  static boolean interesting(BlockState s) {
    Block b = s.getBlock();
    return LIGHT.contains(b) || RS.contains(b) || ART.contains(b) || SPECIAL.contains(b)
        || UNDERGROUND_ORES.contains(b) || b == Blocks.BEDROCK || s.is(BlockTags.RAILS);
  }

  /** Ein begrenzter Scan. step() auf dem Client-Thread aufrufen; null bedeutet: weitere Arbeit offen. */
  public static final class Scan {
    private final Level level;
    private final LevelChunk chunk;
    private final int tick;
    private final boolean naturalFilter;
    private final LevelChunkSection[] sections;
    private final List<BlockPos> storagePositions = new ArrayList<>();
    private final List<BlockPos> redstonePositions = new ArrayList<>();
    private int sectionIndex;
    private int blockIndex;
    private int storage, shulk, spawn, decoy, trial;
    private int light, rs, art, web, rail, moss, bell, frame, reinf, shriek, valuable;
    private int undergroundOres, undergroundBedrock;
    private double[] anchor;
    private Result completed;

    private Scan(Level level, LevelChunk chunk, int tick) {
      this.level = level;
      this.chunk = chunk;
      this.tick = tick;
      this.naturalFilter = Modules.BASE != null && Modules.BASE.on("Natural Filter");
      this.sections = chunk.getSections();

      // Block-Entities sind deutlich weniger als Blockzustände und werden einmal kurz erfasst.
      for (BlockEntity be : chunk.getBlockEntities().values()) {
        BlockEntityType<?> type = be.getType();
        BlockPos pos = be.getBlockPos();
        if (type == BlockEntityType.MOB_SPAWNER) {
          if (FakeSpawnerFilter.isKnownFakeSpawner(level, pos)) decoy++;
          else { spawn++; if (anchor == null) anchor = point(pos); }
        } else if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST || type == BlockEntityType.BARREL
            || type == BlockEntityType.SHULKER_BOX || type == BlockEntityType.ENDER_CHEST) {
          storage++;
          if (type == BlockEntityType.SHULKER_BOX) shulk++;
          if (storagePositions.size() < 512) storagePositions.add(pos);
          if (anchor == null) anchor = point(pos);
        } else if (type == BlockEntityType.TRIAL_SPAWNER || type == BlockEntityType.VAULT) trial++;
      }
    }

    private static double[] point(BlockPos p) { return new double[] {p.getX() + .5, p.getY() + .5, p.getZ() + .5}; }
    public LevelChunk chunk() { return chunk; }

    public Result step(int maxBlockStates) {
      return step(maxBlockStates, Long.MAX_VALUE);
    }

    /** Arbeitet bis Blocklimit oder Zeitdeadline; der Aufrufer kann so einen Tick-Budgetdeckel setzen. */
    public Result step(int maxBlockStates, long deadlineNanos) {
      if (completed != null) return completed;
      int remaining = Math.max(1, maxBlockStates);
      int processed = 0;
      while (sectionIndex < sections.length && remaining > 0) {
        // Nur alle 256 Blockzugriffe die Uhr abfragen, damit die Zeitmessung selbst kaum kostet.
        if ((processed & 255) == 0 && System.nanoTime() >= deadlineNanos) break;
        LevelChunkSection section = sections[sectionIndex];
        if (section == null || section.hasOnlyAir() || !section.maybeHas(ChunkAnalyzer::interesting)) {
          sectionIndex++;
          blockIndex = 0;
          continue;
        }
        // Gleiche Reihenfolge wie zuvor: X, danach Z, danach Y.
        int index = blockIndex++;
        int x = index & 15;
        int z = (index >> 4) & 15;
        int y = (index >> 8) & 15;
        BlockState state = section.getBlockState(x, y, z);
        if (!state.isAir()) count(state, chunk.getPos().x * 16 + x,
            level.getMinY() + sectionIndex * 16 + y, chunk.getPos().z * 16 + z);
        remaining--;
        processed++;
        if (blockIndex >= 4096) { sectionIndex++; blockIndex = 0; }
      }
      if (sectionIndex >= sections.length) completed = finish();
      return completed;
    }

    private void count(BlockState state, int x, int y, int z) {
      Block block = state.getBlock();
      if (y <= 0) {
        if (UNDERGROUND_ORES.contains(block)) undergroundOres++;
        if (block == Blocks.BEDROCK) undergroundBedrock++;
      }
      if (LIGHT.contains(block)) light++;
      else if (RS.contains(block)) {
        rs++;
        if (redstonePositions.size() < 512) redstonePositions.add(new BlockPos(x, y, z));
      } else if (ART.contains(block)) art++;
      else if (block == Blocks.COBWEB) web++;
      else if (block == Blocks.MOSSY_COBBLESTONE) moss++;
      else if (block == Blocks.BELL) bell++;
      else if (block == Blocks.END_PORTAL_FRAME) frame++;
      else if (block == Blocks.REINFORCED_DEEPSLATE) reinf++;
      else if (block == Blocks.SCULK_SHRIEKER) shriek++;
      else if (block == Blocks.NETHERITE_BLOCK || block == Blocks.BEACON) valuable++;
      else if (state.is(BlockTags.RAILS)) rail++;
    }

    private static int largestNearbyCluster(List<BlockPos> positions, int horizontalRadius, int verticalRadius) {
      if (positions.isEmpty()) return 0;
      int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
      for (BlockPos pos : positions) { minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY()); }

      // 3D-Praefixsumme ueber das 16xHoehe x16 Chunk-Volumen. Jede
      // Nachbarschaft kann danach mit acht Array-Zugriffen gezaehlt werden.
      final int sizeX = 18, sizeZ = 18, sizeY = maxY - minY + 2;
      int[] prefix = new int[sizeX * sizeY * sizeZ];
      for (BlockPos pos : positions) {
        int x = Math.floorMod(pos.getX(), 16) + 1;
        int y = pos.getY() - minY + 1;
        int z = Math.floorMod(pos.getZ(), 16) + 1;
        prefix[(x * sizeY + y) * sizeZ + z]++;
      }

      for (int x = 1; x <= 16; x++) for (int y = 1; y <= maxY - minY + 1; y++) for (int z = 1; z <= 16; z++) {
        int i = (x * sizeY + y) * sizeZ + z;
        prefix[i] += prefix[((x - 1) * sizeY + y) * sizeZ + z]
            + prefix[(x * sizeY + y - 1) * sizeZ + z]
            + prefix[(x * sizeY + y) * sizeZ + z - 1]
            - prefix[((x - 1) * sizeY + y - 1) * sizeZ + z]
            - prefix[((x - 1) * sizeY + y) * sizeZ + z - 1]
            - prefix[(x * sizeY + y - 1) * sizeZ + z - 1]
            + prefix[((x - 1) * sizeY + y - 1) * sizeZ + z - 1];
      }

      int largest = 0;
      for (BlockPos center : positions) {
        int x1 = Math.max(1, Math.floorMod(center.getX(), 16) - horizontalRadius + 1);
        int x2 = Math.min(16, Math.floorMod(center.getX(), 16) + horizontalRadius + 1);
        int y1 = Math.max(1, center.getY() - verticalRadius - minY + 1);
        int y2 = Math.min(maxY - minY + 1, center.getY() + verticalRadius - minY + 1);
        int z1 = Math.max(1, Math.floorMod(center.getZ(), 16) - horizontalRadius + 1);
        int z2 = Math.min(16, Math.floorMod(center.getZ(), 16) + horizontalRadius + 1);
        int count = queryBox(prefix, sizeY, x1, x2, y1, y2, z1, z2);
        if (count > largest) largest = count;
        if (largest == positions.size()) break;
      }
      return largest;
    }

    private static int queryBox(int[] p, int sy, int x1, int x2, int y1, int y2, int z1, int z2) {
      if (x1 > x2 || y1 > y2 || z1 > z2) return 0;
      int a = prefixAt(p, sy, x2, y2, z2);
      int b = prefixAt(p, sy, x1 - 1, y2, z2);
      int c = prefixAt(p, sy, x2, y1 - 1, z2);
      int d = prefixAt(p, sy, x2, y2, z1 - 1);
      int e = prefixAt(p, sy, x1 - 1, y1 - 1, z2);
      int f = prefixAt(p, sy, x1 - 1, y2, z1 - 1);
      int g = prefixAt(p, sy, x2, y1 - 1, z1 - 1);
      int h = prefixAt(p, sy, x1 - 1, y1 - 1, z1 - 1);
      return a - b - c - d + e + f + g - h;
    }

    private static int prefixAt(int[] p, int sy, int x, int y, int z) {
      return p[(x * sy + y) * 18 + z];
    }

    private Result finish() {
      int storageCluster = largestNearbyCluster(storagePositions, 8, 16);
      int redstoneCluster = largestNearbyCluster(redstonePositions, 8, 16);
      int score = 0, evidence = 0, naturals = 0;
      List<String> why = new ArrayList<>();

      // Einzelhinweise aus entfernten Chunk-Ecken werden nicht zu einer kuenstlichen Base addiert.
      boolean storageSignal = storage >= 6 && storageCluster >= 4;
      boolean redstoneSignal = rs >= 12 && redstoneCluster >= 8;
      boolean valuableSignal = valuable >= 1;

      if (storageSignal) {
        int points = storage >= 24 ? 36 : storage >= 12 ? 30 : 24;
        score += points; evidence++;
        why.add("Zusammenhaengende Lagerstruktur: " + storage + " gesamt / " + storageCluster + " im Nahbereich (+" + points + ")");
        if (shulk >= 3) { score += 8; why.add("Mehrere Shulker-Boxen: " + shulk + " (+8 Zusatzhinweis)"); }
      } else if (storage > 0) {
        why.add("Lagerbloecke ohne ausreichend dichte Gruppe: " + storage + " (Gruppe " + storageCluster + ")");
      }

      if (redstoneSignal) {
        int points = rs >= 64 ? 36 : rs >= 32 ? 32 : 24;
        score += points; evidence++;
        why.add("Zusammenhaengende Redstone-/Maschinenstruktur: " + rs + " gesamt / " + redstoneCluster + " im Nahbereich (+" + points + ")");
      } else if (rs > 0) {
        why.add("Redstone ohne ausreichend dichte Gruppe: " + rs + " (Gruppe " + redstoneCluster + ")");
      }

      if (valuableSignal) {
        score += 35; evidence++;
        why.add("Seltene Wertbloecke (Netherite/Beacon): " + valuable + " (+35)");
      }
      if (art >= 160 && (storageSignal || redstoneSignal)) {
        score += 5;
        why.add("Groessere kuenstliche Bauumgebung: " + art + " (+5 Zusatzhinweis)");
      }
      if (light >= 6) why.add("Beleuchtung erkannt (nur Kontext): " + light);
      if (spawn > 0) why.add("Spawner erkannt (allein kein Verdachtshinweis): " + spawn);

      boolean dungeon = spawn > 0 && moss > 0;
      if (naturalFilter) {
        if (web > 0 && rail > 0) { score -= 35; naturals++; why.add("Natuerliche Struktur: Mineshaft (-35)"); }
        if (dungeon) { score -= 30; naturals++; why.add("Natuerliche Struktur: Dungeon (-30)"); }
        if (bell > 0) { score -= 40; naturals++; why.add("Natuerliche Struktur: Dorf (-40)"); }
        if (trial > 0) { score -= 40; naturals++; why.add("Natuerliche Struktur: Trial Chamber (-40)"); }
        if (reinf > 0 || shriek > 0) { score -= 40; naturals++; why.add("Natuerliche Struktur: Ancient City (-40)"); }
        if (frame > 0) { score -= 40; naturals++; why.add("Natuerliche Struktur: Stronghold (-40)"); }
      }

      BaseFingerprint fingerprint = new BaseFingerprint(storage, shulk, rs, art, light, spawn,
          web, rail, moss, bell + frame + reinf + shriek, valuable, storageCluster, redstoneCluster);
      int learnedMatch = FakeBaseMemory.matchPercent(fingerprint);
      boolean learnedFake = learnedMatch >= 88;
      if (learnedFake) {
        score = 0;
        evidence = 0;
        why.add("Gelerntes Fake-Base-Muster erkannt (" + learnedMatch + "% Aehnlichkeit)");
      }
      score = Math.max(0, Math.min(100, score));
      if (decoy > 0) why.add("Fake-Spawner an Bedrock-Chunk-Ecke ignoriert: " + decoy);
      boolean isDecoy = learnedFake || (decoy > 0 && score < 25);
      int confidence = Math.max(5, Math.min(85, 15 + 25 * evidence - (naturals > 0 ? 20 : 0)));
      if (anchor == null) {
        int centerX = chunk.getPos().x * 16 + 8, centerZ = chunk.getPos().z * 16 + 8;
        anchor = new double[] {centerX, level.getMinY() + 32, centerZ};
      }
      return new Result(chunk.getPos().x, chunk.getPos().z, score, confidence, evidence, 100,
          isDecoy, learnedFake, why, anchor[0], anchor[1], anchor[2], tick, fingerprint,
          undergroundOres, undergroundBedrock);
    }
  }

  public static Scan begin(Level level, LevelChunk chunk, int tick) { return new Scan(level, chunk, tick); }

  /** Synchroner Helfer fuer den expliziten Ein-Chunk-Befehl /nfsus fake. */
  public static Result analyze(Level level, LevelChunk chunk, int tick) {
    Scan scan = begin(level, chunk, tick);
    Result result;
    do { result = scan.step(65_536); } while (result == null);
    return result;
  }
}
