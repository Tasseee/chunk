package nightfall.core;
public final class Setting {
  public enum T { BOOL, NUM, ENUM, COLOR }
  public final String name; public final T t; public double val, min, max, step = 1; public String[] opts;
  private Setting(String n, T t) { name = n; this.t = t; }
  public static Setting bool(String n, boolean d) { var s = new Setting(n, T.BOOL); s.val = d ? 1 : 0; return s; }
  public static Setting num(String n, double d, double min, double max, double step) { var s = new Setting(n, T.NUM); s.val = d; s.min = min; s.max = max; s.step = step; return s; }
  public static Setting mode(String n, int d, String... o) { var s = new Setting(n, T.ENUM); s.val = d; s.opts = o; return s; }
  public static Setting color(String n, double hue) { var s = new Setting(n, T.COLOR); s.val = hue; s.min = 0; s.max = 1; return s; }
  public boolean b() { return val > 0.5; } public double d() { return val; } public int i() { return (int) Math.round(val); }
  public int rgb() { return 0xFF000000 | (java.awt.Color.HSBtoRGB((float) val, 0.75f, 1f) & 0xFFFFFF); }
  public void cycle() { val = (i() + 1) % opts.length; }
  public String text() {
    return switch (t) { case NUM -> step >= 1 ? String.format("%.0f", val) : String.format("%.2f", val); case ENUM -> opts[i()]; default -> ""; };
  }
}
