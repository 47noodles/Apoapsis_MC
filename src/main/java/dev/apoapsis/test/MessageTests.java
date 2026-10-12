package dev.apoapsis.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.ages.Firsts;
import dev.apoapsis.net.MessagePayload;
import dev.apoapsis.net.MessagePayload.Kind;
import dev.apoapsis.net.PlayerMessages;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** GameTests for the message channel ({@link MessagePayload}, {@link PlayerMessages}) that keeps flight text off the chat. */
final class MessageTests {
    private MessageTests() {}

    /** Every kind survives the codec with its translatable title and line, and an empty line stays empty. */
    @SuppressWarnings("removal")
    static void messagePayloadRoundTrips(GameTestHelper helper, ShipCleanup ships) {
        for (Kind kind : Kind.values()) {
            Component title = Component.translatable("apoapsis.flight.enter_orbit",
                    Component.translatable("apoapsis.flight.body.verdance"));
            Component line = kind == Kind.AGE || kind == Kind.FIRST
                    ? Component.translatable("apoapsis.firsts.orbit.line") : Component.empty();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                    helper.getLevel().getServer().registryAccess());
            MessagePayload.STREAM_CODEC.encode(buf, new MessagePayload(kind, title, line));
            MessagePayload back = MessagePayload.STREAM_CODEC.decode(buf);
            helper.assertTrue(back.kind() == kind, kind + " came back as " + back.kind());
            helper.assertTrue(back.title().equals(title), kind + " lost its title in the codec round trip");
            helper.assertTrue(back.line().equals(line), kind + " lost its line in the codec round trip");
            helper.assertTrue(buf.readableBytes() == 0, kind + " left bytes unread");
        }
        helper.succeed();
    }

    /**
     * A player with no client for the channel (these mock players) is sent every kind as chat, so a message is never lost
     * and sending never fails; the chat form of an Age or a first carries the Custodian's name.
     */
    @SuppressWarnings("removal")
    static void messagesFallBackToChat(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerMessages.event(player, Component.translatable("apoapsis.flight.leave_orbit"));
        PlayerMessages.warning(player, Component.translatable("message.apoapsis.ship.hard_landing"));
        PlayerMessages.age(player, Component.translatable("apoapsis.ages.format", "4",
                Component.translatable("apoapsis.ages.name.4")), Component.translatable("apoapsis.ages.line.4"));
        PlayerMessages.first(player, Component.translatable("apoapsis.advancement.first_flight.title"),
                Component.translatable("apoapsis.firsts.first_flight.line"));
        Component age = PlayerMessages.chatFallback(Kind.AGE, Component.literal("Age 4"), Component.literal("Go."));
        helper.assertTrue(age.getString().contains("Age 4") && age.getString().contains("Go.")
                        && age.getString().contains("[") && age.getString().contains("Custodian"),
                "the chat form of an Age reads '" + age.getString() + "'");
        Component event = PlayerMessages.chatFallback(Kind.EVENT, Component.literal("Leaving orbit"), Component.empty());
        helper.assertTrue(event.getString().equals("Leaving orbit"), "an event is its own text in chat, not '"
                + event.getString() + "'");
        helper.succeed();
    }

    /** The lang entries the notification layer and its senders read are all there, for every Age and every first. */
    @SuppressWarnings("removal")
    static void messageTextIsComplete(GameTestHelper helper, ShipCleanup ships) {
        InputStream stream = MessageTests.class.getResourceAsStream("/assets/" + Apoapsis.MODID + "/lang/en_us.json");
        if (stream == null) {
            helper.fail("the merged en_us lang file is not on the classpath");
            return;
        }
        JsonObject lang;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            helper.fail("the lang file could not be read: " + e.getMessage());
            return;
        }
        require(helper, lang, "apoapsis.notify.first");
        require(helper, lang, "apoapsis.ages.format");
        for (int age = 0; age <= Ages.MAX_AGE; age++) {
            require(helper, lang, "apoapsis.ages.name." + age);
        }
        for (int age = 1; age <= 4; age++) {
            require(helper, lang, "apoapsis.ages.line." + age);
        }
        require(helper, lang, "apoapsis.ages.line.start");
        require(helper, lang, "apoapsis.ages.line.reached");
        for (Firsts.First first : Firsts.First.values()) {
            require(helper, lang, "apoapsis.firsts." + first.id() + ".line");
            require(helper, lang, "apoapsis.advancement." + first.id() + ".title");
        }
        helper.succeed();
    }

    private static void require(GameTestHelper helper, JsonObject lang, String key) {
        if (!lang.has(key) || lang.get(key).getAsString().isBlank()) {
            helper.fail("lang key missing or empty: " + key);
        }
    }
}
