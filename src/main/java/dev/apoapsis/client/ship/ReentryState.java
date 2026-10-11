package dev.apoapsis.client.ship;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Reentry;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Re-entry heating of a ship: how hot it glows and which way the air flows past it. In a surface dimension the synced
 * velocity and the body's air at the ship's height give the heat. In the orbit dimension only the ship the local
 * player pilots has data, from {@link ClientState#orbit}; other ships there are not heated. Client only.
 */
public final class ReentryState {
    /** Heating below this intensity counts as none. */
    static final float MIN_INTENSITY = 0.02f;

    private ReentryState() {}

    /**
     * Heating of one ship. {@code intensity} is 0..1 from {@link Reentry#intensity}; {@code wind} is the unit vector the
     * ship moves along, in dimension axes; {@code speed} is in m/s.
     */
    public record Heat(float intensity, Vec3 wind, double speed) {
        /** No heating. */
        public static final Heat NONE = new Heat(0f, Vec3.ZERO, 0);
    }

    /** The heating of {@code ship} now, or {@link Heat#NONE} when it is below {@link #MIN_INTENSITY}. */
    public static Heat of(ShipEntity ship) {
        ResourceKey<Level> dim = ship.level().dimension();
        if (FlightDimensions.ORBIT.equals(dim)) {
            return inOrbit(ship);
        }
        return inAtmosphere(ship, dim);
    }

    private static Heat inAtmosphere(ShipEntity ship, ResourceKey<Level> dim) {
        Vec3 v = ship.getVelocity();
        double speed = v.length();
        if (speed <= 0.0) {
            return Heat.NONE;
        }
        CelestialBody sys = FlightDimensions.system(FlightDimensions.surfaceBody(dim));
        if (sys.seaLevelDensity <= 0.0) {
            return Heat.NONE;
        }
        double h = Math.max(ship.getY() - FlightDimensions.seaLevel(dim), 0.0);
        return heat(speed, sys.density(h), v.scale(1.0 / speed));
    }

    private static Heat inOrbit(ShipEntity ship) {
        OrbitStatePayload p = ClientState.orbit;
        if (p == null || p.shipId() != ship.getId()) {
            return Heat.NONE;
        }
        CelestialBody body = MeridianSystem.byId(BodyId.byOrdinal(p.body()).id);
        StateVector s = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT
                ? OrbitNow.stateNow(p, 0f)
                : new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        Vec3d v = s.v();
        double speed = v.length();
        if (speed <= 0.0) {
            return Heat.NONE;
        }
        double alt = s.r().length() - body.radius;
        // Inertial velocity mapped to orbit-dimension axes (X, Z, -Y), as FlightHooks.velocityDirection does.
        Vec3 wind = new Vec3(v.x(), v.z(), -v.y()).scale(1.0 / speed);
        return heat(speed, body.density(Math.max(alt, 0.0)), wind);
    }

    /** The heating for a speed in m/s and an air density in kg/m^3, or NONE below the threshold. */
    private static Heat heat(double speed, double rho, Vec3 wind) {
        float intensity = (float) Reentry.intensity(speed, rho);
        return intensity < MIN_INTENSITY ? Heat.NONE : new Heat(intensity, wind, speed);
    }
}
