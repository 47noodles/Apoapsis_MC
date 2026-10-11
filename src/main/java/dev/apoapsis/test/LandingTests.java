package dev.apoapsis.test;

import dev.apoapsis.flight.Environment;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.ship.DescentGear;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Tests of the descent gear: the parachutes' drag and the landing legs' crash limit. */
final class LandingTests {
    /** Local positions of the four parachutes on top of the 3 by 3 hull layer. */
    private static final BlockPos[] CHUTES = {
            new BlockPos(0, 1, 0), new BlockPos(2, 1, 0), new BlockPos(0, 1, 2), new BlockPos(2, 1, 2)};
    /** Where the landing legs go, in order, under the hull layer at local y = 1. */
    private static final BlockPos[] LEGS = {
            new BlockPos(0, 0, 0), new BlockPos(2, 0, 0), new BlockPos(2, 0, 2), new BlockPos(0, 0, 2),
            new BlockPos(1, 0, 0), new BlockPos(1, 0, 2)};

    private LandingTests() {}

    /** A 3 by 3 layer of hull plates at local {@code y}. */
    private static void hullLayer(GameTestHelper helper, ShipStructure structure, int y) {
        BlockState plate = TestSupport.block(helper, "hull_plate").defaultBlockState();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                structure.put(new BlockPos(x, y, z), plate);
            }
        }
    }

    /** Nine hull plates and four parachutes: 4,000 kg, no fuel. */
    private static ShipStructure chuteShip(GameTestHelper helper) {
        ShipStructure structure = new ShipStructure();
        hullLayer(helper, structure, 0);
        BlockState chute = TestSupport.block(helper, "parachute").defaultBlockState();
        for (BlockPos pos : CHUTES) {
            structure.put(pos, chute);
        }
        return structure;
    }

    /**
     * With four chutes open, a falling ship settles at the terminal speed where drag balances weight: within 4% of
     * {@link DescentGear#terminalSpeedMps} for the air density and gravity at the ship.
     */
    static void chuteTerminalSpeed(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, chuteShip(helper), 70));
        ship.setThrottle(0f);
        ship.setVelocity(new Vec3(0, -10, 0));
        ship.deployParachutes();
        helper.assertTrue(ship.getChuteState() > DescentGear.STOWED,
                "the chutes did not deploy: state " + ship.getChuteState());

        helper.startSequence()
                .thenExecuteAfter(120, () -> {
                    helper.assertTrue(ship.isAlive() && !ship.isLanded(), "the ship ended its descent early");
                    helper.assertTrue(ship.getChuteState() == DescentGear.INFLATE_TICKS,
                            "the chutes are not fully open: state " + ship.getChuteState());
                    Environment env = FlightHooks.environment(ship);
                    double hullArea = 0.5 * Math.pow(ship.getStructure().size(), 2.0 / 3.0);
                    double expected = DescentGear.terminalSpeedMps(ship.massKg(), env.gravity().length(),
                            env.airDensity(), hullArea, ship.parachuteCount());
                    double speed = ship.getVelocity().length();
                    helper.assertTrue(Math.abs(speed - expected) <= 0.04 * expected,
                            "speed " + speed + " m/s, expected the terminal speed " + expected + " m/s");
                })
                .thenSucceed();
    }

    /** Deploying faster than the shred speed tears the chutes off the ship for good. */
    static void chutesShredWhenTooFast(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, chuteShip(helper), 70));
        ship.setThrottle(0f);
        ship.setVelocity(new Vec3(0, -300, 0));
        ship.deployParachutes();
        helper.assertTrue(ship.getChuteState() == DescentGear.TORN, "the chutes were not torn: state " + ship.getChuteState());
        helper.assertTrue(ship.parachuteCount() == 0, ship.parachuteCount() + " chutes are left on the ship");
        helper.assertTrue(ship.getStructure().size() == 9,
                "the ship has " + ship.getStructure().size() + " blocks, expected the 9 hull plates");
        helper.succeed();
    }

    /** A ship of nine hull plates with {@code legs} landing legs under it. */
    private static ShipStructure legShip(GameTestHelper helper, int legs) {
        ShipStructure structure = new ShipStructure();
        hullLayer(helper, structure, 1);
        BlockState leg = TestSupport.block(helper, "landing_leg").defaultBlockState();
        for (int i = 0; i < legs; i++) {
            structure.put(LEGS[i], leg);
        }
        return structure;
    }

    /**
     * The crash limit is 12 m/s with fewer than three legs on the lowest layer and 24 m/s with three or more, never
     * above 24: five ships hit the floor flat at once, and live or die by that rule.
     */
    static void legsSetTheCrashLimit(GameTestHelper helper, ShipCleanup ships) {
        helper.assertTrue(DescentGear.verticalCrashLimitMps(0) == 12.0, "0 legs must give 12 m/s");
        helper.assertTrue(DescentGear.verticalCrashLimitMps(2) == 12.0, "2 legs must give 12 m/s");
        helper.assertTrue(DescentGear.verticalCrashLimitMps(3) == 24.0, "3 legs must give 24 m/s");
        helper.assertTrue(DescentGear.verticalCrashLimitMps(6) == 24.0, "6 legs must give the 24 m/s cap");

        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = -5; x <= 8; x++) {
            for (int z = -5; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), stone);
            }
        }
        // The ships stand in a plus, close to the test origin: chunks farther out are not loaded in a GameTest.
        int[][] spots = {{0, 0}, {-4, 0}, {4, 0}, {0, -4}, {0, 4}};
        // legs, impact speed in m/s, whether the ship survives
        int[][] cases = {{0, 15, 0}, {2, 15, 0}, {3, 15, 1}, {6, 22, 1}, {6, 26, 0}};
        ShipEntity[] spawned = new ShipEntity[cases.length];
        for (int i = 0; i < cases.length; i++) {
            ShipStructure structure = legShip(helper, cases[i][0]);
            BlockPos origin = helper.absolutePos(new BlockPos(spots[i][0], 2, spots[i][1]));
            Vec3 com = structure.centerOfMassLocal();
            Vec3 comPos = new Vec3(origin.getX() + com.x, origin.getY() + com.y, origin.getZ() + com.z);
            ShipEntity ship = ships.track(ShipEntity.spawn(helper.getLevel(), structure, comPos, new Quaternionf()));
            ship.setThrottle(0f);
            ship.setVelocity(new Vec3(0, -cases[i][1], 0));
            helper.assertTrue(ship.landingLegCount() == cases[i][0],
                    "the ship has " + ship.landingLegCount() + " legs, expected " + cases[i][0]);
            spawned[i] = ship;
        }

        helper.startSequence()
                .thenExecuteAfter(15, () -> {
                    StringBuilder wrong = new StringBuilder();
                    for (int i = 0; i < cases.length; i++) {
                        boolean survived = spawned[i].isAlive();
                        if (survived != (cases[i][2] == 1)) {
                            wrong.append(cases[i][0]).append(" legs at ").append(cases[i][1]).append(" m/s: expected ")
                                    .append(cases[i][2] == 1 ? "survive" : "crash").append(", was ")
                                    .append(survived ? "alive" : "destroyed").append("; ");
                        }
                    }
                    helper.assertTrue(wrong.length() == 0, wrong.toString());
                })
                .thenSucceed();
    }
}
