package dev.apoapsis.flight;

import dev.apoapsis.ship.ShipEntity;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Time warp. One ship at a time may warp: the one its single pilot flies. Held in memory, cleared on server stop.
 * Levels 1, 5, 10, 50, 100, 1000 apply to universal time only; physics follows it through the orbit model.
 */
final class FlightWarp {
    static final int[] LEVELS = {1, 5, 10, 50, 100, 1000};

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

    /** Pilot pressed warp up (+1) or down (-1). Refused with a calm reason when the conditions do not hold. */
    static void request(ShipEntity ship, int direction) {
        if (!allowed(ship)) {
            clear();
            FlightMessages.overlayToPilot(ship, Component.translatable("apoapsis.flight.warp.need"));
            return;
        }
        int current = ship.getUUID().equals(shipId) ? index : 0;
        int next = Math.max(0, Math.min(LEVELS.length - 1, current + Integer.signum(direction)));
        if (next == current) {
            return;
        }
        index = next;
        shipId = next == 0 ? null : ship.getUUID();
        FlightMessages.overlayToPilot(ship, next == 0
                ? Component.translatable("apoapsis.flight.warp.off")
                : Component.translatable("apoapsis.flight.warp.level", LEVELS[next]));
    }

    /** Drops the warp to 1 when the warping ship no longer meets the conditions, with the reason on the action bar. */
    static void validate(ShipEntity ship) {
        if (shipId == null || !shipId.equals(ship.getUUID())) {
            return;
        }
        if (!allowed(ship)) {
            clear();
            FlightMessages.overlayToPilot(ship, Component.translatable("apoapsis.flight.warp.need"));
        }
    }

    /** Drops the warp to 1 silently (SOI change, transition, server stop). */
    static void clear() {
        shipId = null;
        index = 0;
    }

    private static boolean allowed(ShipEntity ship) {
        FlightState st = FlightAttachments.state(ship);
        if (!(ship.level() instanceof ServerLevel level)) {
            return false;
        }
        return level.getServer().getPlayerCount() == 1
                && ship.getPilot() != null
                && st.regime == FlightRegime.ORBIT
                && ship.getThrottle() <= 0f
                && st.altitude() > FlightDimensions.system(st.body).atmosphereTop;
    }

}
