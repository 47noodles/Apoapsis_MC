package dev.apoapsis.client;

import net.neoforged.bus.api.IEventBus;

/**
 * Pilot input (key mappings, ShipInputPayload sending, drop-key suppression), the ship entity renderer, camera.
 * Client only. STUB from S0; node SHIPC implements it.
 */
public final class ShipClient {
    private ShipClient() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
    }
}
