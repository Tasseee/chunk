package nightfall.gui;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import nightfall.core.Modules;

/** Small, bounded toast queue shared by the Nightfall modules. */
public final class NoticeManager {
  private record Notice(String title, String detail, int color, long expires) {}
  private static final Deque<Notice> QUEUE = new ArrayDeque<>();
  private static final Map<String, Long> LAST = new HashMap<>();
  private NoticeManager() {}

  public static void push(String title, String detail, int color) {
    if (Modules.NOTIFIER == null || !Modules.NOTIFIER.enabled || !Modules.NOTIFIER.on("Enabled")) return;
    long now = System.currentTimeMillis();
    int duration = Modules.NOTIFIER.s("Duration").i() * 1000;
    String key = title + "|" + detail;
    synchronized (QUEUE) {
      long cooldown = Modules.NOTIFIER.s("Cooldown").i() * 1000L;
      if (now - LAST.getOrDefault(key, 0L) < cooldown) return;
      LAST.put(key, now);
      if (LAST.size() > 512) LAST.clear();
      QUEUE.addFirst(new Notice(title, detail, color, now + duration));
      while (QUEUE.size() > 5) QUEUE.removeLast();
    }
    if (Modules.NOTIFIER.on("Sound")) nightfall.NightfallClient.click(true);
  }

  public static void render(GuiGraphics g, Minecraft mc) {
    if (Modules.NOTIFIER == null || !Modules.NOTIFIER.enabled || !Modules.NOTIFIER.on("Enabled")) return;
    long now = System.currentTimeMillis();
    synchronized (QUEUE) {
      QUEUE.removeIf(n -> n.expires < now);
      int y = 8, width = mc.getWindow().getGuiScaledWidth();
      for (Notice n : QUEUE) {
        int w = Math.min(230, Math.max(145, mc.font.width(n.title) + mc.font.width(n.detail) + 28));
        int x = width - w - 8;
        g.fill(x, y, x + w, y + 31, 0xE9110E18);
        g.fill(x, y, x + 3, y + 31, n.color);
        g.fill(x + 7, y + 5, x + w - 6, y + 6, 0x663F3650);
        g.drawString(mc.font, n.title, x + 9, y + 8, 0xFFF3ECFF, false);
        g.drawString(mc.font, n.detail, x + 9, y + 19, 0xFFB8B2CC, false);
        y += 36;
      }
    }
  }
}
