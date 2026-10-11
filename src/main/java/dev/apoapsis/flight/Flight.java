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
        CelestialBody sys = FlightDimensions.system(body);
        double r = sys.radius + altitudeM;
        return spawnInOrbit(server, structure, body, altitudeM, phaseDeg, Math.sqrt(sys.mu / r), 0, false);
    }

    /**
     * Spawns a ship from a structure into the orbit dimension with a given state: at {@code phaseDeg} around the body,
     * moving at {@code speedMps} along a path angle from prograde. The nose points along the velocity, or against it
     * when {@code noseRetro} is set.
     *
     * @param body         central body
     * @param altitudeM    starting altitude above the body's surface, metres
     * @param phaseDeg     ship's angle around the body (0 = +X axis), degrees
     * @param speedMps     speed relative to the body's centre, metres per second
     * @param pathDegDown  flight path angle, degrees: 0 is prograde, positive points the velocity down toward the body
     * @param noseRetro    true to point the nose opposite the velocity
     * @return the spawned ship entity in the apoapsis:orbit dimension, or null on failure
     */
    public static ShipEntity spawnInOrbit(MinecraftServer server, ShipStructure structure, BodyId body,
                                          double altitudeM, double phaseDeg, double speedMps, double pathDegDown,
                                          boolean noseRetro) {
        ServerLevel level = server.getLevel(FlightDimensions.ORBIT);
        if (level == null) {
            return null;
        }
        CelestialBody sys = FlightDimensions.system(body);
        double r = sys.radius + altitudeM;
        double phi = Math.toRadians(phaseDeg);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        double path = Math.toRadians(pathDegDown);
        double cosPath = Math.cos(path);
        double sinPath = Math.sin(path);
        Vec3d pos = new Vec3d(r * cos, r * sin, 0);
        // Prograde (-sin, cos, 0) tilted by the path angle toward the body, which is -radialOut (-cos, -sin, 0).
        double dirX = -cosPath * sin - sinPath * cos;
        double dirY = cosPath * cos - sinPath * sin;
        Vec3d vel = new Vec3d(speedMps * dirX, speedMps * dirY, 0);
        // Orbit-dimension axes of an inertial (X, Y, Z) are (X, Z, -Y). Prograde maps to (-sin, 0, -cos).
        double sign = noseRetro ? -1 : 1;
        Quaternionf orientation = Frames.noseTowards(sign * dirX, 0, -sign * dirY);

        int slot = OrbitSlots.claim(server, level);
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
}
