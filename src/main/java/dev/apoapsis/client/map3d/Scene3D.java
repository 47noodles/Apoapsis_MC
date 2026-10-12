package dev.apoapsis.client.map3d;

import dev.apoapsis.client.map.Labels;
import dev.apoapsis.client.map.MapScene;
import dev.apoapsis.client.map.Palette;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * The 3D map's geometry for one frame, projected through an {@link OrbitCamera} into a {@link MapScene} (UI units).
 * Bodies are textured sphere meshes in the scene's own layer, lit from the sun on +X, with an atmosphere haze and a
 * limb drawn in front. Every line is split by what it passes: the part behind a body (or its far side) goes in the
 * layer under the bodies, so the body hides it exactly, and the rest goes in the layer over them, so an orbit is never
 * cut short of a body's edge and never z-fights with it. Lines are anti-aliased strips of one width at any zoom; the
 * current orbit glows, later patches and the line after a planned burn are dashed. Every line also marks the label
 * solver's grid, so labels keep off them. Everything is computed in double precision relative to the focus, then
 * rounded to float only for the screen. Pure maths, no game state. Client only.
 */
public final class Scene3D {
    static final int SIGNAL = UiPalette.SIGNAL;
    static final int HYDROGEN = UiPalette.HYDROGEN;
    static final int BRASS = UiPalette.BRASS;
    static final int VERDANCE_LIT = Palette.VERDANCE_LIT;
    static final int PALE_LIT = Palette.PALE_LIT;

    private static final int RING = 256;
    /** Dash and gap of a predicted line, UI units. */
    private static final double DASH = 4.5;
    private static final double GAP = 3.5;
    /** A label avoids an orbit line this much more than a grid line. */
    private static final int WEIGHT_ORBIT = 3;
    private static final int WEIGHT_GRID = 1;
    /** The occluding sphere is a hair larger than the mesh so a hidden line never peeks past the limb. */
    private static final double OCCLUDE_GROW = 1.003;

    private final double[] screen = new double[3];
    /** The current patch's samples on screen, for picking: valid where {@link #pickOk} is set. */
    private final double[] pickX = new double[OrbitPlot.SAMPLES + 1];
    private final double[] pickY = new double[OrbitPlot.SAMPLES + 1];
    private final boolean[] pickOk = new boolean[OrbitPlot.SAMPLES + 1];
    private final double[] sx = new double[OrbitPlot.SAMPLES + 1];
    private final double[] sy = new double[OrbitPlot.SAMPLES + 1];
    private final boolean[] ok = new boolean[OrbitPlot.SAMPLES + 1];
    private final boolean[] hid = new boolean[OrbitPlot.SAMPLES + 1];
    private final double[] runX = new double[OrbitPlot.SAMPLES + 2];
    private final double[] runY = new double[OrbitPlot.SAMPLES + 2];
    private final double[] ringX = new double[RING + 1];
    private final double[] ringY = new double[RING + 1];
    private final double[] ringZ = new double[RING + 1];
    /** The two occluding spheres, focus-relative: centre x, y, z, radius. */
    private final double[] occ = new double[8];
    private int occN;

    private MapScene out;
    private Labels labels;
    private OrbitCamera cam;
    private double cx, cy, focal;
    private double fx, fy, fz;
    private double ex, ey, ez;
    /** The ship's mark: screen x, y, then the unit direction it moves in on screen; NaN when it is hidden. */
    public final double[] ship = {Double.NaN, Double.NaN, 1, 0};

    /** How a line looks. {@code glow} is the width of the soft halo (0 for none) at alpha {@code glowAlpha}. */
    private record Style(int argb, double half, double glow, int glowAlpha, double dash, double gap, int weight) {}

