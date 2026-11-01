package nightfall.analyzer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import nightfall.core.Module;

/** One shared policy for hiding only locally confirmed false-positive chunks in ESP modules. */
public final class EspChunkFilter {
  private EspChunkFilter() {}

  public static boolean ignoreChunk(Module module, LevelChunk chunk) {
    if (module == null || chunk == null || !module.on("Ignore Fake Chunks")) return false;
    Result result = OGBaseFinder.results.get(chunk.getPos().toLong());
    return result != null && result.learnedFake();
  }

  public static boolean ignoreBlock(Level level, BlockPos pos) {
    return FakeSpawnerFilter.isKnownFakeSpawner(level, pos);
  }
}
