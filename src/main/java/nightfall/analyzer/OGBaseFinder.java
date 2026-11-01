package nightfall.analyzer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.fabricmc.loader.api.FabricLoader;
import nightfall.core.Modules;
import nightfall.gui.NoticeManager;

/** Zentraler Finder mit Round-Robin-Chunk-Scheduler und begrenzter Arbeit pro Client-Tick. */
public final class OGBaseFinder {
  public static final Map<Long, Result> results = new ConcurrentHashMap<>();
  static final Map<Long, Integer> seen = new HashMap<>();
  static final Set<Long> notified = new HashSet<>();
  static String dim = "";
  static int offR = -1;
  static List<int[]> offs = new ArrayList<>();
  static int offsetCursor;
  static ChunkAnalyzer.Scan pendingScan;
  static long pendingKey;
  static int pendingScanY;
  static final Map<Long, Integer> scanPlayerY = new HashMap<>();
  /** Y-Level der letzten Analyse, bei der der Chunk noch sichtbar verdaechtig war. */
  static final Map<Long, Integer> lastShownScanY = new HashMap<>();
  /** Historische Markierungen bleiben auch nach erneutem Scan oder RTP erhalten. */
  static final Map<String, VanishedMark> vanishedMarks = new ConcurrentHashMap<>();
  static boolean vanishedMarksLoaded;

  public record VanishedMark(int cx, int cz, int previousScore, int previousEvidence,
      int previousY, int currentY, long markedAtMillis, String dimension) {}
  static double lastX = Double.NaN, lastZ = Double.NaN, lastPlayerY = Double.NaN;
  static int rescanAfterDescentUntil;
  static int lastAntiEspNoticeTick = Integer.MIN_VALUE / 2;
  static Object lastWorldInstance;
  static Object lastConnectionInstance;

  public static boolean shown(Result r) {
    if (r == null || r.learnedFake()) return false;
    var m = Modules.BASE;
    if (r.decoy()) return m.on("Show Decoys");
    return r.score() >= Math.max(50, m.s("Min Score").i())
        && r.evidence() >= Math.max(2, m.s("Min Evidence").i());
  }

  public static int color(Result r) {
    if (r.decoy()) return 0xFF6B6B7B;
    return r.score() < 50 ? 0xFFEAB308 : r.score() < 75 ? 0xFFF97316 : 0xFFEF4444;
  }

  private static String vanishedKey(String dimension, int cx, int cz) {
    return dimension + "|" + cx + "," + cz;
  }

  private static Path vanishedFile() {
    return FabricLoader.getInstance().getConfigDir().resolve("nightfall-vanished-bases.csv");
  }

