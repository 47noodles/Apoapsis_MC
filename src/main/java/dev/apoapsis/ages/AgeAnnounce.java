package dev.apoapsis.ages;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** The Custodian's voice: chat lines and a short chime when the Age goes up. */
final class AgeAnnounce {
    private AgeAnnounce() {}

    /** Announces a reached Age: its chat line from the Custodian and a soft chime to this player only. */
    static void ageReached(ServerPlayer player, int age) {
        custodian(player, line(age));
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvents.UI_TOAST_IN),
                SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                0.6F, 1.0F,
                player.getRandom().nextLong()));
    }

    /** A chat line prefixed with "[Custodian]". */
    static void custodian(ServerPlayer player, Component text) {
        player.sendSystemMessage(Component.translatable("apoapsis.ages.custodian.prefix",
                        Component.translatable("apoapsis.ages.custodian").withStyle(ChatFormatting.AQUA))
                .append(text));
    }

    private static Component line(int age) {
        return switch (age) {
            case 1 -> Component.translatable("apoapsis.ages.line.1");
            case 2 -> Component.translatable("apoapsis.ages.line.2");
            case 3 -> Component.translatable("apoapsis.ages.line.3");
            case 4 -> Component.translatable("apoapsis.ages.line.4");
            default -> Component.translatable("apoapsis.ages.line.reached", String.valueOf(age));
        };
    }
}
