package dev.apoapsis.client.map3d;

import dev.apoapsis.orbit.Vec3d;

/**
 * The 3D map's camera: it orbits a focus point at {@link #distance} metres, looking at it. The orientation is a
 * quaternion with no pole lock. Drag turns it about the screen's axes, and the default view sits
 * {@link #DEFAULT_ELEVATION_DEG} above the reference plane (the inertial X-Y plane). Points are projected in double
 * precision and converted to screen pixels only at the end. Pure maths, no game state. Client only.
 */
public final class OrbitCamera {
    /** Vertical field of view, radians. */
    public static final double FOV_Y = Math.toRadians(50);
    public static final double DEFAULT_ELEVATION_DEG = 30;
    /** Drag, radians per screen pixel. */
    static final double RAD_PER_PX = 0.008;
    /** Zoom step: the distance changes by this factor per scroll tick, exponentially. */
    static final double ZOOM_PER_TICK = 0.15;
    /** Points nearer the eye than this share of the distance are not drawn. */
    static final double NEAR_SHARE = 1e-3;

    private Quat q;
    private double distance;

    public OrbitCamera(double distance) {
        this.q = defaultView();
        this.distance = distance;
    }

    private OrbitCamera(Quat q, double distance) {
        this.q = q;
        this.distance = distance;
    }

    /** A camera looking straight down the Z axis from {@code distance} m, with +X to the right and +Y up: the 2D map's view. */
    public static OrbitCamera topDown(double distance) {
        return new OrbitCamera(Quat.fromColumns(new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 1)), distance);
    }

    /** The view looking along -Z in view space, from 30 degrees above the X-Y plane, with +Z roughly up on screen. */
    static Quat defaultView() {
        double c = Math.cos(Math.toRadians(DEFAULT_ELEVATION_DEG));
        double s = Math.sin(Math.toRadians(DEFAULT_ELEVATION_DEG));
        return Quat.fromColumns(new Vec3d(0, 1, 0), new Vec3d(-s, 0, c), new Vec3d(c, 0, s));
    }

    public Quat orientation() {
        return q;
    }

    public double distance() {
        return distance;
    }

    public void setDistance(double metres) {
        distance = metres;
    }

    /** The view's right-hand screen axis in inertial space. */
    public Vec3d right() {
        return q.rotate(new Vec3d(1, 0, 0));
    }

    /** The view's up screen axis in inertial space. */
    public Vec3d up() {
        return q.rotate(new Vec3d(0, 1, 0));
    }

    /** Where the eye sits relative to the focus, inertial, m. */
    public Vec3d eye() {
        return q.rotate(new Vec3d(0, 0, distance));
    }

    /** A drag of {@code dx}, {@code dy} screen pixels: yaw about the screen's up axis, then pitch about its right axis. */
    public void drag(double dx, double dy) {
        q = Quat.axis(up(), -dx * RAD_PER_PX).mul(q);
        q = Quat.axis(right(), dy * RAD_PER_PX).mul(q).normalize();
    }

    /** A scroll of {@code ticks} (positive zooms in). Exponential, so each tick scales the distance by the same factor. */
    public void zoom(double ticks) {
        distance *= Math.exp(-ticks * ZOOM_PER_TICK);
    }

    /**
     * Projects {@code d} (inertial, m, offset from the focus) to pixels: {@code out[0]}, {@code out[1]} hold x and y
     * from the screen centre {@code cx}, {@code cy}, with focal length {@code focal} px. Returns the depth in front of
     * the eye, or NaN when the point is not in front of the eye.
     */
    public double project(double dx, double dy, double dz, double cx, double cy, double focal, double[] out) {
        // View coordinates: the inverse rotation (conjugate: negate the vector part), then vx, vy, vz.
        double ux = -q.x(), uy = -q.y(), uz = -q.z(), w = q.w();
        double c1x = uy * dz - uz * dy, c1y = uz * dx - ux * dz, c1z = ux * dy - uy * dx;
        double tx = 2 * c1x, ty = 2 * c1y, tz = 2 * c1z;
        double vx = dx + w * tx + (uy * tz - uz * ty);
        double vy = dy + w * ty + (uz * tx - ux * tz);
        double vz = dz + w * tz + (ux * ty - uy * tx);
        double depth = distance - vz;
        if (!(depth > distance * NEAR_SHARE)) {
            return Double.NaN;
        }
        out[0] = cx + focal * vx / depth;
        out[1] = cy - focal * vy / depth;
        return depth;
    }

    /** Focal length in pixels for a view {@code heightPx} tall. */
    public static double focal(double heightPx) {
        return (heightPx / 2) / Math.tan(FOV_Y / 2);
    }
}
