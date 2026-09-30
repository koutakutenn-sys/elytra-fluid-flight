package dev.elytrafluidflight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public double waterSpeedMultiplier = 0.6;
    public double lavaSpeedMultiplier = 0.35;
    public boolean lavaRequiresFireResistance = false;
    /** Master switch for the swimming state in lava. */
    public boolean lavaSwimming = false;
    /**
     * When true, only players with the Fire Resistance effect may swim in lava. This is a
     * requirement rather than a second permission: being fire resistant never makes swimming
     * harder, so there is no "only without Fire Resistance" case to configure.
     */
    public boolean lavaSwimmingRequiresFireResistance = false;

    public static FlightConfig load(Path path) {
        if (!Files.exists(path)) {
            FlightConfig defaults = new FlightConfig();
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(defaults) + "\n", StandardCharsets.UTF_8);
            } catch (IOException e) {
                ElytraFluidFlight.LOGGER.error("Could not create {}; using defaults", path, e);
            }
            return defaults;
        }
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) throw new IllegalArgumentException("Config must be a JSON object");
            JsonObject json = root.getAsJsonObject();
            FlightConfig config = GSON.fromJson(json, FlightConfig.class);
            if (config == null) throw new IllegalArgumentException("Config must be a JSON object");
            migrateLavaSwimming(json, config);
            config.waterSpeedMultiplier = validMultiplier(config.waterSpeedMultiplier, 0.6, "waterSpeedMultiplier");
            config.lavaSpeedMultiplier = validMultiplier(config.lavaSpeedMultiplier, 0.35, "lavaSpeedMultiplier");
            return config;
        } catch (IOException | RuntimeException e) {
            // Keep the user's file intact so an invalid value can be corrected.
            ElytraFluidFlight.LOGGER.error("Could not read {}; using defaults (file preserved)", path, e);
            return new FlightConfig();
        }
    }

    /**
     * 1.0.2 first shipped two independent lava swimming switches (with and without Fire Resistance).
     * They could not express the real choice, because a resistant player can always swim where a
     * non-resistant one can. A file that only has the old keys is folded into the new pair.
     */
    private static void migrateLavaSwimming(JsonObject json, FlightConfig config) {
        if (json.has("lavaSwimming")) return;
        boolean with = flag(json, "lavaSwimmingWithFireResistance");
        boolean without = flag(json, "lavaSwimmingWithoutFireResistance");
        if (!with && !without) return;
        config.lavaSwimming = true;
        config.lavaSwimmingRequiresFireResistance = with && !without;
        ElytraFluidFlight.LOGGER.info("Migrated the old lava swimming options: lavaSwimming={}, "
                + "lavaSwimmingRequiresFireResistance={}", config.lavaSwimming, config.lavaSwimmingRequiresFireResistance);
    }

    private static boolean flag(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                && element.getAsBoolean();
    }

    /** Writes this config back to disk, creating the parent directory when needed. */
    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            ElytraFluidFlight.LOGGER.error("Could not write {}", path, e);
        }
    }

    private static double validMultiplier(double value, double fallback, String name) {
        if (Double.isFinite(value) && value > 0.0 && value <= 1.0) return value;
        ElytraFluidFlight.LOGGER.warn("{} must be finite and in (0, 1]; using {}", name, fallback);
        return fallback;
    }
}
