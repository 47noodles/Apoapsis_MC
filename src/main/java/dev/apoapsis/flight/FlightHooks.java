package dev.apoapsis.flight;

import dev.apoapsis.ship.ShipEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The seam between the ship (SE layer) and flight (KSP layer). {@link ShipEntity#tick()} calls these on the server.
 * STUB from S0: Verdance surface everywhere. Node FLT replaces the bodies of these methods; signatures are fixed.
 */
public final class FlightHooks {
    private FlightHooks() {}

    /** Environment at the ship's current position. Called once per server tick before physics. */
    public static Environment environment(ShipEntity ship) {
        double h = ship.getY() - 63.0;
        double rho = h > 12000 ? 0.0 : 1.225 * Math.exp(-Math.max(h, 0) / 2000.0);
        double r = 64000.0 + Math.max(h, 0);
        double g = 4.01818e10 / (r * r);
        return new Environment(new Vec3(0, -g, 0), rho, rho / 1.225, true, false);
    }

    /**
     * Called each server tick when {@link Environment#orbital()} is true, after the ship integrated its rotation.
     *
     * @param thrustAccelWorld thrust acceleration (m/s^2) in the orbit dimension's local axes this tick
     * @param dt               seconds of real time this tick (0.05); FLT applies its own warp factor
     */
    public static void orbitalTick(ShipEntity ship, Vec3 thrustAccelWorld, double dt) {
    }

    /** Called at the end of every server tick of every ship: regime transitions, milestones, client sync. */
    public static void afterTick(ShipEntity ship) {
    }

    /** Current warp factor for this ship (1 in real time). Ships scale their per-tick physics by nothing; FLT owns warp. */
    public static float warp(ShipEntity ship) {
        return 1.0f;
    }

    /** Pilot pressed warp up (+1) or down (-1). */
    public static void requestWarp(ShipEntity ship, int direction) {
    }
}
