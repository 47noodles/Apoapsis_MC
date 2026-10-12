package dev.apoapsis.test;

import dev.apoapsis.ship.DescentGear;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

/** Tests of a ship made fresh in a new level, such as after it crosses between the surface and orbit dimensions. */
final class SpawnSyncTests {
    private SpawnSyncTests() {}

    /**
     * A fresh ship that takes over an open chute: the state the server copies is the one clients see at once, and a
     * later velocity change does not disturb it.
     */
    static void freshShipSendsItsChuteState(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        ShipEntity chute = ships.track(TestSupport.spawnAboveFloor(helper, LandingTests.chuteShip(helper), 70));
        chute.setThrottle(0f);
        chute.setVelocity(new Vec3(0, -10, 0));
        chute.deployParachutes();
        helper.assertTrue(chute.getChuteState() > DescentGear.STOWED,
                "the chutes did not deploy: state " + chute.getChuteState());

        ShipEntity fresh = ships.track(TestSupport.spawnAboveFloor(helper, LandingTests.chuteShip(helper), 80));
        fresh.setThrottle(0f);
        fresh.copyChuteStateFrom(chute);
        helper.assertTrue(fresh.syncedChuteState() == chute.getChuteState(),
                "the synced chute state is " + fresh.syncedChuteState() + ", expected " + chute.getChuteState());
        helper.assertTrue(fresh.syncedChuteState() != DescentGear.STOWED,
                "the synced chute state is " + fresh.syncedChuteState() + ", expected a deployed state, not 0");

        fresh.setVelocity(new Vec3(0, 5, 0));
        helper.assertTrue(fresh.syncedChuteState() == chute.getChuteState(),
                "a velocity change left the synced chute state " + fresh.syncedChuteState()
                        + ", expected " + chute.getChuteState());
        helper.succeed();
    }
}
