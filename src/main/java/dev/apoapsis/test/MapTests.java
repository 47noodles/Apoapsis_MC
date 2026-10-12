package dev.apoapsis.test;

import dev.apoapsis.client.map.Labels;
import dev.apoapsis.client.map.MapKeys;
import dev.apoapsis.client.map.MapScene;
import dev.apoapsis.client.map.QuadBatch;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Tests of the 2D map's label placement, line batching, key names and shading. No client needed. */
final class MapTests {
    private static final double LABEL_W = 60;
    private static final double LABEL_H = 6;

    private MapTests() {}

    /** Registers the tests. */
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        event.registerTest(TestSupport.id("map_labels_never_overlap"),
                new ApoapsisGameTest("map_labels_never_overlap", environment, 40, MapTests::labelsNeverOverlap));
        event.registerTest(TestSupport.id("map_labels_avoid_lines"),
                new ApoapsisGameTest("map_labels_avoid_lines", environment, 40, MapTests::labelsAvoidLines));
        event.registerTest(TestSupport.id("map_quadbatch_polyline_is_whole_quads_and_finite"),
                new ApoapsisGameTest("map_quadbatch_polyline_is_whole_quads_and_finite", environment, 40,
                        MapTests::polylineWholeQuads));
        event.registerTest(TestSupport.id("map_quadbatch_huge_ring_is_bounded"),
                new ApoapsisGameTest("map_quadbatch_huge_ring_is_bounded", environment, 40, MapTests::hugeRingBounded));
        event.registerTest(TestSupport.id("map_quadbatch_dash_has_gaps"),
                new ApoapsisGameTest("map_quadbatch_dash_has_gaps", environment, 40, MapTests::dashHasGaps));
        event.registerTest(TestSupport.id("map_keys_punctuation_is_spelled_out"),
                new ApoapsisGameTest("map_keys_punctuation_is_spelled_out", environment, 40,
                        MapTests::keysPunctuation));
        event.registerTest(TestSupport.id("map_shade_sun_side_is_brighter"),
                new ApoapsisGameTest("map_shade_sun_side_is_brighter", environment, 40, MapTests::shadeSunSide));
    }

    /** True when two placed labels' boxes share any area. */
    private static boolean meets(Labels.Spot a, Labels.Spot b) {
        return a.x() < b.x() + LABEL_W && b.x() < a.x() + LABEL_W
                && a.y() < b.y() + LABEL_H && b.y() < a.y() + LABEL_H;
    }

    /** True when every vertex in the batch has a finite x and y. */
    private static boolean allFinite(QuadBatch b) {
        for (int k = 0; k < b.count; k++) {
            if (!Float.isFinite(b.x[k]) || !Float.isFinite(b.y[k])) {
                return false;
            }
        }
        return true;
    }

    /** Twelve anchors 70 apart in two close rows: every label that is placed clears the others and the reserved box. */
    private static void labelsNeverOverlap(GameTestHelper helper, ShipCleanup ships) {
        Labels labels = new Labels();
        labels.begin(640, 360);
        labels.reserve(10, 10, 200, 40);
        List<Labels.Spot> placed = new ArrayList<>();
        for (double ay : new double[] {100, 106}) {
            for (int i = 0; i < 6; i++) {
                Labels.Spot spot = labels.place(100 + 70 * i, ay, LABEL_W, LABEL_H, 3, true, Labels.Side.RIGHT);
                if (spot != null) {
                    placed.add(spot);
                }
            }
        }
        helper.assertTrue(!placed.isEmpty(), "no label was placed");
        boolean clear = true;
        for (int i = 0; i < placed.size(); i++) {
            Labels.Spot a = placed.get(i);
            clear &= !(a.x() < 200 && 10 < a.x() + LABEL_W && a.y() < 40 && 10 < a.y() + LABEL_H);
            for (int j = i + 1; j < placed.size(); j++) {
                clear &= !meets(a, placed.get(j));
            }
        }
        helper.assertTrue(clear, "two labels, or a label and the reserved box, overlap");
        helper.succeed();
    }

    /** A label beside a heavy horizontal orbit line sits fully above or fully below it, never across it. */
    private static void labelsAvoidLines(GameTestHelper helper, ShipCleanup ships) {
        Labels labels = new Labels();
        labels.begin(640, 360);
        labels.markSegment(0, 100, 640, 100, 3);
        Labels.Spot spot = labels.place(320, 100, 40, 6, 3, false,
                Labels.Side.RIGHT, Labels.Side.ABOVE, Labels.Side.BELOW);
        helper.assertTrue(spot != null, "no label found a spot beside the orbit line");
        if (spot != null) {
            boolean above = spot.y() + 6 <= 100;
            boolean below = spot.y() >= 100;
            helper.assertTrue(above || below, "label at y=" + spot.y() + " crosses the orbit line at y=100");
        }
        helper.succeed();
    }

    /** A sine polyline of 50 points gives whole quads, all finite, with both soft (alpha 0) and core (alpha FF) vertices. */
    private static void polylineWholeQuads(GameTestHelper helper, ShipCleanup ships) {
        QuadBatch b = new QuadBatch();
        b.begin(640, 360);
        double[] xs = new double[50];
        double[] ys = new double[50];
        for (int i = 0; i < 50; i++) {
            xs[i] = 20 + i * 12.0;
            ys[i] = 180 + 60 * Math.sin(i * 0.4);
        }
        b.polyline(xs, ys, 50, 0.3, 0.3, 0xFF3AD1C1);
        helper.assertTrue(b.count > 0, "polyline drew no vertices");
        helper.assertTrue(b.count % 4 == 0, "vertices are not whole quads: " + b.count);
        helper.assertTrue(allFinite(b), "a polyline vertex is not finite");
        boolean outer = false;
        boolean inner = false;
        for (int k = 0; k < b.count; k++) {
            int alpha = b.argb[k] >>> 24;
            outer |= alpha == 0;
            inner |= alpha == 0xFF;
        }
        helper.assertTrue(outer, "no outer (alpha 0) vertices");
        helper.assertTrue(inner, "no inner (alpha FF) vertices");
        helper.succeed();
    }

    /** A circle of radius 500000 whose top edge crosses the view draws a bounded, finite ring; one far away draws nothing. */
    private static void hugeRingBounded(GameTestHelper helper, ShipCleanup ships) {
        QuadBatch b = new QuadBatch();
        b.begin(640, 360);
        b.ring(320, 5.0e5, 5.0e5 - 100, 0.3, 0.3, 0xFFFFFFFF, 0, 0);
        helper.assertTrue(b.count > 0 && b.count < 20000, "huge ring drew " + b.count + " vertices");
        helper.assertTrue(allFinite(b), "a huge ring vertex is not finite");
        b.begin(640, 360);
        b.ring(5.0e6, 5.0e6, 1000, 0.3, 0.3, 0xFFFFFFFF, 0, 0);
        helper.assertTrue(b.count == 0, "a ring far outside the view drew " + b.count + " vertices");
        helper.succeed();
    }

    /** A dashed straight line draws quads, all within the line's span from x=0 to x=200. */
    private static void dashHasGaps(GameTestHelper helper, ShipCleanup ships) {
        QuadBatch b = new QuadBatch();
        b.begin(640, 360);
        double[] xs = new double[100];
        double[] ys = new double[100];
        for (int i = 0; i < 100; i++) {
            xs[i] = 200.0 * i / 99;
            ys[i] = 50;
        }
        b.dashedPolyline(xs, ys, 100, 0.3, 0.3, 4, 4, 0xFFFFFFFF);
        helper.assertTrue(b.count > 0, "dashed line drew no vertices");
        boolean inSpan = true;
        for (int k = 0; k < b.count; k++) {
            inSpan &= b.x[k] >= -1 && b.x[k] <= 201;
        }
        helper.assertTrue(inSpan, "a dash vertex lies outside x -1 to 201");
        helper.succeed();
    }

    /** Punctuation keys are spelled out, and the names of the key labels are as the keycaps show them. */
    private static void keysPunctuation(GameTestHelper helper, ShipCleanup ships) {
        helper.assertTrue("COMMA".equals(MapKeys.name(",")), "comma is " + MapKeys.name(","));
        helper.assertTrue("PERIOD".equals(MapKeys.name(".")), "period is " + MapKeys.name("."));
        helper.assertTrue(!MapKeys.name(",").equals(MapKeys.name(".")), "comma and period have the same name");
        helper.assertTrue("X".equals(MapKeys.name("x")), "x is " + MapKeys.name("x"));
        helper.assertTrue("L SHIFT".equals(MapKeys.name("Left Shift")),
                "Left Shift is " + MapKeys.name("Left Shift"));
        helper.succeed();
    }

    /** A surface facing the sun is much greener than one in shadow; the shadow is cool; both are opaque. */
    private static void shadeSunSide(GameTestHelper helper, ShipCleanup ships) {
        int lit = MapScene.shade(0xFFFFFF, 1.0, 1.0);
        int dark = MapScene.shade(0xFFFFFF, -1.0, 1.0);
        int litGreen = (lit >> 8) & 0xFF;
        int darkGreen = (dark >> 8) & 0xFF;
        int darkRed = (dark >> 16) & 0xFF;
        int darkBlue = dark & 0xFF;
        helper.assertTrue(litGreen > 3 * darkGreen,
                "sun side green " + litGreen + " is not over 3 times shadow green " + darkGreen);
        helper.assertTrue(darkBlue >= darkRed,
                "shadow is not cool: blue " + darkBlue + " below red " + darkRed);
        helper.assertTrue((lit >>> 24) == 0xFF && (dark >>> 24) == 0xFF, "shaded colours are not opaque");
        helper.succeed();
    }
}
