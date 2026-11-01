package nightfall.analyzer;

/** Datensparsames Profil eines Chunk-Scans inklusive raeumlicher Cluster-Merkmale. */
public record BaseFingerprint(
    int storage, int shulkers, int redstone, int artificial, int lights,
    int spawners, int webs, int rails, int mossy, int villageOrStructure,
    int valuable, int storageCluster, int redstoneCluster) {

  private static final double[] SCALE = {24, 8, 64, 160, 64, 4, 12, 12, 16, 8, 4, 16, 64};
  private static final double[] WEIGHT = {3.0, 2.0, 3.0, 1.0, 0.45, 1.1, 0.65, 0.65, 0.55, 1.2, 3.5, 1.8, 1.8};
  // Clusterwerte bestaetigen Dichte, zaehlen aber nicht als unabhaengige Signalarten.
  private static final int[] DISTINCTIVE = {0, 2, 5, 9, 10};

  public int[] values() {
    return new int[] {storage, shulkers, redstone, artificial, lights, spawners,
        webs, rails, mossy, villageOrStructure, valuable, storageCluster, redstoneCluster};
  }

  public boolean canLearn() {
    int[] v = values();
    int independentSignals = 0;
    int signal = 0;
    // Nur voneinander verschiedene Merkmalsgruppen trainieren. Shulker sind
    // bereits Teil des Lager-Signals; Clusterwerte bestaetigen nur die Dichte.
    for (int index : DISTINCTIVE) {
      if (v[index] > 0) {
        independentSignals++;
        signal += v[index];
      }
    }
    return independentSignals >= 2 && signal >= 4;
  }

  /** Returns similarity in percent; unknown legacy dimensions are ignored rather than treated as zero. */
  public int similarity(BaseFingerprint other) {
    if (other == null || !canLearn() || !other.canLearn()) return 0;
    int[] a = values(), b = other.values();
    int distinctiveOverlap = 0;
    for (int index : DISTINCTIVE) if (a[index] > 0 && b[index] > 0) distinctiveOverlap++;
    if (distinctiveOverlap < 2) return 0;
    if (java.util.Arrays.equals(a, b)) return 100;

    double weightedDifference = 0.0, totalWeight = 0.0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] < 0 || b[i] < 0 || (a[i] == 0 && b[i] == 0)) continue;
      double av = Math.min(1.0, a[i] / SCALE[i]);
      double bv = Math.min(1.0, b[i] / SCALE[i]);
      weightedDifference += Math.abs(av - bv) * WEIGHT[i];
      totalWeight += WEIGHT[i];
    }
    if (totalWeight == 0.0) return 0;
    return (int) Math.round((1.0 - weightedDifference / totalWeight) * 100.0);
  }
}
