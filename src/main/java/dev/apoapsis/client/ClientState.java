package dev.apoapsis.client;

import dev.apoapsis.net.AgeSyncPayload;
import dev.apoapsis.net.OrbitStatePayload;

/**
 * Latest server data on the client. Plain data with no client-only imports, so common code may read it safely.
 * Owned by S0 and INT.
 */
public final class ClientState {
    private ClientState() {}

    /** Latest orbit state for the ship the local player pilots, or null when not piloting. */
    public static volatile OrbitStatePayload orbit;
    /** Client-side game time (ticks) when {@link #orbit} arrived. */
    public static volatile long orbitReceivedTick;
    /** The local player's Age, 0..6. */
    public static volatile int age;

    public static void accept(OrbitStatePayload p) {
        orbit = p;
    }

    public static void accept(AgeSyncPayload p) {
        age = p.age();
    }
}
