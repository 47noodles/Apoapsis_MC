package dev.apoapsis.test;

import dev.apoapsis.client.map3d.ManeuverPlan;
import dev.apoapsis.client.map3d.ManeuverPlan.Handle;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.orbit.maneuver.ManeuverNode;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Tests of {@link ManeuverPlan}, the 3D map's maneuver node maths. No client needed. */
final class ManeuverTests {
    /** A circular Verdance orbit at r = 80 km, at exactly the circular speed. */
    private static final StateVector CIRCULAR = new StateVector(new Vec3d(80000, 0, 0),
            new Vec3d(0, Math.sqrt(MeridianSystem.VERDANCE.mu / 80000), 0));

    private ManeuverTests() {}

    /** Registers the tests. */
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        event.registerTest(TestSupport.id("maneuver_handles_orthonormal"),
                new ApoapsisGameTest("maneuver_handles_orthonormal", environment, 40, ManeuverTests::handles));
        event.registerTest(TestSupport.id("maneuver_prograde_raises_apoapsis"),
                new ApoapsisGameTest("maneuver_prograde_raises_apoapsis", environment, 40, ManeuverTests::raises));
        event.registerTest(TestSupport.id("maneuver_add_dv_signs"),
                new ApoapsisGameTest("maneuver_add_dv_signs", environment, 40, ManeuverTests::signs));
        event.registerTest(TestSupport.id("maneuver_timing"),
                new ApoapsisGameTest("maneuver_timing", environment, 40, ManeuverTests::timing));
        event.registerTest(TestSupport.id("maneuver_next_ut"),
                new ApoapsisGameTest("maneuver_next_ut", environment, 40, ManeuverTests::nextUt));
    }

    private static void handles(GameTestHelper helper, ShipCleanup ships) {
        Vec3d p = ManeuverPlan.direction(CIRCULAR, ManeuverPlan.HANDLES.get(0));
        Vec3d n = ManeuverPlan.direction(CIRCULAR, ManeuverPlan.HANDLES.get(2));
        Vec3d r = ManeuverPlan.direction(CIRCULAR, ManeuverPlan.HANDLES.get(4));
        Vec3d retro = ManeuverPlan.direction(CIRCULAR, ManeuverPlan.HANDLES.get(1));
        for (Vec3d v : new Vec3d[] {p, n, r}) {
            helper.assertTrue(Math.abs(v.length() - 1) < 1e-9, "a handle direction is not a unit vector: " + v);
        }
        helper.assertTrue(Math.abs(p.dot(n)) < 1e-9 && Math.abs(p.dot(r)) < 1e-9 && Math.abs(n.dot(r)) < 1e-9,
                "the handle directions are not perpendicular");
        helper.assertTrue(r.dot(CIRCULAR.r()) > 0, "radial out does not point away from the body");
        helper.assertTrue(retro.add(p).length() < 1e-9, "retrograde is not the negation of prograde");
        helper.succeed();
    }

    private static void raises(GameTestHelper helper, ShipCleanup ships) {
        StateVector after = ManeuverPlan.afterBurn(CIRCULAR, new ManeuverNode(0, 50, 0, 0));
        ManeuverPlan.Apsides before = ManeuverPlan.apsides(MeridianSystem.VERDANCE, CIRCULAR);
        ManeuverPlan.Apsides now = ManeuverPlan.apsides(MeridianSystem.VERDANCE, after);
        helper.assertTrue(before.bound() && now.bound(), "an orbit is not bound");
        helper.assertTrue(now.apoapsisAltitude() > before.apoapsisAltitude() + 1000,
                "a prograde burn raised the apoapsis by only " + (now.apoapsisAltitude() - before.apoapsisAltitude()));
        helper.assertTrue(Math.abs(now.periapsisAltitude() - before.periapsisAltitude()) < 5,
                "a prograde burn moved the periapsis by " + (now.periapsisAltitude() - before.periapsisAltitude()));
        helper.succeed();
    }

    private static void signs(GameTestHelper helper, ShipCleanup ships) {
        ManeuverPlan plan = new ManeuverPlan();
        plan.addDv(ManeuverPlan.HANDLES.get(0), 5);
        helper.assertTrue(!plan.has(), "addDv made a node");
        plan.place(100.0);
        plan.addDv(ManeuverPlan.HANDLES.get(1), 10);
        plan.addDv(ManeuverPlan.HANDLES.get(2), 4);
        plan.addDv(ManeuverPlan.HANDLES.get(5), 3);
        ManeuverNode n = plan.node();
        helper.assertTrue(n != null && n.prograde() == -10 && n.normal() == 4 && n.radial() == -3,
                "the components are wrong: " + n);
        plan.moveTo(200.0);
        helper.assertTrue(plan.node() != null && plan.node().ut() == 200.0 && plan.node().prograde() == -10,
                "moveTo changed the components");
        plan.clear();
        helper.assertTrue(!plan.has(), "clear left a node");
        helper.succeed();
    }

    private static void timing(GameTestHelper helper, ShipCleanup ships) {
        ManeuverNode n = new ManeuverNode(120, 100, 0, 0);
        ManeuverPlan.Timing t = ManeuverPlan.timing(n, 0, 20000, 440, 100000);
        helper.assertTrue(t != null && t.burnSeconds() > 0 && t.startUt() < 120, "the timing is wrong: " + t);
        helper.assertTrue(Math.abs(t.secondsToStart() - t.startUt()) < 1e-9, "secondsToStart is not startUt - now");
        helper.assertTrue(ManeuverPlan.timing(n, 0, 20000, 440, 0) == null, "a ship with no thrust has a timing");
        helper.succeed();
    }

    private static void nextUt(GameTestHelper helper, ShipCleanup ships) {
        helper.assertTrue(ManeuverPlan.nextUt(10, 100, 60, 1000) == 130, "a past point did not wrap to the next lap");
        helper.assertTrue(ManeuverPlan.nextUt(500, 100, 60, 1000) == 500, "a future point moved");
        helper.assertTrue(Double.isNaN(ManeuverPlan.nextUt(10, 100, Double.NaN, 1000)), "an open orbit wrapped");
        helper.assertTrue(Double.isNaN(ManeuverPlan.nextUt(10, 100, 60, 120)), "a point past the last time was kept");
        helper.succeed();
    }
}
