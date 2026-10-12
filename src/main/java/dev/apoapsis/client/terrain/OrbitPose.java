package dev.apoapsis.client.terrain;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;

/**
 * The piloted ship's position over its body for the far terrain, with the arrival jitter of the orbit state smoothed
 * out. {@code OrbitNow} anchors its Kepler extrapolation at the client tick the latest {@link OrbitStatePayload} arrived
 * in, so each payload (every 5 ticks) moves the extrapolation by however much its trip differed from the last one's, a
 * tick or so of travel. At 700 m/s that is a 35 m pop of the planet every quarter second, which is invisible from far
 * above the ground but shows as the terrain stuttering under the camera below a few kilometres. The payloads carry the
 * universal time they were made at, so the arrival delay is a steady offset between that time and the client's tick
 * clock plus noise: this class keeps a slowly filtered estimate of the offset and extrapolates by the filtered amount.
 * Position only; in the orbit dimension the ship itself is pinned to its slot and the camera's rotation is the
 * ship's own smoothed orientation, which the level's view matrix already carries. Client only; render thread.
 */
final class OrbitPose {
    /** Share of a new payload's offset error taken up per payload: a time constant of about 2 s at 4 payloads a second. */
    private static final double ALPHA = 0.12;
    /** A payload whose offset differs from the filter's by more than this many ticks is a jump, not jitter: start over. */
    private static final double RESET_TICKS = 6.0;
    /** Most game seconds a payload is extrapolated past its arrival, as {@code OrbitNow}. */
    private static final double MAX_GAME_SECONDS = 2.0;
    private static final double TICKS_PER_SECOND = 20.0;

    /** The payload last folded into the filter, and its warp, ship and body (a change of any starts the filter over). */
    private static OrbitStatePayload anchor;
    /** Universal-time offset of the anchor payload and of the filter: {@code ut - warp * clientTime}, seconds. */
    private static double anchorOffset;
    private static double filteredOffset;

    private record Cached(OrbitStatePayload payload, long ticks, float partialTick, Vec3d r) {}

    private static Cached cached;

    private OrbitPose() {}

    /** The ship's position relative to the body's centre now, inertial, metres, with the arrival jitter taken out. */
    static Vec3d position(OrbitStatePayload p, float partialTick) {
        long ticks = ClientState.clientTicks;
        Cached c = cached;
        if (c != null && c.payload == p && c.ticks == ticks && c.partialTick == partialTick) {
            return c.r;
        }
        Vec3d r = compute(p, partialTick);
        cached = new Cached(p, ticks, partialTick, r);
        return r;
    }

    private static Vec3d compute(OrbitStatePayload p, float partialTick) {
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        if (FlightRegime.byOrdinal(p.regime()) != FlightRegime.ORBIT) {
            return s.r();
        }
        double warp = p.warp();
        foldIn(p, warp);
        double ticksSince = Math.max(0, ClientState.clientTicks - ClientState.orbitReceivedTick) + (double) partialTick;
        double seconds = Math.min(ticksSince, MAX_GAME_SECONDS * TICKS_PER_SECOND) / TICKS_PER_SECOND * warp;
        double limit = RESET_TICKS / TICKS_PER_SECOND * Math.max(warp, 1.0);
        double correction = Math.max(-limit, Math.min(limit, filteredOffset - anchorOffset));
        double mu = MeridianSystem.byId(BodyId.byOrdinal(p.body()).id).mu;
        return Kepler.propagate(mu, s, seconds + correction).r();
    }

    /** Takes a newly arrived payload into the offset filter. */
    private static void foldIn(OrbitStatePayload p, double warp) {
        if (p == anchor) {
            return;
        }
        double offset = p.ut() - warp * ClientState.orbitReceivedTick / TICKS_PER_SECOND;
        boolean restart = anchor == null || anchor.shipId() != p.shipId() || anchor.body() != p.body()
                || anchor.warp() != p.warp()
                || Math.abs(offset - filteredOffset) > RESET_TICKS / TICKS_PER_SECOND * Math.max(warp, 1.0);
        anchor = p;
        anchorOffset = offset;
        filteredOffset = restart ? offset : filteredOffset + ALPHA * (offset - filteredOffset);
    }

    /** Forgets the filter (logout, or when the pilot leaves the ship). */
    static void reset() {
        anchor = null;
        cached = null;
    }
}
