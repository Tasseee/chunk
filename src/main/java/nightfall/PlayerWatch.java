package nightfall;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import nightfall.core.Modules;
import nightfall.gui.NoticeManager;

/** Client-side staff watch. Names are manually provided; online status is based on server tab data. */
public final class PlayerWatch {
  private static final Set<String> STAFF_NAMES = new HashSet<>();
  private static final Set<UUID> STAFF_SEEN = new HashSet<>();
  private static final Set<UUID> WORLD_ONLY_SEEN = new HashSet<>();
  private static boolean loaded;
  private static int lastTick = Integer.MIN_VALUE;
  private static Object lastConnection;
  private static FileTime lastFileTime;
  private static String lastFileContent;
  private static final java.util.Map<UUID, Integer> missingTabSamples = new java.util.HashMap<>();
  private PlayerWatch() {}

  private static Path staffFile() {
    return FabricLoader.getInstance().getConfigDir().resolve("nightfall-staff.txt");
  }

  private static String normalize(String raw) {
    if (raw == null) return "";
    String name = raw.strip();
    while (name.startsWith("@")) name = name.substring(1);
    return name.matches("[A-Za-z0-9_]{1,16}") ? name.toLowerCase(Locale.ROOT) : "";
  }

  /** Supports one-per-line, comma-separated and semicolon-separated files. */
  private static synchronized void loadNames(boolean force) {
    Path file = staffFile();
    try {
      Files.createDirectories(file.getParent());
      if (!Files.exists(file)) Files.writeString(file,
          "# Nightfall Staff List\n# Exact Minecraft usernames; one per line or comma/semicolon separated.\n",
          StandardCharsets.UTF_8);
      FileTime stamp = Files.getLastModifiedTime(file);
      String content = Files.readString(file, StandardCharsets.UTF_8);
      if (loaded && !force && stamp.equals(lastFileTime) && content.equals(lastFileContent)) return;
      Set<String> loadedNames = new HashSet<>();
      for (String line : content.split("\\R")) {
        String value = line.strip();
        if (value.isEmpty() || value.startsWith("#")) continue;
        for (String token : value.split("[,;\\s]+")) {
          String normalized = normalize(token);
          if (!normalized.isEmpty()) loadedNames.add(normalized);
        }
      }
      STAFF_NAMES.clear();
      STAFF_NAMES.addAll(loadedNames);
      lastFileTime = stamp;
      lastFileContent = content;
      loaded = true;
    } catch (Exception e) {
      System.err.println("[Nightfall] staff list read failed: " + e);
      loaded = true;
    }
  }

  private static synchronized boolean saveNames() {
    Path file = staffFile();
    try {
      Files.createDirectories(file.getParent());
      List<String> lines = new ArrayList<>();
      lines.add("# Nightfall Staff List");
      lines.add("# Exact Minecraft usernames, one per line. /nfstaff add|remove|list|status|debug|reload");
      STAFF_NAMES.stream().sorted(Comparator.naturalOrder()).forEach(lines::add);
      Files.write(file, lines, StandardCharsets.UTF_8);
      lastFileTime = Files.getLastModifiedTime(file);
      lastFileContent = String.join(System.lineSeparator(), lines);
      loaded = true;
      return true;
    } catch (Exception e) {
      System.err.println("[Nightfall] staff list write failed: " + e);
      return false;
    }
  }

  public static synchronized boolean addStaffName(String rawName) {
    String key = normalize(rawName);
    if (key.isEmpty()) return false;
    loadNames(false);
    if (!STAFF_NAMES.add(key)) return false;
    if (!saveNames()) { STAFF_NAMES.remove(key); return false; }
    STAFF_SEEN.clear();
    return true;
  }

  public static synchronized boolean removeStaffName(String rawName) {
    String key = normalize(rawName);
    if (key.isEmpty()) return false;
    loadNames(false);
    if (!STAFF_NAMES.remove(key)) return false;
    if (!saveNames()) { STAFF_NAMES.add(key); return false; }
    STAFF_SEEN.clear();
    return true;
  }

  public static synchronized void reloadStaffNames() {
    loaded = false;
    STAFF_NAMES.clear();
    loadNames(true);
    STAFF_SEEN.clear();
  }

  public static synchronized List<String> staffNames() {
    loadNames(false);
    return STAFF_NAMES.stream().sorted(Comparator.naturalOrder()).toList();
  }

