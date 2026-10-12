package dev.apoapsis.net;

import dev.apoapsis.Apoapsis;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: one player-facing message, drawn by the client's own notification layer instead of chat or the
 * action bar, where it would cover the flight instruments. {@code title} is the message; {@code line} is a second
 * line used by Age and first messages (empty for the others). Both are translatable components, so the client reads
 * them in the player's language. See {@code PlayerMessages} for sending with a chat fallback.
 */
public record MessagePayload(Kind kind, Component title, Component line) implements CustomPacketPayload {

    /** What the message is, which decides how the client draws it. */
    public enum Kind {
        /** A flight event or a short status: one calm line near the top of the view. */
        EVENT,
        /** A new Age: a short banner with the Age's name in {@code title} and a sentence in {@code line}. */
        AGE,
        /** A first (achievement): a small chip with its name in {@code title} and a sentence in {@code line}. */
        FIRST,
        /** Something went wrong (a hard landing, a lost ship): like an event line in a warning colour. */
        WARNING
    }

    public static final Type<MessagePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "message"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MessagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.kind);
                ComponentSerialization.STREAM_CODEC.encode(buf, p.title);
                ComponentSerialization.STREAM_CODEC.encode(buf, p.line);
            },
            buf -> new MessagePayload(buf.readEnum(Kind.class),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
