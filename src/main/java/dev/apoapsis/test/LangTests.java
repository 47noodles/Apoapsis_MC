package dev.apoapsis.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Checks that the text keys the map's top readout can show are in the lang file, so no raw key reaches the screen. */
final class LangTests {
    /** The keys of the figures list, the key hints and the clock of the 3D and 2D maps ({@code Map3DScreen}, {@code MapScreen}). */
    private static final String[] MAP_TOP_KEYS = {
            "apoapsis.map.stat.alt", "apoapsis.map.stat.speed", "apoapsis.map.stat.ap", "apoapsis.map.stat.pe",
            "apoapsis.map.stat.inc", "apoapsis.map.stat.escape", "apoapsis.map.stat.gravity", "apoapsis.map.stat.radius",
            "apoapsis.map.stat.atmosphere", "apoapsis.map.stat.none", "apoapsis.map.stat.orbit", "apoapsis.map.stat.period",
            "apoapsis.map.stat.in", "apoapsis.map.hint.zoom", "apoapsis.map.hint.rotate", "apoapsis.map.hint.pan",
            "apoapsis.map.hint.focus", "apoapsis.map.hint.close", "apoapsis.map.hint.more", "apoapsis.map.warp",
            "apoapsis.map.time", "apoapsis.map3d.key.map3d"};

    private LangTests() {}

    /** Registers the tests. */
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        event.registerTest(TestSupport.id("map_top_readout_text_exists"),
                new ApoapsisGameTest("map_top_readout_text_exists", environment, 40, LangTests::mapTopReadoutTextExists));
    }

    /**
     * Every map top-readout key has text. The escape line, "Leaves the sphere in ...", had none: a ship on its way out of
     * Pale's sphere of influence read the raw key {@code apoapsis.map.top.escape} on the 3D map.
     */
    @SuppressWarnings("removal")
    private static void mapTopReadoutTextExists(GameTestHelper helper, ShipCleanup ships) {
        InputStream stream = LangTests.class.getResourceAsStream("/assets/" + Apoapsis.MODID + "/lang/en_us.json");
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
        for (String key : MAP_TOP_KEYS) {
            helper.assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), "lang key missing: " + key);
        }
        helper.succeed();
    }
}
