package nightfall.core;
import java.util.*;
import nightfall.NightfallClient;
public abstract class Module {
  public final String name, desc; public final Category cat;
  public boolean enabled, hold, open; public int key;
  public final List<Setting> settings = new ArrayList<>();
  public Module(String name, String desc, Category cat, int key) { this.name = name; this.desc = desc; this.cat = cat; this.key = key; }
  public Setting s(String n) { for (Setting x : settings) if (x.name.equals(n)) return x; throw new IllegalArgumentException(n); }
  public boolean on(String n) { return s(n).b(); }
  public void toggle() { setEnabled(!enabled); }
  public void setEnabled(boolean e) {
    if (e == enabled) return;
    enabled = e; NightfallClient.click(e);
    if (e) onEnable(); else onDisable();
  }
  public void onEnable() {}
  public void onDisable() {}
}
