package nightfall.gui;
import java.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import nightfall.core.*;
import nightfall.core.Module;
public final class Hud {
  public static final int A = 0xFFA855F7, B = 0xFF6D28D9;
  public static int lerp(int a, int b, float t) {
    int r = 0; for (int s = 24; s >= 0; s -= 8) { int x = (a >> s) & 255, y = (b >> s) & 255; r |= ((int) (x + (y - x) * t) & 255) << s; } return r;
  }
  public static void grad(GuiGraphics g, Font f, String s, int x, int y, int c1, int c2) {
    for (int i = 0; i < s.length(); i++) { String ch = s.substring(i, i + 1);
      g.drawString(f, ch, x, y, lerp(c1, c2, i / (float) Math.max(1, s.length() - 1)), true); x += f.width(ch); }
  }
  public static void render(GuiGraphics g, DeltaTracker dt) {
    var mc = Minecraft.getInstance();
    if (mc.options.hideGui || mc.player == null) return;
    boolean hudEnabled = Modules.HUD != null && Modules.HUD.enabled;
    boolean staffPanelEnabled = Modules.STAFF != null && Modules.STAFF.enabled && Modules.STAFF.on("Show Known Staff");
    // Staff List is an independent HUD panel and must work even when the main HUD is disabled.
    if (!hudEnabled && !staffPanelEnabled) return;
    Font f = mc.font;
    boolean privacy = Modules.OPSEC != null && Modules.OPSEC.enabled;
    boolean hideFps = privacy && Modules.OPSEC.on("Hide FPS");
    boolean hideCoords = privacy && Modules.OPSEC.on("Hide Coordinates");
    boolean hideModuleList = privacy && Modules.OPSEC.on("Hide Module List");

    if (hudEnabled) {
      // Watermark
      String info = hideFps ? "" : " | " + mc.getFps() + " FPS";
      int w = f.width("NIGHTFALL" + info) + 12, sw = mc.getWindow().getGuiScaledWidth();
      int wx = sw - w - 6;
      g.fill(wx, 4, wx + w, 20, 0xA0100C18);
      for (int x = 0; x < w; x++) g.fill(wx + x, 4, wx + x + 1, 6, lerp(A, B, x / (float) w));
      grad(g, f, "NIGHTFALL", wx + 6, 9, A, B);
      g.drawString(f, info, wx + 6 + f.width("NIGHTFALL"), 9, 0xFFB8B2CC, true);

      // Coordinates
      if (!hideCoords) {
        String c = String.format("XYZ  %.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int cx = sw - f.width(c) - 14;
        g.fill(cx - 5, mc.getWindow().getGuiScaledHeight() - 18, cx + f.width(c) + 5,
            mc.getWindow().getGuiScaledHeight() - 4, 0xA0100C18);
        g.drawString(f, c, cx, mc.getWindow().getGuiScaledHeight() - 14, 0xFFE9E4F5, true);
      }

      // Module list
      int y = 4, i = 0;
      if (!hideModuleList) {
        List<Module> on = new ArrayList<>();
        for (Module m : Modules.ALL) if (m.enabled && m != Modules.HUD && m != Modules.GUI) on.add(m);
        on.sort(Comparator.comparingInt((Module m) -> f.width(m.name)).reversed());
        for (Module m : on) {
          int tw = f.width(m.name) + 8;
          g.fill(sw - tw - 6, y, sw - 2, y + 12, 0xA0100C18);
          g.fill(sw - 2, y, sw, y + 12, lerp(A, B, i / (float) Math.max(1, on.size())));
          g.drawString(f, m.name, sw - tw - 2, y + 2, lerp(A, 0xFFE9D5FF, 0.4f), true);
          y += 12; i++;
        }
      }
    }

    if (staffPanelEnabled) {
      List<String> staff = nightfall.PlayerWatch.knownStaffOnlineNames(mc);
      String emptyHint = "Keine Treffer | /nfstaff debug";
      int rows = staff.isEmpty() ? 1 : Math.min(5, staff.size());
      int tw = Math.max(150, f.width(emptyHint) + 14), tx = 6, ty = 26, sh = 22 + rows * 11;
      g.fill(tx, ty, tx + tw, ty + sh, 0xA0100C18);
      g.fill(tx, ty, tx + 2, ty + sh, A);
      g.drawString(f, "STAFF LIST  " + staff.size() + " ONLINE", tx + 7, ty + 4, 0xFFDCCBFF, true);
      int sy = ty + 16;
      if (staff.isEmpty()) g.drawString(f, emptyHint, tx + 7, sy, 0xFFB8B2CC, false);
      else for (int n = 0; n < Math.min(5, staff.size()); n++) {
        g.drawString(f, staff.get(n), tx + 7, sy, 0xFFB8B2CC, false);
        sy += 11;
      }
    }
    if (hudEnabled) NoticeManager.render(g, mc);
  }
}
