package dev.apoapsis.orbit;

/** Position (m) and velocity (m/s) relative to a central body, in its inertial frame (X, Y equatorial, Z north). */
public record StateVector(Vec3d r, Vec3d v) {
}
