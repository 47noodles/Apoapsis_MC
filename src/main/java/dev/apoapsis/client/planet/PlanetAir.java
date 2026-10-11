package dev.apoapsis.client.planet;

import dev.apoapsis.Apoapsis;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Verdance's air: a faint teal in the fog colour. Pale gets nothing here (its air is dark via data). Client only. */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class PlanetAir {
    private PlanetAir() {}

    @SubscribeEvent
    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!PlanetTints.isVerdance(null)) {
            return;
        }
        int target = PlanetTints.VERDANCE_AIR;
        float amount = PlanetTints.VERDANCE_AIR_AMOUNT;
        event.setRed(PlanetTints.mixFloat(event.getRed(), ((target >> 16) & 0xFF) / 255F, amount));
        event.setGreen(PlanetTints.mixFloat(event.getGreen(), ((target >> 8) & 0xFF) / 255F, amount));
        event.setBlue(PlanetTints.mixFloat(event.getBlue(), (target & 0xFF) / 255F, amount));
    }
}
