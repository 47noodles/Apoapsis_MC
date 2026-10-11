package dev.apoapsis.orbit;

/**
 * Orbit summary derived from a state vector. STUB from S0; node ORB implements {@link #of}.
 *
 * @param semiMajorAxis   metres (negative for hyperbolic, infinite for parabolic)
 * @param eccentricity    0 circular, below 1 elliptic, 1 parabolic, above 1 hyperbolic
 * @param inclination     radians from the equatorial (XY) plane
 * @param periapsisRadius metres from the body centre
 * @param apoapsisRadius  metres from the body centre; +Infinity when eccentricity is 1 or more
 * @param period          seconds; NaN when eccentricity is 1 or more
 * @param timeToPeriapsis seconds until the next periapsis (negative if it has passed on an escape trajectory)
 * @param timeToApoapsis  seconds until the next apoapsis; NaN when eccentricity is 1 or more
 */
public record Elements(double semiMajorAxis, double eccentricity, double inclination, double periapsisRadius,
                       double apoapsisRadius, double period, double timeToPeriapsis, double timeToApoapsis) {

    public static Elements of(double mu, StateVector s) {
        return new Elements(0, 0, 0, 0, 0, Double.NaN, 0, Double.NaN);
    }
}
