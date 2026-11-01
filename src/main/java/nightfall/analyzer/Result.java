package nightfall.analyzer;
import java.util.List;
/** Analyse-Ergebnis eines Chunks. Der Score ist ein Analysewert, keine Wahrscheinlichkeit. */
public record Result(int cx, int cz, int score, int conf, int evidence, int coverage,
    boolean decoy, boolean learnedFake, List<String> why,
    double ax, double ay, double az, int tick, BaseFingerprint fingerprint,
    int undergroundOres, int undergroundBedrock) {
  public Result asLearnedFake(int now) {
    return new Result(cx, cz, 0, 5, 0, coverage, true, true,
        List.of("Als Fake-Base-Muster gespeichert und ausgeblendet"), ax, ay, az, now, fingerprint,
        undergroundOres, undergroundBedrock);
  }
}
