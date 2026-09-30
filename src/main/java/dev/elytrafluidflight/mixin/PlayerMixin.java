package dev.elytrafluidflight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.elytrafluidflight.FluidFlight;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyExpressionValue(method = "tryToStartFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWater()Z"))
    private boolean eff$allowUnderwaterLaunch(boolean original) {
        // Water no longer blocks opening the elytra, but a swimming player (in water or, when the
        // config allows it, in lava) keeps the vanilla meaning of jump: swim up. Real fluid
        // detection, breathing and damage remain intact.
        return FluidFlight.swimming((Player) (Object) this);
    }
}
