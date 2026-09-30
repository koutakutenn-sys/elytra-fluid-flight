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
    private void eff$keepGlidingPose(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) return;
        if (FluidFlight.active(player)) {
            // Gliding in a liquid keeps the glide pose instead of the swimming pose.
            player.setSwimming(false);
            ci.cancel();
        } else if (FluidFlight.lavaSwimming(player)) {
            // Optional lava swimming: vanilla never sets the swimming flag outside of water.
            player.setSwimming(true);
            ci.cancel();
        }
    }
}
