package dev.apoapsis.flight;

import dev.apoapsis.net.PlayerMessages;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Calm one-line messages from the flight layer to the pilot of a ship. Strings live in {@code flight.json}. */
final class FlightMessages {
    private FlightMessages() {}

    /**
     * A flight event to the pilot of a ship, if a player is flying it. Sent through {@link PlayerMessages#event}: a
     * modded client gets it as a banner that the map and time warp do not hide; a client without the mod gets a chat
     * line.
     */
    static void toPilot(ShipEntity ship, Component message) {
        if (ship.getPilot() instanceof ServerPlayer pilot) {
            PlayerMessages.event(pilot, message);
        }
    }

    /** A flight status line to the pilot, if a player is flying the ship. Sent through {@link PlayerMessages#event}. */
    static void overlayToPilot(ShipEntity ship, Component message) {
        if (ship.getPilot() instanceof ServerPlayer pilot) {
            PlayerMessages.event(pilot, message);
        }
    }

    /** The display name of a body. */
    static Component bodyName(BodyId body) {
        return Component.translatable("apoapsis.flight.body." + body.id);
    }
}
