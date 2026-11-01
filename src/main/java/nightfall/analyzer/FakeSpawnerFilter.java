package nightfall.analyzer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import nightfall.core.Modules;

/**
 * Gemeinsamer False-Positive-Filter fuer Spawner auf der Bedrock-Ebene an
 * Chunk-Ecken. Der Filter wird sowohl von der Chunk-Analyse als auch von
 * der Spawner-ESP verwendet.
 *
 * Er arbeitet ausschliesslich mit clientseitig geladenen Block-Entity-Daten.
 */
public final class FakeSpawnerFilter {
  private FakeSpawnerFilter() {}

  public static boolean isKnownFakeSpawner(Level level, BlockPos pos) {
    if (level == null || pos == null || Modules.BASE == null) return false;
    if (!Modules.BASE.on("Ignore Bedrock Corner Spawners")) return false;

    int yRange = Modules.BASE.s("Decoy Y Range").i();
    boolean nearBedrock = pos.getY() <= level.getMinY() + yRange;
    if (!nearBedrock) return false;

    int localX = Math.floorMod(pos.getX(), 16);
    int localZ = Math.floorMod(pos.getZ(), 16);
    boolean onCornerX = localX == 0 || localX == 15;
    boolean onCornerZ = localZ == 0 || localZ == 15;
    return onCornerX && onCornerZ;
  }
}
