package dev.apoapsis.client.atmosphere;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

/**
 * Vanilla clouds are flat slabs, fine from the ground and wrong from a ship above them. While the local player flies a
 * ship, the clouds fade out as the camera nears their layer and are gone above it, by scaling the alpha of the cloud
 * colour in the frame's render state (no mixin). On foot nothing changes. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class CloudFade {
    /** The clouds start to fade this far below their layer, blocks, and are gone this far above it. */
    static final double FADE_BELOW = 45.0;
    static final double FADE_ABOVE = 10.0;

    private CloudFade() {}

    @SubscribeEvent
    static void onExtract(ExtractLevelRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity)) {
            return;
        }
        LevelRenderState state = event.getRenderState();
        int alpha = ARGB.alpha(state.cloudColor);
        if (alpha <= 0) {
            return;
        }
        double above = event.getCamera().position().y() - state.cloudHeight;
        double fade = 1.0 - Haze.smoothstep(-FADE_BELOW, FADE_ABOVE, above);
        state.cloudColor = ARGB.color((int) Math.round(alpha * fade), state.cloudColor);
    }
}
