package nightfall.gui;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import nightfall.NightfallClient;
import nightfall.core.*;
import nightfall.core.Module;
import org.lwjgl.glfw.GLFW;
public class ClickGui extends Screen {
  static final int W = 620, H = 380, SB = 146, ACC = Hud.A;
  static Category sel = Category.VISUALS; int scroll;
  Module listening; Setting drag; final Map<Module, Float> anim = new HashMap<>();
  public ClickGui() { super(Component.literal("Nightfall")); }
  int x0() { return (width - W) / 2; } int y0() { return (height - H) / 2; }
  int cx() { return x0() + SB + 16; } int cw() { return W - SB - 32; }
  int kx() { return cx() + cw() - 12 - 30 - 8 - 76; } int sx() { return cx() + cw() - 12 - 30; } int slx() { return cx() + cw() - 12 - 150; }
  List<Module> mods() { return Modules.ALL.stream().filter(m -> m.cat == sel).toList(); }
  int cardH(Module m) { return 38 + (m.open ? m.settings.size() * 22 + 6 : 0); }
  int cardY(List<Module> l, int idx) { int y = y0() + 44 - scroll; for (int i = 0; i < idx; i++) y += cardH(l.get(i)) + 6; return y; }
  int total(List<Module> l) { int t = 0; for (Module m : l) t += cardH(m) + 6; return t; }
  static String keyName(int k) {
    if (k < 0) return "None"; if (k == 344) return "R-Shift"; if (k == 340) return "L-Shift";
    String n = GLFW.glfwGetKeyName(k, 0); return n != null ? n.toUpperCase() : "Key " + k;
  }
  void upd(double mx) {
    double t = Math.max(0, Math.min(1, (mx - slx()) / 150.0));
    if (drag.t == Setting.T.COLOR) drag.val = t;
    else { double v = drag.min + t * (drag.max - drag.min); drag.val = Math.max(drag.min, Math.min(drag.max, Math.round(v / drag.step) * drag.step)); }
  }
  @Override public void render(GuiGraphics g, int mx, int my, float d) {
    int x = x0(), y = y0();
    g.fill(0, 0, width, height, 0x90000000);
    g.fill(x, y, x + W, y + H, 0xF0121018); g.fill(x, y, x + SB, y + H, 0xFF0B0A10); g.fill(x, y, x + W, y + 2, ACC);
    Hud.grad(g, font, "NIGHTFALL", x + 14, y + 13, Hud.A, Hud.B);
    g.drawString(font, "CLIENT CONFIGURATION", x + 14, y + 25, 0xFF746B87, false);
    g.fill(x + SB + 1, y + 34, x + W - 12, y + 35, 0x443F3650);
    Category[] cs = Category.values();
    for (int i = 0; i < cs.length; i++) {
      int ty = y + 44 + i * 25; boolean s = cs[i] == sel, hov = mx >= x && mx < x + SB && my >= ty && my < ty + 22;
      if (s || hov) g.fill(x + 6, ty, x + SB - 6, ty + 22, s ? 0xFF241B3A : 0xFF171421);
      if (s) g.fill(x + 6, ty, x + 8, ty + 22, ACC);
      g.drawString(font, cs[i].label, x + 16, ty + 7, s ? 0xFFFFFFFF : 0xFF9A94AD, false);
    }
    var list = mods();
    g.drawString(font, sel.label.toUpperCase(Locale.ROOT), cx(), y + 13, 0xFFDCCBFF, false);
    g.drawString(font, list.size() + " MODULES", cx() + cw() - 76, y + 13, 0xFF746B87, false);
    if (list.isEmpty()) g.drawString(font, "Keine Module in dieser Kategorie", cx(), y + 42, 0xFF6F6A80, false);
    g.enableScissor(x + SB, y + 6, x + W, y + H - 22);
    for (int i = 0; i < list.size(); i++) {
      Module m = list.get(i); int cy = cardY(list, i), h = cardH(m);
      boolean hov = mx >= cx() && mx < cx() + cw() && my >= cy && my < cy + 38;
      g.fill(cx(), cy, cx() + cw(), cy + h, hov ? 0xFF211D2E : 0xFF1A1724);
      if (m.enabled) g.fill(cx(), cy, cx() + 2, cy + h, ACC);
      g.drawString(font, m.name, cx() + 12, cy + 8, 0xFFFFFFFF, false);
      g.drawString(font, m.desc, cx() + 12, cy + 21, 0xFF8C86A0, false);
      if (!m.settings.isEmpty()) g.drawString(font, m.open ? "v" : ">", kx() - 18, cy + 15, 0xFFB8B2CC, false);
      String kl = listening == m ? "..." : keyName(m.key) + (m.key >= 0 && m.hold ? " (Hold)" : "");
      g.fill(kx(), cy + 9, kx() + 76, cy + 29, 0xFF0F0D16);
      g.drawCenteredString(font, kl, kx() + 38, cy + 15, listening == m ? ACC : 0xFFB8B2CC);
      float a = anim.getOrDefault(m, m.enabled ? 1f : 0f); a += ((m.enabled ? 1f : 0f) - a) * 0.3f; anim.put(m, a);
      g.fill(sx(), cy + 12, sx() + 30, cy + 26, Hud.lerp(0xFF332D44, ACC, a));
      g.fill(sx() + 2 + (int) (16 * a), cy + 14, sx() + 14 + (int) (16 * a), cy + 24, 0xFFFFFFFF);
      if (!m.open) continue;
      for (int j = 0; j < m.settings.size(); j++) {
        Setting s = m.settings.get(j); int ry = cy + 42 + j * 22;
        g.drawString(font, s.name, cx() + 12, ry + 5, 0xFFB8B2CC, false);
        switch (s.t) {
          case BOOL -> { g.fill(sx(), ry + 3, sx() + 30, ry + 17, s.b() ? ACC : 0xFF332D44); g.fill(sx() + (s.b() ? 16 : 2), ry + 5, sx() + (s.b() ? 28 : 14), ry + 15, 0xFFFFFFFF); }
          case ENUM -> { g.fill(slx() + 50, ry + 2, slx() + 150, ry + 18, 0xFF0F0D16); g.drawCenteredString(font, s.text(), slx() + 100, ry + 6, 0xFFFFFFFF); }
          case NUM -> { double fr = (s.val - s.min) / (s.max - s.min);
            g.fill(slx(), ry + 8, slx() + 150, ry + 12, 0xFF332D44); g.fill(slx(), ry + 8, slx() + (int) (150 * fr), ry + 12, ACC);
            g.fill(slx() + (int) (150 * fr) - 2, ry + 5, slx() + (int) (150 * fr) + 2, ry + 15, 0xFFFFFFFF);
            g.drawString(font, s.text(), slx() - font.width(s.text()) - 8, ry + 5, 0xFFFFFFFF, false); }
          case COLOR -> { for (int k = 0; k < 30; k++) g.fill(slx() + k * 5, ry + 7, slx() + k * 5 + 5, ry + 13, 0xFF000000 | (java.awt.Color.HSBtoRGB(k / 30f, 0.75f, 1f) & 0xFFFFFF));
            g.fill(slx() + (int) (150 * s.val) - 2, ry + 4, slx() + (int) (150 * s.val) + 2, ry + 16, 0xFFFFFFFF);
            g.fill(slx() - 24, ry + 3, slx() - 8, ry + 17, s.rgb()); }
        }
      }
    }
    g.disableScissor();
    g.drawString(font, "Klick: an/aus | > Einstellungen | Keybind-Feld: Taste, Rechtsklick Hold/Toggle", x + SB + 16, y + H - 14, 0xFF6F6A80, false);
  }
  @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
    double mx = e.x(), my = e.y(); int b = e.button(), x = x0(), y = y0();
    Category[] cs = Category.values();
    for (int i = 0; i < cs.length; i++) { int ty = y + 44 + i * 25;
      if (mx >= x && mx < x + SB && my >= ty && my < ty + 22) { sel = cs[i]; scroll = 0; NightfallClient.click(true); return true; } }
    if (mx < cx() || mx >= cx() + cw() || my < y + 6 || my >= y + H - 22) return super.mouseClicked(e, dbl);
    var list = mods();
    for (int i = 0; i < list.size(); i++) {
      Module m = list.get(i); int cy = cardY(list, i);
      if (my < cy || my >= cy + cardH(m)) continue;
      if (my < cy + 38) {
        if (mx >= kx() && mx < kx() + 76) { if (b == 1) { m.hold = !m.hold; Config.save(); } else listening = m; }
        else if (mx >= kx() - 28 && mx < kx() - 6 && !m.settings.isEmpty()) m.open = !m.open;
        else if (b == 0 && m != Modules.GUI) { m.toggle(); Config.save(); }
      } else {
        int j = (int) ((my - cy - 42) / 22);
        if (j >= 0 && j < m.settings.size()) { Setting s = m.settings.get(j);
          switch (s.t) { case BOOL -> s.val = s.b() ? 0 : 1; case ENUM -> s.cycle(); default -> { drag = s; upd(mx); } }
          if (m == Modules.FREECAM) Cam.speed = m.s("Speed").d();
          Config.save(); }
      }
      return true;
    }
    return super.mouseClicked(e, dbl);
  }
  @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
    if (drag != null) { upd(e.x()); Modules.FREECAM.s("Speed"); Cam.speed = Modules.FREECAM.s("Speed").d(); return true; }
    return super.mouseDragged(e, dx, dy);
  }
  @Override public boolean mouseReleased(MouseButtonEvent e) { if (drag != null) { drag = null; Config.save(); } return super.mouseReleased(e); }
  @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    scroll = Math.max(0, Math.min(Math.max(0, total(mods()) - (H - 74)), scroll - (int) (sy * 24))); return true;
  }
  @Override public boolean keyPressed(KeyEvent e) {
    int k = e.key();
    if (listening != null) { listening.key = k == GLFW.GLFW_KEY_ESCAPE ? -1 : k; listening = null; Config.save(); return true; }
    if (k == 344) { onClose(); return true; }
    return super.keyPressed(e);
  }
  @Override public void onClose() { Config.save(); super.onClose(); }
  @Override public boolean isPauseScreen() { return false; }
}
