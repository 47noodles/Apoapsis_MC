package dev.apoapsis.mixin.client;

import dev.apoapsis.client.ship.CockpitCamera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips drawing a frame while a pilot who crossed between surface and orbit has not been seated in the new ship yet
 * ({@link CockpitCamera#holdFrame}). The main render target is cleared at the start of {@code render}, so skipping the
 * whole method leaves the last frame on it and the window blits that again: the screen keeps the last good frame
 * instead of showing the new level from a camera inside the hull. Client only.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render()V", at = @At("HEAD"), cancellable = true)
    private void apoapsis$holdWhileUnseated(CallbackInfo ci) {
        if (CockpitCamera.holdFrame()) {
            ci.cancel();
        }
    }
}
