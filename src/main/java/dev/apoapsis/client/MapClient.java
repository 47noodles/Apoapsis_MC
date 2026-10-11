package dev.apoapsis.client;

import net.neoforged.bus.api.IEventBus;

/**
 * The full-screen system map (M) and the flight HUD layer.
 * Client only. STUB from S0; node MAP implements it.
 */
public final class MapClient {
    private MapClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
    }
}
