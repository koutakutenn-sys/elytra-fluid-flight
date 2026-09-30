package dev.elytrafluidflight;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;

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
        return entity.isSwimming() || (entity.isSprinting() && entity.isUnderWater()) || lavaSwimEntry(entity);
    }

    /**
     * True when the config lets this player use the swimming state in lava. Fire Resistance must not
     * be modelled as a second permission: being resistant never makes swimming harder, so this is a
     * single switch plus an optional requirement.
     */
    public static boolean lavaSwimmingAllowed(LivingEntity entity) {
        if (!ElytraFluidFlight.CONFIG.lavaSwimming) return false;
        return !ElytraFluidFlight.CONFIG.lavaSwimmingRequiresFireResistance
                || entity.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    /**
     * True when lava swimming applies to this player at all: the matching option is enabled and the
     * player actually touches lava. Every other lava predicate builds on this one, so a disabled
     * option always leaves vanilla behaviour untouched.
     */
    public static boolean lavaSwimmingInPlay(LivingEntity entity) {
        return entity instanceof Player && entity.isInLava() && lavaSwimmingAllowed(entity);
    }

    /**
     * Vanilla only starts a swimming sprint with the eyes under water; this is the lava counterpart,
     * and it is what the client-side sprint mixin feeds into {@code canStartSprinting()}.
     */
    public static boolean lavaSubmerged(LivingEntity entity) {
        return lavaSwimmingInPlay(entity) && entity.isEyeInFluid(FluidTags.LAVA);
    }

    /**
     * Entering lava swimming: sprinting with the eyes in lava, mirroring the vanilla water condition
     * "sprinting with the eyes under water". Disabled by default, and the Fire Resistance case is
     * configured separately from the non-resistant one.
     */
    public static boolean lavaSwimEntry(LivingEntity entity) {
        return lavaSubmerged(entity) && entity.isSprinting();
    }

    /**
     * Keeping lava swimming: any lava contact is enough, mirroring vanilla, which keeps a swimmer
     * swimming while merely in water instead of re-requiring the eyes to be submerged. Without this
     * the state flickered off every time the player floated up to the lava surface.
     */
    public static boolean lavaSwimMaintenance(LivingEntity entity) {
        return lavaSwimmingInPlay(entity) && entity.isSprinting();
    }

    /**
     * True when a lava swimmer should be moved with the water fluid physics instead of the lava ones.
     *
     * <p>Vanilla routes fluid movement through {@code travelInFluid}, which picks {@code travelInLava}
     * whenever {@code isInWater()} is false. Lava movement halves the horizontal velocity every tick
     * and has no sprint bonus, so a lava swimmer would barely move and could hardly rise. While the
     * swimming state is active (which the options above already gate) the water branch is used
     * instead, making lava swimming feel like water swimming.
     */
    public static boolean lavaSwimmingUsesWaterPhysics(LivingEntity entity) {
        return entity.isSwimming() && lavaSwimmingInPlay(entity);
    }

    /**
     * The swimming flag {@code Entity#updateSwimming} should end this tick with. This mirrors the
     * vanilla two-branch shape exactly (entering vs keeping) and only adds lava on top of it, so a
     * disabled option reproduces vanilla decisions bit for bit.
     */
    public static boolean desiredSwimming(LivingEntity entity) {
        if (entity.isPassenger() || !entity.isSprinting()) return false;
        if (entity.isSwimming()) return entity.isInWater() || lavaSwimMaintenance(entity);
        boolean vanillaEntry = entity.isUnderWater()
                && entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER);
        return vanillaEntry || lavaSwimEntry(entity);
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
