package dev.apoapsis.client;

import dev.apoapsis.net.AgeSyncPayload;
import dev.apoapsis.net.OrbitStatePayload;

/**
 * Latest server data on the client. Plain data with no client-only imports, so common code may read it safely.
 * Owned by S0 and INT. {@link #clientTicks} is advanced by {@code MapClient}.
 */
public final class ClientState {
    private ClientState() {}

    /** Latest orbit state for the ship the local player pilots, or null when not piloting. */
    public static volatile OrbitStatePayload orbit;
    /** Client ticks (not paused) when {@link #orbit} arrived; compare with {@link #clientTicks}. */
    public static volatile long orbitReceivedTick;
    /** Client ticks (not paused) since the client started. */
    public static volatile long clientTicks;
    /** The local player's Age, 0..6. */
    public static volatile int age;

    public static void accept(OrbitStatePayload p) {
        orbitReceivedTick = clientTicks;
        orbit = p;
    }

    /** Forgets the orbit: the local player no longer pilots a ship, or has left the level or the server. */
    public static void clearOrbit() {
        orbit = null;
    }

    public static void accept(AgeSyncPayload p) {
        age = p.age();
    }
}