    /**
     * Builds the frame for a {@code w} by {@code h} view (UI units) centred on {@code focus} (root frame, m) at
     * universal time {@code nowUt}. {@code plot} is the predicted coast; its samples are reused, the bodies and ship are
     * placed now. {@code predicted}, when not null, is the line after a planned burn. A body with {@code textured[k]}
     * (0 Verdance, 1 Pale) wears its surface map; one without is a lit sphere in its tone until the map arrives.
     */
    public void build(MapScene scene, Labels labelSolver, OrbitCamera camera, OrbitPlot plot,
            @Nullable OrbitPlot predicted, double nowUt, double w, double h, Vec3d focus, boolean[] textured) {
        out = scene;
        labels = labelSolver;
        java.util.Arrays.fill(pickOk, false);
        cam = camera;
        cx = w / 2;
        cy = h / 2;
        focal = OrbitCamera.focal(h);
        fx = focus.x();
        fy = focus.y();
        fz = focus.z();
        Vec3d e = camera.eye();
        ex = e.x();
        ey = e.y();
        ez = e.z();

        Vec3d verdance = OrbitPlot.rootPosition(MeridianSystem.VERDANCE, nowUt);
        Vec3d pale = OrbitPlot.rootPosition(MeridianSystem.PALE, nowUt);
        setOccluder(0, verdance, MeridianSystem.VERDANCE.radius);
        setOccluder(1, pale, MeridianSystem.PALE.radius);
        occN = 2;

        // The farther body first, so the nearer one draws over it.
        double dv = dist(verdance), dp = dist(pale);
        if (dv >= dp) {
            body(verdance, MeridianSystem.VERDANCE, VERDANCE_LIT, 0, textured[0]);
            body(pale, MeridianSystem.PALE, PALE_LIT, 1, textured[1]);
        } else {
            body(pale, MeridianSystem.PALE, PALE_LIT, 1, textured[1]);
            body(verdance, MeridianSystem.VERDANCE, VERDANCE_LIT, 0, textured[0]);
        }

        // Reference plane: faint grid rings and Pale's orbit as a quiet ring with a glow.
        double rp = MeridianSystem.PALE.orbitRadius;
        int grid = UiPalette.alpha(UiPalette.STEEL_500, 0x2C);
        for (int k = 1; k <= 3; k++) {
            ring(rp * k / 4, new Style(grid, MapScene.THIN_HALF, 0, 0, 0, 0, WEIGHT_GRID));
        }
        ring(rp, new Style(UiPalette.alpha(UiPalette.SIGNAL_EDGE, 0xE0), MapScene.THIN_HALF + 0.04, 1.6,
                0x1E, 0, 0, WEIGHT_GRID + 1));

        // The coast: later patches dashed and dimmer, the current one solid and glowing.
        for (int i = plot.patches.size() - 1; i >= 0; i--) {
            Style st = i == plot.current
                    ? new Style(SIGNAL, MapScene.ORBIT_HALF, 1.8, 0x34, 0, 0, WEIGHT_ORBIT)
                    : new Style(UiPalette.alpha(HYDROGEN, 0xC8), MapScene.ORBIT_HALF * 0.85, 0, 0, DASH, GAP,
                            WEIGHT_ORBIT);
            polyline(plot.x[i], plot.y[i], plot.z[i], OrbitPlot.SAMPLES + 1, st);
            if (i == plot.current) {
                System.arraycopy(sx, 0, pickX, 0, OrbitPlot.SAMPLES + 1);
                System.arraycopy(sy, 0, pickY, 0, OrbitPlot.SAMPLES + 1);
                for (int k = 0; k <= OrbitPlot.SAMPLES; k++) {
                    pickOk[k] = ok[k] && !hid[k];
                }
            }
        }
        // The line after a planned burn, over the coast: brass and dashed, so it reads as a prediction.
        if (predicted != null) {
            for (int i = predicted.patches.size() - 1; i >= 0; i--) {
                Style st = new Style(i == 0 ? BRASS : UiPalette.alpha(BRASS, 0xB4), MapScene.ORBIT_HALF, i == 0 ? 1.6 : 0,
                        0x2E, DASH, GAP, WEIGHT_ORBIT);
                polyline(predicted.x[i], predicted.y[i], predicted.z[i], OrbitPlot.SAMPLES + 1, st);
            }
        }
        placeShip(plot, nowUt);
    }

    /** Puts the ship's marker in the front layer, pointing the way it flies, unless a body hides it. */
    private void placeShip(OrbitPlot plot, double nowUt) {
        ship[0] = Double.NaN;
        Vec3d at = plot.shipRootAt(nowUt);
        double px = at.x() - fx, py = at.y() - fy, pz = at.z() - fz;
        if (!project(px, py, pz) || hidden(px, py, pz)) {
            return;
        }
        double x = screen[0], y = screen[1];
        StateVector s = plot.shipStateAt(nowUt);
        Vec3d v = s.v();
        double dt = 1.0;
        double dirx = 1, diry = 0;
        double len = v.length();
        if (len > 0) {
            double k = Math.max(1, plot.shipRootAt(nowUt).length() * 0.001) / len;
            if (project(px + v.x() * k * dt, py + v.y() * k * dt, pz + v.z() * k * dt)) {
                double ddx = screen[0] - x, ddy = screen[1] - y;
                double dl = Math.hypot(ddx, ddy);
                if (dl > 1e-9) {
                    dirx = ddx / dl;
                    diry = ddy / dl;
                }
            }
        }
        ship[0] = x;
        ship[1] = y;
        ship[2] = dirx;
        ship[3] = diry;
        double nx = -diry, ny = dirx;
        out.front.disc(x, y, 6.5, 3.5, UiPalette.alpha(SIGNAL, 0x26));
        out.front.triangle(x + dirx * 4.6, y + diry * 4.6, SIGNAL, x - dirx * 3 + nx * 3, y - diry * 3 + ny * 3, SIGNAL,
                x - dirx * 3 - nx * 3, y - diry * 3 - ny * 3, SIGNAL);
    }

