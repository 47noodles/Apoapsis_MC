package dev.apoapsis.client;

import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.client.sky.SkyBodies;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Bodies drawn in the sky: Verdance and Pale from the orbit dimension, Pale from Verdance, Verdance from Pale.
 * Client only. The drawing lives in {@link SkyBodies}; the surface maps in {@link PlanetTextures}, which are dropped on
 * logout and when a client level unloads.
 */
public final class SkyClient {
    private SkyClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, SkyBodies::onSubmitCustomGeometry);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> PlanetTextures.clear());
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, e -> {
            if (e.getLevel() instanceof ClientLevel) {
                PlanetTextures.clear();
            }
        });
    }
}
