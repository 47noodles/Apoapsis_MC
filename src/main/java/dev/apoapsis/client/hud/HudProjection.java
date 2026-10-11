package dev.apoapsis.client.hud;

/**
 * Pure camera maths for the flight HUD's world markers: rotating a direction by a unit quaternion and projecting it to
 * GUI pixels with the camera's vertical field of view. No Minecraft types, no allocation. Client only.
 *
 * <p>Camera space follows vanilla: the camera looks along -Z, with +X to the right and +Y up. A direction ahead of the
 * camera therefore has a negative Z there, and the projection uses depth -Z.
 */
public final class HudProjection {
    private HudProjection() {}

    /**
     * Writes the rotation of (dx, dy, dz) by the unit quaternion (qx, qy, qz, qw) into {@code out[at..at+2]}:
     * v + 2w(u x v) + 2u x (u x v), with u the vector part.
     */
    public static void rotate(double qx, double qy, double qz, double qw, double dx, double dy, double dz,
                              double[] out, int at) {
        double cx = qy * dz - qz * dy;
        double cy = qz * dx - qx * dz;
        double cz = qx * dy - qy * dx;
        double tx = 2 * cx;
        double ty = 2 * cy;
        double tz = 2 * cz;
        out[at] = dx + qw * tx + (qy * tz - qz * ty);
        out[at + 1] = dy + qw * ty + (qz * tx - qx * tz);
        out[at + 2] = dz + qw * tz + (qx * ty - qy * tx);
    }

    /**
     * Writes the camera-space direction of world direction (dx, dy, dz) for a camera rotation q into {@code out[0..2]}:
     * the rotation by the conjugate of q.
     */
    public static void toCamera(double qx, double qy, double qz, double qw, double dx, double dy, double dz,
                                double[] out) {
        rotate(-qx, -qy, -qz, qw, dx, dy, dz, out, 0);
    }

    /**
     * Projects world direction (dx, dy, dz) to GUI pixels for a camera rotation q, a vertical field of view
     * {@code fovDeg} and a GUI of {@code width} by {@code height}. Writes x and y into {@code out[0]} and {@code out[1]}.
     * Uses {@code out[2]} as scratch, so {@code out} needs at least three elements. Returns false for a direction at or
     * behind the camera plane. Points off the GUI are still projected; test them with {@link #onScreen}.
     *
     * <p>The focal length in GUI px is (height / 2) / tan(fov / 2) on both axes: the vertical fov fixes the scale and the
     * aspect ratio only stretches the horizontal, which the GUI width absorbs.
     */
    public static boolean project(double qx, double qy, double qz, double qw, double dx, double dy, double dz,
                                  double fovDeg, double width, double height, double[] out) {
        toCamera(qx, qy, qz, qw, dx, dy, dz, out);
        double depth = -out[2];
        if (!(depth > 1e-9)) {
            return false;
        }
        double focal = (height * 0.5) / Math.tan(Math.toRadians(fovDeg) * 0.5);
        double x = out[0];
        double y = out[1];
        out[0] = width * 0.5 + focal * x / depth;
        out[1] = height * 0.5 - focal * y / depth;
        return true;
    }

    /** True when (x, y) lies inside the GUI rectangle [0, width] by [0, height]. */
    public static boolean onScreen(double x, double y, double width, double height) {
        return x >= 0 && x <= width && y >= 0 && y <= height;
    }
}
