package dev.elytrafluidflight;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class FluidFlight {
    /**
     * Fraction of the configured value that turns into real damping each tick.
     *
     * <p>Scaling the whole velocity with the raw value every tick compounds: 0.6 leaves
     * 0.6^10 = 0.6% of the speed after half a second, which stalls a glide in a liquid instead of
     * just slowing it down. Applying a small share of the configured value keeps the liquid
     * noticeably thicker than air without ever killing the player's momentum.
     */
    private static final double DAMPING_SCALE = 0.10;

    private FluidFlight() {}

    /** True when lava gliding has to stop because the configured Fire Resistance is missing. */
    public static boolean lavaBlocked(LivingEntity entity) {
        return entity instanceof Player && entity.isInLava()
                && ElytraFluidFlight.CONFIG.lavaRequiresFireResistance
                && !entity.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    /**
     * True while the player is swimming. This covers both the pose itself and the vanilla
     * condition that puts a player into it (sprinting with the eyes under water), so the
     * swimming and gliding states can never overlap: water movement wins while swimming, and a
     * swimming player keeps vanilla behaviour when jumping instead of opening the elytra.
     */
    public static boolean swimming(LivingEntity entity) {
        return entity.isSwimming() || (entity.isSprinting() && entity.isUnderWater());
    }

    /** True while gliding in a liquid should override the liquid movement. */
    public static boolean active(LivingEntity entity) {
        return entity instanceof Player && entity.isFallFlying()
                && (entity.isInWater() || entity.isInLava())
                && !lavaBlocked(entity)
                && !swimming(entity);
    }

    /**
     * Fraction of the vanilla elytra velocity kept each tick while flying in a liquid.
     * A value of 1.0 behaves exactly like vanilla air physics.
     */
    public static double retention(LivingEntity entity) {
        // Lava takes precedence at mixed fluid boundaries.
        double configured = entity.isInLava() ? ElytraFluidFlight.CONFIG.lavaSpeedMultiplier
                : ElytraFluidFlight.CONFIG.waterSpeedMultiplier;
        return 1.0 - (1.0 - configured) * DAMPING_SCALE;
    }
}