    /** True when the root-frame point is on screen and no body hides it. */
    public boolean visible(Vec3d root) {
        double px = root.x() - fx, py = root.y() - fy, pz = root.z() - fz;
        return project(px, py, pz) && !hidden(px, py, pz);
    }

    private void setOccluder(int k, Vec3d centre, double radius) {
        occ[4 * k] = centre.x() - fx;
        occ[4 * k + 1] = centre.y() - fy;
        occ[4 * k + 2] = centre.z() - fz;
        occ[4 * k + 3] = radius * OCCLUDE_GROW;
    }

    private double dist(Vec3d centre) {
        double dx = centre.x() - fx - ex, dy = centre.y() - fy - ey, dz = centre.z() - fz - ez;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * The nearest point of the current patch's drawn line to the pointer ({@code mx}, {@code my}): {fractional sample
     * index, distance in UI units}, or null when no visible part of the line is within {@code maxPx}. Points a body
     * hides are not on the line.
     */
    public double @Nullable [] pickCurrent(double mx, double my, double maxPx) {
        double best = maxPx;
        double bestIndex = -1;
        for (int k = 0; k < OrbitPlot.SAMPLES; k++) {
            if (!pickOk[k] || !pickOk[k + 1]) {
                continue;
            }
            double ax = pickX[k], ay = pickY[k];
            double dx = pickX[k + 1] - ax, dy = pickY[k + 1] - ay;
            double len2 = dx * dx + dy * dy;
            double t = len2 > 1e-12 ? Math.max(0, Math.min(1, ((mx - ax) * dx + (my - ay) * dy) / len2)) : 0;
            double d = Math.hypot(ax + t * dx - mx, ay + t * dy - my);
            if (d < best) {
                best = d;
                bestIndex = k + t;
            }
        }
        return bestIndex < 0 ? null : new double[] {bestIndex, best};
    }

    /** A body: its mesh in the bodies' layer, then its atmosphere and limb in front, unless the eye is inside it. */
    private void body(Vec3d centre, CelestialBody b, int rgb, int index, boolean textured) {
        double dx = centre.x() - fx, dy = centre.y() - fy, dz = centre.z() - fz;
        double d = Math.sqrt((dx - ex) * (dx - ex) + (dy - ey) * (dy - ey) + (dz - ez) * (dz - ez));
        if (d < b.radius * 1.02) {
            return;
        }
        out.mesh[index].build(cam, cx, cy, focal, dx, dy, dz, b.radius, ex, ey, ez, 0, textured ? 0xFFFFFF : rgb & 0xFFFFFF);
        out.order[out.meshCount++] = index;
        if (!project(dx, dy, dz)) {
            return;
        }
        double ox = screen[0], oy = screen[1];
        double inner = focal * b.radius / Math.sqrt(d * d - b.radius * b.radius);
        double top = b.atmosphereTop > 0 ? focal * (b.radius + b.atmosphereTop)
                / Math.sqrt(d * d - (b.radius + b.atmosphereTop) * (b.radius + b.atmosphereTop)) : inner;
        // The sun's direction on screen (screen y grows downward) and how much of its light the near side gets.
        Vec3d right = cam.right();
        Vec3d up = cam.up();
        double sunX = right.x();
        double sunY = -up.x();
        double eyeDot = (ex / Math.sqrt(ex * ex + ey * ey + ez * ez + 1e-9)) * MapScene.SUN_X;
        double lit = 0.5 + 0.5 * eyeDot;
        if (b.atmosphereTop > 0) {
            out.atmosphere(ox, oy, inner, top, sunX, sunY, lit);
        }
        out.limb(ox, oy, inner);
        labels.markCircle(ox, oy, inner, 4);
    }

