package dev.elytrafluidflight.test;

import com.mojang.authlib.GameProfile;
import dev.elytrafluidflight.ElytraFluidFlight;
import dev.elytrafluidflight.FlightConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
                double factor = fluid == Blocks.WATER ? 0.6 : 0.35;
                Vec3 actual = travel(p);
                require(actual.subtract(baseline.scale(factor)).length() < 1e-9, name + " configured drag ratio " + factor);

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
                require(!p.isSwimming() && p.isFallFlying(), name + " sprint does not select swimming pose");
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
            require(travel(water).subtract(baseline.scale(0.8)).length() < 1e-9, "Custom water multiplier 0.8");
            ElytraFluidFlight.CONFIG.lavaRequiresFireResistance = false;
            ElytraFluidFlight.CONFIG.lavaSpeedMultiplier = 0.5;
            lava = player(level, Blocks.LAVA);
            lava.tryToStartFallFlying();
            require(travel(lava).subtract(baseline.scale(0.5)).length() < 1e-9, "Custom lava multiplier 0.5");
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

    private static final class TestPlayer extends Player {
        TestPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "FluidFlightTest")); }
        @Override public GameType gameMode() { return GameType.SURVIVAL; }
        void refreshFluid() { firstTick = false; updateFluidInteraction(); }
        void maintainFlight() { updateFallFlying(); }
        boolean fluidTravel() { return shouldTravelInFluid(level().getFluidState(blockPosition())); }
        void durabilityTick() { fallFlyTicks = 19; updateFallFlying(); }
    }
}
