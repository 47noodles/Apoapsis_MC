package dev.apoapsis.flight;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import org.joml.Quaternionf;

/**
 * Flight module entry point and public API (KSP layer). Node FLT implements it.
 */
public final class Flight {
    private Flight() {}

    /** Registers dimensions keys, saved data, events. Called from the mod constructor. */
    public static void register(IEventBus modBus) {
        FlightAttachments.register(modBus);
        FlightEvents.register();
    }

    /**
     * Spawns a ship from a structure directly into a circular, prograde, equatorial orbit.
     *
     * @param body        central body
     * @param altitudeM   orbit altitude above the body's surface, metres
     * @param phaseDeg    ship's angle around the body (0 = +X axis), degrees
     * @return the spawned ship entity in the apoapsis:orbit dimension, or null on failure
     */
    public static ShipEntity spawnInOrbit(MinecraftServer server, ShipStructure structure, BodyId body,
                                          double altitudeM, double phaseDeg) {
        ServerLevel level = server.getLevel(FlightDimensions.ORBIT);
        if (level == null) {
            return null;
        }
        CelestialBody sys = FlightDimensions.system(body);
        double r = sys.radius + altitudeM;
        double phi = Math.toRadians(phaseDeg);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        Vec3d pos = new Vec3d(r * cos, r * sin, 0);
        double speed = Math.sqrt(sys.mu / r);
        Vec3d vel = new Vec3d(-speed * sin, speed * cos, 0);
        // Prograde is body +Y. Inertial (-sin, cos, 0) maps to orbit-dimension axes (X, Z, -Y) = (-sin, 0, -cos).
        Quaternionf orientation = Frames.noseTowards(-sin, 0, -cos);

        int slot = Transitions.freeSlot(level);
        Vec3 at = FlightDimensions.slotPosition(slot);
        level.getChunk(Mth.floor(at.x) >> 4, Mth.floor(at.z) >> 4);
        ShipEntity ship = ShipEntity.spawn(level, structure, at, orientation);
        ship.setData(FlightAttachments.FLIGHT.get(),
                FlightState.inOrbit(body, new StateVector(pos, vel), UniverseTime.get(server).ut(), slot));
        return ship;
    }

    /**
     * Sets universal time so Pale's phase angle (degrees, from +X, prograde) equals {@code paleDeg} now.
     * Ships already in orbit coast through the jump along their orbits, as time passes for them.
     */
    public static void setPalePhase(MinecraftServer server, double paleDeg) {
        CelestialBody pale = MeridianSystem.PALE;
        double period = pale.orbitPeriod();
        double ut = (Math.toRadians(paleDeg) - pale.orbitPhaseAtEpoch) / (2 * Math.PI) * period;
        ut = ((ut % period) + period) % period;
        UniverseTime.get(server).setUt(ut);
    }

    /** Universal time in seconds for this server (persisted). */
    public static double universalTime(MinecraftServer server) {
        return UniverseTime.get(server).ut();
    }
}
