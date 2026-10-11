package dev.apoapsis.client.map3d;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Vec3d;

/**
 * The 3D map's geometry for one frame, as screen-space quads (4 vertices each, pixels from the view's top left, ARGB
 * per vertex). Spheres are front-facing triangles, shaded from the sun on +X; the atmosphere is an annulus of fading
 * quads; orbits and the ship's coast are 2 px ribbons, hidden where a sphere is in front of them. Everything is
 * computed in double precision relative to the focus, then rounded to float only for the screen. The sphere mesh is
 * built once; each frame only transforms it. Pure maths, no game state. Client only.
 */
public final class Scene3D {
    /** Capacity in vertices. A frame uses about 26 thousand. */
    public static final int MAX_VERTS = 65536;
    static final int SIGNAL = 0xFF3AD1C1;
    static final int HYDROGEN_RGB = 0x5AA9F5;
    static final int STEEL_500 = 0xFF56616D;
    static final int VERDANCE_LIT = 0xFF6FBBA8;
    static final int PALE_LIT = 0xFFB8BEC4;
    /** Brightness of the unlit side of a body, 0..1. */
    static final double AMBIENT = 0.10;
    /** Atmosphere haze at the inner edge, as alpha 0..255; it fades to zero at the outer edge. */
    static final int HAZE_ALPHA = 0x50;

    private static final int LAT = 20;
    private static final int LON = 40;
    private static final int TRIS = 2 * LAT * LON;
    private static final int RING = 256;
    /** Unit sphere, nine coordinates per triangle, and the unit normal of each triangle's centroid. */
    private static final double[] TRI_V = new double[TRIS * 9];
    private static final double[] TRI_N = new double[TRIS * 3];

