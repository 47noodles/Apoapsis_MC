package dev.apoapsis.mixin.client;

import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the ship the local player sits in from the first frame after a change of dimension. Vanilla draws an entity
 * only once the render section it stands in has been compiled and has faded in ({@code isEntityVisible}), so it
 * does not show before the terrain under it. After a transition to the orbit dimension every section is new: the
 * pilot is already seated, but the ship, and with it the cockpit deck, stays undrawn for about a second while the
 * section around it compiles. The ship the pilot rides has no terrain to pop in before, so it skips that test. Client
 * only.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private @Nullable ClientLevel level;

    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void apoapsis$drawRiddenShipAtOnce(Entity entity, Frustum frustum, double camX, double camY, double camZ,
            float partialTicks, long chunkFadeDuration, CallbackInfoReturnable<Boolean> cir) {
        if (level != null && entity instanceof ShipEntity && minecraft.player != null
                && entity.hasIndirectPassenger(minecraft.player)) {
            cir.setReturnValue(true);
        }
    }
}
