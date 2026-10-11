package dev.apoapsis.orbit;

import java.util.function.Function;

/** Numerical integration for powered or draggy flight. */
public final class Integrator {
    private Integrator() {}

    /**
     * One classic RK4 step of dt seconds around {@code body}: point-mass gravity plus {@code extraAccel(state)}
     * (thrust, drag), in m/s^2. A null {@code extraAccel} means no extra acceleration.
     */
    public static StateVector rk4(CelestialBody body, StateVector s, double dt, Function<StateVector, Vec3d> extraAccel) {
        StateVector k1 = derivative(body, s, extraAccel);
        StateVector k2 = derivative(body, offset(s, k1, dt / 2), extraAccel);
        StateVector k3 = derivative(body, offset(s, k2, dt / 2), extraAccel);
        StateVector k4 = derivative(body, offset(s, k3, dt), extraAccel);
        Vec3d r = s.r().add(k1.r().add(k2.r().scale(2)).add(k3.r().scale(2)).add(k4.r()).scale(dt / 6));
        Vec3d v = s.v().add(k1.v().add(k2.v().scale(2)).add(k3.v().scale(2)).add(k4.v()).scale(dt / 6));
        return new StateVector(r, v);
    }

    /** Time derivative of the state: (velocity, acceleration). */
    private static StateVector derivative(CelestialBody body, StateVector s, Function<StateVector, Vec3d> extraAccel) {
        double rMag = s.r().length();
        Vec3d acceleration = rMag > 0 ? s.r().scale(-body.mu / (rMag * rMag * rMag)) : Vec3d.ZERO;
        if (extraAccel != null) {
            acceleration = acceleration.add(extraAccel.apply(s));
        }
        return new StateVector(s.v(), acceleration);
    }

    /** The state moved by {@code h} along a derivative: s + h * k. */
    private static StateVector offset(StateVector s, StateVector k, double h) {
        return new StateVector(s.r().add(k.r().scale(h)), s.v().add(k.v().scale(h)));
    }
}
