package dev.apoapsis.orbit.maneuver;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.Vec3d;
import java.util.List;

/** Burn geometry, post-burn state, and burn timing for maneuver nodes. */
public final class Maneuvers {

    private static final double G0 = 9.80665;

    private Maneuvers() {
    }

    /**
     * World-frame delta-v (m/s) of {@code node} applied to state {@code s}. Radial is radial-out,
     * prograde x normal, so a positive radial burn points away from the body (the KSP convention).
     */
    public static Vec3d worldDeltaV(StateVector s, ManeuverNode node) {
        Vec3d prograde = s.v().normalize();
        Vec3d normal = s.r().cross(s.v()).normalize();
        Vec3d radial = prograde.cross(normal).normalize();
        return prograde.scale(node.prograde())
                .add(normal.scale(node.normal()))
                .add(radial.scale(node.radial()));
    }

    /** State immediately after the burn; position unchanged, velocity increased by the burn. */
    public static StateVector applyAt(StateVector s, ManeuverNode node) {
        return new StateVector(s.r(), s.v().add(worldDeltaV(s, node)));
    }

    /**
     * Propagates {@code s} from {@code ut} to the node, applies the burn, and predicts the trajectory from there.
     */
    public static List<Patch> predictAfter(CelestialBody body, StateVector s, double ut, ManeuverNode node,
            double horizonSeconds, int maxPatches) {
        StateVector atNode = Kepler.propagate(body.mu, s, node.ut() - ut);
        return TrajectoryPredictor.predict(body, applyAt(atNode, node), node.ut(), horizonSeconds, maxPatches);
    }

    /** Burn duration (s) for {@code dv} m/s with constant thrust {@code thrustN} on a ship of {@code massKg}. */
    public static double burnSeconds(double dv, double massKg, double ispS, double thrustN) {
        double ve = ispS * G0;
        return massKg * ve / thrustN * (1 - Math.exp(-dv / ve));
    }

    /** Ignition time (s) that centres the burn on {@code node.ut()}. */
    public static double burnStartUt(ManeuverNode node, double massKg, double ispS, double thrustN) {
        return node.ut() - burnSeconds(node.deltaV(), massKg, ispS, thrustN) / 2;
    }
}
