package dev.apoapsis.client.map3d;

import dev.apoapsis.orbit.Vec3d;

/**
 * A unit quaternion for the 3D map's camera: a rotation with no pole, so the view can go over the top of any axis.
 * Pure maths, no game state. Client only.
 */
public record Quat(double w, double x, double y, double z) {
    public static final Quat IDENTITY = new Quat(1, 0, 0, 0);

    /** The rotation of {@code angle} radians about the unit vector {@code axis}. */
    public static Quat axis(Vec3d axis, double angle) {
        double s = Math.sin(angle / 2);
        return new Quat(Math.cos(angle / 2), axis.x() * s, axis.y() * s, axis.z() * s);
    }

    /**
     * The rotation whose columns are {@code r}, {@code u} and {@code b} (orthonormal, right-handed): the image of the
     * view's x, y and z axes in inertial space.
     */
    public static Quat fromColumns(Vec3d r, Vec3d u, Vec3d b) {
        double m00 = r.x(), m01 = u.x(), m02 = b.x();
        double m10 = r.y(), m11 = u.y(), m12 = b.y();
        double m20 = r.z(), m21 = u.z(), m22 = b.z();
        double trace = m00 + m11 + m22;
        if (trace > 0) {
            double s = Math.sqrt(trace + 1) * 2;
            return new Quat(s / 4, (m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s).normalize();
        } else if (m00 > m11 && m00 > m22) {
            double s = Math.sqrt(1 + m00 - m11 - m22) * 2;
            return new Quat((m21 - m12) / s, s / 4, (m01 + m10) / s, (m02 + m20) / s).normalize();
        } else if (m11 > m22) {
            double s = Math.sqrt(1 + m11 - m00 - m22) * 2;
            return new Quat((m02 - m20) / s, (m01 + m10) / s, s / 4, (m12 + m21) / s).normalize();
        }
        double s = Math.sqrt(1 + m22 - m00 - m11) * 2;
        return new Quat((m10 - m01) / s, (m02 + m20) / s, (m12 + m21) / s, s / 4).normalize();
    }

    /** This rotation after {@code o}: applying the result equals applying {@code o} then this. */
    public Quat mul(Quat o) {
        return new Quat(
                w * o.w - x * o.x - y * o.y - z * o.z,
                w * o.x + x * o.w + y * o.z - z * o.y,
                w * o.y - x * o.z + y * o.w + z * o.x,
                w * o.z + x * o.y - y * o.x + z * o.w);
    }

    public Quat normalize() {
        double n = Math.sqrt(w * w + x * x + y * y + z * z);
        return new Quat(w / n, x / n, y / n, z / n);
    }

    /** {@code v} turned by this rotation. */
    public Vec3d rotate(Vec3d v) {
        Vec3d u = new Vec3d(x, y, z);
        Vec3d t = u.cross(v).scale(2);
        return v.add(t.scale(w)).add(u.cross(t));
    }
}
