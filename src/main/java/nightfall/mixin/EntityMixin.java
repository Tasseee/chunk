package nightfall.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import nightfall.core.Cam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class EntityMixin {
  @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
  private void nf$turn(double yRot, double xRot, CallbackInfo ci) {
    if (Cam.active() && (Object) this == Minecraft.getInstance().player) {
      Cam.yaw += (float) yRot * 0.15f; Cam.pitch = Mth.clamp(Cam.pitch + (float) xRot * 0.15f, -90f, 90f); ci.cancel();
    }
  }
}
