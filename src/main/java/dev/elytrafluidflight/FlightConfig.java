package dev.elytrafluidflight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public double waterSpeedMultiplier = 0.6;
    public double lavaSpeedMultiplier = 0.35;
    public boolean lavaRequiresFireResistance = false;

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
            FlightConfig config = GSON.fromJson(reader, FlightConfig.class);
            if (config == null) throw new IllegalArgumentException("Config must be a JSON object");
            config.waterSpeedMultiplier = validMultiplier(config.waterSpeedMultiplier, 0.6, "waterSpeedMultiplier");
            config.lavaSpeedMultiplier = validMultiplier(config.lavaSpeedMultiplier, 0.35, "lavaSpeedMultiplier");
            return config;
        } catch (IOException | RuntimeException e) {
            // Keep the user's file intact so an invalid value can be corrected.
            ElytraFluidFlight.LOGGER.error("Could not read {}; using defaults (file preserved)", path, e);
            return new FlightConfig();
        }
    }

    private static double validMultiplier(double value, double fallback, String name) {
        if (Double.isFinite(value) && value > 0.0 && value <= 1.0) return value;
        ElytraFluidFlight.LOGGER.warn("{} must be finite and in (0, 1]; using {}", name, fallback);
        return fallback;
    }
}
