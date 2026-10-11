package dev.apoapsis.flight;

import dev.apoapsis.ship.ShipEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Calm one-line messages from the flight layer to the pilot of a ship. Strings live in {@code flight.json}. */
final class FlightMessages {
    private FlightMessages() {}

    /** A chat line to the pilot, if a player is flying the ship. */
    static void toPilot(ShipEntity ship, Component message) {
        if (ship.getPilot() instanceof ServerPlayer pilot) {
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
