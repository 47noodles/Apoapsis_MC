package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the player's current Age (0..6), sent on login and whenever it changes. */
public record AgeSyncPayload(int age) implements CustomPacketPayload {

    public static final Type<AgeSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "age_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AgeSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeVarInt(p.age),
            buf -> new AgeSyncPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
