package nightfall.mixin;
import net.minecraft.client.player.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import nightfall.core.Cam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Blockiert die Spielerbewegung waehrend Freecam. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
  @Inject(method = "tick", at = @At("TAIL"))
  private void nf$tick(CallbackInfo ci) { if (Cam.freecam) { keyPresses = Input.EMPTY; moveVector = Vec2.ZERO; } }
}