    static {
        int t = 0;
        for (int i = 0; i < LAT; i++) {
            double p0 = -Math.PI / 2 + Math.PI * i / LAT;
            double p1 = -Math.PI / 2 + Math.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                double l0 = 2 * Math.PI * j / LON;
                double l1 = 2 * Math.PI * (j + 1) / LON;
                double[] a = point(p0, l0), b = point(p1, l0), c = point(p1, l1), d = point(p0, l1);
                t = triangle(t, a, b, c);
                t = triangle(t, a, c, d);
            }
        }
    }

    public final float[] x = new float[MAX_VERTS];
    public final float[] y = new float[MAX_VERTS];
    public final int[] argb = new int[MAX_VERTS];
    /** Vertices in use; always a multiple of four. */
    public int count;

    private final double[] screen = new double[2];
    private final double[] sx = new double[OrbitPlot.SAMPLES + 1];
    private final double[] sy = new double[OrbitPlot.SAMPLES + 1];
    private final boolean[] ok = new boolean[OrbitPlot.SAMPLES + 1];
    private final double[] ringX = new double[RING + 1];
    private final double[] ringY = new double[RING + 1];
    private final double[] ringZ = new double[RING + 1];
    /** The two occluding spheres, focus-relative: centre x, y, z, radius. */
    private final double[] occ = new double[8];
    private int occN;

    private OrbitCamera cam;
    private double cx, cy, focal;
    private double fx, fy, fz;
    private double ex, ey, ez;

    private static double[] point(double lat, double lon) {
        double c = Math.cos(lat);
        return new double[] {c * Math.cos(lon), c * Math.sin(lon), Math.sin(lat)};
    }

    private static int triangle(int t, double[] a, double[] b, double[] c) {
        int k = 9 * t;
        double[][] p = {a, b, c};
        double nx = 0, ny = 0, nz = 0;
        for (int v = 0; v < 3; v++) {
            TRI_V[k + 3 * v] = p[v][0];
            TRI_V[k + 3 * v + 1] = p[v][1];
            TRI_V[k + 3 * v + 2] = p[v][2];
            nx += p[v][0];
            ny += p[v][1];
            nz += p[v][2];
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        TRI_N[3 * t] = len > 0 ? nx / len : 0;
        TRI_N[3 * t + 1] = len > 0 ? ny / len : 0;
        TRI_N[3 * t + 2] = len > 0 ? nz / len : 1;
        return t + 1;
    }

    /**
     * Builds the frame for a {@code w} by {@code h} view centred on {@code focus} (root frame, m) at universal time
     * {@code nowUt}. {@code plot} is the predicted coast; its samples are reused, the bodies and ship are placed now.
     */
    public void build(OrbitCamera camera, OrbitPlot plot, double nowUt, double w, double h, Vec3d focus) {
        count = 0;
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
            body(verdance, MeridianSystem.VERDANCE, VERDANCE_LIT);
            body(pale, MeridianSystem.PALE, PALE_LIT);
        } else {
            body(pale, MeridianSystem.PALE, PALE_LIT);
            body(verdance, MeridianSystem.VERDANCE, VERDANCE_LIT);
        }

        // Reference plane: the grid (faint) and Pale's orbit (a quiet ring).
        double rp = MeridianSystem.PALE.orbitRadius;
        for (int k = 1; k <= 3; k++) {
            ring(rp * k / 4, 0x28000000 | STEEL_500 & 0xFFFFFF, 0.5);
        }
        for (int s = 0; s < 8; s++) {
            double a = 2 * Math.PI * s / 8;
            spoke(a, rp * 1.05, 0x28000000 | STEEL_500 & 0xFFFFFF);
        }
        ring(rp, 0xA0000000 | STEEL_500 & 0xFFFFFF, 0.5);

        // The coast: later patches dimmer, the current one bright.
        for (int i = plot.patches.size() - 1; i >= 0; i--) {
            int colour = i == plot.current ? SIGNAL : 0x90000000 | HYDROGEN_RGB;
            polyline(plot.x[i], plot.y[i], plot.z[i], OrbitPlot.SAMPLES + 1, colour, 1.0);
        }

        Vec3d ship = plot.shipRootAt(nowUt);
        if (project(ship.x() - fx, ship.y() - fy, ship.z() - fz)) {
            put(screen[0] - 3, screen[1] - 3, SIGNAL);
            put(screen[0] + 3, screen[1] - 3, SIGNAL);
            put(screen[0] + 3, screen[1] + 3, SIGNAL);
            put(screen[0] - 3, screen[1] + 3, SIGNAL);
        }
    }

    private void setOccluder(int k, Vec3d centre, double radius) {
        occ[4 * k] = centre.x() - fx;
        occ[4 * k + 1] = centre.y() - fy;
        occ[4 * k + 2] = centre.z() - fz;
        occ[4 * k + 3] = radius;
    }

    private double dist(Vec3d centre) {
        double dx = centre.x() - fx - ex, dy = centre.y() - fy - ey, dz = centre.z() - fz - ez;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** A body's haze, then its lit sphere, unless the eye is inside it. */
    private void body(Vec3d centre, CelestialBody b, int rgb) {
        double dx = centre.x() - fx, dy = centre.y() - fy, dz = centre.z() - fz;
        double d = Math.sqrt((dx - ex) * (dx - ex) + (dy - ey) * (dy - ey) + (dz - ez) * (dz - ez));
        if (d < b.radius * 1.02) {
            return;
        }
        if (b.atmosphereTop > 0) {
            haze(dx, dy, dz, b.radius, b.radius + b.atmosphereTop, d);
        }
        sphere(dx, dy, dz, b.radius, rgb);
    }

    /** The lit sphere: front-facing triangles, each shaded by its normal's sun term. */
    private void sphere(double ccx, double ccy, double ccz, double radius, int rgb) {
        for (int t = 0; t < TRIS; t++) {
            double nx = TRI_N[3 * t], ny = TRI_N[3 * t + 1], nz = TRI_N[3 * t + 2];
            double facing = nx * (ex - (ccx + radius * nx)) + ny * (ey - (ccy + radius * ny))
                    + nz * (ez - (ccz + radius * nz));
            if (facing <= 0) {
                continue;
            }
            int k = 9 * t;
            if (!project(ccx + radius * TRI_V[k], ccy + radius * TRI_V[k + 1], ccz + radius * TRI_V[k + 2])) {
                continue;
            }
            double x0 = screen[0], y0 = screen[1];
            if (!project(ccx + radius * TRI_V[k + 3], ccy + radius * TRI_V[k + 4], ccz + radius * TRI_V[k + 5])) {
                continue;
            }
            double x1 = screen[0], y1 = screen[1];
            if (!project(ccx + radius * TRI_V[k + 6], ccy + radius * TRI_V[k + 7], ccz + radius * TRI_V[k + 8])) {
                continue;
            }
            // Smooth shading: each corner is lit by its own unit normal, which is its unit position.
            put(x0, y0, shade(rgb, TRI_V[k]));
            put(x1, y1, shade(rgb, TRI_V[k + 3]));
            put(screen[0], screen[1], shade(rgb, TRI_V[k + 6]));
            put(screen[0], screen[1], shade(rgb, TRI_V[k + 6]));
        }
    }

    /** {@code rgb} lit from the sun on +X by a surface whose normal has x component {@code nx}, opaque. */
    static int shade(int rgb, double nx) {
        double light = AMBIENT + (1 - AMBIENT) * Math.max(0, nx);
        int r = (int) Math.round(((rgb >> 16) & 0xFF) * light);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) * light);
        int b = (int) Math.round((rgb & 0xFF) * light);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * The haze: an annulus from the body's disc out to its atmosphere's top, drawn as quads whose alpha fades from
     * {@link #HAZE_ALPHA} at the disc to zero at the top. The disc's radius on screen is taken as f tan(asin(R/d)).
     */
    private void haze(double ccx, double ccy, double ccz, double radius, double top, double d) {
        if (!project(ccx, ccy, ccz)) {
            return;
        }
        double ox = screen[0], oy = screen[1];
        double inner = focal * radius / Math.sqrt(d * d - radius * radius);
        double outer = focal * top / Math.sqrt(d * d - top * top);
        if (!(outer > inner)) {
            return;
        }
        int segments = 96;
        for (int s = 0; s < segments; s++) {
            double a0 = 2 * Math.PI * s / segments, a1 = 2 * Math.PI * (s + 1) / segments;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            int hazeIn = HAZE_ALPHA << 24 | HYDROGEN_RGB;
            put(ox + inner * c0, oy + inner * s0, hazeIn);
            put(ox + inner * c1, oy + inner * s1, hazeIn);
            put(ox + outer * c1, oy + outer * s1, HYDROGEN_RGB);
            put(ox + outer * c0, oy + outer * s0, HYDROGEN_RGB);
        }
    }

    /** A circle of {@code radius} m in the reference plane around the root origin. */
    private void ring(double radius, int argb, double halfWidthPx) {
        for (int k = 0; k <= RING; k++) {
            double a = 2 * Math.PI * k / RING;
            ringX[k] = radius * Math.cos(a);
            ringY[k] = radius * Math.sin(a);
            ringZ[k] = 0;
        }
        polyline(ringX, ringY, ringZ, RING + 1, argb, halfWidthPx);
    }

    /** A straight grid line in the reference plane from the origin out to {@code length} m. */
    private void spoke(double angle, double length, int argb) {
        ringX[0] = 0;
        ringY[0] = 0;
        ringZ[0] = 0;
        ringX[1] = length * Math.cos(angle);
        ringY[1] = length * Math.sin(angle);
        ringZ[1] = 0;
        polyline(ringX, ringY, ringZ, 2, argb, 0.5);
    }

    /** A root-frame polyline of {@code n} points as ribbons, dropping points that are behind the eye or hidden. */
    private void polyline(double[] X, double[] Y, double[] Z, int n, int argb, double halfWidthPx) {
        for (int k = 0; k < n; k++) {
            double px = X[k] - fx, py = Y[k] - fy, pz = Z[k] - fz;
            ok[k] = project(px, py, pz) && !hidden(px, py, pz);
            if (ok[k]) {
                sx[k] = screen[0];
                sy[k] = screen[1];
            }
        }
        for (int k = 0; k + 1 < n; k++) {
            if (ok[k] && ok[k + 1]) {
                segment(sx[k], sy[k], sx[k + 1], sy[k + 1], halfWidthPx, argb);
            }
        }
    }

    /** One ribbon segment: a quad {@code halfWidthPx} either side of the line. */
    private void segment(double ax, double ay, double bx, double by, double halfWidthPx, int argb) {
        double dx = bx - ax, dy = by - ay;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (!(len > 1e-9)) {
            return;
        }
        double nx = -dy / len * halfWidthPx, ny = dx / len * halfWidthPx;
        put(ax + nx, ay + ny, argb);
        put(bx + nx, by + ny, argb);
        put(bx - nx, by - ny, argb);
        put(ax - nx, ay - ny, argb);
    }

    /** Projects a focus-relative point into {@link #screen}; false when it is behind the eye. */
    private boolean project(double dx, double dy, double dz) {
        return !Double.isNaN(cam.project(dx, dy, dz, cx, cy, focal, screen));
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

    /** Appends one vertex. Drops it when the buffer is full. */
    private void put(double px, double py, int colour) {
        if (count >= MAX_VERTS) {
            return;
        }
        x[count] = (float) px;
        y[count] = (float) py;
        argb[count] = colour;
        count++;
    }
}
