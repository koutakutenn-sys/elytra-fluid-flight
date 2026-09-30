package dev.elytrafluidflight.mixin;

import dev.elytrafluidflight.FluidFlight;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void eff$updateSwimming(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) return;
        if (FluidFlight.active(player)) {
            // Gliding in a liquid keeps the glide pose instead of the swimming pose.
            player.setSwimming(false);
            ci.cancel();
            return;
        }
        // Lava swimming, when configured: take over with the same entering/keeping shape vanilla
        // uses for water. Any other case (including an option that is off) keeps vanilla in charge.
        if (!FluidFlight.lavaSwimmingInPlay(player)) return;
        player.setSwimming(FluidFlight.desiredSwimming(player));
        ci.cancel();
    }
}
