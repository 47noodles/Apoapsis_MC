package dev.apoapsis.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Guide;
import dev.apoapsis.client.hud.HudFormat;
import dev.apoapsis.client.hud.Navball;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTests for the flight HUD's pure parts: reading formats, the navball's horizon frame, and the lang text the HUD and
 * the guide panel read.
 */
final class HudTests {
    private static final int MAX_SHORT_CHARS = 105;

    private HudTests() {}

    /** Readings split into a number and a unit, with grouping, a true minus and a sign on a climb. */
    static void hudNumbersAreFormatted(GameTestHelper helper, ShipCleanup ships) {
        expect(helper, HudFormat.distance(850), "850", "m");
        expect(helper, HudFormat.distance(14_200), "14.2", "km");
        expect(helper, HudFormat.distance(312_000), "312", "km");
        expect(helper, HudFormat.speed(2_940.4), "2 940", "m/s");
        expect(helper, HudFormat.signedSpeed(45), "+45", "m/s");
        expect(helper, HudFormat.signedSpeed(-9), "−9", "m/s");
        expect(helper, HudFormat.signedSpeed(0.2), "0", "m/s");
        expect(helper, HudFormat.distance(Double.NaN), "–", "");
        helper.assertTrue(HudFormat.integer(-1_234_567).equals("−1 234 567"), "grouping reads wrong");
        helper.assertTrue(HudFormat.percent(0.724).equals("72"), "percent reads wrong");
        helper.succeed();
    }

    /** North and east along the horizon are unit, perpendicular to each other and to the vertical, for any vertical. */
    static void navballHorizonFrameIsOrthonormal(GameTestHelper helper, ShipCleanup ships) {
        double[][] ups = {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0.6, 0.64, 0.48}, {-0.3, -0.9539, 0}};
        double[] out = new double[6];
        for (double[] u : ups) {
            double l = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
            double ux = u[0] / l;
            double uy = u[1] / l;
            double uz = u[2] / l;
            Navball.horizon(ux, uy, uz, out);
            double nn = dot(out, 0, out, 0);
            double ee = dot(out, 3, out, 3);
            double ne = dot(out, 0, out, 3);
            double nu = out[0] * ux + out[1] * uy + out[2] * uz;
            double eu = out[3] * ux + out[4] * uy + out[5] * uz;
            helper.assertTrue(Math.abs(nn - 1) < 1e-9 && Math.abs(ee - 1) < 1e-9 && Math.abs(ne) < 1e-9
                    && Math.abs(nu) < 1e-9 && Math.abs(eu) < 1e-9, "the horizon frame is not orthonormal for up "
                    + ux + ", " + uy + ", " + uz);
        }
        helper.succeed();
    }

    /** Every lang key the HUD and the guide panel read is there, and each step's short line is short. */
    @SuppressWarnings("removal")
    static void hudTextIsComplete(GameTestHelper helper, ShipCleanup ships) {
        InputStream stream = HudTests.class.getResourceAsStream("/assets/" + Apoapsis.MODID + "/lang/en_us.json");
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
        String[] keys = {"label.alt", "label.speed", "label.vs", "label.orb", "label.ap", "label.pe", "label.radar",
                "label.throttle", "label.fuel", "label.dv", "sas.off", "sas.hold", "sas.prograde", "sas.retrograde",
                "fuel.empty", "fuel.low", "warp", "hint.chase", "hint.throttle", "hint.sas", "hint.map", "hint.warp",
                "burn.in", "burn.now", "burn.low", "impact", "escape", "none", "regime", "prograde", "retrograde",
                "target"};
        for (String key : keys) {
            require(helper, lang, "apoapsis.hud." + key);
        }
        require(helper, lang, "apoapsis.guide.step");
        for (Guide.Step step : Guide.Step.values()) {
            String key = "apoapsis.guide." + step.id() + ".short";
            require(helper, lang, key);
            int length = lang.get(key).getAsString().length();
            helper.assertTrue(length <= MAX_SHORT_CHARS, key + " is " + length + " characters, over " + MAX_SHORT_CHARS);
        }
        for (String cap : new String[] {"throttle", "full", "cut", "sas", "map", "warp", "disassemble", "chutes"}) {
            require(helper, lang, "apoapsis.guide.cap." + cap);
        }
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, String[] got, String number, String unit) {
        helper.assertTrue(got[0].equals(number) && got[1].equals(unit),
                "expected '" + number + "' '" + unit + "' but got '" + got[0] + "' '" + got[1] + "'");
    }

    private static double dot(double[] a, int ai, double[] b, int bi) {
        return a[ai] * b[bi] + a[ai + 1] * b[bi + 1] + a[ai + 2] * b[bi + 2];
    }

    private static void require(GameTestHelper helper, JsonObject lang, String key) {
        if (!lang.has(key) || lang.get(key).getAsString().isBlank()) {
            helper.fail("lang key missing or empty: " + key);
        }
    }
}
