package dev.apoapsis.orbit;

/**
 * One conic segment of a predicted trajectory around {@code body}, valid from startUt to endUt.
 * STUB from S0; node ORB implements {@link #stateAt}.
 */
public record Patch(CelestialBody body, double startUt, double endUt, StateVector startState, EndReason end) {

    public enum EndReason {
        /** The prediction horizon was reached. */
        NONE,
        /** Entered a child body's SOI; the next patch is around the child. */
        ENCOUNTER,
        /** Left this body's SOI; the next patch is around the parent. */
        ESCAPE,
        /** Dropped below the top of the atmosphere. */
        ATMOSPHERE,
        /** Hit the surface. */
        IMPACT
    }

    /** State relative to {@code body} at time {@code ut} (two-body propagation from startState). */
    public StateVector stateAt(double ut) {
        return Kepler.propagate(body.mu, startState, ut - startUt);
    }
}
