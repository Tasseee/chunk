package nightfall.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import nightfall.core.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Slows only the local player's hand-swing animation ticker, not attack cooldown or server actions. */
@Mixin(LivingEntity.class)
public abstract class SlowSwingMixin {
  @Unique private static float nightfall$swingAccumulator;

  @Inject(method = "updateSwingTime", at = @At("HEAD"), cancellable = true)
  private void nightfall$slowLocalSwing(CallbackInfo ci) {
    Minecraft mc = Minecraft.getInstance();
    if ((Object) this != mc.player || Modules.SLOW_SWING == null || !Modules.SLOW_SWING.enabled) {
      if ((Object) this == mc.player) nightfall$swingAccumulator = 0;
      return;
    }
    float speed = (float) Modules.SLOW_SWING.s("Speed").d();
    if (speed >= 0.999f) { nightfall$swingAccumulator = 0; return; }
    nightfall$swingAccumulator += speed;
    if (nightfall$swingAccumulator < 1.0f) ci.cancel();
    else nightfall$swingAccumulator -= 1.0f;
  }
}
