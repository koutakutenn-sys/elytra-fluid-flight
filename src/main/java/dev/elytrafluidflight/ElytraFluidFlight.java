package dev.elytrafluidflight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ElytraFluidFlight implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("elytra_fluid_flight");
    public static FlightConfig CONFIG = new FlightConfig();

    @Override
    public void onInitialize() {
        CONFIG = FlightConfig.load(FabricLoader.getInstance().getConfigDir().resolve("elytra_fluid_flight.json"));
        LOGGER.info("Elytra Fluid Flight: water={}, lava={}, lavaRequiresFireResistance={}",
                CONFIG.waterSpeedMultiplier, CONFIG.lavaSpeedMultiplier, CONFIG.lavaRequiresFireResistance);
    }
}
