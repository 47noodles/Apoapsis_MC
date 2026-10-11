package dev.apoapsis;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.MapClient;
import dev.apoapsis.client.ShipClient;
import dev.apoapsis.client.SkyClient;
import dev.apoapsis.net.AgeSyncPayload;
import dev.apoapsis.net.OrbitStatePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client entry point. Never loaded on a dedicated server. */
@Mod(value = Apoapsis.MODID, dist = Dist.CLIENT)
public final class ApoapsisClient {
    public ApoapsisClient(IEventBus modBus, ModContainer container) {
        ShipClient.register(modBus);
        MapClient.register(modBus);
        SkyClient.register(modBus);
        modBus.addListener(ApoapsisClient::onClientPayloads);
    }

    private static void onClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OrbitStatePayload.TYPE, (p, ctx) -> ClientState.accept(p));
        event.register(AgeSyncPayload.TYPE, (p, ctx) -> ClientState.accept(p));
    }
}
