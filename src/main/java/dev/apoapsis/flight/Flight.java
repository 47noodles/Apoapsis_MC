package dev.apoapsis.flight;

import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;

/**
 * Flight module entry point and public API (KSP layer). STUB from S0; node FLT implements it.
 */
public final class Flight {
    private Flight() {}

    /** Registers dimensions keys, saved data, events. Called from the mod constructor. */
    public static void register(IEventBus modBus) {
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
        return null;
    }

    /** Sets universal time so Pale's phase angle (degrees, from +X, prograde) equals {@code paleDeg} now. */
    public static void setPalePhase(MinecraftServer server, double paleDeg) {
    }

    /** Universal time in seconds for this server (persisted). */
    public static double universalTime(MinecraftServer server) {
        return 0;
    }
}
