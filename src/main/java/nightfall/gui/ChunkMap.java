package nightfall.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import nightfall.analyzer.*;
import org.lwjgl.glfw.GLFW;
/** Chunk Explorer: 2D-Raster mit Zoom, Verschieben, Regionslinien und Detailansicht. Nicht analysierte Chunks bleiben grau. */
public class ChunkMap extends Screen {
  double ox, oz; int cell = 10; int selectedX, selectedZ; boolean selected;
  public ChunkMap() { super(Component.literal("Chunk Explorer")); var p = Minecraft.getInstance().player; ox = p.getBlockX() / 16.0; oz = p.getBlockZ() / 16.0; }
  @Override public void render(GuiGraphics g, int mx, int my, float d) {
    var mc = Minecraft.getInstance(); int x0 = 26, y0 = 38, x1 = width - 244, y1 = height - 42, mxC = (x0 + x1) / 2, myC = (y0 + y1) / 2;
    g.fill(0, 0, width, height, 0xF0100D16); g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, Hud.A);  g.fill(x0, y0, x1, y1, 0xFF0B0A10);
    int shownN = 0; for (Result r : OGBaseFinder.results.values()) if (OGBaseFinder.shown(r)) shownN++;
    String dimension = mc.level == null ? "" : mc.level.dimension().toString();
    int vanishedN = OGBaseFinder.vanishedMarks(dimension).size();
    Hud.grad(g, font, "CHUNK EXPLORER", x0, 12, Hud.A, Hud.B);
    g.drawString(font, "Treffer " + shownN + "  /  Verschwunden " + vanishedN + "  /  Zoom " + cell + "px", x0 + 126, 12, 0xFFB8B2CC, false);
    g.enableScissor(x0, y0, x1, y1);
    int ca = (int) Math.floor(ox - (mxC - x0) / (double) cell) - 1, cb = (int) Math.ceil(ox + (x1 - mxC) / (double) cell) + 1;
    int za = (int) Math.floor(oz - (myC - y0) / (double) cell) - 1, zb = (int) Math.ceil(oz + (y1 - myC) / (double) cell) + 1;
    int gap = cell > 6 ? 1 : 0; Result hov = null; int hx = 0, hz = 0;
    int hovA = (int) Math.floor(ox + (mx - mxC) / (double) cell), hovB = (int) Math.floor(oz + (my - myC) / (double) cell);
    for (int a = ca; a <= cb; a++) for (int b = za; b <= zb; b++) {
      int sx = (int) (mxC + (a - ox) * cell), sz = (int) (myC + (b - oz) * cell);
      Result r = OGBaseFinder.results.get(ChunkPos.asLong(a, b)); int c;
      boolean vanished = mc.level != null && OGBaseFinder.isVanishedMarked(a, b, dimension);
      // A vanished-base marker is historical evidence and remains visible after a re-scan.
      // Normal analyzed chunks stay neutral unless they pass the suspicion filters.
      if (vanished) c = 0xFF38BDF8;
      else if (r != null && OGBaseFinder.shown(r)) c = OGBaseFinder.color(r);
      else if (mc.level != null && mc.level.getChunkSource().getChunkNow(a, b) != null) c = 0xFF2A2735;
      else c = 0xFF14121A;
      g.fill(sx, sz, sx + cell - gap, sz + cell - gap, c);
      if (Math.floorMod(a, 32) == 0) g.fill(sx - 1, sz, sx, sz + cell, 0x66FFFFFF);
      if (Math.floorMod(b, 32) == 0) g.fill(sx, sz - 1, sx + cell, sz, 0x66FFFFFF);
      if (a == hovA && b == hovB) { hov = r; hx = a; hz = b; g.fill(sx, sz, sx + cell, sz + 1, 0xFFFFFFFF); }
    }
    int px = (int) (mxC + (mc.player.getBlockX() / 16.0 - ox) * cell), pz = (int) (myC + (mc.player.getBlockZ() / 16.0 - oz) * cell);
    g.fill(px - 2, pz - 2, px + 3, pz + 3, 0xFFFFFFFF); g.fill(px - 1, pz - 1, px + 2, pz + 2, Hud.A);
    g.disableScissor();
    int sidebarX = x1 + 14, pw = width - sidebarX - 20;
    g.fill(sidebarX, y0, sidebarX + pw, y1, 0xFF14111C);
    g.fill(sidebarX, y0, sidebarX + 2, y1, Hud.A);
    g.drawString(font, "CHUNK DETAILS", sidebarX + 12, y0 + 12, 0xFFDCCBFF, false);
    Result detail = selected ? OGBaseFinder.results.get(ChunkPos.asLong(selectedX, selectedZ)) : null;
    OGBaseFinder.VanishedMark vanishedDetail = selected && mc.level != null
        ? OGBaseFinder.vanishedMark(selectedX, selectedZ, dimension) : null;
    int dy = y0 + 34;
    if (!selected) {
      g.drawString(font, "Chunk auf der Karte", sidebarX + 12, dy, 0xFFAAA2BB, false);
      g.drawString(font, "anklicken fuer Details", sidebarX + 12, dy + 12, 0xFF777087, false);
    } else {
      String[] lines = { "Chunk " + selectedX + ", " + selectedZ, "Block " + (selectedX * 16) + ", " + (selectedZ * 16),
        detail == null ? "Noch nicht analysiert" : "Score: " + detail.score() + " / 100",
        detail == null ? "Keine Daten" : "Konfidenz: " + detail.conf() + "%",
        detail == null ? "" : "Beweise: " + detail.evidence(),
        detail == null ? "" : "Abdeckung: " + detail.coverage() + "%" };
      for (String line : lines) { g.drawString(font, line, sidebarX + 12, dy, 0xFFE9E4F5, false); dy += 13; }
      if (detail != null) {
        int color = OGBaseFinder.color(detail); g.fill(sidebarX + 12, dy + 2, sidebarX + pw - 12, dy + 6, color); dy += 16;
        g.drawString(font, "ERKANNTE HINWEISE", sidebarX + 12, dy, 0xFFDCCBFF, false); dy += 14;
        for (String why : detail.why()) { if (dy > y1 - 20) break; g.drawString(font, why, sidebarX + 12, dy, 0xFFAAA2BB, false); dy += 12; }
      }
      if (vanishedDetail != null) {
        if (dy > y1 - 58) dy = y1 - 58;
        g.fill(sidebarX + 10, dy, sidebarX + pw - 10, dy + 1, 0xFF38BDF8); dy += 8;
        g.drawString(font, "AUTOMATISCH MARKIERT", sidebarX + 12, dy, 0xFF38BDF8, false); dy += 13;
        g.drawString(font, "Base-Verdacht nach Abstieg verschwunden", sidebarX + 12, dy, 0xFFE9E4F5, false); dy += 13;
        g.drawString(font, "Score vorher: " + vanishedDetail.previousScore(), sidebarX + 12, dy, 0xFFAAA2BB, false); dy += 12;
        g.drawString(font, "Y " + vanishedDetail.previousY() + " -> " + vanishedDetail.currentY(), sidebarX + 12, dy, 0xFFAAA2BB, false);
      }
    }
    String[] lg = { "Unbekannt", "Niedrig", "Mittel", "Hoch", "Sehr hoch", "Decoy", "Verschwunden" }; int[] lc = { 0xFF2A2735, 0xFF22C55E, 0xFFEAB308, 0xFFF97316, 0xFFEF4444, 0xFF6B6B7B, 0xFF38BDF8 }; int lx = x0;
    for (int i = 0; i < lg.length; i++) { g.fill(lx, y1 + 10, lx + 8, y1 + 18, lc[i]); g.drawString(font, lg[i], lx + 12, y1 + 10, 0xFFB8B2CC, false); lx += font.width(lg[i]) + 28; }
    g.drawString(font, "Ziehen: Karte  |  Mausrad: Zoom  |  Linksklick: Auswahl  |  Rechtsklick: Koordinaten kopieren  |  R: zentrieren", x0, y1 + 24, 0xFF6F6A80, false);
    if (mx >= x0 && mx < x1 && my >= y0 && my < y1) {
      java.util.List<String> t = new java.util.ArrayList<>(); t.add("Chunk " + hx + ", " + hz + "  (Block " + hx * 16 + ", " + hz * 16 + ")");
      if (hov == null) t.add("Nicht analysiert (keine Daten)");
      else { t.add("Score " + hov.score() + " | Konfidenz " + hov.conf() + "% | Beweise " + hov.evidence() + " | Abdeckung " + hov.coverage() + "%"); if (hov.decoy()) t.add("Als DECOY eingestuft"); t.addAll(hov.why()); }
      OGBaseFinder.VanishedMark hoverMark = mc.level == null ? null : OGBaseFinder.vanishedMark(hx, hz, dimension);
      if (hoverMark != null) {
        t.add("AUTOMATISCH MARKIERT: Base-Verdacht verschwand nach Abstieg");
        t.add("Vorher Score " + hoverMark.previousScore() + " | Y " + hoverMark.previousY() + " -> " + hoverMark.currentY());
      }
      int w = 0; for (String s : t) w = Math.max(w, font.width(s)); int tx = Math.min(mx + 12, width - w - 12), ty = Math.min(my + 12, height - t.size() * 11 - 8);
      g.fill(tx - 4, ty - 4, tx + w + 4, ty + t.size() * 11 + 2, 0xEE000000);
      for (int i = 0; i < t.size(); i++) g.drawString(font, t.get(i), tx, ty + i * 11, i == 0 ? Hud.A : 0xFFE9E4F5, false);
    }
  }
  @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { ox -= dx / cell; oz -= dy / cell; return true; }
  @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { cell = Math.max(3, Math.min(40, cell + (int) Math.signum(sy) * 2)); return true; }
  @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
    int x0 = 26, y0 = 38, x1 = width - 244, y1 = height - 42, mxC = (x0 + x1) / 2, myC = (y0 + y1) / 2;
    if (e.x() >= x0 && e.x() < x1 && e.y() >= y0 && e.y() < y1) {
      int a = (int) Math.floor(ox + (e.x() - mxC) / (double) cell), b = (int) Math.floor(oz + (e.y() - myC) / (double) cell);
      if (e.button() == 1) { Minecraft.getInstance().keyboardHandler.setClipboard((a * 16) + " " + (b * 16)); return true; }
      if (e.button() == 0) { selectedX = a; selectedZ = b; selected = true; return true; }
    }
    return super.mouseClicked(e, dbl);
  }
  @Override public boolean keyPressed(KeyEvent e) {
    if (e.key() == GLFW.GLFW_KEY_R) { var p = Minecraft.getInstance().player; ox = p.getBlockX() / 16.0; oz = p.getBlockZ() / 16.0; selected = false; return true; }
    return super.keyPressed(e);
  }
  @Override public boolean isPauseScreen() { return false; }
}
