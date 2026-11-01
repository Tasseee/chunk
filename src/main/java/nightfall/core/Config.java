package nightfall.core;
import com.google.gson.*;
import java.nio.file.*; import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import nightfall.NightfallClient;
public final class Config {
  static class E { int key; boolean hold, enabled; Map<String, Double> s = new LinkedHashMap<>(); }
  static class F { boolean sound = true; Map<String, E> mods = new LinkedHashMap<>(); }
  private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
  private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("nightfall.json"); }
  public static void save() {
    try {
      F f = new F(); f.sound = NightfallClient.sound;
      for (Module m : Modules.ALL) { E e = new E(); e.key = m.key; e.hold = m.hold; e.enabled = m.enabled;
        for (Setting s : m.settings) e.s.put(s.name, s.val); f.mods.put(m.name, e); }
      Files.writeString(file(), G.toJson(f));
    } catch (Exception ex) { System.err.println("[Nightfall] save failed: " + ex); }
  }
  public static void load() {
    try {
      if (!Files.exists(file())) return;
      F f = G.fromJson(Files.readString(file()), F.class); if (f == null) return;
      NightfallClient.sound = f.sound;
      for (Module m : Modules.ALL) {
        E e = f.mods.get(m.name);
        // Migrate settings from the previous internal display name.
        boolean legacyBlockEsp = e == null && m == Modules.BLOCK_ESP;
        if (legacyBlockEsp) e = f.mods.get("Block ESP (Base Finder)");
        if (e == null) continue;
        m.key = e.key; m.hold = e.hold;
        // Restore only safe presentation/diagnostic modules; movement and UI-opening
        // modules deliberately remain off after restart to avoid surprising actions.
        if (m == Modules.HUD || m == Modules.STAFF || m == Modules.ANTIVANISH || m == Modules.DARK_STEEL_SKY)
          m.enabled = e.enabled;
        for (Setting s : m.settings) { Double v = e.s == null ? null : e.s.get(s.name); if (v != null) s.val = v; }
        // The previous build forced a minimap onto the HUD. Keep it hidden after upgrade.
        if (legacyBlockEsp && m == Modules.BLOCK_ESP) m.s("Mini-map").val = 0;
      }
    } catch (Exception ex) { System.err.println("[Nightfall] load failed: " + ex); }
  }
}
