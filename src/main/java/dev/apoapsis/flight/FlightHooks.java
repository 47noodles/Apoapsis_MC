package dev.apoapsis.flight;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The seam between the ship (SE layer) and flight (KSP layer). {@link ShipEntity#tick()} calls these on the server.
 * Signatures are fixed by docs/dev/prototype-contract.md.
 */
public final class FlightHooks {
    private FlightHooks() {}

    /** Environment at the ship's current position. Called once per server tick before physics. */
    public static Environment environment(ShipEntity ship) {
        ResourceKey<Level> dimension = ship.level().dimension();
        if (FlightDimensions.ORBIT.equals(dimension)) {
            FlightState st = FlightAttachments.state(ship);
            CelestialBody body = FlightDimensions.system(st.body);
            double rho = body.density(Math.max(st.altitude(), 0.0));
            return new Environment(Vec3.ZERO, rho, pressureFraction(body, rho), false, true);
        }
        // Pale's surface, the overworld, and any other dimension (Verdance-like defaults).
        BodyId body = FlightDimensions.surfaceBody(dimension);
        CelestialBody sys = FlightDimensions.system(body);
        double h = ship.getY() - FlightDimensions.seaLevel(body);
        double g = sys.gravity(sys.radius + h);
        double rho = sys.density(Math.max(h, 0.0));
        return new Environment(new Vec3(0, -g, 0), rho, pressureFraction(sys, rho), true, false);
    }

    private static double pressureFraction(CelestialBody body, double rho) {
        return body.seaLevelDensity > 0 ? rho / body.seaLevelDensity : 0.0;
    }

    /**
     * Called each server tick when {@link Environment#orbital()} is true, after the ship integrated its rotation.
     * Advances the orbit to the current universal time; the ship keeps its slot position in the orbit dimension.
     *
     * @param thrustAccelWorld thrust acceleration (m/s^2) in the orbit dimension's local axes this tick
     * @param dt               seconds of real time this tick (0.05); unused, FLT advances by universal time
     */
    public static void orbitalTick(ShipEntity ship, Vec3 thrustAccelWorld, double dt) {
        if (!(ship.level() instanceof ServerLevel level)) {
            return;
        }
        FlightState st = FlightAttachments.state(ship);
        if (st.regime != FlightRegime.ORBIT) {
            return;
        }
        OrbitFlight.advance(ship, st, thrustAccelWorld, UniverseTime.get(level.getServer()).ut());
    }

    /** Called at the end of every server tick of every ship: regime transitions, milestones, client sync. */
    public static void afterTick(ShipEntity ship) {
        FlightRules.afterTick(ship);
    }

    /** Current warp factor for this ship (1 in real time). Ships scale their per-tick physics by nothing; FLT owns warp. */
    public static float warp(ShipEntity ship) {
        return FlightWarp.factorFor(ship);
    }

    /** Pilot pressed warp up (+1) or down (-1). */
    public static void requestWarp(ShipEntity ship, int direction) {
        FlightWarp.request(ship, direction);
    }
}
