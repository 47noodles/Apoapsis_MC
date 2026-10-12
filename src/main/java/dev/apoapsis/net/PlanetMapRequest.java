package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: please stream the tiles of refined planet map {@code level} (1 and up) of {@code body}. A client
 * asks when it first draws that body from far away; the server answers only a player who flies or sits in orbit (or
 * stands on Pale, where Verdance is always in the sky), once per body and level, and may ignore the request.
 */
public record PlanetMapRequest(String body, int level) implements CustomPacketPayload {
    public static final Type<PlanetMapRequest> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "planet_map_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetMapRequest> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.body, 32);
                buf.writeVarInt(r.level);
            },
            buf -> new PlanetMapRequest(buf.readUtf(32), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
