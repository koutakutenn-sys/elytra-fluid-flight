package dev.elytrafluidflight.test;

import com.mojang.authlib.GameProfile;
import dev.elytrafluidflight.ElytraFluidFlight;
import dev.elytrafluidflight.FlightConfig;
import dev.elytrafluidflight.FluidFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class FlightIntegrationTests {
    private static int checks;
    private static final StringBuilder REPORT = new StringBuilder();

    public static void run(MinecraftServer server) {
        try {
            configTests();
            ServerLevel level = server.overworld();
            ElytraFluidFlight.CONFIG = new FlightConfig();
            TestPlayer air = player(level, Blocks.AIR);
            require(air.tryToStartFallFlying(), "Air launch");
            Vec3 baseline = travel(air);

            for (Block fluid : new Block[]{Blocks.WATER, Blocks.LAVA}) {
                String name = fluid == Blocks.WATER ? "Water" : "Lava";
                TestPlayer p = player(level, fluid);
                require(fluid == Blocks.WATER ? p.isInWater() : p.isInLava(), name + " detected from real blocks");
                require(p.tryToStartFallFlying(), name + " submerged launch");
                p.maintainFlight();
                require(p.isFallFlying(), name + " stays gliding");
                require(!p.fluidTravel(), name + " uses elytra physics");
                double retention = retention(fluid);
                Vec3 actual = travel(p);
                require(actual.subtract(baseline.scale(retention)).length() < 1e-9,
                        name + " applies per-tick retention " + retention);

                // Extra drag must not compound into a stop: after ten ticks of gliding in a liquid
                // the player still keeps most of their horizontal speed.
                TestPlayer steady = player(level, fluid);
                steady.tryToStartFallFlying();
                steady.setDeltaMovement(0, -0.1, 0.6);
                double startSpeed = steady.getDeltaMovement().horizontalDistance();
                for (int step = 0; step < 10; step++) {
                    steady.setPos(10.5, 122, 10.5);
                    steady.travel(Vec3.ZERO);
                }
                require(steady.getDeltaMovement().horizontalDistance() > startSpeed * 0.5,
                        name + " drag does not compound to a stop");

                // Existing air flight entering a liquid must retain its state.
                TestPlayer entering = player(level, Blocks.AIR);
                entering.tryToStartFallFlying();
                fill(level, fluid);
                entering.refreshFluid();
                entering.maintainFlight();
                require(entering.isFallFlying() && !entering.fluidTravel(), name + " air-to-fluid entry");
                fill(level, Blocks.AIR);
                entering.refreshFluid();
                require(travel(entering).subtract(baseline).length() < 1e-9, name + " exit restores air physics");

                p = player(level, fluid);
                p.tryToStartFallFlying();
                p.setSprinting(true);
                p.setSwimming(true);
                p.updateSwimming();
                if (fluid == Blocks.WATER) {
                    require(p.isSwimming() && p.fluidTravel() && p.isFallFlying(),
                            name + " sprint swimming keeps vanilla water movement");
                } else {
                    require(!p.isSwimming() && !p.fluidTravel(), name + " sprint does not select swimming pose");
                }
                p.setDeltaMovement(Vec3.ZERO);
                FireworkRocketEntity rocket = new FireworkRocketEntity(level, new ItemStack(Items.FIREWORK_ROCKET), p);
                rocket.tick();
                require(p.getDeltaMovement().dot(p.getLookAngle()) > 0.8, name + " vanilla firework thrust");
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIREWORK_ROCKET, 2));
                Items.FIREWORK_ROCKET.use(level, p, InteractionHand.MAIN_HAND);
                require(p.getMainHandItem().getCount() == 1, name + " firework use consumes exactly one");

                p.stopFallFlying();
                p.setOnGround(true);
                require(!p.tryToStartFallFlying(), name + " grounded launch denied");
                p.setOnGround(false);
                p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                require(!p.tryToStartFallFlying(), name + " missing elytra denied");
                ItemStack broken = new ItemStack(Items.ELYTRA);
                broken.setDamageValue(broken.getMaxDamage() - 1);
                p.setItemSlot(EquipmentSlot.CHEST, broken);
                require(!p.tryToStartFallFlying(), name + " broken elytra denied");
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
                p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200));
                require(!p.tryToStartFallFlying(), name + " levitation still denies launch");
                p.removeEffect(MobEffects.LEVITATION);
                p.tryToStartFallFlying();
                p.durabilityTick();
                require(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 1, name + " vanilla durability cost");
                p.setOnGround(true);
                p.maintainFlight();
                require(!p.isFallFlying(), name + " landing still closes elytra");
            }

            ElytraFluidFlight.CONFIG.lavaRequiresFireResistance = true;
            TestPlayer lava = player(level, Blocks.LAVA);
            require(!lava.tryToStartFallFlying(), "Lava protection requirement blocks launch");

            // Swimming and gliding must stay separate states.
            TestPlayer swimmer = player(level, Blocks.WATER);
            swimmer.setSprinting(true);
            swimmer.setSwimming(true);
            require(!swimmer.tryToStartFallFlying(), "Swimming blocks glide launch");
            require(swimmer.fluidTravel(), "Swimming keeps vanilla water movement");
            swimmer.setSprinting(false);
            swimmer.setSwimming(false);
            require(swimmer.tryToStartFallFlying(), "Glide launch works when not swimming");

            // Optional lava swimming (1.0.2): off by default, because vanilla never enters the
            // swimming pose outside of water.
            ElytraFluidFlight.CONFIG.lavaRequiresFireResistance = false;
            TestPlayer lavaGlider = player(level, Blocks.LAVA);
            lavaGlider.tryToStartFallFlying();
            lavaGlider.setSprinting(true);
            lavaGlider.updateSwimming();
            require(lavaGlider.isFallFlying() && !lavaGlider.isSwimming() && !lavaGlider.fluidTravel(),
                    "Lava swimming off by default");
            // These two are what the client sprint mixin ORs into canStartSprinting and
            // shouldStopSwimSprinting, so a disabled option provably leaves vanilla sprinting alone.
            require(!FluidFlight.lavaSwimmingInPlay(lavaGlider) && !FluidFlight.lavaSubmerged(lavaGlider),
                    "Lava sprint gates off by default");

            ElytraFluidFlight.CONFIG.lavaSwimming = true;
            require(FluidFlight.lavaSwimmingInPlay(lavaGlider) && FluidFlight.lavaSubmerged(lavaGlider),
                    "Lava sprint gates follow the option");

            // The path this fix targets: glide -> lava -> sprint -> swimming -> fluid travel.
            TestPlayer lavaSwimmer = player(level, Blocks.LAVA);
            lavaSwimmer.tryToStartFallFlying();
            lavaSwimmer.setSprinting(true);
            require(FluidFlight.lavaSwimEntry(lavaSwimmer), "Gliding in lava can enter lava swimming");
            lavaSwimmer.updateSwimming();
            require(lavaSwimmer.isSwimming() && lavaSwimmer.fluidTravel() && lavaSwimmer.isFallFlying(),
                    "Lava swimming allowed without Fire Resistance");

            // Keeping the state: as in vanilla, once swimming only lava contact is required, so the
            // eyes may leave the lava at the surface without dropping the state again. Eye height
            // depends on the pose (standing 1.62, fall-flying/swimming 0.4), so the standing pose
            // keeps the eyes clearly above the lava while the body stays inside it.
            lavaSwimmer.setPose(Pose.STANDING);
            lavaSwimmer.setPos(10.5, 124.5, 10.5);
            lavaSwimmer.refreshFluid();
            require(lavaSwimmer.isInLava() && !lavaSwimmer.isEyeInFluid(FluidTags.LAVA),
                    "Lava surface exposes the eyes");
            require(!FluidFlight.lavaSwimEntry(lavaSwimmer) && FluidFlight.lavaSwimMaintenance(lavaSwimmer),
                    "Entering and keeping lava swimming are separate conditions");
            lavaSwimmer.updateSwimming();
            require(lavaSwimmer.isSwimming() && lavaSwimmer.fluidTravel(),
                    "Lava swimming survives the lava surface");

            // Leaving the lava ends the state through the vanilla decision.
            fill(level, Blocks.AIR);
            lavaSwimmer.refreshFluid();
            lavaSwimmer.updateSwimming();
            require(!lavaSwimmer.isSwimming(), "Lava swimming exits outside the lava");

            // Releasing sprint ends it too, exactly like a water swim.
            lavaSwimmer = player(level, Blocks.LAVA);
            lavaSwimmer.setSprinting(true);
            lavaSwimmer.updateSwimming();
            require(lavaSwimmer.isSwimming(), "Lava swimming enters from a plain sprint");
            lavaSwimmer.setSprinting(false);
            lavaSwimmer.updateSwimming();
            require(!lavaSwimmer.isSwimming(), "Lava swimming exits when the sprint ends");

            // Swimming in lava is moved with the water fluid physics (vanilla lava movement halves the
            // horizontal velocity every tick and cannot be sprinted), so a lava swimmer must travel
            // exactly like a water swimmer, while a lava player who is not swimming keeps lava physics.
            TestPlayer waterReference = player(level, Blocks.WATER);
            waterReference.setSprinting(true);
            waterReference.setSwimming(true);
            Vec3 waterSwimMovement = travel(waterReference);
            require(!FluidFlight.lavaSwimmingUsesWaterPhysics(waterReference),
                    "Water swimming keeps its own physics");

            TestPlayer lavaReference = player(level, Blocks.LAVA);
            lavaReference.setSprinting(true);
            lavaReference.setSwimming(true);
            require(FluidFlight.lavaSwimmingUsesWaterPhysics(lavaReference),
                    "Lava swimming opts into water physics");
            require(travel(lavaReference).subtract(waterSwimMovement).length() < 1e-9,
                    "Lava swimming moves exactly like water swimming");

            TestPlayer lavaWalk = player(level, Blocks.LAVA);
            require(!FluidFlight.lavaSwimmingUsesWaterPhysics(lavaWalk),
                    "Lava physics stay untouched without the swimming state");
            require(travel(lavaWalk).subtract(waterSwimMovement).length() > 1e-6,
                    "Plain lava movement is still slower than swimming");

            // The Fire Resistance requirement only narrows the group that may swim: a resistant player
            // can always swim, because being resistant never makes swimming harder.
            lavaSwimmer = player(level, Blocks.LAVA);
            lavaSwimmer.tryToStartFallFlying();
            lavaSwimmer.setSprinting(true);
            lavaSwimmer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
            lavaSwimmer.updateSwimming();
            require(lavaSwimmer.isSwimming() && lavaSwimmer.fluidTravel(),
                    "Fire Resistance swims while nothing is required");

            ElytraFluidFlight.CONFIG.lavaSwimmingRequiresFireResistance = true;
            lavaSwimmer.updateSwimming();
            require(lavaSwimmer.isSwimming() && lavaSwimmer.fluidTravel(),
                    "Required Fire Resistance allows a resistant player");

            lavaSwimmer.removeEffect(MobEffects.FIRE_RESISTANCE);
            lavaSwimmer.updateSwimming();
            require(!lavaSwimmer.isSwimming() && !lavaSwimmer.fluidTravel(),
                    "Required Fire Resistance blocks a player without it");

            TestPlayer lavaLaunch = player(level, Blocks.LAVA);
            lavaLaunch.setSprinting(true);
            lavaLaunch.setSwimming(true);
            require(!lavaLaunch.tryToStartFallFlying(), "Lava swimming blocks glide launch");

            // Water has to keep the vanilla state machine untouched: enter while submerged (through
            // the real baseTick, which fills in wasEyeInWater), keep the state at the surface, leave
            // the state outside the water.
            TestPlayer surface = player(level, Blocks.WATER);
            surface.setSprinting(true);
            surface.baseTick();
            require(surface.isSwimming(), "Water swimming still enters while submerged");
            surface.setPose(Pose.STANDING);
            surface.setPos(10.5, 124.5, 10.5);
            surface.refreshFluid();
            require(surface.isInWater() && !surface.isEyeInFluid(FluidTags.WATER),
                    "Water surface exposes the eyes");
            surface.updateSwimming();
            require(surface.isSwimming() && surface.fluidTravel(),
                    "Water swimming still keeps the state at the surface");
            fill(level, Blocks.AIR);
            surface.refreshFluid();
            surface.updateSwimming();
            require(!surface.isSwimming(), "Water swimming still exits outside the water");

            // Later checks read the block at the player position, so put the tank back to lava.
            fill(level, Blocks.LAVA);

            ElytraFluidFlight.CONFIG.lavaSwimming = false;
            ElytraFluidFlight.CONFIG.lavaSwimmingRequiresFireResistance = false;
            ElytraFluidFlight.CONFIG.lavaRequiresFireResistance = true;

            lava.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
            require(lava.tryToStartFallFlying(), "Fire Resistance allows lava launch");
            lava.removeEffect(MobEffects.FIRE_RESISTANCE);
            lava.maintainFlight();
            require(!lava.isFallFlying() && lava.fluidTravel(), "Expired protection closes elytra and restores lava movement");
            TestPlayer water = player(level, Blocks.WATER);
            require(water.tryToStartFallFlying(), "Lava protection option leaves water available");
            ElytraFluidFlight.CONFIG.waterSpeedMultiplier = 1.0;
            require(travel(water).subtract(baseline).length() < 1e-9, "Config 1.0 removes extra drag");
            ElytraFluidFlight.CONFIG.waterSpeedMultiplier = 0.8;
            water = player(level, Blocks.WATER);
            water.tryToStartFallFlying();
            require(travel(water).subtract(baseline.scale(0.98)).length() < 1e-9, "Custom water multiplier 0.8");
            ElytraFluidFlight.CONFIG.lavaRequiresFireResistance = false;
            ElytraFluidFlight.CONFIG.lavaSpeedMultiplier = 0.5;
            lava = player(level, Blocks.LAVA);
            lava.tryToStartFallFlying();
            require(travel(lava).subtract(baseline.scale(0.95)).length() < 1e-9, "Custom lava multiplier 0.5");
            Files.writeString(Path.of("test-result.txt"), "PASS: " + checks + " checks\n" + REPORT);
            ElytraFluidFlight.LOGGER.info("FLUID FLIGHT TESTS PASSED: {} checks", checks);
        } catch (Throwable error) {
            ElytraFluidFlight.LOGGER.error("FLUID FLIGHT TESTS FAILED", error);
            try { Files.writeString(Path.of("test-result.txt"), "FAIL\n" + REPORT + error); }
            catch (Exception ignored) { }
        } finally {
            ElytraFluidFlight.CONFIG = new FlightConfig();
        }
    }

    private static void configTests() throws Exception {
        Path dir = Files.createTempDirectory(Path.of("."), "config-test-");
        Path path = dir.resolve("config.json");
        FlightConfig defaults = FlightConfig.load(path);
        require(Files.isRegularFile(path) && defaults.waterSpeedMultiplier == 0.6 && defaults.lavaSpeedMultiplier == 0.35,
                "Missing config creates defaults");
        Files.writeString(path, "{\"waterSpeedMultiplier\":0.8,\"lavaRequiresFireResistance\":true}");
        FlightConfig custom = FlightConfig.load(path);
        require(custom.waterSpeedMultiplier == 0.8 && custom.lavaSpeedMultiplier == 0.35 && custom.lavaRequiresFireResistance,
                "Partial config merges defaults");
        for (String invalid : new String[]{"0", "-1", "2", "1e400"}) {
            Files.writeString(path, "{\"waterSpeedMultiplier\":" + invalid + "}");
            require(FlightConfig.load(path).waterSpeedMultiplier == 0.6, "Invalid multiplier fallback: " + invalid);
        }
        Files.writeString(path, "{broken");
        require(FlightConfig.load(path).waterSpeedMultiplier == 0.6 && Files.readString(path).equals("{broken"),
                "Malformed config falls back without overwriting");

        // The two independent 1.0.2 lava swimming switches fold into the single switch plus the
        // optional Fire Resistance requirement.
        Files.writeString(path, "{\"lavaSwimmingWithFireResistance\":true}");
        FlightConfig legacyWith = FlightConfig.load(path);
        require(legacyWith.lavaSwimming && legacyWith.lavaSwimmingRequiresFireResistance,
                "Legacy 'with Fire Resistance' becomes a requirement");
        Files.writeString(path, "{\"lavaSwimmingWithoutFireResistance\":true}");
        FlightConfig legacyWithout = FlightConfig.load(path);
        require(legacyWithout.lavaSwimming && !legacyWithout.lavaSwimmingRequiresFireResistance,
                "Legacy 'without Fire Resistance' becomes no requirement");
        Files.writeString(path, "{\"lavaSwimmingWithFireResistance\":true,\"lavaSwimmingWithoutFireResistance\":true}");
        FlightConfig legacyBoth = FlightConfig.load(path);
        require(legacyBoth.lavaSwimming && !legacyBoth.lavaSwimmingRequiresFireResistance,
                "Legacy both-on file becomes swimming without a requirement");
        Files.writeString(path, "{\"lavaSwimming\":false,\"lavaSwimmingWithFireResistance\":true}");
        require(!FlightConfig.load(path).lavaSwimming, "New lava swimming switch wins over legacy keys");

        Files.delete(path);
        Files.delete(dir);
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
        REPORT.append("OK ").append(name).append('\n');
    }

    private static TestPlayer player(ServerLevel level, Block block) {
        fill(level, block);
        TestPlayer player = new TestPlayer(level);
        player.setPos(10.5, 122, 10.5);
        player.setPose(Pose.FALL_FLYING);
        player.setOnGround(false);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        player.setXRot(-10);
        player.setYRot(0);
        player.refreshFluid();
        return player;
    }

    private static void fill(ServerLevel level, Block block) {
        // Remove the previous tank first so replacing water with lava cannot create obsidian.
        for (BlockPos pos : BlockPos.betweenClosed(7, 120, 7, 14, 125, 14)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        for (BlockPos pos : BlockPos.betweenClosed(7, 120, 7, 14, 125, 14)) {
            level.setBlock(pos, block.defaultBlockState(), 2);
        }
    }

    private static Vec3 travel(TestPlayer player) {
        player.setDeltaMovement(0, -0.1, 1);
        player.travel(Vec3.ZERO);
        return player.getDeltaMovement();
    }

    /** Mirrors FluidFlight.retention for the defaults used by these tests. */
    private static double retention(Block fluid) {
        double configured = fluid == Blocks.WATER ? 0.6 : 0.35;
        return 1.0 - (1.0 - configured) * 0.10;
    }

    private static final class TestPlayer extends Player {
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "FluidFlightTest")); }
        @Override public GameType gameMode() { return GameType.SURVIVAL; }
        void refreshFluid() { firstTick = false; updateFluidInteraction(); }
        void maintainFlight() { updateFallFlying(); }
        boolean fluidTravel() { return shouldTravelInFluid(level().getFluidState(blockPosition())); }
        void durabilityTick() { fallFlyTicks = 19; updateFallFlying(); }
    }
}
