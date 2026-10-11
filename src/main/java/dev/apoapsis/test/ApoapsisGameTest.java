package dev.apoapsis.test;

import dev.apoapsis.ship.ShipEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * A GameTest whose body is Java code, registered from {@link GameTests} without a test_instance JSON file. It extends
 * {@link FunctionGameTestInstance} only so the codec works for registry sync; the function key is the test's name and
 * is never looked up, because {@link #run} is overridden. Ships a test spawns are discarded when it ends.
 */
final class ApoapsisGameTest extends FunctionGameTestInstance {
    /** A test body. Spawned ships go through {@code ships} so they are discarded however the test ends. */
    @FunctionalInterface
    interface Body {
        void run(GameTestHelper helper, ShipCleanup ships);
    }

    private final Body body;

    ApoapsisGameTest(String name, Holder<TestEnvironmentDefinition<?>> environment, int maxTicks, Body body) {
        super(
                ResourceKey.create(Registries.TEST_FUNCTION, TestSupport.id(name)),
                new TestData<>(
                        environment, Level.OVERWORLD, TestSupport.PLOT, maxTicks, 0, true, Rotation.NONE, false, 1, 1,
                        true, TestSupport.PADDING));
        this.body = body;
    }

    @Override
    public void run(GameTestHelper helper) {
        ShipCleanup ships = new ShipCleanup();
        helper.testInfo.addListener(ships);
        this.body.run(helper, ships);
    }

    /** Tracks the ships a test spawns and discards them when the test passes or fails. */
    static final class ShipCleanup implements GameTestListener {
        private final List<ShipEntity> ships = new ArrayList<>();

        ShipEntity track(ShipEntity ship) {
            this.ships.add(ship);
            return ship;
        }

        @Override
        public void testStructureLoaded(GameTestInfo testInfo) {
        }

        @Override
        public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
            discardAll();
        }

        @Override
        public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
            discardAll();
        }

        @Override
        public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
        }

        private void discardAll() {
            for (ShipEntity ship : this.ships) {
                if (ship.isAlive()) {
                    ship.discard();
                }
            }
            this.ships.clear();
        }
    }
}
