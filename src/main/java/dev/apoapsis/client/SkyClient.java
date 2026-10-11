package dev.apoapsis.client;

import dev.apoapsis.client.sky.SkyBodies;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bodies drawn in the sky: Verdance and Pale from the orbit dimension, Pale from Verdance, Verdance from Pale.
 * Client only. The drawing lives in {@link SkyBodies}.
 */
public final class SkyClient {
    private SkyClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, SkyBodies::onSubmitCustomGeometry);
    }
}
