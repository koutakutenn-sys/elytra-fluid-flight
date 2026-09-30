package dev.elytrafluidflight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public final class ElytraFluidFlight implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("elytra_fluid_flight");
    public static final String CONFIG_FILE_NAME = "elytra_fluid_flight.json";
    public static FlightConfig CONFIG = new FlightConfig();

    /** Config file of the side this code is running on. */
    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    @Override
    public void onInitialize() {
        CONFIG = FlightConfig.load(configPath());
        LOGGER.info("Elytra Fluid Flight: water={}, lava={}, lavaRequiresFireResistance={}",
                CONFIG.waterSpeedMultiplier, CONFIG.lavaSpeedMultiplier, CONFIG.lavaRequiresFireResistance);
    }
}
