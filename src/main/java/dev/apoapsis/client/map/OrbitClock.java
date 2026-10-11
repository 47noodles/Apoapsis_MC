package dev.apoapsis.client.map;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.net.OrbitStatePayload;

/**
 * The piloted ship's latest payload, and the universal time the map draws at. The extrapolation lives in
 * {@link OrbitNow}, which the sky and the flight HUD share. Client only; call from the render thread.
 */
final class OrbitClock {
    private OrbitClock() {}

    /** The latest payload for the piloted ship, or null when not piloting. */
    static OrbitStatePayload current() {
        return ClientState.orbit;
    }

    /** Universal time now, seconds: the payload's ut advanced by the client ticks since it arrived, at its warp. */
    static double nowUt(OrbitStatePayload p, float partialTick) {
        return OrbitNow.utNow(p, partialTick);
    }
}
