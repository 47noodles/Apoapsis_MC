package dev.apoapsis.client.sky;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Vec3d;

/**
 * Frames and horizon tests for the sky: the orbit-dimension axes, the surface tangent map shared by the
 * overworld and Pale, and the system-frame positions of the bodies. Pure maths, no Minecraft types. Client only.
 */
final class SkyMath {
    private SkyMath() {}

    /**
     * Three rows taking an inertial vector {@code w} to dimension-local axes: (a.w, b.w, c.w). Client only.
     */
    record Axes(Vec3d a, Vec3d b, Vec3d c) {
        Vec3d apply(Vec3d w) {
            return new Vec3d(a.dot(w), b.dot(w), c.dot(w));
        }
    }

    /** The orbit dimension: body-inertial (X, Y, Z) to local axes (x, y, z) = (X, Z, -Y). A proper rotation. */
    static final Axes ORBIT = new Axes(new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), new Vec3d(0, -1, 0));

    /** Position of {@code body} relative to Verdance (the system origin) at {@code ut}; zero for the root. */
    static Vec3d systemPosition(CelestialBody body, double ut) {
        return body.parent == null ? Vec3d.ZERO : body.stateAt(ut).r();
    }

    /** Unit radial direction of a surface point at longitude {@code lon} and latitude {@code lat} (radians). */
    static Vec3d upAt(double lon, double lat) {
        return new Vec3d(Math.cos(lat) * Math.cos(lon), Math.cos(lat) * Math.sin(lon), Math.sin(lat));
    }

    /** The surface axes (x east, y up, z south) at (lon, lat), as rows over inertial vectors. */
    static Axes surfaceAxes(double lon, double lat) {
        Vec3d east = new Vec3d(-Math.sin(lon), Math.cos(lon), 0);
        Vec3d north = new Vec3d(-Math.sin(lat) * Math.cos(lon), -Math.sin(lat) * Math.sin(lon), Math.cos(lat));
        return new Axes(east, upAt(lon, lat), north.scale(-1));
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
