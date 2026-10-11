package dev.apoapsis.client.map3d;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.Vec3d;
import java.util.ArrayList;
import java.util.List;

/**
 * The ship's predicted coast in 3D, sampled once and reused until the next recompute. Every sample is in the root
 * frame (Verdance at the origin, inertial, metres): each patch's points are its body's centre plus the patch's relative
 * state. The current patch is anchored to its body now; a future patch to its body at the patch's start, as the 2D map
 * does. Also the marks on it: apsides, encounters, impact, escape, and the ascending and descending nodes against the
 * reference plane (the inertial X-Y plane, Pale's orbital plane). Pure orbit maths, no game state. Client only.
 */
public final class OrbitPlot {
    /** Samples per patch, so a patch is SAMPLES segments. */
    public static final int SAMPLES = 320;
    static final int MAX_PATCHES = 3;
    private static final double CLOSED_HORIZON_FACTOR = 1.2;
    private static final double OPEN_HORIZON_S = 4 * 3600.0;
    private static final double HORIZON_CAP_S = 6 * 3600.0;
    /** Below this eccentricity an orbit has no apsides to mark: its direction is undefined. */
    private static final double APSIDAL_MIN_ECC = 1e-6;

    /** What a mark records. */
    public enum Kind { APOAPSIS, PERIAPSIS, ENCOUNTER, ESCAPE, ATMOSPHERE, IMPACT, ASCENDING_NODE, DESCENDING_NODE }

    /**
     * A point of interest. {@code at} is in the root frame at the mark's time; {@code body} is the body entered, for an
     * encounter, else null.
     */
    public record Mark(Kind kind, double ut, Vec3d at, CelestialBody body) {}

    public final List<Patch> patches;
    /** Root-frame samples [patch][sample]. */
    public final double[][] x, y, z;
    /** Start and end universal time of each patch's samples. */
    public final double[] t0, t1;
    /** The patch the ship is on at {@link #nowUt}. */
    public final int current;
    public final double nowUt;
    public final double horizon;
    public final List<Mark> marks;

    private OrbitPlot(List<Patch> patches, double horizon, double nowUt, double[][] x, double[][] y, double[][] z,
            double[] t0, double[] t1, int current, List<Mark> marks) {
        this.patches = patches;
        this.horizon = horizon;
        this.nowUt = nowUt;
        this.x = x;
        this.y = y;
        this.z = z;
        this.t0 = t0;
        this.t1 = t1;
        this.current = current;
        this.marks = marks;
    }

    /**
     * Predicts the coast of a ship around {@code body} in state {@code s} at {@code ut}, and samples it for {@code nowUt}.
     * The horizon is 1.2 periods for a closed orbit, else four hours, never more than six.
     */
    public static OrbitPlot compute(CelestialBody body, StateVector s, double ut, double nowUt) {
        Elements el = Elements.of(body.mu, s);
        boolean closed = el.eccentricity() < 1 && Double.isFinite(el.period()) && el.period() > 0;
        double horizon = Math.min(HORIZON_CAP_S, closed ? CLOSED_HORIZON_FACTOR * el.period() : OPEN_HORIZON_S);
        List<Patch> patches = TrajectoryPredictor.predict(body, s, ut, horizon, MAX_PATCHES);
        int n = patches.size();
        int cur = 0;
        for (int i = 0; i < n; i++) {
            if (patches.get(i).startUt() <= nowUt) {
                cur = i;
            }
        }
        double[][] xs = new double[n][SAMPLES + 1];
        double[][] ys = new double[n][SAMPLES + 1];
        double[][] zs = new double[n][SAMPLES + 1];
        double[] starts = new double[n];
        double[] ends = new double[n];
        for (int i = 0; i < n; i++) {
            Patch patch = patches.get(i);
            double a = patch.startUt();
            double b = Double.isFinite(patch.endUt()) && patch.endUt() >= a ? patch.endUt() : a + horizon;
            starts[i] = a;
            ends[i] = b;
            Vec3d centre = rootPosition(patch.body(), i == cur ? nowUt : a);
            for (int j = 0; j <= SAMPLES; j++) {
                Vec3d r = patch.stateAt(a + (b - a) * j / SAMPLES).r();
                xs[i][j] = centre.x() + r.x();
                ys[i][j] = centre.y() + r.y();
                zs[i][j] = centre.z() + r.z();
            }
        }
        List<Mark> marks = marks(patches, starts, ends, cur, xs, ys, zs);
        return new OrbitPlot(patches, horizon, nowUt, xs, ys, zs, starts, ends, cur, marks);
    }

