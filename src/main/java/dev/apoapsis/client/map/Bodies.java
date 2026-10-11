package dev.apoapsis.client.map;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;

/**
 * Body lookup and frame helpers. Plots use the root body's inertial frame (Verdance at the origin); a child body
 * is offset by the sum of its parents' orbits. Client only.
 */
final class Bodies {
    private Bodies() {}

    /** The central body named by a state payload. */
    static CelestialBody of(OrbitStatePayload p) {
        return MeridianSystem.byId(BodyId.byOrdinal(p.body()).id);
    }

    /** The payload's state, relative to its central body. */
    static StateVector stateOf(OrbitStatePayload p) {
        return new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
    }

    /** Position and velocity of {@code body}'s centre relative to the root body at {@code ut}. */
    static StateVector rootState(CelestialBody body, double ut) {
        Vec3d r = Vec3d.ZERO;
        Vec3d v = Vec3d.ZERO;
        for (CelestialBody b = body; b.parent != null; b = b.parent) {
            StateVector s = b.stateAt(ut);
            r = r.add(s.r());
            v = v.add(s.v());
        }
        return new StateVector(r, v);
    }

    /** Air pressure as a fraction of sea level at altitude {@code altM} (metres): the engines' p/p0. */
    static double pressureFraction(CelestialBody body, double altM) {
        if (!(body.seaLevelDensity > 0)) {
            return 0;
        }
        return body.density(Math.max(0, altM)) / body.seaLevelDensity;
    }
}
