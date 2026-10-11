package dev.apoapsis.test;

import com.mojang.authlib.GameProfile;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Ages;
import dev.apoapsis.ages.Firsts;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.Flight;
import dev.apoapsis.flight.FlightHooks;
import dev.apoapsis.flight.FlightRules;
import dev.apoapsis.grid.FuelRefineryBlockEntity;
import dev.apoapsis.grid.FuelTankBlockEntity;
import dev.apoapsis.ownership.ShipOwnership;
import dev.apoapsis.ship.ShipAssembler;
import dev.apoapsis.ship.ShipEntities;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.test.ApoapsisGameTest.ShipCleanup;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * GameTests for the prototype, written against docs/dev/prototype-contract.md. They are registered from code when
 * {@link RegisterGameTestsEvent} fires (dev runs and {@code ./gradlew runGameTestServer}), so no JSON is needed.
 */
@EventBusSubscriber(modid = Apoapsis.MODID)
public final class GameTests {
    private GameTests() {}

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                TestSupport.id("default"), new TestEnvironmentDefinition.AllOf());
        event.registerTest(TestSupport.id("ages_default_and_set"),
                new ApoapsisGameTest("ages_default_and_set", environment, 40, GameTests::agesDefaultAndSet));
        event.registerTest(TestSupport.id("assembly_roundtrip"),
                new ApoapsisGameTest("assembly_roundtrip", environment, 600, GameTests::assemblyRoundtrip));
        event.registerTest(TestSupport.id("ship_falls_without_thrust"),
                new ApoapsisGameTest("ship_falls_without_thrust", environment, 400, GameTests::shipFallsWithoutThrust));
        event.registerTest(TestSupport.id("ship_crashes_on_hard_impact"),
                new ApoapsisGameTest("ship_crashes_on_hard_impact", environment, 200, GameTests::shipCrashesOnHardImpact));
        event.registerTest(TestSupport.id("ship_climbs_with_thrust"),
                new ApoapsisGameTest("ship_climbs_with_thrust", environment, 120, GameTests::shipClimbsWithThrust));
        event.registerTest(TestSupport.id("orbit_spawn"),
                new ApoapsisGameTest("orbit_spawn", environment, 40, GameTests::orbitSpawn));
        event.registerTest(TestSupport.id("start_age_three"),
                new ApoapsisGameTest("start_age_three", environment, 40, GameTests::startAgeThree));
        event.registerTest(TestSupport.id("first_flight_needs_landing"),
                new ApoapsisGameTest("first_flight_needs_landing", environment, 40, GameTests::firstFlightNeedsLanding));
        event.registerTest(TestSupport.id("first_flight_grants_once"),
                new ApoapsisGameTest("first_flight_grants_once", environment, 40, GameTests::firstFlightGrantsOnce));
        event.registerTest(TestSupport.id("landing_without_altitude_grants_nothing"),
                new ApoapsisGameTest("landing_without_altitude_grants_nothing", environment, 40,
                        GameTests::landingWithoutAltitudeGrantsNothing));
        event.registerTest(TestSupport.id("fuel_refinery_makes_hydrolox"),
                new ApoapsisGameTest("fuel_refinery_makes_hydrolox", environment, 300, GameTests::fuelRefineryMakesHydrolox));
        event.registerTest(TestSupport.id("fuel_refinery_source_water_is_unlimited"),
                new ApoapsisGameTest("fuel_refinery_source_water_is_unlimited", environment, 300,
                        GameTests::fuelRefinerySourceWaterIsUnlimited));
        event.registerTest(TestSupport.id("firsts_record_once"),
                new ApoapsisGameTest("firsts_record_once", environment, 40, GameTests::firstsRecordOnce));
        event.registerTest(TestSupport.id("suborbital_advancement_awarded"),
                new ApoapsisGameTest("suborbital_advancement_awarded", environment, 40,
                        GameTests::suborbitalAdvancementAwarded));
        event.registerTest(TestSupport.id("pale_sample_grants_age_five"),
                new ApoapsisGameTest("pale_sample_grants_age_five", environment, 40, GameTests::paleSampleGrantsAgeFive));
        event.registerTest(TestSupport.id("ship_owner_may_pilot"),
                new ApoapsisGameTest("ship_owner_may_pilot", environment, 40, GameTests::shipOwnerMayPilot));
        event.registerTest(TestSupport.id("ship_stranger_may_not_pilot"),
                new ApoapsisGameTest("ship_stranger_may_not_pilot", environment, 40, GameTests::shipStrangerMayNotPilot));
        event.registerTest(TestSupport.id("ship_teammate_may_pilot"),
                new ApoapsisGameTest("ship_teammate_may_pilot", environment, 40, GameTests::shipTeammateMayPilot));
        event.registerTest(TestSupport.id("unclaimed_ship_allows_anyone"),
                new ApoapsisGameTest("unclaimed_ship_allows_anyone", environment, 40, GameTests::unclaimedShipAllowsAnyone));
        event.registerTest(TestSupport.id("ship_owner_survives_save"),
                new ApoapsisGameTest("ship_owner_survives_save", environment, 40, GameTests::shipOwnerSurvivesSave));
        event.registerTest(TestSupport.id("ship_boarding_checks_owner"),
                new ApoapsisGameTest("ship_boarding_checks_owner", environment, 40, GameTests::shipBoardingChecksOwner));
        event.registerTest(TestSupport.id("ship_owner_survives_rebuild"),
                new ApoapsisGameTest("ship_owner_survives_rebuild", environment, 40, GameTests::shipOwnerSurvivesRebuild));
    }

    /** Scoreboard team names used by the ownership tests; each test removes its teams when it is done. */
    private static final String CREW_TEAM = "apoapsis_test_crew";
    private static final String RIVAL_TEAM = "apoapsis_test_rival";

    /**
     * A bare player with its own name. Every vanilla mock player is called "test-mock-player", and scoreboard teams
     * are keyed by name, so the team tests need players that can be told apart.
     */
    private static Player namedPlayer(GameTestHelper helper, String name) {
        return new Player(helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
    }

    /** A ship held in the test plot, claimed by {@code owner} unless that is null. */
    private static ShipEntity claimedShip(GameTestHelper helper, ShipCleanup ships, Player owner) {
        TestSupport.stoneFloor(helper);
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 6));
        ShipOwnership.claim(ship, owner);
        return ship;
    }

    /** After claim, the claimer may pilot and disassemble the ship, and a later claim by someone else changes nothing. */
    @SuppressWarnings("removal")
    private static void shipOwnerMayPilot(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Player other = namedPlayer(helper, "latecomer");
        ShipEntity ship = claimedShip(helper, ships, owner);
        helper.assertTrue(ShipOwnership.ownerOf(ship).isPresent(), "the ship has no owner after claim");
        helper.assertTrue(ShipOwnership.mayPilot(ship, owner), "the owner may not pilot their own ship");
        helper.assertTrue(ShipOwnership.mayDisassemble(ship, owner), "the owner may not disassemble their own ship");
        ShipOwnership.claim(ship, other);
        helper.assertTrue(ShipOwnership.ownerOf(ship).get().id().equals(owner.getUUID()),
                "a second claim took the ship from its owner");
        helper.assertTrue(!ShipOwnership.mayPilot(ship, other), "a second claim let the claimer pilot the ship");
        helper.succeed();
    }

    /** A second player with no team may not pilot or disassemble, even when the owner is on a team. */
    @SuppressWarnings("removal")
    private static void shipStrangerMayNotPilot(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Player stranger = namedPlayer(helper, "stranger");
        ShipEntity ship = claimedShip(helper, ships, owner);
        helper.assertTrue(!ShipOwnership.mayPilot(ship, stranger), "a stranger with no team may pilot the ship");
        helper.assertTrue(!ShipOwnership.mayDisassemble(ship, stranger), "a stranger with no team may disassemble the ship");

        Scoreboard scoreboard = helper.getLevel().getScoreboard();
        PlayerTeam crew = scoreboard.addPlayerTeam(CREW_TEAM);
        try {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), crew);
            helper.assertTrue(!ShipOwnership.mayPilot(ship, stranger),
                    "a stranger with no team may pilot a ship whose owner is on a team");
        } finally {
            scoreboard.removePlayerTeam(crew);
        }
        helper.succeed();
    }

    /** A second player on the owner's team may pilot and disassemble; a player on another team may not. */
    @SuppressWarnings("removal")
    private static void shipTeammateMayPilot(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Player mate = namedPlayer(helper, "crewmate");
        Player rival = namedPlayer(helper, "rival");
        ShipEntity ship = claimedShip(helper, ships, owner);

        Scoreboard scoreboard = helper.getLevel().getScoreboard();
        PlayerTeam crew = scoreboard.addPlayerTeam(CREW_TEAM);
        PlayerTeam others = scoreboard.addPlayerTeam(RIVAL_TEAM);
        try {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), crew);
            scoreboard.addPlayerToTeam(mate.getScoreboardName(), crew);
            scoreboard.addPlayerToTeam(rival.getScoreboardName(), others);
            helper.assertTrue(ShipOwnership.mayPilot(ship, mate), "a player on the owner's team may not pilot the ship");
            helper.assertTrue(ShipOwnership.mayDisassemble(ship, mate),
                    "a player on the owner's team may not disassemble the ship");
            helper.assertTrue(!ShipOwnership.mayPilot(ship, rival), "a player on another team may pilot the ship");
        } finally {
            scoreboard.removePlayerTeam(crew);
            scoreboard.removePlayerTeam(others);
        }
        helper.succeed();
    }

    /** A ship with no owner, as in an old save, allows anyone to pilot and disassemble it. */
    private static void unclaimedShipAllowsAnyone(GameTestHelper helper, ShipCleanup ships) {
        Player anyone = namedPlayer(helper, "anyone");
        ShipEntity ship = claimedShip(helper, ships, null);
        helper.assertTrue(ShipOwnership.ownerOf(ship).isEmpty(), "a ship nobody claimed has an owner");
        helper.assertTrue(ShipOwnership.mayPilot(ship, anyone), "an unclaimed ship refused a pilot");
        helper.assertTrue(ShipOwnership.mayDisassemble(ship, anyone), "an unclaimed ship refused a disassembly");
        helper.succeed();
    }

    /** The owner is saved with the ship: a ship loaded from its saved data has the same owner, and a stranger is refused. */
    @SuppressWarnings("removal")
    private static void shipOwnerSurvivesSave(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ShipEntity ship = claimedShip(helper, ships, owner);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        ship.saveWithoutId(out);
        ShipEntity loaded = new ShipEntity(ShipEntities.SHIP.get(), helper.getLevel());
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), out.buildResult()));
        helper.assertTrue(ShipOwnership.ownerOf(loaded).equals(ShipOwnership.ownerOf(ship)),
                "the loaded ship's owner is " + ShipOwnership.ownerOf(loaded) + ", expected " + ShipOwnership.ownerOf(ship));
        helper.assertTrue(ShipOwnership.mayPilot(loaded, owner), "the owner may not pilot the ship after a save and load");
        helper.assertTrue(!ShipOwnership.mayPilot(loaded, namedPlayer(helper, "stranger")),
                "a stranger may pilot the ship after a save and load");
        helper.succeed();
    }

    /** Right-clicking a claimed ship seats its owner but not a stranger: the check is on the boarding path itself. */
    @SuppressWarnings("removal")
    private static void shipBoardingChecksOwner(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        ShipEntity ship = claimedShip(helper, ships, owner);
        InteractionResult refused = ship.interact(stranger, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(refused == InteractionResult.FAIL, "boarding by a stranger returned " + refused + ", expected FAIL");
        helper.assertTrue(ship.getPilot() == null, "a stranger was seated in someone else's ship");
        ship.interact(owner, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(ship.getPilot() == owner, "the owner was not seated in their own ship");
        helper.succeed();
    }

    /**
     * A ship rebuilt as a new entity (flight/Transitions.move, on a surface-to-orbit crossing) keeps its owner through
     * ShipOwnership.copy, and a rebuilt unclaimed ship stays unclaimed. The orbit dimension is not loaded in the
     * GameTest world, so the copy is tested on two ships in the test level.
     */
    @SuppressWarnings("removal")
    private static void shipOwnerSurvivesRebuild(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ShipEntity old = claimedShip(helper, ships, owner);
        ShipEntity fresh = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 12));
        ShipOwnership.copy(old, fresh);
        helper.assertTrue(ShipOwnership.ownerOf(fresh).equals(ShipOwnership.ownerOf(old)),
                "the rebuilt ship's owner is " + ShipOwnership.ownerOf(fresh) + ", expected " + ShipOwnership.ownerOf(old));
        helper.assertTrue(!ShipOwnership.mayPilot(fresh, namedPlayer(helper, "stranger")),
                "a stranger may pilot the rebuilt ship");
        ShipEntity unclaimed = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 18));
        ShipEntity rebuilt = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 24));
        ShipOwnership.copy(unclaimed, rebuilt);
        helper.assertTrue(ShipOwnership.ownerOf(rebuilt).isEmpty(), "rebuilding an unclaimed ship gave it an owner");
        helper.succeed();
    }

    /** Recording SUBORBITAL twice leaves one entry on the player's attachment: the second call does nothing. */
    @SuppressWarnings("removal")
    private static void firstsRecordOnce(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Firsts.record(player, Firsts.First.SUBORBITAL);
        Firsts.record(player, Firsts.First.SUBORBITAL);
        List<String> recorded = Firsts.recorded(player);
        helper.assertTrue(recorded.equals(List.of("suborbital")),
                "after two records of SUBORBITAL the firsts are " + recorded + ", expected [suborbital]");
        helper.succeed();
    }

    /** Recording SUBORBITAL awards apoapsis:suborbital, so its advancement progress is done. */
    @SuppressWarnings("removal")
    private static void suborbitalAdvancementAwarded(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Firsts.record(player, Firsts.First.SUBORBITAL);
        AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(TestSupport.id("suborbital"));
        helper.assertTrue(holder != null, "apoapsis:suborbital is not in the server's advancement manager");
        helper.assertTrue(player.getAdvancements().getOrStartProgress(holder).isDone(),
                "apoapsis:suborbital is not awarded after SUBORBITAL was recorded");
        helper.succeed();
    }

    /**
     * A pilot at Age 4 with PALE_LANDING recorded and a sample in the inventory, landing on Verdance, reaches Age 5 and
     * keeps the sample.
     */
    @SuppressWarnings("removal")
    private static void paleSampleGrantsAgeFive(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        Ages.setAge(pilot, 4, false);
        Firsts.record(pilot, Firsts.First.PALE_LANDING);
        pilot.getInventory().add(new ItemStack(TestSupport.item(helper, "pale_sample")));
        helper.assertTrue(Firsts.sampleAgeDue(true, true, true),
                "the landing decision is not due after PALE_LANDING, a Verdance landing and a sample");
        helper.assertTrue(!Firsts.sampleAgeDue(false, true, true),
                "the landing decision is due without PALE_LANDING");
        Ages.onLanded(pilot, BodyId.VERDANCE);
        helper.assertTrue(Ages.getAge(pilot) == 5,
                "after the Verdance landing with a sample the pilot is at Age " + Ages.getAge(pilot) + ", expected 5");
        helper.assertTrue(Firsts.has(pilot, Firsts.First.CREWED_RETURN), "CREWED_RETURN was not recorded");
        helper.assertTrue(Firsts.has(pilot, Firsts.First.PALE_SAMPLE), "PALE_SAMPLE was not recorded");
        helper.assertTrue(pilot.getInventory().countItem(TestSupport.item(helper, "pale_sample")) == 1,
                "the pale sample was taken from the inventory; it should be kept");
        helper.succeed();
    }

    /**
     * A fresh player is at Age 3 once the login start has run (makeMockServerPlayerInLevel fires the login event), and a
     * second Ages.start leaves a player who has already started alone.
     */
    @SuppressWarnings("removal")
    private static void startAgeThree(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int start = Ages.getAge(player);
        helper.assertTrue(start == 3, "after the login start the player is at Age " + start + ", expected 3");
        Ages.setAge(player, 5, false);
        Ages.start(player);
        int again = Ages.getAge(player);
        helper.assertTrue(again == 5, "a second Ages.start changed the Age to " + again + ", expected 5");
        helper.succeed();
    }

    /** Reaching 1,500 m alone grants nothing: the milestone waits for a landing with the pilot aboard. */
    @SuppressWarnings("removal")
    private static void firstFlightNeedsLanding(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!FlightRules.firstFlightDue(true, false, true, false, true),
                "First Flight was due at 1,500 m before any landing");
        helper.assertTrue(Ages.getAge(pilot) == 3, "reaching 1,500 m moved the pilot to Age " + Ages.getAge(pilot));
        helper.succeed();
    }

    /** 1,500 m, then a landing on Verdance with the pilot aboard grants Age 4 once. */
    @SuppressWarnings("removal")
    private static void firstFlightGrantsOnce(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        boolean due = FlightRules.firstFlightDue(true, false, true, true, true);
        helper.assertTrue(due, "a landing on Verdance after 1,500 m with the pilot aboard should grant First Flight");
        Ages.onFirstFlight(pilot);
        helper.assertTrue(Ages.getAge(pilot) == 4, "after First Flight the pilot is at Age " + Ages.getAge(pilot) + ", expected 4");
        helper.assertTrue(!FlightRules.firstFlightDue(true, true, true, true, true),
                "First Flight was due a second time after it was granted");
        helper.succeed();
    }

    /** A landing without having reached 1,500 m grants nothing. */
    @SuppressWarnings("removal")
    private static void landingWithoutAltitudeGrantsNothing(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!FlightRules.firstFlightDue(false, false, true, true, true),
                "First Flight was due on a landing that never reached 1,500 m");
        helper.assertTrue(Ages.getAge(pilot) == 3, "a landing without altitude moved the pilot to Age " + Ages.getAge(pilot));
        helper.succeed();
    }

    /** Relative positions for the refinery tests: the refinery on the stone floor, a fuel tank on its +x side. */
    private static final BlockPos REFINERY = new BlockPos(0, 1, 0);
    private static final BlockPos REFINERY_TANK = new BlockPos(1, 1, 0);
    private static final BlockPos REFINERY_SOURCE = new BlockPos(-1, 1, 0);

    /**
     * One water bucket and one coal's burn time make one 300 kg cycle in 200 ticks, which reaches the adjacent tank
     * within 260 ticks. The bucket is used up.
     */
    private static void fuelRefineryMakesHydrolox(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        helper.setBlock(REFINERY, TestSupport.block(helper, "fuel_refinery"));
        helper.setBlock(REFINERY_TANK, TestSupport.block(helper, "fuel_tank"));
        helper.getBlockEntity(REFINERY, FuelRefineryBlockEntity.class).setWaterBuckets(1);
        helper.getBlockEntity(REFINERY, FuelRefineryBlockEntity.class).setHeatTicks(1600);

        helper.startSequence()
                .thenExecuteAfter(260, () -> {
                    FuelTankBlockEntity tank = helper.getBlockEntity(REFINERY_TANK, FuelTankBlockEntity.class);
                    helper.assertTrue(Math.abs(tank.getFuelKg() - 300.0) <= 1.0,
                            "the tank holds " + tank.getFuelKg() + " kg of hydrolox after 260 ticks, expected 300");
                    FuelRefineryBlockEntity refinery = helper.getBlockEntity(REFINERY, FuelRefineryBlockEntity.class);
                    helper.assertTrue(refinery.getWaterBuckets() == 0,
                            "the refinery has " + refinery.getWaterBuckets() + " water buckets left, expected 0");
                })
                .thenSucceed();
    }

    /**
     * With a water source block beside it, the refinery keeps its one bucket while it burns heat and refines.
     */
    private static void fuelRefinerySourceWaterIsUnlimited(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        helper.setBlock(REFINERY, TestSupport.block(helper, "fuel_refinery"));
        helper.setBlock(REFINERY_SOURCE, Blocks.WATER.defaultBlockState());
        helper.getBlockEntity(REFINERY, FuelRefineryBlockEntity.class).setWaterBuckets(1);
        helper.getBlockEntity(REFINERY, FuelRefineryBlockEntity.class).setHeatTicks(1600);

        helper.startSequence()
                .thenExecuteAfter(260, () -> {
                    FuelRefineryBlockEntity refinery = helper.getBlockEntity(REFINERY, FuelRefineryBlockEntity.class);
                    helper.assertTrue(refinery.getWaterBuckets() == 1,
                            "the refinery has " + refinery.getWaterBuckets() + " water buckets, expected 1 (source water is unlimited)");
                    helper.assertTrue(refinery.getHeatTicks() < 1600,
                            "the refinery did not refine: heat is still " + refinery.getHeatTicks() + " ticks");
                })
                .thenSucceed();
    }

    /**
     * Age 0 by default, Ages.setAge(4) takes effect, and the gates for the spark thruster and hull plate hold.
     * makeMockServerPlayerInLevel is deprecated, but its replacement has no network connection, so Ages.setAge could
     * not sync to it; this one places a real player in the level.
     */
    @SuppressWarnings("removal")
    private static void agesDefaultAndSet(GameTestHelper helper, ShipCleanup ships) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int start = Ages.getAge(player);
        helper.assertTrue(start == 3,
                "a new survival player should start at Age 3 (the login start), but Ages.getAge returned " + start);

        Ages.setAge(player, 4);
        int after = Ages.getAge(player);
        helper.assertTrue(after == 4, "after Ages.setAge(player, 4), getAge returned " + after + ", expected 4");

        int thruster = Ages.requiredAge(new ItemStack(TestSupport.item(helper, "spark_thruster")));
        helper.assertTrue(thruster == 3, "requiredAge(apoapsis:spark_thruster) is " + thruster + ", expected 3");

        int hull = Ages.requiredAge(new ItemStack(TestSupport.item(helper, "hull_plate")));
        helper.assertTrue(hull == 3, "requiredAge(apoapsis:hull_plate) is " + hull + ", expected 3");
        helper.succeed();
    }

    /**
     * Assembles the three blocks standing on the floor, checks the ship and the emptied world, waits for the ship to
     * land, then disassembles it and checks the blocks and the tank's 600 kg are back.
     */
    private static void assemblyRoundtrip(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        helper.setBlock(TestSupport.THRUSTER, TestSupport.withFacing(helper, TestSupport.block(helper, "spark_thruster"), "down"));
        helper.setBlock(TestSupport.TANK, TestSupport.block(helper, "fuel_tank"));
        helper.getBlockEntity(TestSupport.TANK, FuelTankBlockEntity.class).setFuelKg(600.0);
        helper.setBlock(TestSupport.COCKPIT, TestSupport.withFacing(helper, TestSupport.block(helper, "cockpit"), "north"));

        ShipAssembler.Result result = ShipAssembler.assemble(helper.getLevel(), helper.absolutePos(TestSupport.COCKPIT), null);
        helper.assertTrue(result.ship() != null, "ShipAssembler.assemble failed: " + TestSupport.message(result.error()));
        ShipEntity ship = ships.track(result.ship());
        helper.assertTrue(ship.isAlive() && ship.isAddedToLevel(), "the assembled ShipEntity is not alive in the level");
        helper.assertTrue(ship.getStructure().size() == 3,
                "the ship has " + ship.getStructure().size() + " blocks, expected 3");
        helper.assertTrue(Math.abs(ship.fuelKg() - 600.0) <= 0.01,
                "the ship has " + ship.fuelKg() + " kg of fuel after assembly, expected 600");
        for (BlockPos pos : List.of(TestSupport.THRUSTER, TestSupport.TANK, TestSupport.COCKPIT)) {
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.isAir(), "the block at " + pos + " should be air after assembly, but is " + state);
        }

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ship.isLanded(),
                        "the ship has not landed on the floor; entity y is " + ship.getY()))
                .thenExecute(() -> {
                    Component error = ShipAssembler.disassemble(ship);
                    helper.assertTrue(error == null,
                            "ShipAssembler.disassemble refused a landed ship: " + TestSupport.message(error));
                    assertBlockAt(helper, TestSupport.THRUSTER, "spark_thruster", "down");
                    assertBlockAt(helper, TestSupport.TANK, "fuel_tank", null);
                    FuelTankBlockEntity tank = helper.getBlockEntity(TestSupport.TANK, FuelTankBlockEntity.class);
                    helper.assertTrue(Math.abs(tank.getFuelKg() - 600.0) <= 0.01,
                            "the tank holds " + tank.getFuelKg() + " kg after disassembly, expected 600");
                    assertBlockAt(helper, TestSupport.COCKPIT, "cockpit", "north");
                })
                .thenSucceed();
    }

    /**
     * With no throttle, a ship 6 blocks up is lower after 60 ticks and then lands on the floor. Six blocks hit at about
     * 11 m/s, a hard landing below the 12 m/s crash limit.
     */
    private static void shipFallsWithoutThrust(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 6));
        ship.setThrottle(0f);
        double startY = ship.getY();

        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(ship.isAlive(), "the ship was removed during its fall");
                    helper.assertTrue(ship.getY() < startY,
                            "the ship did not fall: y went from " + startY + " to " + ship.getY() + " in 60 ticks");
                })
                .thenWaitUntil(() -> helper.assertTrue(ship.isLanded(),
                        "the ship has not landed on the floor; entity y is " + ship.getY()))
                .thenSucceed();
    }

    /** A ship dropped from 25 blocks hits at about 22 m/s, above the 12 m/s limit, and is destroyed. */
    private static void shipCrashesOnHardImpact(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 600.0), 25));
        ship.setThrottle(0f);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!ship.isAlive(),
                        "the ship survived a 25-block fall; entity y is " + ship.getY()))
                .thenSucceed();
    }

    /** At full throttle, the same ship with 1,200 kg of fuel is higher after 40 ticks and has burned fuel. */
    private static void shipClimbsWithThrust(GameTestHelper helper, ShipCleanup ships) {
        TestSupport.stoneFloor(helper);
        ShipEntity ship = ships.track(TestSupport.spawnAboveFloor(helper, TestSupport.threeBlockShip(helper, 1200.0), 10));
        ship.setThrottle(1f);
        double startY = ship.getY();

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(ship.getY() > startY,
                            "the ship did not climb at full throttle: y went from " + startY + " to " + ship.getY()
                                    + " in 40 ticks");
                    helper.assertTrue(ship.fuelKg() < 1200.0,
                            "no fuel was burned at full throttle: " + ship.fuelKg() + " kg left of 1200");
                })
                .thenSucceed();
    }

    /** Flight.spawnInOrbit puts a ship in apoapsis:orbit, where FlightHooks reports it as orbital. */
    private static void orbitSpawn(GameTestHelper helper, ShipCleanup ships) {
        ResourceKey<Level> orbit = ResourceKey.create(Registries.DIMENSION, TestSupport.id("orbit"));
        if (helper.getLevel().getServer().getLevel(orbit) == null) {
            // The vanilla GameTest server bakes its world without datapack dimensions, so apoapsis:orbit does not
            // exist here. spawnInOrbit must then fail safely; the orbit itself is covered by the testDriver run.
            helper.assertTrue(Flight.spawnInOrbit(helper.getLevel().getServer(), TestSupport.threeBlockShip(helper, 600.0),
                    BodyId.VERDANCE, 16000.0, 0.0) == null, "spawnInOrbit returned a ship without an orbit dimension");
            helper.succeed();
            return;
        }
        ShipEntity ship = Flight.spawnInOrbit(
                helper.getLevel().getServer(), TestSupport.threeBlockShip(helper, 600.0), BodyId.VERDANCE, 16000.0, 0.0);
        helper.assertTrue(ship != null, "Flight.spawnInOrbit returned null for Verdance at 16 km");
        ships.track(ship);

        helper.assertTrue(ship.level().dimension().equals(orbit),
                "the ship spawned in " + ship.level().dimension().identifier() + ", expected apoapsis:orbit");
        helper.assertTrue(FlightHooks.environment(ship).orbital(),
                "FlightHooks.environment(ship).orbital() is false for a ship in apoapsis:orbit");
        helper.succeed();
    }

    /** Asserts that the block at a relative position is the apoapsis block {@code path} with the given facing. */
    private static void assertBlockAt(GameTestHelper helper, BlockPos pos, String path, String facing) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(TestSupport.block(helper, path)),
                "expected apoapsis:" + path + " at " + pos + " after disassembly, found " + state);
        if (facing != null) {
            String found = TestSupport.facingOf(state);
            helper.assertTrue(facing.equals(found),
                    "expected facing " + facing + " on apoapsis:" + path + " at " + pos + ", found " + found);
        }
    }
}
