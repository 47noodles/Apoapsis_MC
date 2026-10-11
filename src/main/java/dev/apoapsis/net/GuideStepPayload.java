package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: show a Custodian guide step. The client draws it as a small panel in the top right instead of
 * chat, where a step's text would cover the flight instruments. The step is its position in the guide
 * ({@code Guide.Step#ordinal}); the text is the client's own lang entries, so keys read as the player's bindings.
 */
public record GuideStepPayload(int step) implements CustomPacketPayload {

    public static final Type<GuideStepPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "guide_step"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuideStepPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeVarInt(p.step),
            buf -> new GuideStepPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
