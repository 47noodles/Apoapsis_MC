package dev.apoapsis.client;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;

/**
 * The piloted ship's state and universal time at the render instant. The server sends an {@link OrbitStatePayload}
 * every 5 ticks (about 4 Hz); this class advances the latest one to now by the client ticks since it arrived, at its
 * warp. In the ORBIT regime the state is propagated analytically with {@link Kepler}; in the SURFACE regime the
 * payload's own state is returned, since the ship's entity position is already smooth there. Read by the sky and the
 * flight HUD so both move every frame. Client only; call from the render thread.
 */
public final class OrbitNow {
    /** Most game time, seconds, a payload is extrapolated past its arrival, so a stale payload cannot run away. */
    private static final double MAX_GAME_SECONDS = 2.0;
    private static final double TICKS_PER_SECOND = 20.0;

    /** The last propagated state, reused while the payload, client tick and partial tick are all unchanged. */
    private record Cached(OrbitStatePayload payload, long ticks, float partialTick, StateVector state) {}

    private static Cached cached;

    private OrbitNow() {}

    /** Universal time now for payload {@code p}, seconds: its ut advanced by the client ticks since it arrived, at its warp. */
    public static double utNow(OrbitStatePayload p, float partialTick) {
        return p.ut() + sinceArrival(partialTick) * p.warp() / TICKS_PER_SECOND;
    }

    /**
     * The state now for payload {@code p}, relative to its central body. Cached per payload, client tick and partial
     * tick, so the sky and the HUD in one frame share one propagation.
     */
    public static StateVector stateNow(OrbitStatePayload p, float partialTick) {
        long ticks = ClientState.clientTicks;
        Cached c = cached;
        if (c != null && c.payload == p && c.ticks == ticks && c.partialTick == partialTick) {
            return c.state;
        }
        StateVector state = propagate(p, partialTick);
        cached = new Cached(p, ticks, partialTick, state);
        return state;
    }

    /** The payload's state advanced to now. Only the ORBIT regime moves. */
    private static StateVector propagate(OrbitStatePayload p, float partialTick) {
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        if (FlightRegime.byOrdinal(p.regime()) != FlightRegime.ORBIT) {
            return s;
        }
        return Kepler.propagate(bodyOf(p).mu, s, sinceArrival(partialTick) * p.warp() / TICKS_PER_SECOND);
    }

    /** Client ticks since the latest payload arrived, plus the partial tick: never negative, at most 2 s of game time. */
    private static double sinceArrival(float partialTick) {
        double ticks = Math.max(0, ClientState.clientTicks - ClientState.orbitReceivedTick) + (double) partialTick;
        return Math.min(ticks, MAX_GAME_SECONDS * TICKS_PER_SECOND);
    }

    private static CelestialBody bodyOf(OrbitStatePayload p) {
        return MeridianSystem.byId(BodyId.byOrdinal(p.body()).id);
    }
}
