package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server, every client tick while the player pilots a ship.
 *
 * @param shipId     entity id of the {@code ShipEntity} being piloted
 * @param pitch      -1, 0 or +1 (W = nose down is -1, S = nose up is +1)
 * @param yaw        -1, 0 or +1 (A = left is -1, D = right is +1)
 * @param roll       -1, 0 or +1 (Q = roll left is -1, E = roll right is +1)
 * @param throttle   one of the THROTTLE_* commands
 * @param flags      bit set of the FLAG_* one-shot presses this tick
 */
public record ShipInputPayload(int shipId, byte pitch, byte yaw, byte roll, byte throttle, byte flags)
        implements CustomPacketPayload {

    public static final byte THROTTLE_HOLD = 0;
    public static final byte THROTTLE_UP = 1;
    public static final byte THROTTLE_DOWN = 2;
    public static final byte THROTTLE_FULL = 3;
    public static final byte THROTTLE_CUT = 4;

    public static final byte FLAG_TOGGLE_SAS = 1;
    public static final byte FLAG_ASSEMBLY = 2;
    public static final byte FLAG_WARP_UP = 4;
    public static final byte FLAG_WARP_DOWN = 8;
    public static final byte FLAG_DEPLOY_CHUTES = 16;

    public static final Type<ShipInputPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "ship_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShipInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.shipId);
                buf.writeByte(p.pitch);
                buf.writeByte(p.yaw);
                buf.writeByte(p.roll);
                buf.writeByte(p.throttle);
                buf.writeByte(p.flags);
            },
            buf -> new ShipInputPayload(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readByte(),
                    buf.readByte(), buf.readByte()));

    public boolean has(byte flag) {
        return (flags & flag) != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
