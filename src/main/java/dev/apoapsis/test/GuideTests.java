package dev.apoapsis.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.ages.Firsts;
import dev.apoapsis.ages.Guide;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * GameTests for the Custodian's survival guide ({@link Guide}): steps open on login, on firsts, on crafting a cockpit and
 * on the guide command; they survive a save; a save from before the guide is caught up quietly; and the lang text is
 * complete.
 */
final class GuideTests {
    private GuideTests() {}

    /** A new player has the first step from the login start, and reaching it again shows nothing. */
    @SuppressWarnings("removal")
    static void guideFirstStepOnStart(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(Guide.has(player, Guide.Step.PARTS), "a new player has no PARTS step");
        helper.assertTrue(!Guide.has(player, Guide.Step.BUILD), "BUILD is shown before a cockpit is crafted");
        helper.assertTrue(Guide.current(player) == Guide.Step.PARTS,
                "current step is " + Guide.current(player) + ", expected PARTS");
        helper.assertTrue(!Guide.reach(player, Guide.Step.PARTS), "PARTS was shown again by reach");
        helper.succeed();
    }

    /** Each first, boarding and coming home opens its own step, once, and the latest step is current. */
    @SuppressWarnings("removal")
    static void guideFirstsOpenSteps(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        opensOn(helper, player, Guide.Step.ORBIT, () -> Firsts.record(player, Firsts.First.SUBORBITAL));
        opensOn(helper, player, Guide.Step.MAP, () -> Firsts.record(player, Firsts.First.ORBIT));
        opensOn(helper, player, Guide.Step.PALE, () -> Firsts.record(player, Firsts.First.PALE_FLYBY));
        opensOn(helper, player, Guide.Step.SAMPLE, () -> Firsts.record(player, Firsts.First.PALE_LANDING));
        opensOn(helper, player, Guide.Step.HOME, () -> Guide.homeward(player));
        opensOn(helper, player, Guide.Step.DONE, () -> Firsts.record(player, Firsts.First.PALE_SAMPLE));
        opensOn(helper, player, Guide.Step.ASCENT, () -> Ages.onFirstFlight(player));
        opensOn(helper, player, Guide.Step.LAUNCH, () -> Guide.onBoard(player));
        helper.assertTrue(Guide.current(player) == Guide.Step.DONE,
                "current step is " + Guide.current(player) + ", expected DONE");
        helper.succeed();
    }

    /** A step a first already opened is not shown again by reaching it, or by recording the first again. */
    @SuppressWarnings("removal")
    static void guideStepShownOnce(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Firsts.record(player, Firsts.First.SUBORBITAL);
        helper.assertTrue(!Guide.reach(player, Guide.Step.ORBIT),
                "ORBIT was shown again by reach after SUBORBITAL opened it");
        int before = Firsts.recorded(player).size();
        Firsts.record(player, Firsts.First.SUBORBITAL);
        helper.assertTrue(Firsts.recorded(player).size() == before, "recording SUBORBITAL twice added a second entry");
        helper.assertTrue(Guide.reach(player, Guide.Step.MAP), "MAP was not shown by its first reach");
        helper.assertTrue(!Guide.reach(player, Guide.Step.MAP), "MAP was shown again by a second reach");
        helper.succeed();
    }

