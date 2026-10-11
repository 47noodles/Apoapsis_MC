package dev.apoapsis.client.sky;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Vec3d;

/**
 * Frames and horizon tests for the sky: the orbit-dimension axes, the surface tangent map shared by the
 * overworld and Pale, and the system-frame positions of the bodies. Pure maths, no Minecraft types. Client only.
 */
final class SkyMath {
    private SkyMath() {}

    /** Body-inertial (X, Y, Z) to orbit-dimension local axes (x, y, z) = (X, Z, -Y). A proper rotation. */
    static Vec3d inertialToLocal(Vec3d v) {
        return new Vec3d(v.x(), v.z(), -v.y());
    }

    /** Position of {@code body} relative to Verdance (the system origin) at {@code ut}; zero for the root. */
    static Vec3d systemPosition(CelestialBody body, double ut) {
        return body.parent == null ? Vec3d.ZERO : body.stateAt(ut).r();
    }

    /** Unit radial direction of a surface point at longitude {@code lon} and latitude {@code lat} (radians). */
    static Vec3d upAt(double lon, double lat) {
        return new Vec3d(Math.cos(lat) * Math.cos(lon), Math.cos(lat) * Math.sin(lon), Math.sin(lat));
    }

    /**
     * Inertial vector {@code w} expressed in surface axes (x east, y up, z south) at (lon, lat). The tangent map
     * sends surface (x, y, z) to x*E + y*U - z*N, so the inverse is x = w.E, y = w.U, z = -w.N.
     */
    static Vec3d inertialToSurface(Vec3d w, double lon, double lat) {
        Vec3d east = new Vec3d(-Math.sin(lon), Math.cos(lon), 0);
        Vec3d north = new Vec3d(-Math.sin(lat) * Math.cos(lon), -Math.sin(lat) * Math.sin(lon), Math.cos(lat));
        return new Vec3d(w.dot(east), w.dot(upAt(lon, lat)), -w.dot(north));
    }

    /**
     * True when the ray from a surface point {@code p} (relative to the body centre) along the unit direction
     * {@code w} runs into the body of the given radius, i.e. the target is below the horizon.
     */
    static boolean belowHorizon(Vec3d p, Vec3d w, double radius) {
        double pw = p.dot(w);
        return pw < 0 && pw * pw >= p.lengthSquared() - radius * radius;
    }
}
