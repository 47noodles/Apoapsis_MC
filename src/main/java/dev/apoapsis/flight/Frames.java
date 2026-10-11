package dev.apoapsis.flight;

import dev.apoapsis.orbit.Vec3d;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * Surface and orbit frame conversions (contract "Frames"). The surface dimension is a tangent map around
 * (lat 0, lon 0): lon = x / R, lat = -z / R. Orientation maps body axes into dimension axes.
 */
public final class Frames {
    private static final double SQRT_HALF = Math.sqrt(0.5);
    /** Inertial (X, Y, Z) to orbit-dimension axes (X, Z, -Y): a rotation of -90 degrees about X. */
    static final Quaternionf INERTIAL_TO_ORBIT = new Quaternionf((float) -SQRT_HALF, 0f, 0f, (float) SQRT_HALF);

    private Frames() {}

    /** Local basis at a surface point: east, north, up (the radial direction), and the frame matrix (E, U, -N). */
    public record Tangent(double lat, double lon, Vec3d e, Vec3d n, Vec3d u, Quaternionf frame) {

        static Tangent at(double lat, double lon) {
            double sl = Math.sin(lat), cl = Math.cos(lat), so = Math.sin(lon), co = Math.cos(lon);
            Vec3d e = new Vec3d(-so, co, 0);
            Vec3d n = new Vec3d(-sl * co, -sl * so, cl);
            Vec3d u = new Vec3d(cl * co, cl * so, sl);
            return new Tangent(lat, lon, e, n, u, quaternionOf(e, u, n.scale(-1)));
        }

        /** Quaternion of the rotation whose columns are the three vectors (proper rotation). */
        private static Quaternionf quaternionOf(Vec3d c0, Vec3d c1, Vec3d c2) {
            double m00 = c0.x(), m01 = c1.x(), m02 = c2.x();
            double m10 = c0.y(), m11 = c1.y(), m12 = c2.y();
            double m20 = c0.z(), m21 = c1.z(), m22 = c2.z();
            double trace = m00 + m11 + m22;
            double x, y, z, w;
            if (trace > 0) {
                double s = Math.sqrt(trace + 1) * 2;
                w = 0.25 * s;
                x = (m21 - m12) / s;
                y = (m02 - m20) / s;
                z = (m10 - m01) / s;
            } else if (m00 > m11 && m00 > m22) {
                double s = Math.sqrt(1 + m00 - m11 - m22) * 2;
                w = (m21 - m12) / s;
                x = 0.25 * s;
                y = (m01 + m10) / s;
                z = (m02 + m20) / s;
            } else if (m11 > m22) {
                double s = Math.sqrt(1 + m11 - m00 - m22) * 2;
                w = (m02 - m20) / s;
                x = (m01 + m10) / s;
                y = 0.25 * s;
                z = (m12 + m21) / s;
            } else {
                double s = Math.sqrt(1 + m22 - m00 - m11) * 2;
                w = (m10 - m01) / s;
                x = (m02 + m20) / s;
                y = (m12 + m21) / s;
                z = 0.25 * s;
            }
            return new Quaternionf((float) x, (float) y, (float) z, (float) w).normalize();
        }
    }

    /** Tangent basis at the surface point under a surface position (x, y, z) on a body of radius R. */
    public static Tangent tangentAt(Vec3 surface, double radius) {
        return Tangent.at(-surface.z / radius, surface.x / radius);
    }

    /** Tangent basis at an inertial position relative to the body centre. */
    public static Tangent tangentAt(Vec3d r) {
        double len = r.length();
        return Tangent.at(Math.asin(clamp(r.z() / len)), Math.atan2(r.y(), r.x()));
    }

    /** Inertial position of a surface position: r = (R + h) * up, with h = y - sea level. */
    static Vec3d toInertialPosition(Vec3 surface, double radius, double seaLevel, Tangent t) {
        double h = surface.y - seaLevel;
        return t.u().scale(radius + h);
    }

    /** Inertial velocity of a surface velocity: v = vx E - vz N + vy U. */
    static Vec3d toInertialVelocity(Vec3 v, Tangent t) {
        return t.e().scale(v.x).add(t.n().scale(-v.z)).add(t.u().scale(v.y));
    }

    /**
     * Surface position of an inertial position r (relative to the body centre), with sea level and radius. The height
     * is kept at 1 m or more, so a ship that arrives below the surface is placed on it, not in the void.
     */
    static Vec3 toSurfacePosition(Vec3d r, double radius, double seaLevel, Tangent t) {
        double h = Math.max(1.0, r.length() - radius);
        return new Vec3(t.lon() * radius, seaLevel + h, -t.lat() * radius);
    }

    /** Surface velocity of an inertial velocity: vx = v.E, vy = v.U, vz = -v.N. */
    static Vec3 toSurfaceVelocity(Vec3d v, Tangent t) {
        return new Vec3(v.dot(t.e()), v.dot(t.u()), -v.dot(t.n()));
    }

    /** Orbit-dimension orientation of a surface orientation: P * (M * q). */
    public static Quaternionf surfaceToOrbitOrientation(Quaternionfc surfaceOrientation, Tangent t) {
        Quaternionf inertial = new Quaternionf(t.frame()).mul(surfaceOrientation);
        return new Quaternionf(INERTIAL_TO_ORBIT).mul(inertial);
    }

    /** Surface orientation of an orbit-dimension orientation: M^-1 * (P^-1 * q). */
    public static Quaternionf orbitToSurfaceOrientation(Quaternionfc orbitOrientation, Tangent t) {
        Quaternionf inertial = conjugate(INERTIAL_TO_ORBIT).mul(orbitOrientation);
        return conjugate(t.frame()).mul(inertial).normalize();
    }

    /** Minimal rotation that takes body +Y to the unit direction d (dimension axes). */
    static Quaternionf noseTowards(double dx, double dy, double dz) {
        // axis = (+Y) x d, angle = acos(d.y)
        double ax = dz, ay = 0, az = -dx;
        double dot = clamp(dy);
        double len = Math.sqrt(ax * ax + az * az);
        if (len < 1e-12) {
            // Parallel or antiparallel to +Y: identity, or a half turn about X.
            return dot >= 0 ? new Quaternionf() : new Quaternionf(1f, 0f, 0f, 0f);
        }
        double angle = Math.acos(dot);
        double s = Math.sin(angle / 2) / len;
        return new Quaternionf((float) (ax * s), (float) (ay * s), (float) (az * s), (float) Math.cos(angle / 2))
                .normalize();
    }

    static Quaternionf conjugate(Quaternionfc q) {
        return new Quaternionf(-q.x(), -q.y(), -q.z(), q.w());
    }

    private static double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }
}
