package dev.apoapsis.orbit.maneuver;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Finds the prograde burn that sends a ship from a parking orbit around a moon back to the parent body, with the
 * parent-side periapsis at a chosen height (for a return into the parent's atmosphere). The burn is searched over a
 * window of burn times with the same patched-conic predictor the map uses, so the plan and the map agree. Pure maths,
 * no game state: the test driver and the unit tests share it.
 */
public final class EscapeSearch {
    /** Largest prograde burn tried, m/s. */
    public static final double MAX_DV_MPS = 200.0;
    /** Resolution of the burn size, m/s. */
    public static final double DV_TOLERANCE_MPS = 0.02;
    /** How far ahead each candidate is predicted, s. */
    public static final double HORIZON_S = 6 * 3600.0;
    /** Weight of the burn size against the longitude error in {@link #score}: radians per m/s. */
    private static final double DV_WEIGHT_RAD_PER_MPS = 1e-3;

    private EscapeSearch() {}

    /**
     * One escape plan.
     *
     * @param burnUt         universal time of the burn, s
     * @param deltaV         prograde burn, m/s
     * @param periapsisAltM  parent-side periapsis height above the parent's radius after the burn, m
     * @param entryUt        universal time the ship reaches the parent's atmosphere (or surface) top, s
     * @param entryLonRad    longitude (angle from +X in the parent's inertial frame) of that point, rad
     */
    public record Plan(double burnUt, double deltaV, double periapsisAltM, double entryUt, double entryLonRad) {}

    /**
     * Height above the parent's radius of the periapsis of the parent-side patch the coasting state leaves {@code moon}
     * for, or NaN if the state does not leave the moon's sphere of influence within {@link #HORIZON_S}.
     *
     * @param moon the body the state is relative to; its parent is the body returned to
     * @param s    state relative to the moon, inertial frame
     * @param ut   universal time of the state, s
     */
    public static double parentPeriapsisAltM(CelestialBody moon, StateVector s, double ut) {
        Patch patch = parentPatch(moon, s, ut);
        if (patch == null) {
            return Double.NaN;
        }
        return Elements.of(moon.parent.mu, patch.startState()).periapsisRadius() - moon.parent.radius;
    }

    /** The first predicted patch around {@code moon}'s parent, or null. */
    private static Patch parentPatch(CelestialBody moon, StateVector s, double ut) {
        if (moon.parent == null) {
            return null;
        }
        List<Patch> patches = TrajectoryPredictor.predict(moon, s, ut, HORIZON_S, 3);
        for (Patch p : patches) {
            if (p.body() == moon.parent) {
                return p;
            }
        }
        return null;
    }

    /**
     * The smallest prograde burn at time {@code burnUt} that puts the parent-side periapsis at or below
     * {@code targetPeAltM}, or null if no burn up to {@link #MAX_DV_MPS} does. The state is coasting (no thrust) from
     * {@code ut} to the burn.
     */
    public static Plan planAt(CelestialBody moon, StateVector s, double ut, double burnUt, double targetPeAltM) {
        StateVector atBurn = Kepler.propagate(moon.mu, s, burnUt - ut);
        if (!reaches(moon, atBurn, burnUt, MAX_DV_MPS, targetPeAltM)) {
            return null;
        }
        double lo = 0;
        double hi = MAX_DV_MPS;
        while (hi - lo > DV_TOLERANCE_MPS) {
            double mid = 0.5 * (lo + hi);
            if (reaches(moon, atBurn, burnUt, mid, targetPeAltM)) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        StateVector after = Maneuvers.applyAt(atBurn, new ManeuverNode(burnUt, hi, 0, 0));
        Patch parent = parentPatch(moon, after, burnUt);
        if (parent == null) {
            return null;
        }
        double pe = Elements.of(moon.parent.mu, parent.startState()).periapsisRadius() - moon.parent.radius;
        StateVector entry = parent.stateAt(parent.endUt());
        return new Plan(burnUt, hi, pe, parent.endUt(), Math.atan2(entry.r().y(), entry.r().x()));
    }

    private static boolean reaches(CelestialBody moon, StateVector atBurn, double burnUt, double dv, double targetPeAltM) {
        StateVector after = Maneuvers.applyAt(atBurn, new ManeuverNode(burnUt, dv, 0, 0));
        double pe = parentPeriapsisAltM(moon, after, burnUt);
        return pe <= targetPeAltM;
    }

    /**
     * Searches burn times from {@code fromUt} to {@code toUt} (coarse steps of {@code stepS}, then a fine pass around the
     * best) and returns the best plan, or null when no time in the window works. With a finite {@code lonTargetRad} the
     * best plan is the one whose parent-side entry longitude is closest to it (a small burn size breaks ties); with NaN
     * it is the smallest burn.
     *
     * @param moon          body the state is relative to
     * @param s             state relative to the moon at {@code ut}
     * @param ut            universal time of the state, s
     * @param fromUt        earliest burn time, s
     * @param toUt          latest burn time, s
     * @param stepS         coarse step between candidate burn times, s
     * @param targetPeAltM  wanted parent-side periapsis height, m
     * @param lonTargetRad  wanted entry longitude, rad, or NaN for none
     * @param maxDvMps      plans that need a bigger burn than this are ignored
     */
    public static Plan find(CelestialBody moon, StateVector s, double ut, double fromUt, double toUt, double stepS,
                            double targetPeAltM, double lonTargetRad, double maxDvMps) {
        DoubleUnaryOperator error = Double.isNaN(lonTargetRad) ? lon -> 0
                : lon -> Math.abs(Math.IEEEremainder(lon - lonTargetRad, 2 * Math.PI));
        return find(moon, s, ut, fromUt, toUt, stepS, targetPeAltM, error, maxDvMps);
    }

    /**
     * As {@link #find(CelestialBody, StateVector, double, double, double, double, double, double, double)} with the
     * entry longitude judged by {@code lonErrorRad} instead of the distance to one target: it maps an entry longitude
     * (rad, from +X) to a penalty in radians, 0 for a good one (for instance the distance to the nearest dry landing
     * site). The best plan has the smallest penalty, a small burn size breaking ties.
     */
    public static Plan find(CelestialBody moon, StateVector s, double ut, double fromUt, double toUt, double stepS,
                            double targetPeAltM, DoubleUnaryOperator lonErrorRad, double maxDvMps) {
        Plan best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (double t = fromUt; t <= toUt; t += stepS) {
            Plan p = planAt(moon, s, ut, t, targetPeAltM);
            if (p != null && p.deltaV() <= maxDvMps && score(p, lonErrorRad) < bestScore) {
                best = p;
                bestScore = score(p, lonErrorRad);
            }
        }
        if (best == null) {
            return null;
        }
        double fine = stepS / 10;
        double centre = best.burnUt();
        for (double t = Math.max(fromUt, centre - stepS); t <= Math.min(toUt, centre + stepS); t += fine) {
            Plan p = planAt(moon, s, ut, t, targetPeAltM);
            if (p != null && p.deltaV() <= maxDvMps && score(p, lonErrorRad) < bestScore) {
                best = p;
                bestScore = score(p, lonErrorRad);
            }
        }
        return best;
    }

    /** Lower is better: the entry longitude penalty in radians plus a small burn-size term. */
    static double score(Plan p, DoubleUnaryOperator lonErrorRad) {
        return lonErrorRad.applyAsDouble(p.entryLonRad()) + DV_WEIGHT_RAD_PER_MPS * p.deltaV();
    }
}
