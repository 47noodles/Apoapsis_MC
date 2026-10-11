package dev.apoapsis.orbit.maneuver;

/**
 * A planned burn at universal time {@code ut}, in the local prograde / normal / radial frame (m/s).
 * Prograde is along velocity, normal along r x v, and radial is radial-out along prograde x normal
 * (in the orbital plane, perpendicular to velocity, pointing away from the body).
 */
public record ManeuverNode(double ut, double prograde, double normal, double radial) {

    /** Magnitude of the burn's delta-v, m/s. */
    public double deltaV() {
        return Math.sqrt(prograde * prograde + normal * normal + radial * radial);
    }
}
