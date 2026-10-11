package dev.apoapsis.ship;

import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.net.ShipInputPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server handler for pilot input. Only the pilot of a ship may drive it. */
public final class ShipControls {
    /** Throttle change per tick while UP or DOWN is held: full travel in 40 ticks (2 s). */
    private static final float THROTTLE_STEP = 1f / 40f;

    private ShipControls() {}

    public static void handle(ShipInputPayload payload, IPayloadContext context) {
        Player player = context.player();
        context.enqueueWork(() -> apply(payload, player));
    }

    private static void apply(ShipInputPayload payload, Player player) {
        if (!(player.level().getEntity(payload.shipId()) instanceof ShipEntity ship) || ship.getPilot() != player) {
            return;
        }
        ship.setPilotInput(payload.pitch(), payload.yaw(), payload.roll());

        byte throttle = payload.throttle();
        if (throttle == ShipInputPayload.THROTTLE_UP) {
            ship.setThrottle(ship.getThrottle() + THROTTLE_STEP);
        } else if (throttle == ShipInputPayload.THROTTLE_DOWN) {
            ship.setThrottle(ship.getThrottle() - THROTTLE_STEP);
        } else if (throttle == ShipInputPayload.THROTTLE_FULL) {
            ship.setThrottle(1f);
        } else if (throttle == ShipInputPayload.THROTTLE_CUT) {
            ship.setThrottle(0f);
        }

        if (payload.has(ShipInputPayload.FLAG_TOGGLE_SAS)) {
            ship.setSas(!ship.isSasOn());
        }
        if (payload.has(ShipInputPayload.FLAG_WARP_UP)) {
            FlightHooks.requestWarp(ship, 1);
        }
        if (payload.has(ShipInputPayload.FLAG_WARP_DOWN)) {
            FlightHooks.requestWarp(ship, -1);
        }
        if (payload.has(ShipInputPayload.FLAG_ASSEMBLY)) {
            Component error = ShipAssembler.disassemble(ship);
            if (error != null) {
                player.sendOverlayMessage(error);
            }
        }
    }
}
