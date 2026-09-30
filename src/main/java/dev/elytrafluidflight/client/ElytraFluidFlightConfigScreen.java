package dev.elytrafluidflight.client;

import dev.elytrafluidflight.ElytraFluidFlight;
import dev.elytrafluidflight.FlightConfig;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen shown by Mod Menu. It edits the client's config in memory and writes the file
 * when the screen closes; a dedicated server is still configured through its own config file.
 *
 * <p>Only stock widgets are used, so the project needs no extra runtime dependency for this screen.
 */
public final class ElytraFluidFlightConfigScreen extends Screen {
    private static final String KEY_PREFIX = "elytra_fluid_flight.config.";
    private static final int WIDGET_WIDTH = 220;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    private static final double STEP = 0.05;
    private static final double MIN_MULTIPLIER = 0.05;

    private final Screen parent;

    public ElytraFluidFlightConfigScreen(Screen parent) {
        super(text("title", "Elytra Fluid Flight settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FlightConfig config = ElytraFluidFlight.CONFIG;
        int x = this.width / 2 - WIDGET_WIDTH / 2;
        int y = Math.max(30, this.height / 6);

        addRenderableWidget(new MultiplierSlider(x, y, "waterSpeedMultiplier", "Water drag strength: %s",
                config.waterSpeedMultiplier, value -> config.waterSpeedMultiplier = value));
        y += ROW_HEIGHT;
        addRenderableWidget(new MultiplierSlider(x, y, "lavaSpeedMultiplier", "Lava drag strength: %s",
                config.lavaSpeedMultiplier, value -> config.lavaSpeedMultiplier = value));
        y += ROW_HEIGHT + 8;

        y = addToggle(x, y, "lavaRequiresFireResistance", "Lava gliding requires Fire Resistance",
                config.lavaRequiresFireResistance, value -> config.lavaRequiresFireResistance = value);
        y = addToggle(x, y, "lavaSwimmingWithFireResistance", "Allow lava swimming with Fire Resistance",
                config.lavaSwimmingWithFireResistance, value -> config.lavaSwimmingWithFireResistance = value);
        addToggle(x, y, "lavaSwimmingWithoutFireResistance", "Allow lava swimming without Fire Resistance",
                config.lavaSwimmingWithoutFireResistance, value -> config.lavaSwimmingWithoutFireResistance = value);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(x, this.height - 34, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    private int addToggle(int x, int y, String option, String fallback, boolean selected, Consumer<Boolean> setter) {
        addRenderableWidget(Checkbox.builder(text(option, fallback), this.font)
                .pos(x, y)
                .selected(selected)
                .onValueChange((box, value) -> setter.accept(value))
                .build());
        return y + ROW_HEIGHT;
    }

    @Override
    public void onClose() {
        ElytraFluidFlight.CONFIG.save(ElytraFluidFlight.configPath());
        if (this.minecraft != null) this.minecraft.gui.setScreen(this.parent);
    }

    private static Component text(String option, String fallback, Object... args) {
        return Component.translatableWithFallback(KEY_PREFIX + option, fallback, args);
    }

    /** Slider for one of the drag strengths, snapped to steps of 0.05. */
    private static final class MultiplierSlider extends AbstractSliderButton {
        private final String option;
        private final String fallback;
        private final DoubleConsumer setter;

        MultiplierSlider(int x, int y, String option, String fallback, double value, DoubleConsumer setter) {
            super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty(), snap(value));
            this.option = option;
            this.fallback = fallback;
            this.setter = setter;
            this.updateMessage();
        }

        private static double snap(double value) {
            double clamped = Math.min(1.0, Math.max(MIN_MULTIPLIER, value));
            return Math.round(clamped / STEP) * STEP;
        }

        @Override
        protected void updateMessage() {
            setMessage(text(this.option, this.fallback,
                    String.format(Locale.ROOT, "%.2f", this.value)));
        }

        @Override
        protected void applyValue() {
            double snapped = snap(this.value);
            if (snapped != this.value) {
                this.value = snapped;
                this.updateMessage();
            }
            this.setter.accept(this.value);
        }
    }
}
