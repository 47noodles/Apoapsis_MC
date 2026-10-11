package dev.apoapsis.test;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Shared helpers for the prototype GameTests: registry lookups by id, floors, and the three-block test ship. */
final class TestSupport {
    /** The vanilla empty structure; each test's plot is widened by {@link #PADDING} instead of a custom file. */
    static final Identifier PLOT = Identifier.withDefaultNamespace("empty");

    /**
     * Blocks of space around each test's plot. The grid spawner spaces tests by their padded bounds, so padding keeps
     * the ships and floors of tests in the same batch apart.
     */
    static final int PADDING = 16;

    /** Relative positions used by the assembly test; the floor is the stone layer at y = 0. */
    static final BlockPos THRUSTER = new BlockPos(0, 1, 0);
    static final BlockPos TANK = new BlockPos(0, 2, 0);
    static final BlockPos COCKPIT = new BlockPos(0, 3, 0);

    private TestSupport() {}

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Apoapsis.MODID, path);
    }

    /** The block with this apoapsis id; fails the test with a clear message if it is not registered. */
    static Block block(GameTestHelper helper, String path) {
        Identifier id = id(path);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            helper.fail("block " + id + " is not registered");
        }
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    /** The item with this apoapsis id; fails the test with a clear message if it is not registered. */
    static Item item(GameTestHelper helper, String path) {
        Identifier id = id(path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            helper.fail("item " + id + " is not registered");
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    /** The default state of {@code block} with its "facing" property set to {@code direction} (e.g. "down"). */
    static BlockState withFacing(GameTestHelper helper, Block block, String direction) {
        BlockState state = block.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("facing")) {
                return withNamedValue(helper, state, property, direction);
            }
        }
        helper.fail(block + " has no facing property");
        return state;
    }

    /** The "facing" property of a state as its serialized name, or "none". */
    static String facingOf(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("facing")) {
                return nameOf(state, property);
            }
        }
        return "none";
    }

    private static <T extends Comparable<T>> BlockState withNamedValue(
            GameTestHelper helper, BlockState state, Property<T> property, String name) {
        Optional<T> value = property.getValue(name);
        if (value.isEmpty()) {
            helper.fail(name + " is not a valid value for property " + property.getName());
        }
        return state.setValue(property, value.get());
    }

    private static <T extends Comparable<T>> String nameOf(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /**
     * The corner of the chunk under the test origin with the lowest x and z, as a position relative to the origin (x and
     * z from 0 to -15, y 0). The runner starts every run at a random spot and waits only for the chunk under the
     * one-block plot to tick, so the origin can sit anywhere in that chunk, and a ship in any other chunk never ticks.
     * A test that builds or spawns anything wider than a block lays it out from this corner, inside the chunk's 16 by
     * 16 columns.
     */
    static BlockPos chunkCorner(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(-(origin.getX() & 15), 0, -(origin.getZ() & 15));
    }

    /** Lays a 5 by 5 stone floor whose top surface is at relative y = 1. */
    static void stoneFloor(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(new BlockPos(x, 0, z), stone);
            }
        }
    }

    /**
     * The test ship in its own grid at local y 0, 1, 2: spark thruster (facing down), fuel tank holding
     * {@code fuelKg}, cockpit (facing north). The assembly test builds the same column one block higher (y 1 to 3).
     */
    static ShipStructure threeBlockShip(GameTestHelper helper, double fuelKg) {
        ShipStructure structure = new ShipStructure();
        structure.put(new BlockPos(0, 0, 0), withFacing(helper, block(helper, "spark_thruster"), "down"));
        structure.put(new BlockPos(0, 1, 0), block(helper, "fuel_tank").defaultBlockState());
        structure.put(new BlockPos(0, 2, 0), withFacing(helper, block(helper, "cockpit"), "north"));
        structure.setFuel(new BlockPos(0, 1, 0), fuelKg);
        return structure;
    }

    /**
     * Spawns {@code structure} with its lowest block {@code blocksAboveFloor} blocks above the stone floor's top
     * surface, unrotated and at rest in the test plot. The entity position is the centre of mass.
     */
    static ShipEntity spawnAboveFloor(GameTestHelper helper, ShipStructure structure, int blocksAboveFloor) {
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1 + blocksAboveFloor, 0));
        Vec3 com = structure.centerOfMassLocal();
        Vec3 comPos = new Vec3(origin.getX() + com.x, origin.getY() + com.y, origin.getZ() + com.z);
        return ShipEntity.spawn(helper.getLevel(), structure, comPos, new Quaternionf());
    }

    /** The message of an error component, or a fallback when a call returned none. */
    static String message(Component error) {
        return error == null ? "no reason given" : error.getString();
    }
}
