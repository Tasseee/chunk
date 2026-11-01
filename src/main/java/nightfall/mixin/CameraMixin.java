package nightfall.mixin;
import net.minecraft.client.Camera;
import nightfall.core.Cam;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class CameraMixin {
  @Shadow protected abstract void setRotation(float yRot, float xRot);
  @Shadow protected abstract void setPosition(double x, double y, double z);
  @Inject(method = "setup", at = @At("TAIL"))
  private void nf$setup(CallbackInfo ci) {
    if (!Cam.active()) return;
    Cam.frame(); setRotation(Cam.yaw, Cam.pitch);
    if (Cam.freecam) setPosition(Cam.x, Cam.y, Cam.z);
  }
}
