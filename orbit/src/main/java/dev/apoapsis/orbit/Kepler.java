package dev.apoapsis.orbit;

/** Two-body propagation. STUB from S0; node ORB implements it. */
public final class Kepler {
    private Kepler() {}

    /**
     * Propagates a two-body state by {@code dt} seconds (dt may be negative) for any conic (elliptic, parabolic,
     * hyperbolic), using the universal-variable formulation.
     */
    public static StateVector propagate(double mu, StateVector s, double dt) {
        return s;
    }
}
