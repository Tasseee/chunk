package nightfall.visuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import nightfall.core.Module;
import nightfall.core.Modules;
public final class Viewmodel {
  // x, y, z, scale, rotX, rotY, rotZ
  static final double[][] P = { null, { 0.12, -0.22, -0.05, 0.85, 25, -12, 18 }, { 0.0, -0.18, 0.0, 0.9, 0, 0, 0 } };
  public static void apply(PoseStack ps) {
    Module m = Modules.VIEW; int p = m.s("Preset").i();
    double[] v = p > 0 ? P[p] : new double[] { m.s("X").d(), m.s("Y").d(), m.s("Z").d(), m.s("Scale").d(), m.s("Rot X").d(), m.s("Rot Y").d(), m.s("Rot Z").d() };
    ps.translate(v[0], v[1], v[2]);
    ps.mulPose(Axis.XP.rotationDegrees((float) v[4])); ps.mulPose(Axis.YP.rotationDegrees((float) v[5])); ps.mulPose(Axis.ZP.rotationDegrees((float) v[6]));
    ps.scale((float) v[3], (float) v[3], (float) v[3]);
  }
}