  private static synchronized void loadVanishedMarks() {
    if (vanishedMarksLoaded) return;
    vanishedMarksLoaded = true;
    Path file = vanishedFile();
    if (!Files.isRegularFile(file)) return;
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        String value = line.trim();
        if (value.isEmpty() || value.startsWith("#")) continue;
        String[] p = value.split(",", -1);
        if (p.length != 8) continue;
        try {
          String dimension = new String(Base64.getUrlDecoder().decode(p[0]), StandardCharsets.UTF_8);
          VanishedMark mark = new VanishedMark(Integer.parseInt(p[1]), Integer.parseInt(p[2]),
              Math.max(0, Math.min(100, Integer.parseInt(p[3]))), Math.max(0, Integer.parseInt(p[4])),
              Integer.parseInt(p[5]), Integer.parseInt(p[6]), Math.max(0L, Long.parseLong(p[7])), dimension);
          vanishedMarks.put(vanishedKey(dimension, mark.cx(), mark.cz()), mark);
        } catch (RuntimeException ignored) { /* Beschaedigte Zeile ueberspringen. */ }
      }
    } catch (IOException ignored) { /* Markierungen bleiben optional; Spielbetrieb laeuft weiter. */ }
  }

  private static synchronized void saveVanishedMarks() {
    loadVanishedMarks();
    try {
      List<VanishedMark> all = new ArrayList<>(vanishedMarks.values());
      if (all.size() > 512) {
        all.sort(Comparator.comparingLong(VanishedMark::markedAtMillis));
        for (int i = 0; i < all.size() - 512; i++) {
          VanishedMark old = all.get(i);
          vanishedMarks.remove(vanishedKey(old.dimension(), old.cx(), old.cz()));
        }
        all = new ArrayList<>(vanishedMarks.values());
      }
      List<String> lines = new ArrayList<>();
      lines.add("# Dimension(base64url),ChunkX,ChunkZ,previousScore,previousEvidence,previousY,currentY,markedAtMillis");
      for (VanishedMark mark : all) {
        String encodedDimension = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(mark.dimension().getBytes(StandardCharsets.UTF_8));
        lines.add(encodedDimension + "," + mark.cx() + "," + mark.cz() + "," + mark.previousScore()
            + "," + mark.previousEvidence() + "," + mark.previousY() + "," + mark.currentY() + "," + mark.markedAtMillis());
      }
      Files.createDirectories(vanishedFile().getParent());
      Files.write(vanishedFile(), lines, StandardCharsets.UTF_8);
    } catch (IOException ignored) { /* Anzeige funktioniert auch ohne persistente Speicherung. */ }
  }

  public static boolean isVanishedMarked(int cx, int cz, String dimension) {
    loadVanishedMarks();
    return vanishedMarks.containsKey(vanishedKey(dimension, cx, cz));
  }

  public static VanishedMark vanishedMark(int cx, int cz, String dimension) {
    loadVanishedMarks();
    return vanishedMarks.get(vanishedKey(dimension, cx, cz));
  }

  public static Collection<VanishedMark> vanishedMarks(String dimension) {
    loadVanishedMarks();
    List<VanishedMark> out = new ArrayList<>();
    for (VanishedMark mark : vanishedMarks.values())
      if (mark.dimension().equals(dimension)) out.add(mark);
    out.sort(Comparator.comparingLong(VanishedMark::markedAtMillis).reversed());
    return out;
  }

  public static void clearVanishedMarks(String dimension) {
    loadVanishedMarks();
    vanishedMarks.entrySet().removeIf(entry -> entry.getValue().dimension().equals(dimension));
    saveVanishedMarks();
  }

  private static void markVanished(Minecraft mc, Result previous, long key, int previousY, int currentY) {
    int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
    VanishedMark mark = new VanishedMark(cx, cz, previous.score(), previous.evidence(), previousY,
        currentY, System.currentTimeMillis(), mc.level.dimension().toString());
    loadVanishedMarks();
    String markKey = vanishedKey(mark.dimension(), cx, cz);
    boolean isNew = !vanishedMarks.containsKey(markKey);
    vanishedMarks.put(markKey, mark);
    saveVanishedMarks();
    String detail = "Chunk " + cx + ", " + cz + " | vorher Score " + previous.score()
        + ", Beweise " + previous.evidence() + " | Y " + previousY + " -> " + currentY
        + ". Vorher sichtbare Hinweise sind nach dem Abstieg verschwunden.";
    if (isNew) {
      mc.player.displayClientMessage(Component.literal("[Nightfall] BASE-VERDACHT VERSCHWUNDEN: "
          + (cx * 16) + " " + (cz * 16) + " (Chunk automatisch markiert)"), false);
      NoticeManager.push("Base-Verdacht nach Abstieg verschwunden", detail, 0xFF38BDF8);
    }
  }

  public static void clearForTeleport() {
    results.clear();
    seen.clear();
    notified.clear();
    scanPlayerY.clear();
    lastShownScanY.clear();
    pendingScan = null;
    offsetCursor = 0;
    lastPlayerY = Double.NaN;
    rescanAfterDescentUntil = 0;
  }

  /** Speichert das aktuelle Chunk-Profil als vom Spieler bestaetigtes False-Positive-Muster. */
  public static String learnFake(Minecraft mc, int cx, int cz) {
    if (mc == null || mc.level == null || mc.player == null) return "Keine aktive Welt.";
    FakeBaseMemory.setScope(memoryScope(mc));
    long key = ChunkPos.asLong(cx, cz);
    Result result = results.get(key);
    if (result == null) {
      LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
      if (chunk == null) return "Chunk ist clientseitig nicht geladen; stelle dich naeher heran und versuche es erneut.";
      result = ChunkAnalyzer.analyze(mc.level, chunk, mc.player.tickCount);
    }
    if (result.fingerprint() == null || !result.fingerprint().canLearn())
      return "Zu wenige unterschiedliche Merkmale in diesem Chunk; kein Muster gespeichert.";
    if (FakeBaseMemory.size() >= 32) return "Speicher voll (32 Muster). Entferne zuerst ein Muster mit /nfsus remove <nummer>.";
    if (!FakeBaseMemory.learn(result.fingerprint()))
      return "Muster wurde nicht hinzugefuegt (bereits aehnlich vorhanden oder Datei nicht beschreibbar).";
    results.put(key, result.asLearnedFake(mc.player.tickCount));
    notified.remove(key);
    trimResults();
    return "Fake-Base-Muster gespeichert (#" + FakeBaseMemory.size() + "). Dieser Chunk und sehr aehnliche Muster werden ausgeblendet.";
  }

  private static void trimResults() {
    var m = Modules.BASE;
    int maxSus = Math.max(8, Math.min(256, m.s("Max Sus Chunks").i()));
    int maxStored = Math.max(maxSus, Math.max(64, Math.min(1024, m.s("Max Stored Chunks").i())));
    int shownCount = 0;
    for (Result result : results.values()) if (shown(result)) shownCount++;
    if (results.size() <= maxStored && shownCount <= maxSus) return;

    List<Map.Entry<Long, Result>> suspects = new ArrayList<>();
    for (var entry : results.entrySet()) if (shown(entry.getValue())) suspects.add(entry);
    suspects.sort(Comparator.<Map.Entry<Long, Result>>comparingInt(e -> e.getValue().score()).reversed()
        .thenComparing(Comparator.comparingInt((Map.Entry<Long, Result> e) -> e.getValue().tick()).reversed()));
    for (int i = maxSus; i < suspects.size(); i++) results.remove(suspects.get(i).getKey(), suspects.get(i).getValue());

    while (results.size() > maxStored) {
      Map.Entry<Long, Result> victim = results.entrySet().stream().min(
          Comparator.<Map.Entry<Long, Result>>comparingInt(e -> shown(e.getValue()) ? 1 : 0)
              .thenComparingInt(e -> e.getValue().score())
              .thenComparingInt(e -> e.getValue().tick())).orElse(null);
      if (victim == null) break;
      results.remove(victim.getKey(), victim.getValue());
      notified.remove(victim.getKey());
    }
    notified.removeIf(key -> !results.containsKey(key));
    scanPlayerY.keySet().removeIf(key -> !results.containsKey(key));
    lastShownScanY.keySet().removeIf(key -> !results.containsKey(key));
  }

  private static boolean beginNextScan(Minecraft mc, int playerChunkX, int playerChunkZ, int tick) {
    if (offs.isEmpty()) return false;
    int checks = Math.min(offs.size(), 48);
    int nearRecheck = Math.max(20, Modules.BASE.s("Near Recheck Ticks").i());
    for (int i = 0; i < checks; i++) {
      int[] offset = offs.get(offsetCursor);
      offsetCursor++;
      if (offsetCursor >= offs.size()) offsetCursor = 0;

      int cx = playerChunkX + offset[0], cz = playerChunkZ + offset[1];
      long key = ChunkPos.asLong(cx, cz);
      Result old = results.get(key);
      int distance = Math.max(Math.abs(offset[0]), Math.abs(offset[1]));
      boolean refreshNearbySuspect = old != null && shown(old) && distance <= 2
          && tick - old.tick() >= nearRecheck;
      boolean recheckAfterDescent = old != null && tick <= rescanAfterDescentUntil
          && distance <= 2 && tick - old.tick() >= 20;
      if (old != null && tick - old.tick() < 6000 && !refreshNearbySuspect && !recheckAfterDescent) continue;

      LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
      if (chunk == null) continue;
      int firstSeen = seen.computeIfAbsent(key, ignored -> tick);
      if (tick - firstSeen < 40) continue;

      pendingKey = key;
      pendingScanY = mc.player.getBlockY();
      pendingScan = ChunkAnalyzer.begin(mc.level, chunk, tick);
      return true;
    }
    return false;
  }

  private static void finishScan(Minecraft mc, Result result, long key, int scanY) {
    Result previous = results.get(key);
    int previousY = scanPlayerY.getOrDefault(key, Integer.MIN_VALUE);
    int lastVisibleY = lastShownScanY.getOrDefault(key, previousY);
    if (previous != null && shown(previous) && !shown(result)) {
      int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
      int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
      int distance = Math.max(Math.abs(cx - pcx), Math.abs(cz - pcz));
      int scoreDrop = previous.score() - result.score();
      boolean meaningfulDrop = scoreDrop >= 10 || result.evidence() < previous.evidence() || result.score() <= 20;
      // Nur markieren, wenn der Chunk geladen blieb, der Spieler deutlich tiefer ist,
      // und der zuvor sichtbare Verdacht bei der erneuten Analyse wirklich wegfaellt.
      // Ein normales Verlassen/Entladen des Chunks kommt gar nicht bis finishScan().
      if (Modules.BASE.on("Auto Mark Vanished Bases") && distance <= 1
          && lastVisibleY != Integer.MIN_VALUE && scanY <= lastVisibleY - 8 && meaningfulDrop) {
        markVanished(mc, previous, key, lastVisibleY, scanY);
      }
    }
    if (previous != null && previousY > 5 && scanY <= 5) {
      int oreDelta = result.undergroundOres() - previous.undergroundOres();
      int bedrockDelta = result.undergroundBedrock() - previous.undergroundBedrock();
      // Der v0.7/v0.8-Beschreibung zufolge werden Fake-Deepslate-Bloecke unter Y=0
      // beim Abstieg unter Y=5 wieder durch echte Blockupdates ersetzt. Das erkennt
      // nur eine beobachtete Zustandsaenderung, nicht jede moegliche Anti-ESP-Version.
      boolean restoredUndergroundData = (oreDelta >= 2 && bedrockDelta >= 2)
          || (oreDelta + bedrockDelta >= 8 && (oreDelta >= 1 || bedrockDelta >= 4));
      if (restoredUndergroundData && mc.player.tickCount - lastAntiEspNoticeTick > 1200) {
        lastAntiEspNoticeTick = mc.player.tickCount;
        String detail = "Unter Y=0 neu sichtbar: +" + oreDelta + " Erze, +" + bedrockDelta
            + " Bedrock. Moegliche serverseitige AntiESP-Obfuskation erkannt.";
        mc.player.displayClientMessage(Component.literal("[Nightfall] " + detail), false);
        NoticeManager.push("AntiESP-Blockdatenwechsel", detail, 0xFF60A5FA);
      }
    }
    results.put(key, result);
    scanPlayerY.put(key, scanY);
    if (shown(result)) lastShownScanY.put(key, scanY);
    trimResults();
    var module = Modules.BASE;
    if (results.get(key) == result && module.on("Notify") && !result.decoy() && shown(result)) {
      int cx = result.cx(), cz = result.cz();
      boolean near = false;
      for (long notifiedKey : notified) {
        if (Math.max(Math.abs(ChunkPos.getX(notifiedKey) - cx), Math.abs(ChunkPos.getZ(notifiedKey) - cz)) <= 3) {
          near = true;
          break;
        }
      }
      if (!near) {
        notified.add(key);
        mc.player.displayClientMessage(Component.literal("[Nightfall] Auffaelliger Chunk bei " + (cx * 16) + " " + (cz * 16)
            + " | Score " + result.score() + " | Konfidenz " + result.conf() + "%"), false);
        NoticeManager.push("Sus-Chunk gefunden", (cx * 16) + ", " + (cz * 16) + " | Score " + result.score(), color(result));
      }
    }
  }

  private static String memoryScope(Minecraft mc) {
    String server = mc.getCurrentServer() == null ? "singleplayer" : mc.getCurrentServer().ip;
    return server + "|" + (mc.level == null ? "unknown" : mc.level.dimension().toString());
  }

  public static void tick(Minecraft mc) {
    var module = Modules.BASE;
    if (mc.level == null || mc.player == null || mc.getConnection() == null) {
      if (lastWorldInstance != null || lastConnectionInstance != null) clearForTeleport();
      lastWorldInstance = null;
      lastConnectionInstance = null;
      lastX = lastZ = lastPlayerY = Double.NaN;
      pendingScan = null;
      return;
    }
    String dimension = mc.level.dimension().toString();
    FakeBaseMemory.setScope(memoryScope(mc));
    // A new world/connection may use the same dimension and nearby spawn coordinates.
    // Never let old chunk results or learned-fake flags leak across server sessions.
    if (mc.level != lastWorldInstance || mc.getConnection() != lastConnectionInstance || !dimension.equals(dim)) {
      clearForTeleport();
      lastWorldInstance = mc.level;
      lastConnectionInstance = mc.getConnection();
      dim = dimension;
      lastX = lastZ = lastPlayerY = Double.NaN;
    }

    double px = mc.player.getX(), pz = mc.player.getZ();
    int currentPlayerY = mc.player.getBlockY();
    if (!Double.isNaN(lastPlayerY) && currentPlayerY <= lastPlayerY - 8) {
      // Ein laufender Chunk-Scan koennte sonst Daten vor und nach dem Server-Update mischen.
      // Nach einem deutlichen Abstieg wird er verworfen und der Nahbereich neu eingereiht.
      rescanAfterDescentUntil = mc.player.tickCount + 400;
      pendingScan = null;
    }
    if (!Double.isNaN(lastPlayerY) && lastPlayerY > 5 && currentPlayerY <= 5) {
      rescanAfterDescentUntil = mc.player.tickCount + 400;
      NoticeManager.push("Pruefe Blockdaten", "Spieler unter Y=5: geladene Chunks werden auf wiederhergestellte Untergrunddaten geprueft", 0xFF60A5FA);
    }
    lastPlayerY = currentPlayerY;
    if (!Double.isNaN(lastX) && module.on("Auto Clear RTP")) {
      double threshold = module.s("RTP Threshold").d();
      if ((px - lastX) * (px - lastX) + (pz - lastZ) * (pz - lastZ) > threshold * threshold) {
        clearForTeleport();
        NoticeManager.push("Gebiet gewechselt", "Alte Chunk-Markierungen entfernt", 0xFFA855F7);
      }
    }
    lastX = px;
    lastZ = pz;
    if (!module.enabled) { pendingScan = null; return; }

    int range = module.s("Range").i();
    if (range != offR) {
      offR = range;
      offs = new ArrayList<>();
      for (int x = -range; x <= range; x++) for (int z = -range; z <= range; z++) offs.add(new int[] {x, z});
      offs.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
      offsetCursor = 0;
      pendingScan = null;
    }

    int tick = mc.player.tickCount;
    int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
    if (tick % 200 == 0 && seen.size() > 512) {
      int keepDistance = range + 4;
      seen.keySet().removeIf(key -> Math.max(Math.abs(ChunkPos.getX(key) - pcx), Math.abs(ChunkPos.getZ(key) - pcz)) > keepDistance);
    }

    // Blockzugriffe bleiben auf dem Client-Thread, aber die Arbeit pro Tick ist begrenzt.
    if (pendingScan != null) {
      LevelChunk stillLoaded = mc.level.getChunkSource().getChunkNow(ChunkPos.getX(pendingKey), ChunkPos.getZ(pendingKey));
      if (stillLoaded != pendingScan.chunk()) {
        pendingScan = null;
      } else {
        int speed = Math.max(1, Math.min(4, module.s("Scan Speed").i()));
        int blocksPerTick = Math.max(512, Math.min(32768, module.s("Scan Blocks/Tick").i() * speed));
        int budgetMs = Math.max(1, Math.min(4, module.s("Scan Budget (ms)").i()));
        long deadline = System.nanoTime() + budgetMs * 1_000_000L;
        Result completed = pendingScan.step(blocksPerTick, deadline);
        if (completed != null) {
          long key = pendingKey;
          int scanY = mc.player.getBlockY();
          pendingScan = null;
          finishScan(mc, completed, key, scanY);
        }
      }
    }
    if (pendingScan == null) beginNextScan(mc, pcx, pcz, tick);
  }
}
