package dev.apoapsis.orbit;

import java.util.function.Function;

/** Numerical integration for powered or draggy flight. STUB from S0; node ORB implements it. */
public final class Integrator {
    private Integrator() {}

    /**
     * One classic RK4 step of dt seconds around {@code body}: point-mass gravity plus {@code extraAccel(state)}
     * (thrust, drag), in m/s^2.
     */
    public static StateVector rk4(CelestialBody body, StateVector s, double dt, Function<StateVector, Vec3d> extraAccel) {
        return s;
    }
}
