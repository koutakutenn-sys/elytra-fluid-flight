package dev.elytrafluidflight.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Menu integration. Mod Menu is optional: it is only a compile-time dependency, and this
 * entrypoint is only loaded on a client that actually has Mod Menu installed.
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory<Screen>) ElytraFluidFlightConfigScreen::new;
    }
}
