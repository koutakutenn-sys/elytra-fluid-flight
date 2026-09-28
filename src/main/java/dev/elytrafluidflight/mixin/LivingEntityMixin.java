package dev.elytrafluidflight.mixin;

import dev.elytrafluidflight.FluidFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "shouldTravelInFluid", at = @At("HEAD"), cancellable = true)
    private void eff$useGlidingMovement(FluidState state, CallbackInfoReturnable<Boolean> cir) {
        if (FluidFlight.active((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "updateFallFlyingMovement", at = @At("RETURN"), cancellable = true)
    private void eff$applyFluidDrag(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (FluidFlight.active(self)) cir.setReturnValue(cir.getReturnValue().scale(FluidFlight.multiplier(self)));
    }

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void eff$requireFireResistance(CallbackInfoReturnable<Boolean> cir) {
        if (FluidFlight.lavaBlocked((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void eff$stopWhenProtectionExpires(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (FluidFlight.lavaBlocked(self)) {
            self.stopFallFlying();
            ci.cancel();
        }
    }
}
