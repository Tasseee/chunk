package nightfall.analyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Locally learned false-positive profiles, isolated per server and dimension. */
public final class FakeBaseMemory {
  private static final int MAX_PATTERNS = 32;
  private static final List<BaseFingerprint> PATTERNS = new ArrayList<>();
  private static boolean loaded;
  private static String activeScope = "uninitialized";
  private static Path activeFile;
  private FakeBaseMemory() {}

  private static Path legacyFile() {
    return FabricLoader.getInstance().getConfigDir().resolve("nightfall-fake-base-patterns.txt");
  }

  private static String hash(String value) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (int i = 0; i < 10; i++) out.append(String.format("%02x", bytes[i]));
      return out.toString();
    } catch (Exception ignored) { return Integer.toHexString(value.hashCode()); }
  }

  public static synchronized void setScope(String scope) {
    String safe = (scope == null || scope.isBlank()) ? "unknown" : scope;
    if (safe.equals(activeScope) && loaded) return;
    if (loaded) save();
    activeScope = safe;
    activeFile = FabricLoader.getInstance().getConfigDir().resolve("nightfall-fake-base-patterns-" + hash(safe) + ".txt");
    PATTERNS.clear();
    loaded = false;
    load();
  }

  private static synchronized void load() {
    if (loaded) return;
    loaded = true;
    Path path = activeFile;
    if (path == null) {
      activeFile = FabricLoader.getInstance().getConfigDir().resolve("nightfall-fake-base-patterns-" + hash(activeScope) + ".txt");
      path = activeFile;
    }
    // One-time migration: old global patterns are assigned to the first active server only.
    if (!Files.isRegularFile(path) && Files.isRegularFile(legacyFile())) {
      try {
        readPatterns(legacyFile());
        if (save()) {
          Files.move(legacyFile(), legacyFile().resolveSibling("nightfall-fake-base-patterns.migrated.txt"),
              java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          return;
        }
        PATTERNS.clear();
        loaded = false;
      } catch (IOException ignored) { /* Continue with empty scoped memory. */ }
    }
    readPatterns(path);
  }

  private static void readPatterns(Path path) {
    try {
      for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String value = line.trim();
        if (value.isEmpty() || value.startsWith("#")) continue;
        String[] parts = value.split(",");
        if (parts.length != 11 && parts.length != 13) continue;
        try {
          int[] v = new int[13];
          for (int i = 0; i < 11; i++) v[i] = Math.max(0, Math.min(1_000_000, Integer.parseInt(parts[i].trim())));
          if (parts.length == 13) {
            for (int i = 11; i < 13; i++) v[i] = Math.max(-1, Math.min(1_000_000, Integer.parseInt(parts[i].trim())));
          } else { v[11] = -1; v[12] = -1; }
          BaseFingerprint fp = new BaseFingerprint(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12]);
          if (fp.canLearn() && PATTERNS.size() < MAX_PATTERNS) PATTERNS.add(fp);
        } catch (NumberFormatException ignored) { /* Skip malformed row. */ }
      }
    } catch (IOException ignored) { /* No learned profiles; scanning remains available. */ }
  }

  private static synchronized boolean save() {
    if (!loaded || activeFile == null) return true;
    List<String> lines = new ArrayList<>();
    lines.add("# Scoped Nightfall false-positive patterns for scope hash " + activeFile.getFileName());
    lines.add("# 13 numeric fingerprint values; -1 means unknown");
    for (BaseFingerprint fp : PATTERNS) {
      int[] v = fp.values();
      StringBuilder line = new StringBuilder();
      for (int i = 0; i < v.length; i++) { if (i > 0) line.append(','); line.append(v[i]); }
      lines.add(line.toString());
    }
    try {
      Files.createDirectories(activeFile.getParent());
      Files.write(activeFile, lines, StandardCharsets.UTF_8);
      return true;
    } catch (IOException ignored) { return false; }
  }

  public static synchronized int matchPercent(BaseFingerprint fp) {
    load();
    int best = 0;
    for (BaseFingerprint known : PATTERNS) best = Math.max(best, known.similarity(fp));
    return best >= 88 ? best : 0;
  }

  public static synchronized boolean learn(BaseFingerprint fp) {
    load();
    if (fp == null || !fp.canLearn() || PATTERNS.size() >= MAX_PATTERNS) return false;
    for (BaseFingerprint known : PATTERNS) if (known.similarity(fp) >= 92) return false;
    PATTERNS.add(fp);
    if (save()) return true;
    PATTERNS.remove(PATTERNS.size() - 1);
    return false;
  }

  public static synchronized int size() { load(); return PATTERNS.size(); }
  public static synchronized boolean remove(int oneBasedIndex) {
    load();
    if (oneBasedIndex < 1 || oneBasedIndex > PATTERNS.size()) return false;
    BaseFingerprint removed = PATTERNS.remove(oneBasedIndex - 1);
    if (save()) return true;
    PATTERNS.add(oneBasedIndex - 1, removed);
    return false;
  }
  public static synchronized boolean clear() {
    load();
    List<BaseFingerprint> previous = new ArrayList<>(PATTERNS);
    PATTERNS.clear();
    if (save()) return true;
    PATTERNS.addAll(previous);
    return false;
  }
}
