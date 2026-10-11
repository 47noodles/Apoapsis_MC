package dev.apoapsis.test;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.Environment;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.flight.OrbitFlight;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
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

    /** Stone over the whole 16 by 16 chunk that holds the test origin, top surface at relative y = 1. */
    private static void chunkFloor(GameTestHelper helper, BlockPos corner) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(corner.offset(x, 0, z), stone);
            }
        }
    }

    /** Spawns {@code structure} at rest with its local origin at {@code at}, a position relative to the test origin. */
    private static ShipEntity spawnAt(GameTestHelper helper, ShipCleanup ships, ShipStructure structure, BlockPos at) {
        BlockPos origin = helper.absolutePos(at);
        Vec3 com = structure.centerOfMassLocal();
        Vec3 comPos = new Vec3(origin.getX() + com.x, origin.getY() + com.y, origin.getZ() + com.z);
        return ships.track(ShipEntity.spawn(helper.getLevel(), structure, comPos, new Quaternionf()));
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
        BlockPos corner = TestSupport.chunkCorner(helper);
        chunkFloor(helper, corner);
        ShipEntity ship = spawnAt(helper, ships, chuteShip(helper), corner.offset(6, 71, 6));
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

    /** Nine hull plates and no chutes: the control ship for the orbit test. */
    private static ShipStructure hullOnly(GameTestHelper helper) {
        ShipStructure structure = new ShipStructure();
        hullLayer(helper, structure, 0);
        return structure;
    }

    /**
     * Inside the atmosphere the open chutes slow a falling ship in the orbit regime too. A chute ship and a hull-only
     * control fall radially from 8 km altitude at 200 m/s through {@link OrbitFlight#simulate} for 20 s. The chute ship
     * settles at the terminal speed for the air at its end altitude (within 8%) and ends far slower than the control.
     * Also checks the three deploy-check cases, and that a ship given the chute state keeps it.
     */
    static void chuteSlowsInOrbitRegime(GameTestHelper helper, ShipCleanup ships) {
        BlockPos corner = TestSupport.chunkCorner(helper);
        chunkFloor(helper, corner);
        ShipEntity chute = spawnAt(helper, ships, chuteShip(helper), corner.offset(4, 71, 4));
        ShipEntity control = spawnAt(helper, ships, hullOnly(helper), corner.offset(10, 71, 10));
        chute.setThrottle(0f);
        control.setThrottle(0f);
        chute.setVelocity(new Vec3(0, -10, 0));
        chute.deployParachutes();
        helper.assertTrue(chute.getChuteState() > DescentGear.STOWED,
                "the chutes did not deploy: state " + chute.getChuteState());

        helper.startSequence()
                .thenExecuteAfter(DescentGear.INFLATE_TICKS + 5, () -> {
                    helper.assertTrue(chute.getChuteState() == DescentGear.INFLATE_TICKS,
                            "the chutes are not fully open: state " + chute.getChuteState());

                    double radius = MeridianSystem.VERDANCE.radius;
                    StateVector start = new StateVector(new Vec3d(radius + 8000.0, 0, 0), new Vec3d(-200.0, 0, 0));
                    StateVector chuteEnd = OrbitFlight.simulate(chute, BodyId.VERDANCE, start, 20.0);
                    StateVector controlEnd = OrbitFlight.simulate(control, BodyId.VERDANCE, start, 20.0);
                    double chuteSpeed = chuteEnd.v().length();
                    double controlSpeed = controlEnd.v().length();
                    double r = chuteEnd.r().length();
                    double alt = r - radius;
                    double expected = DescentGear.terminalSpeedMps(chute.massKg(), MeridianSystem.VERDANCE.gravity(r),
                            MeridianSystem.VERDANCE.density(alt), 0.5 * Math.pow(chute.getStructure().size(), 2.0 / 3.0),
                            chute.parachuteCount());
                    Apoapsis.LOG.info("chute_orbit_regime: chute " + chuteSpeed + " m/s, control " + controlSpeed
                            + " m/s, terminal " + expected + " m/s at alt " + alt + " m");

                    helper.assertTrue(Math.abs(chuteSpeed - expected) <= 0.08 * expected,
                            "chute ship speed " + chuteSpeed + " m/s, expected the terminal speed " + expected
                                    + " m/s at " + alt + " m altitude");
                    helper.assertTrue(chuteSpeed < 0.5 * controlSpeed,
                            "the chutes did not slow the ship: " + chuteSpeed + " m/s against " + controlSpeed
                                    + " m/s without them");

                    helper.assertTrue(DescentGear.deployCheck(0.0, 10.0) == DescentGear.DeployCheck.NO_AIR,
                            "a vacuum must refuse the chutes");
                    helper.assertTrue(DescentGear.deployCheck(1.225, 300.0) == DescentGear.DeployCheck.TOO_FAST,
                            "300 m/s must tear the chutes");
                    helper.assertTrue(DescentGear.deployCheck(1.225, 200.0) == DescentGear.DeployCheck.OK,
                            "200 m/s in air must let the chutes open");

                    control.copyChuteStateFrom(chute);
                    helper.assertTrue(control.getChuteState() == chute.getChuteState(),
                            "the copied chute state " + control.getChuteState() + " is not "
                                    + chute.getChuteState());
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

        // Every ship stands inside the chunk under the test origin, the one chunk the runner keeps ticking: a 3 by 3 ship
        // whose centre fell in the next chunk would hang in the air and never reach the floor.
        BlockPos corner = TestSupport.chunkCorner(helper);
        chunkFloor(helper, corner);
        // The ships stand in a plus; each entry is the ship's corner in the chunk, with a block between neighbours.
        int[][] spots = {{6, 6}, {2, 6}, {10, 6}, {6, 2}, {6, 10}};
        // legs, impact speed in m/s, whether the ship survives
        int[][] cases = {{0, 15, 0}, {2, 15, 0}, {3, 15, 1}, {6, 22, 1}, {6, 26, 0}};
        ShipEntity[] spawned = new ShipEntity[cases.length];
        for (int i = 0; i < cases.length; i++) {
            BlockPos at = corner.offset(spots[i][0], 2, spots[i][1]);
            ShipEntity ship = spawnAt(helper, ships, legShip(helper, cases[i][0]), at);
            ship.setThrottle(0f);
            ship.setVelocity(new Vec3(0, -cases[i][1], 0));
            helper.assertTrue(ship.landingLegCount() == cases[i][0],
                    "the ship has " + ship.landingLegCount() + " legs, expected " + cases[i][0]);
            spawned[i] = ship;
        }

        helper.startSequence()
                // Judge each ship only once it has met the floor, destroyed or landed: a ship still in the air is
                // neither a survivor nor a wreck yet.
                .thenWaitUntil(() -> {
                    for (int i = 0; i < cases.length; i++) {
                        helper.assertTrue(!spawned[i].isAlive() || spawned[i].isLanded(),
                                cases[i][0] + " legs at " + cases[i][1] + " m/s: not on the floor yet, y "
                                        + spawned[i].getY());
                    }
                })
                .thenExecute(() -> {
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
