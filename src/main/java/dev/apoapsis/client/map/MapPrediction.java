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
 * A ship's predicted coast for the map: patches, each sampled as a polyline relative to the patch's body, plus the
 * marks on it (apsides, encounters, atmosphere entry, impact), each relative to its patch's body too. The map places
 * them at the current time (see {@link Bodies#placeNow}), so nothing here depends on the clock once computed.
 * Immutable after {@link #compute} and {@link #resampled}. Client only.
 */
final class MapPrediction {
    /** Upper bound on samples per patch. */
    static final int MAX_SAMPLES = 2000;
    /** The longest coast predicted for the map, s. Keeps the predictor's work on the render thread bounded. */
    static final double HORIZON_CAP_S = 6 * 3600.0;
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
     * @param kind   what the mark is
     * @param ut     universal time of the mark, s
     * @param anchor the body the mark's offset is measured from: the body of the patch it sits on
     * @param x      offset of the mark from {@code anchor}'s centre in the inertial plane, m
     * @param y      offset of the mark from {@code anchor}'s centre in the inertial plane, m
     * @param altM   altitude above the anchor's surface (apsides), m
     * @param body   id of the body entered (encounters), else empty
     * @param patch  index of the patch the mark sits on (its anchor is that patch's body)
     */
    record Mark(Kind kind, double ut, CelestialBody anchor, double x, double y, double altM, String body, int patch) {}

    /** The payload this prediction was made from. */
    final OrbitStatePayload source;
    private final double horizon;
    private final List<Patch> patches;
    private final double[][] xs;
    private final double[][] ys;
    private final List<Mark> marks;

    private MapPrediction(OrbitStatePayload source, double horizon, List<Patch> patches, double[][] xs, double[][] ys,
                          List<Mark> marks) {
        this.source = source;
        this.horizon = horizon;
        this.patches = patches;
        this.xs = xs;
        this.ys = ys;
        this.marks = marks;
    }

    /**
     * Predicts the ship's coast from {@code p} and samples it for a view at {@code pxPerMetre}. The horizon is 1.2
     * periods for a closed orbit, otherwise four hours, and never more than {@link #HORIZON_CAP_S}.
     */
    static MapPrediction compute(OrbitStatePayload p, double pxPerMetre) {
        CelestialBody body = Bodies.of(p);
        StateVector s = Bodies.stateOf(p);
        Elements el = Elements.of(body.mu, s);
        double wanted = isClosed(el) ? CLOSED_HORIZON_FACTOR * el.period() : OPEN_HORIZON_S;
        double horizon = Math.min(HORIZON_CAP_S, wanted);
        List<Patch> patches = TrajectoryPredictor.predict(body, s, p.ut(), horizon, MAX_PATCHES);
        double[][][] samples = samplePatches(patches, horizon, pxPerMetre);
        return new MapPrediction(p, horizon, patches, samples[0], samples[1], marks(patches, horizon));
    }

    /** The same prediction sampled again for a new view scale. Patches and marks are shared, not recomputed. */
    MapPrediction resampled(double pxPerMetre) {
        double[][][] samples = samplePatches(patches, horizon, pxPerMetre);
        return new MapPrediction(source, horizon, patches, samples[0], samples[1], marks);
    }

    private static boolean isClosed(Elements el) {
        return el.eccentricity() < 1 && Double.isFinite(el.period()) && el.period() > 0;
    }

    private static double endUt(Patch patch, double horizon) {
        double t1 = patch.endUt();
        return Double.isFinite(t1) && t1 >= patch.startUt() ? t1 : patch.startUt() + horizon;
    }

    /** Samples every patch relative to its body. Returns {xs, ys}, each indexed by patch. */
    private static double[][][] samplePatches(List<Patch> patches, double horizon, double pxPerMetre) {
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
                Vec3d q = patch.stateAt(t0 + (t1 - t0) * j / count).r();
                xs[i][j] = q.x();
                ys[i][j] = q.y();
            }
        }
        return new double[][][] {xs, ys};
    }

    /** Samples about one per two screen pixels of arc, from a coarse pass over the patch. */
    private static int sampleCount(Patch patch, double t0, double t1, double pxPerMetre) {
        double length = 0;
        Vec3d prev = null;
        for (int j = 0; j <= COARSE_SAMPLES; j++) {
            Vec3d q = patch.stateAt(t0 + (t1 - t0) * j / COARSE_SAMPLES).r();
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
            marks.add(apsis(Kind.APOAPSIS, first, 0, ut, el.apoapsisRadius() - body.radius));
        }
        if (Double.isFinite(el.timeToPeriapsis()) && el.timeToPeriapsis() >= 0 && t0 + el.timeToPeriapsis() <= t1) {
            double ut = t0 + el.timeToPeriapsis();
            marks.add(apsis(Kind.PERIAPSIS, first, 0, ut, el.periapsisRadius() - body.radius));
        }
        for (int i = 0; i < patches.size(); i++) {
            Patch patch = patches.get(i);
            double ut = endUt(patch, horizon);
            switch (patch.end()) {
                case ENCOUNTER -> {
                    String entered = i + 1 < patches.size() ? patches.get(i + 1).body().id : "";
                    marks.add(point(Kind.ENCOUNTER, patch, i, ut, entered));
                }
                case ATMOSPHERE -> marks.add(point(Kind.ATMOSPHERE, patch, i, ut, ""));
                case IMPACT -> marks.add(point(Kind.IMPACT, patch, i, ut, ""));
                default -> {
                }
            }
        }
        return marks;
    }

    private static Mark apsis(Kind kind, Patch patch, int index, double ut, double altM) {
        Vec3d at = patch.stateAt(ut).r();
        return new Mark(kind, ut, patch.body(), at.x(), at.y(), altM, "", index);
    }

    private static Mark point(Kind kind, Patch patch, int index, double ut, String body) {
        Vec3d at = patch.stateAt(ut).r();
        return new Mark(kind, ut, patch.body(), at.x(), at.y(), 0, body, index);
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

    /** Universal time patch {@code index} starts, s. */
    double startUt(int index) {
        return patches.get(index).startUt();
    }

    /** The body patch {@code index} is around. */
    CelestialBody bodyOf(int index) {
        return patches.get(index).body();
    }

    /** The ship's position and velocity on patch {@code index} at {@code ut}, relative to that patch's body. */
    StateVector relativeState(int index, double ut) {
        return patches.get(index).stateAt(ut);
    }

    int patchCount() {
        return patches.size();
    }

    /** Sample x coordinates of patch {@code index}, relative to its body, metres. Length is the sample count plus one. */
    double[] xs(int index) {
        return xs[index];
    }

    /** Sample y coordinates of patch {@code index}, relative to its body, metres. */
    double[] ys(int index) {
        return ys[index];
    }

    List<Mark> marks() {
        return marks;
    }
}
