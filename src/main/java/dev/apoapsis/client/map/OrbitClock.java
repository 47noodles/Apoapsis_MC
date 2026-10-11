package dev.apoapsis.client.map;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.net.OrbitStatePayload;
import net.minecraft.client.Minecraft;

/**
 * Arrival tracking for the piloted ship's state and the universal-time extrapolation between payloads.
 * ClientState does not stamp {@code orbitReceivedTick}, so this class notes the client tick at which each payload
 * object first appears. Client only; call from the render thread.
 */
final class OrbitClock {
    private static OrbitStatePayload seen;
    private static long seenTick;

    private OrbitClock() {}

    /** The latest payload for the piloted ship, or null when not piloting. Records when a new one first shows. */
    static OrbitStatePayload current() {
        OrbitStatePayload p = ClientState.orbit;
        Minecraft mc = Minecraft.getInstance();
        if (p == null || mc.level == null) {
            seen = null;
            return null;
        }
        if (p != seen) {
            seen = p;
            seenTick = mc.level.getGameTime();
        }
        return p;
    }

    /** Universal time now, seconds: the payload's ut advanced by the client ticks since it arrived, at its warp. */
    static double nowUt(OrbitStatePayload p, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        double elapsedTicks = mc.level == null ? 0 : (mc.level.getGameTime() - seenTick) + partialTick;
        return p.ut() + Math.max(0, elapsedTicks) * p.warp() / 20.0;
    }
}
