package dev.apoapsis.orbit;

import java.util.ArrayList;
import java.util.List;

/**
 * Patched-conic prediction. Each patch is a two-body arc around one body. The march samples the state every step and
 * detects events by sign change; each event time is then bisected to 1e-3 s. Every sample is propagated analytically
 * from the patch start, so steps never accumulate error.
 */
public final class TrajectoryPredictor {
    private static final double MIN_STEP = 1;
    private static final double MAX_STEP = 60;
    private static final double BISECTION_TOLERANCE = 1e-3;
    private static final double STEPS_PER_PERIOD = 720;
    private static final double STEPS_PER_RADIAN_OF_MOTION = 50;

    private TrajectoryPredictor() {}

    /** An event that can end a patch, with the other body involved (the child for ENCOUNTER). */
    private record Check(Patch.EndReason kind, CelestialBody other) {}

    /** The first event found on a patch, with its time. */
    private record Hit(Patch.EndReason kind, CelestialBody other, double time) {}

    /**
     * Predicts the coasting trajectory from state {@code s} around {@code body} at {@code ut}, switching bodies at SOI
     * boundaries, for up to {@code horizonSeconds} and {@code maxPatches} patches. Bodies considered: {@code body},
     * its parent, and its children among the MeridianSystem bodies.
     *
     * <p>The last patch ends with NONE at the horizon. If IMPACT or ATMOSPHERE ends the prediction, the last patch
     * carries that reason. If {@code maxPatches} is reached at an SOI change, the last patch ends with NONE at that
     * change time (the next patch is not built). The result always holds at least one patch.
     */
    public static List<Patch> predict(CelestialBody body, StateVector s, double ut, double horizonSeconds,
                                      int maxPatches) {
        int cap = Math.max(1, maxPatches);
        double horizonEnd = ut + Math.max(0, horizonSeconds);
        List<Patch> patches = new ArrayList<>();
        CelestialBody cur = body;
        StateVector start = s;
        double startUt = ut;
        while (true) {
            Hit hit = firstEvent(cur, start, startUt, horizonEnd);
            if (hit == null) {
                patches.add(new Patch(cur, startUt, horizonEnd, start, Patch.EndReason.NONE));
                return patches;
            }
            if (hit.kind() == Patch.EndReason.IMPACT || hit.kind() == Patch.EndReason.ATMOSPHERE) {
                patches.add(new Patch(cur, startUt, hit.time(), start, hit.kind()));
                return patches;
            }
            if (patches.size() + 1 >= cap) {
                patches.add(new Patch(cur, startUt, hit.time(), start, Patch.EndReason.NONE));
                return patches;
            }
            patches.add(new Patch(cur, startUt, hit.time(), start, hit.kind()));

            StateVector atEvent = Kepler.propagate(cur.mu, start, hit.time() - startUt);
            if (hit.kind() == Patch.EndReason.ENCOUNTER) {
                StateVector child = hit.other().stateAt(hit.time());
                start = new StateVector(atEvent.r().sub(child.r()), atEvent.v().sub(child.v()));
                cur = hit.other();
            } else {
                StateVector parent = cur.stateAt(hit.time());
                start = new StateVector(atEvent.r().add(parent.r()), atEvent.v().add(parent.v()));
                cur = cur.parent;
            }
            startUt = hit.time();
        }
    }

    /**
     * Marches one patch from {@code startUt} to {@code end} and returns the earliest event, or null if none occurs.
     * An event is a predicate turning true between two samples (from above to below for atmosphere, impact and
     * encounter; from inside to outside for escape).
     */
    private static Hit firstEvent(CelestialBody cur, StateVector start, double startUt, double end) {
        double span = end - startUt;
        if (!(span > 0)) {
            return null;
        }
        List<Check> checks = checksFor(cur);
        boolean[] previous = new boolean[checks.size()];
        for (int i = 0; i < checks.size(); i++) {
            previous[i] = holdsAt(cur, checks.get(i), start, startUt);
        }
        double dt = stepFor(cur, start);
        int steps = (int) Math.ceil(span / dt);
        double previousTime = startUt;
        for (int k = 1; k <= steps; k++) {
            double t = k == steps ? end : startUt + k * dt;
            StateVector sample = Kepler.propagate(cur.mu, start, t - startUt);
            Hit best = null;
            for (int i = 0; i < checks.size(); i++) {
                Check check = checks.get(i);
                boolean now = holdsAt(cur, check, sample, t);
                if (!previous[i] && now) {
                    double eventTime = bisect(cur, check, start, startUt, previousTime, t);
                    if (best == null || eventTime < best.time()) {
                        best = new Hit(check.kind(), check.other(), eventTime);
                    }
                }
                previous[i] = now;
            }
            if (best != null) {
                return best;
            }
            previousTime = t;
        }
        return null;
    }

    /** Events that can end a patch around {@code cur}: impact, atmosphere, escape and one encounter per child. */
    private static List<Check> checksFor(CelestialBody cur) {
        List<Check> checks = new ArrayList<>();
        checks.add(new Check(Patch.EndReason.IMPACT, null));
        if (cur.atmosphereTop > 0) {
            checks.add(new Check(Patch.EndReason.ATMOSPHERE, null));
        }
        if (cur.parent != null) {
            checks.add(new Check(Patch.EndReason.ESCAPE, null));
        }
        for (CelestialBody child : MeridianSystem.children(cur)) {
            checks.add(new Check(Patch.EndReason.ENCOUNTER, child));
        }
        return checks;
    }

    /** Step size: min(period/720, |r|/|v|/50) for elliptic orbits, |r|/|v|/50 otherwise, clamped to [1, 60] s. */
    private static double stepFor(CelestialBody cur, StateVector start) {
        double rMag = start.r().length();
        double vMag = start.v().length();
        double dt = vMag > 0 ? rMag / vMag / STEPS_PER_RADIAN_OF_MOTION : Double.POSITIVE_INFINITY;
        Elements elements = Elements.of(cur.mu, start);
        if (elements.eccentricity() < 1) {
            dt = Math.min(dt, elements.period() / STEPS_PER_PERIOD);
        }
        if (Double.isNaN(dt)) {
            return MIN_STEP;
        }
        return Math.max(MIN_STEP, Math.min(MAX_STEP, dt));
    }

    /** Predicate of {@code check} for a state {@code sample} relative to {@code cur} at time {@code t}. */
    private static boolean holdsAt(CelestialBody cur, Check check, StateVector sample, double t) {
        double rMag = sample.r().length();
        return switch (check.kind()) {
            case IMPACT -> rMag < cur.radius;
            case ATMOSPHERE -> rMag - cur.radius < cur.atmosphereTop;
            case ESCAPE -> rMag > cur.soiRadius();
            case ENCOUNTER -> sample.r().sub(check.other().stateAt(t).r()).length() < check.other().soiRadius();
            case NONE -> false;
        };
    }

    /** Bisects a false-to-true transition in [lo, hi] down to 1e-3 s; returns the first time the predicate holds. */
    private static double bisect(CelestialBody cur, Check check, StateVector start, double startUt, double lo,
                                 double hi) {
        double low = lo;
        double high = hi;
        while (high - low > BISECTION_TOLERANCE) {
            double mid = 0.5 * (low + high);
            StateVector sample = Kepler.propagate(cur.mu, start, mid - startUt);
            if (holdsAt(cur, check, sample, mid)) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return high;
    }
}