  private static void resetSession(Object connection) {
    lastConnection = connection;
    lastTick = Integer.MIN_VALUE;
    STAFF_SEEN.clear();
    WORLD_ONLY_SEEN.clear();
    missingTabSamples.clear();
  }

  public static void tick(Minecraft mc) {
    if (mc.player == null || mc.level == null || mc.getConnection() == null) {
      if (lastConnection != null) resetSession(null);
      return;
    }
    Object connection = mc.getConnection();
    if (connection != lastConnection) resetSession(connection);
    int tick = mc.player.tickCount;
    if (tick == lastTick) return;
    lastTick = tick;
    if (tick % 20 != 0) return;

    loadNames(false); // also picks up edits made directly in nightfall-staff.txt
    Set<UUID> tab = new HashSet<>();
    for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
      var profile = info.getProfile();
      UUID id = profile.id();
      tab.add(id);
      String name = profile.name();
      if (Modules.STAFF.enabled && STAFF_NAMES.contains(normalize(name)) && STAFF_SEEN.add(id)) {
        if (Modules.STAFF.on("Notify"))
          NoticeManager.push("Staff online", name + " (manuelle Liste)", 0xFFA855F7);
      }
    }

    // AntiVanish is deliberately conservative: a player must be visible in-world
    // but absent from the server tab list for multiple consecutive samples.
    if (!Modules.ANTIVANISH.enabled || !Modules.ANTIVANISH.on("Notify")) {
      missingTabSamples.clear();
      WORLD_ONLY_SEEN.clear();
    } else {
      for (Player p : mc.level.players()) {
        double range = Modules.ANTIVANISH.s("Range").d();
        if (p == mc.player || p.distanceToSqr(mc.player) > range * range) continue;
        UUID id = p.getUUID();
        if (tab.contains(id)) { missingTabSamples.remove(id); continue; }
        int samples = missingTabSamples.merge(id, 1, Integer::sum);
        int requiredSamples = Math.max(2, Math.min(8, Modules.ANTIVANISH.s("Missing Tab Samples").i()));
        if (samples >= requiredSamples && WORLD_ONLY_SEEN.add(id)) {
          String detail = p.getName().getString() + " ist sichtbar, fehlt aber wiederholt in Tab. Kein sicherer Vanish-Nachweis.";
          NoticeManager.push("Tab/World-Abweichung", detail, 0xFFEAB308);
          mc.player.displayClientMessage(Component.literal("[Nightfall] " + detail), false);
        }
      }
    }
    STAFF_SEEN.retainAll(tab);
    WORLD_ONLY_SEEN.removeIf(id -> mc.level.players().stream().noneMatch(p -> p.getUUID().equals(id)));
    missingTabSamples.keySet().removeIf(id -> mc.level.players().stream().noneMatch(p -> p.getUUID().equals(id)) || tab.contains(id));
  }

  public static List<String> knownStaffOnlineNames(Minecraft mc) {
    if (!Modules.STAFF.enabled || mc.getConnection() == null || !Modules.STAFF.on("Show Known Staff")) return List.of();
    // File timestamp is polled in tick() at most once per second, not on every HUD frame.
    if (!loaded) loadNames(false);
    List<String> names = new ArrayList<>();
    for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
      String name = info.getProfile().name();
      if (STAFF_NAMES.contains(normalize(name))) names.add(name);
    }
    names.sort(String.CASE_INSENSITIVE_ORDER);
    return List.copyOf(names);
  }

  public static int antiVanishCandidateCount() {
    return missingTabSamples.size();
  }

  public static List<String> antiVanishCandidates(Minecraft mc) {
    if (mc == null || mc.level == null) return List.of();
    List<String> names = new ArrayList<>();
    for (Player player : mc.level.players()) {
      Integer samples = missingTabSamples.get(player.getUUID());
      if (samples != null && samples > 0) names.add(player.getName().getString() + " (" + samples + ")");
    }
    names.sort(String.CASE_INSENSITIVE_ORDER);
    return List.copyOf(names);
  }

  public static List<String> tabNames(Minecraft mc) {
    if (mc.getConnection() == null) return List.of();
    List<String> names = new ArrayList<>();
    for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) names.add(info.getProfile().name());
    names.sort(String.CASE_INSENSITIVE_ORDER);
    return List.copyOf(names);
  }
}
