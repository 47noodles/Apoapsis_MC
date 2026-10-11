package dev.apoapsis.client.map;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.net.OrbitStatePayload;

/**
 * The piloted ship's latest state and the universal-time extrapolation between payloads. {@link ClientState} stamps
 * when each payload arrives, in client ticks, so this class only reads it. Client only; call from the render thread.
 */
final class OrbitClock {
    private OrbitClock() {}

    /** The latest payload for the piloted ship, or null when not piloting. */
    static OrbitStatePayload current() {
        return ClientState.orbit;
    }

    /** Universal time now, seconds: the payload's ut advanced by the client ticks since it arrived, at its warp. */
    static double nowUt(OrbitStatePayload p, float partialTick) {
        long ticks = Math.max(0, ClientState.clientTicks - ClientState.orbitReceivedTick);
        return p.ut() + (ticks + partialTick) * p.warp() / 20.0;
    }
}
