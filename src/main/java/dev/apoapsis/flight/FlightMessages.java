package dev.apoapsis.flight;

import dev.apoapsis.ship.ShipEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Calm one-line messages from the flight layer to the pilot of a ship. Strings live in {@code flight.json}. */
final class FlightMessages {
    private FlightMessages() {}

    /**
     * A flight event to the pilot of a ship, if a player is flying it: an action-bar line, so the map (which hides
     * chat) and time warp changes do not hide it, plus the same line in chat as a log.
     */
    static void toPilot(ShipEntity ship, Component message) {
        if (ship.getPilot() instanceof ServerPlayer pilot) {
            pilot.sendOverlayMessage(message);
            pilot.sendSystemMessage(message);
        }
    }

    /** An action-bar line to the pilot, if a player is flying the ship. */
    static void overlayToPilot(ShipEntity ship, Component message) {
        if (ship.getPilot() instanceof ServerPlayer pilot) {
            pilot.sendOverlayMessage(message);
        }
    }

    /** The display name of a body. */
    static Component bodyName(BodyId body) {
        return Component.translatable("apoapsis.flight.body." + body.id);
    }
}
