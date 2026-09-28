package dev.elytrafluidflight;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class FluidFlight {
    private FluidFlight() {}

    public static boolean lavaBlocked(LivingEntity entity) {
        return entity instanceof Player && entity.isInLava()
                && ElytraFluidFlight.CONFIG.lavaRequiresFireResistance
                && !entity.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    public static boolean active(LivingEntity entity) {
        return entity instanceof Player && entity.isFallFlying()
                && (entity.isInWater() || entity.isInLava()) && !lavaBlocked(entity);
    }

    public static double multiplier(LivingEntity entity) {
        // Lava takes precedence at mixed fluid boundaries.
        return entity.isInLava() ? ElytraFluidFlight.CONFIG.lavaSpeedMultiplier
                : ElytraFluidFlight.CONFIG.waterSpeedMultiplier;
    }
}
