package dev.apoapsis.client.hud;

/**
 * The navball's geometry, pure maths. The ball is a sphere seen from its nose: its centre is the nose, its depth axis
 * points at the viewer along the nose, and its two screen axes are fixed to the ship. So the horizon and the prograde
 * marker move and tilt with the ship's attitude against the local vertical. Client only.
 *
 * <p>Ball coordinates are (right, up, nose) in the ship's frame. Everything is in dimension axes, the frame the ship's
 * orientation and the cockpit camera use.
 */
public final class Navball {
    /** The sky half, blue. */
    public static final int SKY = 0xFF2B5C8F;
    /** The ground half, brown. */
    public static final int GROUND = 0xFF6A4A2C;

    private Navball() {}

    /**
     * The ball's three axes in dimension coordinates, written to {@code out[0..8]}: right at 0..2, up at 3..5, and the
     * nose at 6..8. The orientation (qx, qy, qz, qw) turns body axes into dimension axes, and (tx, ty, tz) is the nose
     * in body axes. Up is the body axis perpendicular to the nose that is nearest +Z, and right is up x nose.
     */
    public static void axes(double qx, double qy, double qz, double qw, double tx, double ty, double tz, double[] out) {
        double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
        tx /= tl;
        ty /= tl;
        tz /= tl;
        // The body axis for "up": +Z, or +X when the nose is nearly along Z.
        double ax = 0;
        double az = 1;
        if (Math.abs(tz) > 0.9) {
            ax = 1;
            az = 0;
        }
        double d = ax * tx + az * tz;
        double bx = ax - d * tx;
        double by = -d * ty;
        double bz = az - d * tz;
        double bl = Math.sqrt(bx * bx + by * by + bz * bz);
        bx /= bl;
        by /= bl;
        bz /= bl;
        HudProjection.rotate(qx, qy, qz, qw, tx, ty, tz, out, 6);
        HudProjection.rotate(qx, qy, qz, qw, bx, by, bz, out, 3);
        double nx = out[6];
        double ny = out[7];
        double nz = out[8];
        double ux = out[3];
        double uy = out[4];
        double uz = out[5];
        out[0] = uy * nz - uz * ny;
        out[1] = uz * nx - ux * nz;
        out[2] = ux * ny - uy * nx;
    }

    /** Ball coordinates (right, up, nose) of dimension vector (vx, vy, vz), written to {@code out[0..2]}. */
    public static void toBall(double[] axes, double vx, double vy, double vz, double[] out) {
        out[0] = vx * axes[0] + vy * axes[1] + vz * axes[2];
        out[1] = vx * axes[3] + vy * axes[4] + vz * axes[5];
        out[2] = vx * axes[6] + vy * axes[7] + vz * axes[8];
    }

    /**
     * The colour of the ball at screen point (sx, sy), each in -1..1 with up positive, or 0 outside the disc. The
     * point is on the sphere's front, at depth sqrt(1 - sx^2 - sy^2). It is sky where its side of the horizon faces the
     * local vertical. {@code up} is the vertical in ball coordinates, from {@link #toBall}.
     */
    public static int classify(double[] up, double sx, double sy) {
        double r2 = sx * sx + sy * sy;
        if (r2 >= 1) {
            return 0;
        }
        double depth = Math.sqrt(1 - r2);
        return up[0] * sx + up[1] * sy + up[2] * depth >= 0 ? SKY : GROUND;
    }

    /**
     * The compass heading of the nose, degrees 0..360 from north, measured in the local horizontal plane. North is
     * dimension -Z, or +X when that is along the vertical. Returns NaN when the nose is vertical.
     */
    public static double heading(double nx, double ny, double nz, double ux, double uy, double uz) {
        // North: -Z projected onto the horizon. If that is degenerate (vertical is along Z), +X instead.
        double nd = -uz;
        double px = -nd * ux;
        double py = -nd * uy;
        double pz = -1 - nd * uz;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 1e-6) {
            px = 1 - ux * ux;
            py = -ux * uy;
            pz = -ux * uz;
            pl = Math.sqrt(px * px + py * py + pz * pz);
        }
        px /= pl;
        py /= pl;
        pz /= pl;
        // East is north x up, so the heading runs north, east, south, west.
        double ex = py * uz - pz * uy;
        double ey = pz * ux - px * uz;
        double ez = px * uy - py * ux;
        double nodeDot = nx * ux + ny * uy + nz * uz;
        double hx = nx - nodeDot * ux;
        double hy = ny - nodeDot * uy;
        double hz = nz - nodeDot * uz;
        double hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (hl < 1e-6) {
            return Double.NaN;
        }
        double toNorth = (hx * px + hy * py + hz * pz) / hl;
        double toEast = (hx * ex + hy * ey + hz * ez) / hl;
        double deg = Math.toDegrees(Math.atan2(toEast, toNorth));
        return deg < 0 ? deg + 360 : deg;
    }
}
