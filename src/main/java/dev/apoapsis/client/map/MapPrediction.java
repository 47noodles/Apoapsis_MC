package dev.apoapsis.client.map;

import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A ship's predicted trajectory for the map: patches, each sampled as a polyline in the root body's inertial XY
 * plane (metres), plus the marks on it (apsides, encounters, atmosphere entry, impact). Immutable after
 * {@link #compute}. Client only.
 */
final class MapPrediction {
    /** Upper bound on samples per patch. */
    static final int MAX_SAMPLES = 2000;
    private static final int MIN_SAMPLES = 32;
    private static final int COARSE_SAMPLES = 64;
    private static final int MAX_PATCHES = 3;
    private static final double CLOSED_HORIZON_FACTOR = 1.2;
    private static final double OPEN_HORIZON_S = 4 * 3600.0;

    /** What a mark records. */
    enum Kind { APOAPSIS, PERIAPSIS, ENCOUNTER, ATMOSPHERE, IMPACT }

    /**
     * A point of interest on the prediction.
     *
     * @param ut    universal time of the mark, s
     * @param x     root-frame position of the mark, m
     * @param y     root-frame position of the mark, m
     * @param cx    root-frame centre of the body the mark is measured from (apsides), m
     * @param cy    root-frame centre of the body the mark is measured from (apsides), m
     * @param altM  altitude above that body's surface (apsides), m
     * @param body  id of the body entered (encounters), else empty
     */
    record Mark(Kind kind, double ut, double x, double y, double cx, double cy, double altM, String body) {}

    /** The payload this prediction was made from. */
    final OrbitStatePayload source;
    private final List<Patch> patches;
    private final double[][] xs;
    private final double[][] ys;
    private final List<Mark> marks;

    private MapPrediction(OrbitStatePayload source, List<Patch> patches, double[][] xs, double[][] ys,
                          List<Mark> marks) {
        this.source = source;
        this.patches = patches;
        this.xs = xs;
        this.ys = ys;
        this.marks = marks;
    }

    /**
     * Predicts the ship's coast from {@code p} and samples it for a view at {@code pxPerMetre}. The horizon is 1.2
     * periods for a closed orbit, otherwise four hours.
     */
    static MapPrediction compute(OrbitStatePayload p, double pxPerMetre) {
        CelestialBody body = Bodies.of(p);
        StateVector s = Bodies.stateOf(p);
        Elements el = Elements.of(body.mu, s);
        double horizon = isClosed(el) ? CLOSED_HORIZON_FACTOR * el.period() : OPEN_HORIZON_S;
        List<Patch> patches = TrajectoryPredictor.predict(body, s, p.ut(), horizon, MAX_PATCHES);
        int n = patches.size();
        double[][] xs = new double[n][];
        double[][] ys = new double[n][];
        for (int i = 0; i < n; i++) {
            Patch patch = patches.get(i);
            double t0 = patch.startUt();
            double t1 = endUt(patch, horizon);
            int count = sampleCount(patch, t0, t1, pxPerMetre);
            xs[i] = new double[count + 1];
            ys[i] = new double[count + 1];
            for (int j = 0; j <= count; j++) {
                Vec3d q = rootState(patch, t0 + (t1 - t0) * j / count).r();
                xs[i][j] = q.x();
                ys[i][j] = q.y();
            }
        }
        return new MapPrediction(p, patches, xs, ys, marks(patches, horizon));
    }

    private static boolean isClosed(Elements el) {
        return el.eccentricity() < 1 && Double.isFinite(el.period()) && el.period() > 0;
    }

    private static double endUt(Patch patch, double horizon) {
        double t1 = patch.endUt();
        return Double.isFinite(t1) && t1 >= patch.startUt() ? t1 : patch.startUt() + horizon;
    }

    /** Samples about one per two screen pixels of arc, from a coarse pass over the patch. */
    private static int sampleCount(Patch patch, double t0, double t1, double pxPerMetre) {
        double length = 0;
        Vec3d prev = null;
        for (int j = 0; j <= COARSE_SAMPLES; j++) {
            Vec3d q = rootState(patch, t0 + (t1 - t0) * j / COARSE_SAMPLES).r();
            if (prev != null) {
                length += Math.hypot(q.x() - prev.x(), q.y() - prev.y());
            }
            prev = q;
        }
        double px = length * pxPerMetre;
        if (!(px > 0)) {
            return MIN_SAMPLES;
        }
        return (int) Math.min(MAX_SAMPLES, Math.max(MIN_SAMPLES, px / 2));
    }

    /** Apsides of the first patch, and the events that end each patch. */
    private static List<Mark> marks(List<Patch> patches, double horizon) {
        List<Mark> marks = new ArrayList<>();
        if (patches.isEmpty()) {
            return marks;
        }
        Patch first = patches.get(0);
        CelestialBody body = first.body();
        double t0 = first.startUt();
        double t1 = endUt(first, horizon);
        Elements el = Elements.of(body.mu, first.startState());
        if (Double.isFinite(el.timeToApoapsis()) && t0 + el.timeToApoapsis() <= t1) {
            double ut = t0 + el.timeToApoapsis();
            marks.add(apsis(Kind.APOAPSIS, first, ut, el.apoapsisRadius() - body.radius));
        }
        if (Double.isFinite(el.timeToPeriapsis()) && el.timeToPeriapsis() >= 0 && t0 + el.timeToPeriapsis() <= t1) {
            double ut = t0 + el.timeToPeriapsis();
            marks.add(apsis(Kind.PERIAPSIS, first, ut, el.periapsisRadius() - body.radius));
        }
        for (int i = 0; i < patches.size(); i++) {
            Patch patch = patches.get(i);
            double ut = endUt(patch, horizon);
            switch (patch.end()) {
                case ENCOUNTER -> {
                    String entered = i + 1 < patches.size() ? patches.get(i + 1).body().id : "";
                    marks.add(point(Kind.ENCOUNTER, patch, ut, entered));
                }
                case ATMOSPHERE -> marks.add(point(Kind.ATMOSPHERE, patch, ut, ""));
                case IMPACT -> marks.add(point(Kind.IMPACT, patch, ut, ""));
                default -> {
                }
            }
        }
        return marks;
    }

    private static Mark apsis(Kind kind, Patch patch, double ut, double altM) {
        StateVector at = rootState(patch, ut);
        Vec3d centre = Bodies.rootState(patch.body(), ut).r();
        return new Mark(kind, ut, at.r().x(), at.r().y(), centre.x(), centre.y(), altM, "");
    }

    private static Mark point(Kind kind, Patch patch, double ut, String body) {
        StateVector at = rootState(patch, ut);
        return new Mark(kind, ut, at.r().x(), at.r().y(), 0, 0, 0, body);
    }

    /** Position and velocity of a patch's ship at {@code ut}, relative to the root body. */
    static StateVector rootState(Patch patch, double ut) {
        StateVector rel = patch.stateAt(ut);
        StateVector off = Bodies.rootState(patch.body(), ut);
        return new StateVector(rel.r().add(off.r()), rel.v().add(off.v()));
    }

    /** Index of the patch the ship is on at {@code ut}. */
    int indexAt(double ut) {
        int index = 0;
        for (int i = 0; i < patches.size(); i++) {
            if (patches.get(i).startUt() <= ut) {
                index = i;
            }
        }
        return index;
    }

    /** Position and velocity of the ship at {@code ut} on patch {@code index}, relative to the root body. */
    StateVector rootState(int index, double ut) {
        return rootState(patches.get(index), ut);
    }

    int patchCount() {
        return patches.size();
    }

    /** Sample x coordinates of patch {@code index}, root frame, metres. Length is the sample count plus one. */
    double[] xs(int index) {
        return xs[index];
    }

    /** Sample y coordinates of patch {@code index}, root frame, metres. */
    double[] ys(int index) {
        return ys[index];
    }

    List<Mark> marks() {
        return marks;
    }
}
