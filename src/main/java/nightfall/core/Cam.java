package nightfall.core;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
/** Gemeinsamer Kamera-Zustand fuer Freelook und Freecam. */
public final class Cam {
  public static boolean freelook, freecam;
  public static float yaw, pitch; public static double x, y, z, speed = 12.0;
  private static long last;
  public static boolean active() { return freelook || freecam; }
  public static void start() {
    var p = Minecraft.getInstance().player; if (p == null) return;
    yaw = p.getYRot(); pitch = p.getXRot();
    var e = p.getEyePosition(); x = e.x; y = e.y; z = e.z; last = System.nanoTime();
  }
  private static boolean down(long w, int k) { return GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS; }
  /** Wird pro gerendertem Frame aufgerufen, damit Freecam fluessig ist. */
  public static void frame() {
    long now = System.nanoTime(); double dt = Math.min(0.1, (now - last) / 1e9); last = now;
    var mc = Minecraft.getInstance();
    if (!freecam || mc.screen != null) return;
    long w = mc.getWindow().handle();
    double f = (down(w, 87) ? 1 : 0) - (down(w, 83) ? 1 : 0), s = (down(w, 68) ? 1 : 0) - (down(w, 65) ? 1 : 0),
           u = (down(w, 32) ? 1 : 0) - (down(w, 340) ? 1 : 0);
    double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch), v = speed * dt;
    x += (f * -Math.sin(yr) * Math.cos(pr) + s * -Math.cos(yr)) * v;
    y += (f * -Math.sin(pr) + u) * v;
    z += (f * Math.cos(yr) * Math.cos(pr) + s * -Math.sin(yr)) * v;
  }
}
