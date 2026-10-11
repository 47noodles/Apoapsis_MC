package dev.apoapsis.client.ship;

import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Builds and sends the pilot's {@link ShipInputPayload} every client tick. Client only. */
public final class PilotInput {
    private PilotInput() {}

    /**
     * Reads this tick's pilot keys and sends them to the server. Runs at the start of the client tick, before vanilla
     * handles keys. Presses are taken on every tick; the payload is sent only while the local player rides a ship.
     * Key state, and the warp keys, are sent only with no screen open.
     */
    public static void tick(Minecraft mc) {
        boolean full = PilotKeys.pressed(PilotKeys.THROTTLE_FULL);
        boolean cut = PilotKeys.pressed(PilotKeys.THROTTLE_CUT);
        boolean sas = PilotKeys.pressed(PilotKeys.SAS);
        boolean disassemble = PilotKeys.pressed(PilotKeys.DISASSEMBLE);
        boolean warpUp = PilotKeys.pressed(PilotKeys.WARP_UP);
        boolean warpDown = PilotKeys.pressed(PilotKeys.WARP_DOWN);

        Entity vehicle = mc.player == null ? null : mc.player.getVehicle();
        if (!(vehicle instanceof ShipEntity ship) || mc.getConnection() == null) {
            return;
        }
        PilotKeys.swallowVanillaClashes(mc.options);

        // While a screen (chat, inventory, pause, system map) has focus, held keys send as at rest; presses were still
        // taken above. The system map sends its own warp presses (MapScreen.keyPressed).
        Screen screen = mc.gui.screen();
        boolean inGame = screen == null;
        Options options = mc.options;

        byte pitch = 0;
        byte yaw = 0;
        byte roll = 0;
        byte throttle = ShipInputPayload.THROTTLE_HOLD;
        byte flags = 0;
        if (inGame) {
            pitch = axis(options.keyDown.isDown(), options.keyUp.isDown());
            yaw = axis(options.keyRight.isDown(), options.keyLeft.isDown());
            roll = axis(PilotKeys.ROLL_RIGHT.isDown(), PilotKeys.ROLL_LEFT.isDown());
            throttle = throttle(full, cut, PilotKeys.THROTTLE_UP.isDown(), PilotKeys.THROTTLE_DOWN.isDown());
            if (sas) {
                flags |= ShipInputPayload.FLAG_TOGGLE_SAS;
            }
            if (disassemble) {
                flags |= ShipInputPayload.FLAG_ASSEMBLY;
            }
            // Time warp is read only with no screen: vanilla clicks the warp keys only then.
            if (warpUp) {
                flags |= ShipInputPayload.FLAG_WARP_UP;
            }
            if (warpDown) {
                flags |= ShipInputPayload.FLAG_WARP_DOWN;
            }
        }
        ShipInputPayload payload = new ShipInputPayload(ship.getId(), pitch, yaw, roll, throttle, flags);
        ClientPacketDistributor.sendToServer(payload);
    }

    /** -1 for the negative key, +1 for the positive key, 0 when neither or both are held. */
    private static byte axis(boolean positive, boolean negative) {
        return (byte) ((positive ? 1 : 0) - (negative ? 1 : 0));
    }

    /** One-shot presses win over held keys; cut beats full. Held R and F together do nothing. */
    private static byte throttle(boolean full, boolean cut, boolean up, boolean down) {
        if (cut) {
            return ShipInputPayload.THROTTLE_CUT;
        }
        if (full) {
            return ShipInputPayload.THROTTLE_FULL;
        }
        if (up == down) {
            return ShipInputPayload.THROTTLE_HOLD;
        }
        return up ? ShipInputPayload.THROTTLE_UP : ShipInputPayload.THROTTLE_DOWN;
    }
}
