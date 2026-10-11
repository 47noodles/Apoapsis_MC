package dev.apoapsis.test;

import dev.apoapsis.client.map3d.OrbitCamera;
import dev.apoapsis.client.map3d.TexturedSphere;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Tests of {@link TexturedSphere}, the 3D map's textured body mesh. No client needed. */
final class SphereTests {
    private static final double R = 1000;
    private static final double D = 100000;
    private static final double FOCAL = OrbitCamera.focal(480);
    private static final double CELLS = TexturedSphere.LAT * TexturedSphere.LON;

    private SphereTests() {}

    /** Registers the tests. */
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        event.registerTest(TestSupport.id("sphere_textured_front_half"),
                new ApoapsisGameTest("sphere_textured_front_half", environment, 40, SphereTests::frontHalf));
        event.registerTest(TestSupport.id("sphere_textured_uv_seam"),
                new ApoapsisGameTest("sphere_textured_uv_seam", environment, 40, SphereTests::uvSeam));
        event.registerTest(TestSupport.id("sphere_textured_eye_inside"),
                new ApoapsisGameTest("sphere_textured_eye_inside", environment, 40, SphereTests::eyeInside));
    }

    /** From far away the front half of the sphere is drawn: about half the cells, all on screen. */
    private static void frontHalf(GameTestHelper helper, ShipCleanup ships) {
        TexturedSphere s = new TexturedSphere();
        s.build(new OrbitCamera(D), 0, 0, FOCAL, 0, 0, 0, R, D, 0, 0);
        int visible = s.count / 4;
        helper.assertTrue(s.count % 4 == 0, "vertices are not whole quads");
        helper.assertTrue(visible > 0.45 * CELLS && visible < 0.6 * CELLS,
                "front half shows " + visible + " of " + CELLS + " cells");
        boolean finite = true;
        for (int k = 0; k < s.count; k++) {
            finite &= Float.isFinite(s.x[k]) && Float.isFinite(s.y[k]);
        }
        helper.assertTrue(finite, "a visible vertex is not on screen");
        helper.succeed();
    }

    /** No quad spans the longitude seam, and every U lies in 0.5 to 1.5, from every side of the sphere. */
    private static void uvSeam(GameTestHelper helper, ShipCleanup ships) {
        OrbitCamera cam = new OrbitCamera(D);
        TexturedSphere s = new TexturedSphere();
        double[][] eyes = {{D, 0, 0}, {-D, 0, 0}, {0, D, 0}, {0, -D, 0}, {0, 0, D}, {0, 0, -D}};
        boolean seamFree = true;
        boolean inRange = true;
        for (double[] e : eyes) {
            s.build(cam, 0, 0, FOCAL, 0, 0, 0, R, e[0], e[1], e[2]);
            for (int q = 0; q < s.count; q += 4) {
                float lo = Math.min(Math.min(s.u[q], s.u[q + 1]), Math.min(s.u[q + 2], s.u[q + 3]));
                float hi = Math.max(Math.max(s.u[q], s.u[q + 1]), Math.max(s.u[q + 2], s.u[q + 3]));
                seamFree &= hi - lo <= 1.0 / TexturedSphere.LON + 1e-6f;
                inRange &= lo >= 0.5f && hi <= 1.5f;
            }
        }
        helper.assertTrue(seamFree, "a quad spans the longitude seam");
        helper.assertTrue(inRange, "a texture U lies outside 0.5 to 1.5");
        helper.succeed();
    }

    /** An eye inside the sphere faces no cell, so nothing is drawn and nothing is built. */
    private static void eyeInside(GameTestHelper helper, ShipCleanup ships) {
        OrbitCamera cam = new OrbitCamera(D);
        TexturedSphere s = new TexturedSphere();
        s.build(cam, 0, 0, FOCAL, 0, 0, 0, R, R / 2, 0, 0);
        helper.assertTrue(s.count == 0, "an eye off-centre inside the sphere drew " + s.count + " vertices");
        s.build(cam, 0, 0, FOCAL, 0, 0, 0, R, 0, 0, 0);
        helper.assertTrue(s.count == 0, "an eye at the centre drew " + s.count + " vertices");
        helper.succeed();
    }
}