    /** A circle of {@code radius} m in the reference plane around the root origin. */
    private void ring(double radius, Style style) {
        for (int k = 0; k <= RING; k++) {
            double a = 2 * Math.PI * k / RING;
            ringX[k] = radius * Math.cos(a);
            ringY[k] = radius * Math.sin(a);
            ringZ[k] = 0;
        }
        polyline(ringX, ringY, ringZ, RING + 1, style);
    }

    /**
     * A root-frame polyline of {@code n} points. Points behind the eye break it. Each run of segments goes to the layer
     * under the bodies when it touches a point a body hides, else to the layer over them.
     */
    private void polyline(double[] X, double[] Y, double[] Z, int n, Style st) {
        for (int k = 0; k < n; k++) {
            double px = X[k] - fx, py = Y[k] - fy, pz = Z[k] - fz;
            ok[k] = project(px, py, pz);
            hid[k] = ok[k] && hidden(px, py, pz);
            if (ok[k]) {
                sx[k] = screen[0];
                sy[k] = screen[1];
            }
        }
        int layer = -1;
        int m = 0;
        for (int k = 0; k + 1 < n; k++) {
            if (!ok[k] || !ok[k + 1]) {
                flush(layer, m, st);
                layer = -1;
                m = 0;
                continue;
            }
            int l = hid[k] || hid[k + 1] ? 0 : 1;
            if (l != layer) {
                flush(layer, m, st);
                layer = l;
                m = 0;
                runX[m] = sx[k];
                runY[m] = sy[k];
                m++;
            }
            runX[m] = sx[k + 1];
            runY[m] = sy[k + 1];
            m++;
            if (l == 1) {
                labels.markSegment(sx[k], sy[k], sx[k + 1], sy[k + 1], st.weight());
            }
        }
        flush(layer, m, st);
    }

    private void flush(int layer, int m, Style st) {
        if (layer < 0 || m < 2) {
            return;
        }
        var batch = layer == 0 ? out.back : out.front;
        if (st.glow() > 0) {
            batch.polyline(runX, runY, m, 0, st.glow(), UiPalette.alpha(st.argb(), st.glowAlpha()));
        }
        if (st.dash() > 0) {
            batch.dashedPolyline(runX, runY, m, st.half(), MapScene.FEATHER, st.dash(), st.gap(), st.argb());
        } else {
            batch.polyline(runX, runY, m, st.half(), MapScene.FEATHER, st.argb());
        }
    }

    /** Projects a focus-relative point into {@link #screen}; false when it is behind the eye. */
    private boolean project(double dx, double dy, double dz) {
        return !Double.isNaN(cam.project(dx, dy, dz, cx, cy, focal, screen));
    }

    /** The screen radius, UI units, of a body of {@code radius} m at root-frame {@code at}; 0 when it is behind the eye. */
    public double radiusPx(Vec3d at, double radius) {
        double dx = at.x() - fx - ex, dy = at.y() - fy - ey, dz = at.z() - fz - ez;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return d > radius ? focal * radius / Math.sqrt(d * d - radius * radius) : 0;
    }

    /** True when a sphere lies between the eye and the point (focus-relative), so the point is out of sight. */
    private boolean hidden(double px, double py, double pz) {
        double dx = px - ex, dy = py - ey, dz = pz - ez;
        double a = dx * dx + dy * dy + dz * dz;
        for (int k = 0; k < occN; k++) {
            double ocx = ex - occ[4 * k], ocy = ey - occ[4 * k + 1], ocz = ez - occ[4 * k + 2];
            double rad = occ[4 * k + 3];
            double b = 2 * (ocx * dx + ocy * dy + ocz * dz);
            double c = ocx * ocx + ocy * ocy + ocz * ocz - rad * rad;
            double disc = b * b - 4 * a * c;
            if (disc < 0) {
                continue;
            }
            double t = (-b - Math.sqrt(disc)) / (2 * a);
            if (t > 1e-9 && t < 1 - 1e-9) {
                return true;
            }
        }
        return false;
    }

    /** {@code rgb} lit from the sun on +X by a surface whose normal has x component {@code nx}, opaque. Kept for the menus' planet. */
    static int shade(int rgb, double nx) {
        double light = 0.10 + 0.90 * Math.max(0, nx);
        int r = (int) Math.round(((rgb >> 16) & 0xFF) * light);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) * light);
        int b = (int) Math.round((rgb & 0xFF) * light);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
