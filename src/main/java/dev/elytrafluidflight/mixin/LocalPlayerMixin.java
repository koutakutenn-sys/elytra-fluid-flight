package dev.elytrafluidflight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.elytrafluidflight.FluidFlight;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-side half of lava swimming.
 *
 * <p>Vanilla's swimming sprint is hard-wired to water: {@code canStartSprinting()} refuses to start
 * a sprint while gliding unless {@code isUnderWater()} holds, and {@code shouldStopSwimSprinting()}
 * stops the sprint as soon as {@code isInWater()} is false. A player who is swimming in lava fails
 * both, so setting the swimming flag also cancelled its own sprint in the same tick and lava
 * swimming could never be sustained. Both checks accept lava instead while the option is on and the
 * player is in lava; with the option off both injectors return the vanilla value unchanged.
 *
 * <p>Only the two water-only gates are widened, so every other vanilla condition (food, using an
 * item, forward input, being on the ground) still decides as before.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    private LocalPlayer eff$self() {
        return (LocalPlayer) (Object) this;
    }

    @ModifyExpressionValue(method = "canStartSprinting",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z", ordinal = 0))
    private boolean eff$lavaCountsAsSubmergedWhileGliding(boolean original) {
        // Vanilla gate: !isFallFlying() || isUnderWater(). An elytra stays deployed inside a liquid,
        // so without this a player gliding into lava could never start the swimming sprint.
        return original || FluidFlight.lavaSubmerged(eff$self());
    }

    @ModifyExpressionValue(method = "canStartSprinting",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z", ordinal = 1))
    private boolean eff$lavaCountsAsSubmergedWhileSlow(boolean original) {
        // Vanilla gate: !isMovingSlowly() || isUnderWater().
        return original || FluidFlight.lavaSubmerged(eff$self());
    }

    @ModifyExpressionValue(method = "shouldStopSwimSprinting",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z"))
    private boolean eff$lavaKeepsTheSwimSprint(boolean original) {
        // Vanilla stops a swimming sprint outside of water; lava swims on while the option is on.
        return original || FluidFlight.lavaSwimmingInPlay(eff$self());
    }
}
