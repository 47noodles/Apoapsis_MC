package dev.apoapsis.net;

import dev.apoapsis.net.MessagePayload.Kind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends a player-facing message on the {@link MessagePayload} channel. A player whose client does not have the channel
 * (a fake player in a GameTest, a client without the mod) gets the old chat line instead, so a message is never lost.
 */
public final class PlayerMessages {
    private PlayerMessages() {}

    /** A flight event or status line. */
    public static void event(ServerPlayer player, Component text) {
        send(player, Kind.EVENT, text, Component.empty());
    }

    /** A warning line. */
    public static void warning(ServerPlayer player, Component text) {
        send(player, Kind.WARNING, text, Component.empty());
    }

    /** An Age banner: the Age's name and a sentence. */
    public static void age(ServerPlayer player, Component name, Component line) {
        send(player, Kind.AGE, name, line);
    }

    /** A first (achievement) chip: its name and a sentence. */
    public static void first(ServerPlayer player, Component name, Component line) {
        send(player, Kind.FIRST, name, line);
    }

    /** Sends the message to the player, as the payload if their client has the channel, else as chat. */
    public static void send(ServerPlayer player, Kind kind, Component title, Component line) {
        if (player.connection != null && player.connection.hasChannel(MessagePayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new MessagePayload(kind, title, line));
            return;
        }
        player.sendSystemMessage(chatFallback(kind, title, line));
    }

    /** The chat form of a message: Age and first lines carry the Custodian's name as they used to. */
    public static Component chatFallback(Kind kind, Component title, Component line) {
        Component body = line.getString().isEmpty() ? title : Component.empty().append(title).append(" ").append(line);
        if (kind == Kind.AGE || kind == Kind.FIRST) {
            return Component.translatable("apoapsis.ages.custodian.prefix",
                    Component.translatable("apoapsis.ages.custodian").withStyle(ChatFormatting.AQUA)).append(body);
        }
        return body;
    }
}
