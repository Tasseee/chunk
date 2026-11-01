package nightfall.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.attribute.EnvironmentAttributes;
import nightfall.core.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Nightfall's local steel palette after Minecraft resolves environment attributes. */
@Mixin(EnvironmentAttributeSystem.class)
public abstract class DarkSteelSkyMixin {
  @Inject(method = "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/attribute/SpatialAttributeInterpolator;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
  private <Value> void nightfall$darkSteelSky(EnvironmentAttribute<Value> attribute, Vec3 position,
      SpatialAttributeInterpolator interpolator, CallbackInfoReturnable<Value> cir) {
    if (Modules.DARK_STEEL_SKY == null || !Modules.DARK_STEEL_SKY.enabled || cir.getReturnValue() == null) return;
    double strength;
    int target;
    if (attribute == EnvironmentAttributes.SKY_COLOR) {
      strength = Modules.DARK_STEEL_SKY.s("Strength").d();
      target = 0x172235;
    } else if (attribute == EnvironmentAttributes.SUNRISE_SUNSET_COLOR) {
      strength = Modules.DARK_STEEL_SKY.s("Strength").d();
      target = 0x4B3A62;
    } else if (attribute == EnvironmentAttributes.CLOUD_COLOR) {
      strength = Modules.DARK_STEEL_SKY.s("Cloud Strength").d();
      target = 0x38475B;
    } else if (attribute == EnvironmentAttributes.FOG_COLOR) {
      strength = Modules.DARK_STEEL_SKY.s("Fog Strength").d();
      target = 0x182332;
    } else {
      return;
    }
    if (!(cir.getReturnValue() instanceof Integer source)) return;
    int mixed = blend(source, target, strength);
    @SuppressWarnings("unchecked") Value value = (Value) Integer.valueOf(mixed);
    cir.setReturnValue(value);
  }

  private static int blend(int source, int target, double amount) {
    double t = Math.max(0.0, Math.min(1.0, amount));
    int a = (source >>> 24) & 255;
    int r = channel((source >>> 16) & 255, (target >>> 16) & 255, t);
    int g = channel((source >>> 8) & 255, (target >>> 8) & 255, t);
    int b = channel(source & 255, target & 255, t);
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  private static int channel(int from, int to, double amount) {
    return (int) Math.round(from + (to - from) * amount);
  }
}