    /** Crafting a cockpit opens BUILD; crafting another part does not. */
    @SuppressWarnings("removal")
    static void guideCockpitCraftOpensBuild(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!Guide.has(player, Guide.Step.BUILD), "BUILD was shown before any craft");
        craft(player, TestSupport.item(helper, "hull_plate"));
        helper.assertTrue(!Guide.has(player, Guide.Step.BUILD), "crafting a hull plate opened BUILD");
        craft(player, TestSupport.item(helper, "cockpit"));
        helper.assertTrue(Guide.has(player, Guide.Step.BUILD), "crafting a cockpit did not open BUILD");
        helper.succeed();
    }

    /** The shown steps are saved with the player and come back after a load. */
    @SuppressWarnings("removal")
    static void guideProgressSurvivesSave(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer a = helper.makeMockServerPlayerInLevel();
        Firsts.record(a, Firsts.First.SUBORBITAL);
        Firsts.record(a, Firsts.First.ORBIT);
        CompoundTag tag = save(helper, a);
        ServerPlayer b = helper.makeMockServerPlayerInLevel();
        load(helper, b, tag);
        helper.assertTrue(Guide.has(b, Guide.Step.PARTS), "PARTS was lost in the save");
        helper.assertTrue(Guide.has(b, Guide.Step.ORBIT), "ORBIT was lost in the save");
        helper.assertTrue(Guide.has(b, Guide.Step.MAP), "MAP was lost in the save");
        helper.assertTrue(!Guide.has(b, Guide.Step.PALE), "PALE was shown after load without PALE_FLYBY");
        helper.assertTrue(Guide.current(b) == Guide.Step.MAP,
                "current step after load is " + Guide.current(b) + ", expected MAP");
        helper.succeed();
    }

    /** A started player whose save has no guide record is marked past the steps their progress covers. */
    @SuppressWarnings("removal")
    static void guideOldSaveIsCaughtUp(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer a = helper.makeMockServerPlayerInLevel();
        Firsts.record(a, Firsts.First.SUBORBITAL);
        CompoundTag tag = save(helper, a);
        // Imitate a save from before the guide: drop the guide's entry, keep the firsts and the started flag.
        CompoundTag attachments = tag.getCompoundOrEmpty("neoforge:attachments");
        String guideKey = TestSupport.id("guide").toString();
        if (!attachments.contains(guideKey)) {
            helper.fail("the save has no " + guideKey + " entry; attachment keys: "
                    + String.join(", ", attachments.keySet()));
        }
        // A load only overwrites the entries a tag holds, so the old save carries an empty guide, not none.
        CompoundTag emptyGuide = new CompoundTag();
        emptyGuide.put("guide", new ListTag());
        attachments.put(guideKey, emptyGuide);
        ServerPlayer b = helper.makeMockServerPlayerInLevel();
        load(helper, b, tag);
        helper.assertTrue(!Guide.has(b, Guide.Step.PARTS), "B's guide is not empty before the start");
        Ages.start(b);
        helper.assertTrue(Guide.has(b, Guide.Step.PARTS), "PARTS is not shown after the catch-up");
        helper.assertTrue(Guide.has(b, Guide.Step.BUILD), "BUILD was not caught up");
        helper.assertTrue(Guide.has(b, Guide.Step.LAUNCH), "LAUNCH was not caught up");
        helper.assertTrue(Guide.has(b, Guide.Step.ORBIT), "ORBIT was not caught up from SUBORBITAL");
        helper.assertTrue(!Guide.has(b, Guide.Step.MAP), "MAP was caught up without ORBIT");
        helper.assertTrue(Guide.current(b) == Guide.Step.ORBIT,
                "current step after catch-up is " + Guide.current(b) + ", expected ORBIT");
        helper.succeed();
    }

    /** /apoapsis guide returns the current step and /apoapsis guide book hands over the whole guide. */
    @SuppressWarnings("removal")
    static void guideCommandShowsCurrent(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Firsts.record(player, Firsts.First.SUBORBITAL);
        try {
            int shown = helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute("apoapsis guide", player.createCommandSourceStack());
            helper.assertTrue(shown == Guide.Step.ORBIT.number(),
                    "/apoapsis guide returned " + shown + ", expected " + Guide.Step.ORBIT.number());
            int book = helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute("apoapsis guide book", player.createCommandSourceStack());
            helper.assertTrue(book == 1, "/apoapsis guide book returned " + book + ", expected 1");
            WrittenBookContent content = null;
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() == Items.WRITTEN_BOOK) {
                    content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
                }
            }
            helper.assertTrue(content != null, "the guide book is not in the inventory");
            int pages = Guide.TOTAL * Guide.LINES;
            helper.assertTrue(content.pages().size() == pages,
                    "the guide book has " + content.pages().size() + " pages, expected " + pages);
            helper.assertTrue("Custodian Guide".equals(content.title().raw()),
                    "the guide book title is '" + content.title().raw() + "', expected 'Custodian Guide'");
        } catch (CommandSyntaxException e) {
            helper.fail("guide command did not parse: " + e.getMessage());
        }
        helper.succeed();
    }

    /** Every step has its title and body lines in the lang file, and no line names a missing key argument. */
    @SuppressWarnings("removal")
    static void guideTextIsComplete(GameTestHelper helper, ShipCleanup ships) {
        InputStream stream = GuideTests.class.getResourceAsStream("/assets/" + Apoapsis.MODID + "/lang/en_us.json");
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
        Pattern placeholder = Pattern.compile("%(\\d+)\\$s");
        for (Guide.Step step : Guide.Step.values()) {
            requireKey(helper, lang, step.titleKey());
            for (int line = 1; line <= Guide.LINES; line++) {
                String key = step.lineKey(line);
                requireKey(helper, lang, key);
                Matcher matcher = placeholder.matcher(lang.get(key).getAsString());
                while (matcher.find()) {
                    int n = Integer.parseInt(matcher.group(1));
                    helper.assertTrue(n >= 1 && n <= step.args().length,
                            key + " uses %" + n + "$s but the step has " + step.args().length + " arguments");
                }
            }
        }
        helper.succeed();
    }

    /** Asserts the step is not shown yet, runs the trigger, then asserts the trigger showed it. */
    private static void opensOn(GameTestHelper helper, ServerPlayer player, Guide.Step step, Runnable trigger) {
        helper.assertTrue(!Guide.has(player, step), step.id() + " was shown before its trigger");
        trigger.run();
        helper.assertTrue(Guide.has(player, step), step.id() + " was not shown by its trigger");
    }

    private static void craft(ServerPlayer player, Item item) {
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, new ItemStack(item), new SimpleContainer(1)));
    }

    /** Saves the player the way a world save does, and returns the tag. */
    private static CompoundTag save(GameTestHelper helper, ServerPlayer player) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess());
        player.saveWithoutId(out);
        return out.buildResult();
    }

    /** Loads a saved tag into the player, as a world load does. */
    private static void load(GameTestHelper helper, ServerPlayer player, CompoundTag tag) {
        player.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    private static void requireKey(GameTestHelper helper, JsonObject lang, String key) {
        if (!lang.has(key)) {
            helper.fail("lang key missing: " + key);
        }
    }
}
