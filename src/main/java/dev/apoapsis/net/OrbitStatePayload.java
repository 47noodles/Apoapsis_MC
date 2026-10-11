package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client, every 5 ticks, to the pilot of a ship (in every regime).
 * The state is relative to {@code body}, in that body's inertial frame (metres, m/s; see the contract, "Frames").
 *
 * @param shipId  entity id of the ship
 * @param body    BodyId ordinal of the central body
 * @param regime  FlightRegime ordinal (SURFACE or ORBIT)
 * @param ut      universal time, seconds
 * @param warp    current time-warp factor (1 = real time)
 */
public record OrbitStatePayload(int shipId, byte body, byte regime, double ut, float warp,
                                double rx, double ry, double rz, double vx, double vy, double vz)
        implements CustomPacketPayload {

    public static final Type<OrbitStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "orbit_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrbitStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.shipId);
                buf.writeByte(p.body);
                buf.writeByte(p.regime);
                buf.writeDouble(p.ut);
                buf.writeFloat(p.warp);
                buf.writeDouble(p.rx);
                buf.writeDouble(p.ry);
                buf.writeDouble(p.rz);
                buf.writeDouble(p.vx);
                buf.writeDouble(p.vy);
                buf.writeDouble(p.vz);
            },
            buf -> new OrbitStatePayload(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readDouble(),
                    buf.readFloat(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
