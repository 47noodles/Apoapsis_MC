package dev.apoapsis.client.hud;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Hides the local player's hand and held item while the player rides a ship, so the cockpit view is not covered by a
 * held item. Vanilla draws the hand only in first person, so this changes nothing else. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class FlightHand {
    private FlightHand() {}

    /** Cancels the hand's render while the local player is in a ship. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof ShipEntity) {
            event.setCanceled(true);
        }
    }
}
