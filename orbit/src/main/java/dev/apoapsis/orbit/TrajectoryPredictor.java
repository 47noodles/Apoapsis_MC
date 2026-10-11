package dev.apoapsis.orbit;

import java.util.ArrayList;
import java.util.List;

/**
 * Patched-conic prediction. Each patch is a two-body arc around one body. The march samples the state at a step chosen
 * from the state at each sample, detects events by sign change, and bisects each event time to 1e-3 s. Every sample is
 * propagated analytically from the patch start, so steps never accumulate error.
 */
public final class TrajectoryPredictor {
    /** Longest horizon a caller can get: 24 h. A longer request is predicted only to this far. */
    public static final double MAX_HORIZON_SECONDS = 86_400;
    /**
     * Most march samples in one patch. A patch that uses them all ends with NONE at its last sample, which may be
     * before the horizon. Bisection steps are not counted.
     */
    public static final int MAX_STEPS_PER_PATCH = 20_000;

    private static final double MIN_STEP = 1;
    private static final double MAX_STEP = 600;
    private static final double BISECTION_TOLERANCE = 1e-3;
    /** Hard bound on bisection iterations; 64 halvings cover any interval the predictor uses. */
    private static final int MAX_BISECTION_STEPS = 64;
    private static final double STEPS_PER_PERIOD = 720;
    private static final double STEPS_PER_RADIAN_OF_MOTION = 50;

    private TrajectoryPredictor() {}

    /** An event that can end a patch, with the other body involved (the child for ENCOUNTER). */
    private record Check(Patch.EndReason kind, CelestialBody other) {}

    /** The first event found on a patch, with its time. NONE means the patch ended at {@code time} without an event. */
    private record Hit(Patch.EndReason kind, CelestialBody other, double time) {}

    /**
     * Predicts the coasting trajectory from state {@code s} around {@code body} at {@code ut}, switching bodies at SOI
     * boundaries, for up to {@code horizonSeconds} and {@code maxPatches} patches. Bodies considered: {@code body},
     * its parent, and its children among the MeridianSystem bodies.
     *
     * <p>Limits: the horizon is clamped to {@link #MAX_HORIZON_SECONDS} (24 h). Each patch takes at most
     * {@link #MAX_STEPS_PER_PATCH} march samples; a patch that runs out ends with NONE at its last sample, so the
     * prediction can stop short of the horizon.
     *
     * <p>If {@code s} lies inside a child's SOI, the prediction starts around that child at {@code ut} (no patch is
     * added for the switch).
     *
     * <p>The last patch ends with NONE at the horizon, or at a step-limit stop as above. If IMPACT or ATMOSPHERE ends
     * the prediction, the last patch carries that reason. If {@code maxPatches} is reached at an SOI change, the last
     * patch ends with NONE at that change time (the next patch is not built). The result always holds at least one
     * patch.
     */
    public static List<Patch> predict(CelestialBody body, StateVector s, double ut, double horizonSeconds,
                                      int maxPatches) {
        int cap = Math.max(1, maxPatches);
        double span = horizonSeconds > 0 ? Math.min(horizonSeconds, MAX_HORIZON_SECONDS) : 0;
        double horizonEnd = ut + span;
        List<Patch> patches = new ArrayList<>();
        CelestialBody cur = body;
        StateVector start = s;
        double startUt = ut;
        CelestialBody exited = null;
        while (true) {
            // A start state inside a child's SOI belongs to that child. The child just left is skipped: a state at its
            // boundary can round to just inside, and switching straight back would make a spurious patch.
            CelestialBody inside;
            while ((inside = childContaining(cur, start, startUt, exited)) != null) {
                StateVector c = inside.stateAt(startUt);
                start = new StateVector(start.r().sub(c.r()), start.v().sub(c.v()));
                cur = inside;
                exited = null;
            }
            Hit hit = firstEvent(cur, start, startUt, horizonEnd);
            if (hit.kind() == Patch.EndReason.NONE) {
                patches.add(new Patch(cur, startUt, hit.time(), start, Patch.EndReason.NONE));
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
                exited = null;
            } else {
                StateVector parent = cur.stateAt(hit.time());
                start = new StateVector(atEvent.r().add(parent.r()), atEvent.v().add(parent.v()));
                exited = cur;
                cur = cur.parent;
            }
            startUt = hit.time();
        }
    }

