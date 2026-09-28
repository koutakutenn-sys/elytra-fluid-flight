package dev.elytrafluidflight.test.mixin;

import dev.elytrafluidflight.test.FlightIntegrationTests;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class TestServerMixin {
    @Unique private boolean eff$tested;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void eff$test(BooleanSupplier timeLeft, CallbackInfo ci) {
        if (eff$tested) return;
        eff$tested = true;
        MinecraftServer server = (MinecraftServer) (Object) this;
        FlightIntegrationTests.run(server);
        server.halt(false);
    }
}