    private static List<Mark> marks(List<Patch> patches, double[] starts, double[] ends, int cur, double[][] xs,
            double[][] ys, double[][] zs) {
        List<Mark> marks = new ArrayList<>();
        Patch now = patches.get(cur);
        Elements el = Elements.of(now.body().mu, now.startState());
        if (el.eccentricity() < 1 && el.eccentricity() > APSIDAL_MIN_ECC && Double.isFinite(el.period())) {
            Vec3d e = eccentricityVector(now.body().mu, now.startState()).normalize();
            double utAp = now.startUt() + el.timeToApoapsis();
            double utPe = now.startUt() + el.timeToPeriapsis();
            if (utAp <= ends[cur]) {
                Vec3d at = rootPosition(now.body(), utAp).add(e.scale(-el.apoapsisRadius()));
                marks.add(new Mark(Kind.APOAPSIS, utAp, at, null));
            }
            if (utPe <= ends[cur]) {
                Vec3d at = rootPosition(now.body(), utPe).add(e.scale(el.periapsisRadius()));
                marks.add(new Mark(Kind.PERIAPSIS, utPe, at, null));
            }
        }
        for (int i = 0; i < patches.size(); i++) {
            Patch p = patches.get(i);
            if (!Double.isFinite(p.endUt())) {
                continue;
            }
            Vec3d at = rootPosition(p.body(), p.endUt()).add(p.stateAt(p.endUt()).r());
            switch (p.end()) {
                case ENCOUNTER -> {
                    if (i + 1 < patches.size()) {
                        marks.add(new Mark(Kind.ENCOUNTER, p.endUt(), at, patches.get(i + 1).body()));
                    }
                }
                case ESCAPE -> marks.add(new Mark(Kind.ESCAPE, p.endUt(), at, null));
                case ATMOSPHERE -> marks.add(new Mark(Kind.ATMOSPHERE, p.endUt(), at, null));
                case IMPACT -> marks.add(new Mark(Kind.IMPACT, p.endUt(), at, null));
                default -> {
                }
            }
        }
        if (now.body() == MeridianSystem.VERDANCE) {
            double[] X = xs[cur], Y = ys[cur], Z = zs[cur];
            for (int j = 0; j < SAMPLES; j++) {
                if ((Z[j] < 0) == (Z[j + 1] < 0)) {
                    continue;
                }
                double f = Z[j] / (Z[j] - Z[j + 1]);
                double ut = starts[cur] + (ends[cur] - starts[cur]) * (j + f) / SAMPLES;
                Vec3d at = new Vec3d(X[j] + f * (X[j + 1] - X[j]), Y[j] + f * (Y[j + 1] - Y[j]), 0);
                marks.add(new Mark(Z[j] < 0 ? Kind.ASCENDING_NODE : Kind.DESCENDING_NODE, ut, at, null));
            }
        }
        return marks;
    }

    /** Root-frame position of {@code b}'s centre at {@code ut}: the sum of its parents' orbits. */
    public static Vec3d rootPosition(CelestialBody b, double ut) {
        Vec3d r = Vec3d.ZERO;
        for (CelestialBody c = b; c.parent != null; c = c.parent) {
            r = r.add(c.stateAt(ut).r());
        }
        return r;
    }

    /** The ship's root-frame position at {@code ut}, on the current patch. */
    public Vec3d shipRootAt(double ut) {
        Patch p = patches.get(current);
        return rootPosition(p.body(), ut).add(p.stateAt(ut).r());
    }

    /** The ship's state relative to the body it is around, at {@code ut}. */
    public StateVector shipStateAt(double ut) {
        return patches.get(current).stateAt(ut);
    }

    /** The body the ship is around at {@link #nowUt}. */
    public CelestialBody shipBody() {
        return patches.get(current).body();
    }

    /** Eccentricity vector of state {@code s} around a body of gravitational parameter {@code mu}. */
    public static Vec3d eccentricityVector(double mu, StateVector s) {
        Vec3d h = s.r().cross(s.v());
        return s.v().cross(h).scale(1 / mu).sub(s.r().normalize());
    }

    /**
     * Angle between the orbit's normal and the reference plane's normal (+Z), in degrees: the relative inclination of
     * the orbit against Pale's plane (the inertial X-Y plane) for an orbit of Verdance. Zero for an equatorial orbit.
     */
    public static double relativeInclinationDeg(StateVector s) {
        Vec3d h = s.r().cross(s.v());
        double c = Math.max(-1, Math.min(1, h.z() / h.length()));
        return Math.toDegrees(Math.acos(c));
    }
}