    /**
     * Marches one patch from {@code startUt} to {@code end} and returns the earliest event. Returns NONE at {@code end}
     * if no event occurs, or at the last sample reached if the patch uses {@link #MAX_STEPS_PER_PATCH} samples first.
     * An event is a predicate turning true between two samples (from above to below for atmosphere, impact and
     * encounter; from inside to outside for escape).
     */
    private static Hit firstEvent(CelestialBody cur, StateVector start, double startUt, double end) {
        List<Check> checks = checksFor(cur);
        boolean[] previous = new boolean[checks.size()];
        for (int i = 0; i < checks.size(); i++) {
            previous[i] = holdsAt(cur, checks.get(i), start, startUt);
        }
        double t = startUt;
        StateVector state = start;
        for (int steps = 0; t < end; steps++) {
            if (steps == MAX_STEPS_PER_PATCH) {
                return new Hit(Patch.EndReason.NONE, null, t);
            }
            double next = Math.min(end, t + stepFor(cur, state));
            StateVector sample = Kepler.propagate(cur.mu, start, next - startUt);
            Hit best = null;
            for (int i = 0; i < checks.size(); i++) {
                Check check = checks.get(i);
                boolean now = holdsAt(cur, check, sample, next);
                if (!previous[i] && now) {
                    double eventTime = bisect(cur, check, start, startUt, t, next);
                    if (best == null || eventTime < best.time()) {
                        best = new Hit(check.kind(), check.other(), eventTime);
                    }
                }
                previous[i] = now;
            }
            if (best != null) {
                return best;
            }
            t = next;
            state = sample;
        }
        return new Hit(Patch.EndReason.NONE, null, end);
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

    /**
     * Step to the next sample, chosen from the state {@code s} at the current sample: |r|/|v|/50, clamped to [1 s,
     * 600 s]. For an elliptic orbit it is also at most period/720, but only when that bound is below 600 s.
     */
    private static double stepFor(CelestialBody cur, StateVector s) {
        double rMag = s.r().length();
        double vMag = s.v().length();
        double dt = vMag > 0 ? rMag / vMag / STEPS_PER_RADIAN_OF_MOTION : Double.POSITIVE_INFINITY;
        if (Double.isNaN(dt)) {
            return MIN_STEP;
        }
        Elements elements = Elements.of(cur.mu, s);
        if (elements.eccentricity() < 1) {
            double periodStep = elements.period() / STEPS_PER_PERIOD;
            if (periodStep < MAX_STEP) {
                dt = Math.min(dt, periodStep);
            }
        }
        return Math.max(MIN_STEP, Math.min(MAX_STEP, dt));
    }

    /**
     * The child of {@code cur} whose SOI holds {@code s} at {@code ut}, ignoring {@code exclude}; null if there is none.
     */
    private static CelestialBody childContaining(CelestialBody cur, StateVector s, double ut, CelestialBody exclude) {
        for (CelestialBody child : MeridianSystem.children(cur)) {
            if (child != exclude && s.r().sub(child.stateAt(ut).r()).length() < child.soiRadius()) {
                return child;
            }
        }
        return null;
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
        // The cap bounds the loop when ut is so large that adjacent doubles are wider than the tolerance.
        for (int i = 0; i < MAX_BISECTION_STEPS && high - low > BISECTION_TOLERANCE; i++) {
            double mid = 0.5 * (low + high);
            if (mid <= low || mid >= high) {
                break;
            }
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
