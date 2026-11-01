package nightfall.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import nightfall.core.Modules;
import nightfall.visuals.Viewmodel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class ViewmodelMixin {
  @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
  private void nf$head(CallbackInfo ci, @Local(argsOnly = true) PoseStack ps) { if (Modules.VIEW.enabled) { ps.pushPose(); Viewmodel.apply(ps); } }
  @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
  private void nf$ret(CallbackInfo ci, @Local(argsOnly = true) PoseStack ps) { if (Modules.VIEW.enabled) ps.popPose(); }
}
