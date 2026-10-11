package dev.apoapsis.flight;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.ship.ShipEntity;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Time warp. One ship at a time may warp: the one its single pilot flies. Held in memory, cleared on server stop.
 * Levels 1, 5, 10, 50, 100, 1000 apply to universal time only; physics follows it through the orbit model.
 * Warp needs the ship above max(atmosphere, {@link #MIN_ALTITUDE_M}). Before each tick the level is held to the
 * highest one whose next tick keeps the ship {@link #MARGIN_M} above its transition altitude and inside its sphere
 * of influence, so a warped tick can never carry the ship below the surface.
 */
final class FlightWarp {
    static final int[] LEVELS = {1, 5, 10, 50, 100, 1000};
    /** Lowest altitude that allows warp at all, metres (or the body's atmosphere top, if higher). */
    static final double MIN_ALTITUDE_M = 10_000.0;
    /** Height a warped tick must keep above the transition altitude, metres. */
    static final double MARGIN_M = 2_000.0;
    /** Points sampled along one warped tick when its path is checked. */
    static final int SAMPLES = 16;

    private static UUID shipId;
    private static int index;

    private FlightWarp() {}

    /** Warp factor for the server this tick: 1 unless a ship holds a warp and the server still has one player. */
    static float factor(MinecraftServer server) {
        if (shipId == null) {
            return 1f;
        }
        if (server.getPlayerCount() != 1) {
            clear();
            return 1f;
        }
        return LEVELS[index];
    }

    /** Warp factor for one ship (1 unless it holds the warp). */
    static float factorFor(ShipEntity ship) {
        return ship.getUUID().equals(shipId) ? LEVELS[index] : 1f;
    }

    /** Pilot pressed warp up (+1) or down (-1). Refused with the failing condition when it does not hold. */
    static void request(ShipEntity ship, int direction) {
        Component refusal = refusal(ship);
        if (refusal != null) {
            clear();
            FlightMessages.overlayToPilot(ship, refusal);
            return;
        }
        int current = ship.getUUID().equals(shipId) ? index : 0;
        int wanted = Math.max(0, Math.min(LEVELS.length - 1, current + Integer.signum(direction)));
        if (wanted == current) {
            return;
        }
        int next = Math.min(wanted, highestSafe(ship, wanted));
        if (next == current) {
            FlightMessages.overlayToPilot(ship, held(current));
            return;
        }
        setLevel(ship, next);
        FlightMessages.overlayToPilot(ship, next < wanted ? held(next) : levelMessage(next));
    }

    /**
     * Drops the warp when the warping ship no longer meets the conditions, and holds it to the highest level whose
     * next tick stays clear of the surface and of any SOI change. The reason goes to the pilot's action bar.
     */
    static void validate(ShipEntity ship) {
        if (shipId == null || !shipId.equals(ship.getUUID())) {
            return;
        }
        Component refusal = refusal(ship);
        if (refusal != null) {
            clear();
            FlightMessages.overlayToPilot(ship, refusal);
            return;
        }
        int safe = highestSafe(ship, index);
        if (safe < index) {
            setLevel(ship, safe);
            FlightMessages.overlayToPilot(ship, held(safe));
        }
    }

    /** Drops the warp to 1 silently (SOI change, transition, server stop). */
    static void clear() {
        shipId = null;
        index = 0;
    }

    /** The first condition that stops warp for this ship, or null when warp is allowed. */
    private static Component refusal(ShipEntity ship) {
        if (!(ship.level() instanceof ServerLevel level)) {
            return Component.translatable("apoapsis.flight.warp.orbit");
        }
        if (level.getServer().getPlayerCount() != 1) {
            return Component.translatable("apoapsis.flight.warp.singleplayer");
        }
        if (ship.getPilot() == null) {
            return Component.translatable("apoapsis.flight.warp.pilot");
        }
        FlightState st = FlightAttachments.state(ship);
        if (st.regime != FlightRegime.ORBIT) {
            return Component.translatable("apoapsis.flight.warp.orbit");
        }
        if (ship.getThrottle() > 0f) {
            return Component.translatable("apoapsis.flight.warp.engines");
        }
        CelestialBody body = FlightDimensions.system(st.body);
        if (st.altitude() <= Math.max(body.atmosphereTop, MIN_ALTITUDE_M)) {
            return body.atmosphereTop > 0
                    ? Component.translatable("apoapsis.flight.warp.atmosphere")
                    : Component.translatable("apoapsis.flight.warp.too_close", FlightMessages.bodyName(st.body));
        }
        return null;
    }

    /** The highest level index at or below {@code from} whose next tick stays clear; 0 (real time) when none does. */
    private static int highestSafe(ShipEntity ship, int from) {
        if (!(ship.level() instanceof ServerLevel level)) {
            return 0;
        }
        double ut = UniverseTime.get(level.getServer()).ut();
        FlightState st = FlightAttachments.state(ship);
        for (int i = from; i >= 1; i--) {
            if (clearOverTick(st, ut, FlightEvents.TICK_SECONDS * LEVELS[i])) {
                return i;
            }
        }
        return 0;
    }

    /**
     * True when the state, advanced from its epoch to {@code ut} and then over one warped tick of {@code dt} seconds,
     * stays {@link #MARGIN_M} above the transition altitude and inside the same sphere of influence at every sample.
     * The samples are exact Kepler states, so the check matches the physics of the coming tick.
     */
    static boolean clearOverTick(FlightState st, double ut, double dt) {
        CelestialBody body = FlightDimensions.system(st.body);
        double floor = FlightDimensions.transitionAltitude(st.body) + MARGIN_M;
        double step = dt / SAMPLES;
        StateVector s = Kepler.propagate(body.mu, st.stateVector(), ut - st.epochUt);
        double t = ut;
        for (int k = 0; k < SAMPLES; k++) {
            s = Kepler.propagate(body.mu, s, step);
            t += step;
            if (s.r().length() - body.radius < floor || soiChanged(st.body, s, t)) {
                return false;
            }
        }
        return true;
    }

    /** Mirrors the sphere-of-influence test of OrbitFlight: true where the ship would switch body at time {@code ut}. */
    private static boolean soiChanged(BodyId body, StateVector s, double ut) {
        CelestialBody pale = MeridianSystem.PALE;
        if (body == BodyId.VERDANCE) {
            return s.r().sub(pale.stateAt(ut).r()).length() < pale.soiRadius();
        }
        return s.r().length() > pale.soiRadius();
    }

    private static void setLevel(ShipEntity ship, int level) {
        index = level;
        shipId = level == 0 ? null : ship.getUUID();
    }

    private static Component held(int level) {
        return level == 0
                ? Component.translatable("apoapsis.flight.warp.surface")
                : Component.translatable("apoapsis.flight.warp.held", LEVELS[level]);
    }

    private static Component levelMessage(int level) {
        return level == 0
                ? Component.translatable("apoapsis.flight.warp.off")
                : Component.translatable("apoapsis.flight.warp.level", LEVELS[level]);
    }
}
