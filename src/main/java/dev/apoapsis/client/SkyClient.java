package dev.apoapsis.client;

import net.neoforged.bus.api.IEventBus;

/**
 * Bodies drawn in the sky: Verdance and Pale from the orbit dimension, Pale from Verdance, Verdance from Pale.
 * Client only. STUB from S0; node SKY implements it.
 */
public final class SkyClient {
    private SkyClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
    }
}
