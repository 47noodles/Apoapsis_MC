package dev.apoapsis.net;

import dev.apoapsis.planet.PlanetMaps;
import dev.apoapsis.planet.TerrainTiles;
import dev.apoapsis.ship.ShipControls;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers every payload. Client-bound handlers are bound in {@code ApoapsisClient}. */
public final class ApNetwork {
    private ApNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ApNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(ShipInputPayload.TYPE, ShipInputPayload.STREAM_CODEC, ShipControls::handle);
        r.playToClient(OrbitStatePayload.TYPE, OrbitStatePayload.STREAM_CODEC);
        r.playToClient(AgeSyncPayload.TYPE, AgeSyncPayload.STREAM_CODEC);
        r.playToClient(GuideStepPayload.TYPE, GuideStepPayload.STREAM_CODEC);
        r.playToClient(MessagePayload.TYPE, MessagePayload.STREAM_CODEC);
        r.playToClient(PlanetMapPayload.TYPE, PlanetMapPayload.STREAM_CODEC);
        r.playToClient(PlanetMapTilePayload.TYPE, PlanetMapTilePayload.STREAM_CODEC);
        r.playToServer(PlanetMapRequest.TYPE, PlanetMapRequest.STREAM_CODEC, PlanetMaps::handleRequest);
        r.playToClient(TerrainTilePayload.TYPE, TerrainTilePayload.STREAM_CODEC);
        r.playToServer(TerrainTilePayload.Request.TYPE, TerrainTilePayload.Request.STREAM_CODEC,
                TerrainTiles::handleRequest);
    }
}
