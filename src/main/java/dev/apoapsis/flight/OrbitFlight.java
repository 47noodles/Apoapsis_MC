package dev.apoapsis.flight;

import dev.apoapsis.ages.Firsts;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Integrator;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Orbital translation of a ship in the orbit dimension: Kepler propagation while coasting, RK4 with thrust and drag
 * inside an atmosphere or under thrust, and sphere-of-influence switching between Verdance and Pale.
 */
final class OrbitFlight {
    /** Largest RK4 substep, seconds of game time. */
    static final double MAX_STEP_S = 0.05;
    /** Cap on RK4 substeps in one call, so a long unload gap cannot stall the server thread. */
    static final int MAX_SUBSTEPS = 2000;

    private OrbitFlight() {}

    /**
     * Advances the ship's orbit from its last update to universal time {@code ut}, pins it to its slot, and switches
     * sphere of influence if needed.
     *
     * @param thrustLocal thrust acceleration in orbit-dimension axes (m/s^2)
     */
    static void advance(ShipEntity ship, FlightState st, Vec3 thrustLocal, double ut) {
        CelestialBody body = FlightDimensions.system(st.body);
        // Orbit-dimension axes (x, y, z) = inertial (X, Z, -Y), so X = x, Y = -z, Z = y.
        Vec3d thrust = new Vec3d(thrustLocal.x, -thrustLocal.z, thrustLocal.y);
        double mass = Math.max(ship.massKg(), 1.0);
        double cdA = 0.5 * Math.pow(Math.max(1, ship.getStructure().size()), 2.0 / 3.0);
        double dragK = 0.5 * cdA / mass;
        Function<StateVector, Vec3d> accel = s -> {
            double alt = Math.max(s.r().length() - body.radius, 0.0);
            double rho = body.density(alt);
            if (rho <= 0) {
                return thrust;
            }
            return thrust.add(s.v().scale(-rho * s.v().length() * dragK));
        };

        StateVector s0 = st.stateVector();
        double span = ut - st.epochUt;
        double alt = s0.r().length() - body.radius;
        boolean inAir = alt < body.atmosphereTop;
        boolean thrusting = thrust.lengthSquared() > 0;
        StateVector s1 = !thrusting && !inAir
                ? Kepler.propagate(body.mu, s0, span)
                : integrate(body, s0, span, accel);

        st.setState(s1);
        st.epochUt = ut;
        ship.setPos(FlightDimensions.slotPosition(st.slot));
        ship.setDeltaMovement(Vec3.ZERO);
        switchSoi(ship, st, ut);
    }

    private static StateVector integrate(CelestialBody body, StateVector s0, double span,
                                         Function<StateVector, Vec3d> accel) {
        int n = (int) Math.min(MAX_SUBSTEPS, Math.ceil(Math.abs(span) / MAX_STEP_S));
        if (n == 0) {
            return s0;
        }
        double h = span / n;
        StateVector s = s0;
        for (int i = 0; i < n; i++) {
            s = Integrator.rk4(body, s, h, accel);
        }
        return s;
    }

    /** Verdance to Pale inside Pale's sphere of influence, and back out of it. Positions are relative to the body. */
    private static void switchSoi(ShipEntity ship, FlightState st, double ut) {
        CelestialBody pale = MeridianSystem.PALE;
        StateVector palePos = pale.stateAt(ut);
        if (st.body == BodyId.VERDANCE) {
            Vec3d rel = st.r().sub(palePos.r());
            if (rel.length() < pale.soiRadius()) {
                StateVector s = st.stateVector();
                st.setState(new StateVector(rel, s.v().sub(palePos.v())));
                st.body = BodyId.PALE;
                Firsts.paleFlyby(ship.getPilot());
                FlightWarp.clear();
                FlightMessages.toPilot(ship, Component.translatable("apoapsis.flight.soi.enter_pale"));
            }
        } else if (st.r().length() > pale.soiRadius()) {
            StateVector s = st.stateVector();
            st.setState(new StateVector(s.r().add(palePos.r()), s.v().add(palePos.v())));
            st.body = BodyId.VERDANCE;
            FlightWarp.clear();
            FlightMessages.toPilot(ship, Component.translatable("apoapsis.flight.soi.enter_verdance"));
        }
    }
}
