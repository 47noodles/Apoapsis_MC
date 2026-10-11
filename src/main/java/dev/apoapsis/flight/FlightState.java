package dev.apoapsis.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;

/**
 * A ship's flight state, kept as a NeoForge data attachment on {@link dev.apoapsis.ship.ShipEntity} and saved with it.
 * Mutable and package-private: only the flight package touches it. r and v are inertial, relative to {@link #body}.
 */
final class FlightState {
    static final MapCodec<FlightState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("regime").forGetter(s -> s.regime.ordinal()),
            Codec.INT.fieldOf("body").forGetter(s -> s.body.ordinal()),
            Codec.DOUBLE.fieldOf("rx").forGetter(s -> s.rx),
            Codec.DOUBLE.fieldOf("ry").forGetter(s -> s.ry),
            Codec.DOUBLE.fieldOf("rz").forGetter(s -> s.rz),
            Codec.DOUBLE.fieldOf("vx").forGetter(s -> s.vx),
            Codec.DOUBLE.fieldOf("vy").forGetter(s -> s.vy),
            Codec.DOUBLE.fieldOf("vz").forGetter(s -> s.vz),
            Codec.DOUBLE.fieldOf("epoch_ut").forGetter(s -> s.epochUt),
            Codec.INT.fieldOf("slot").forGetter(s -> s.slot),
            Codec.BOOL.optionalFieldOf("first_flight", false).forGetter(s -> s.firstFlight),
            Codec.INT.optionalFieldOf("airborne_ticks", 0).forGetter(s -> s.airborneTicks)
    ).apply(i, FlightState::fromSaved));

    /** Regime; the ship's dimension decides it in practice. */
    FlightRegime regime = FlightRegime.SURFACE;
    /** Central body of the state (surface: the body whose surface the ship is on). */
    BodyId body = BodyId.VERDANCE;
    double rx;
    double ry;
    double rz;
    double vx;
    double vy;
    double vz;
    /** Universal time of the stored r and v, seconds. */
    double epochUt;
    /** Orbit slot index in the orbit dimension, -1 when not in orbit. */
    int slot = -1;
    /** Milestone flag: First Flight has been granted for this ship. */
    boolean firstFlight;
    /** Ticks since the ship last left the ground, reset on landing. */
    int airborneTicks;

    FlightState() {}

    private static FlightState fromSaved(int regime, int body, double rx, double ry, double rz, double vx, double vy,
                                         double vz, double epochUt, int slot, boolean firstFlight, int airborneTicks) {
        FlightState s = new FlightState();
        s.regime = FlightRegime.byOrdinal(regime);
        s.body = BodyId.byOrdinal(body);
        s.rx = finite(rx);
        s.ry = finite(ry);
        s.rz = finite(rz);
        s.vx = finite(vx);
        s.vy = finite(vy);
        s.vz = finite(vz);
        s.epochUt = finite(epochUt);
        s.slot = slot;
        s.firstFlight = firstFlight;
        s.airborneTicks = airborneTicks;
        return s;
    }

    /** The value, or 0 if it is not finite (a corrupt save must not poison the orbit maths). */
    private static double finite(double v) {
        return Double.isFinite(v) ? v : 0;
    }

    /** A new orbit state at the given inertial state. */
    static FlightState inOrbit(BodyId body, StateVector state, double ut, int slot) {
        FlightState s = new FlightState();
        s.regime = FlightRegime.ORBIT;
        s.body = body;
        s.setState(state);
        s.epochUt = ut;
        s.slot = slot;
        return s;
    }

    /** Position relative to the body centre, metres. */
    Vec3d r() {
        return new Vec3d(rx, ry, rz);
    }

    /** Velocity relative to the body centre, m/s. */
    Vec3d v() {
        return new Vec3d(vx, vy, vz);
    }

    StateVector stateVector() {
        return new StateVector(r(), v());
    }

    void setState(StateVector s) {
        rx = s.r().x();
        ry = s.r().y();
        rz = s.r().z();
        vx = s.v().x();
        vy = s.v().y();
        vz = s.v().z();
    }

    /** Altitude above the surface of {@link #body}, metres. */
    double altitude() {
        return r().length() - FlightDimensions.system(body).radius;
    }
}
