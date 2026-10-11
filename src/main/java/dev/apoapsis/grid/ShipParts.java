package dev.apoapsis.grid;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What each block means to a ship: mass, thrust, torque, fuel. Owned by node BLK.
 * Signatures are fixed (see docs/dev/prototype-contract.md).
 */
public final class ShipParts {
    private ShipParts() {}

    /** Masses in kg, excluding fuel. */
    private static final double HULL_PLATE_MASS_KG = 400.0;
    private static final double COCKPIT_MASS_KG = 800.0;
    private static final double GYROSCOPE_MASS_KG = 500.0;
    private static final double FUEL_TANK_DRY_MASS_KG = 150.0;
    private static final double SPARK_THRUSTER_MASS_KG = 350.0;
    private static final double GLASS_MASS_KG = 300.0;
    private static final double NOSE_CONE_MASS_KG = 150.0;
    private static final double FIN_MASS_KG = 150.0;
    /** Hull block with a porthole: lighter than a plate, about the weight of the glass it holds. */
    private static final double WINDOW_MASS_KG = 300.0;
    private static final double METAL_BLOCK_MASS_KG = 2000.0;
    private static final double LAMP_MASS_KG = 200.0;
    private static final double DEFAULT_MASS_KG = 1000.0;

    /** Spark engine: 100 kN in vacuum, Isp 440 s in vacuum and 360 s at sea level. */
    private static final double SPARK_THRUST_VAC_N = 100_000.0;
    private static final double SPARK_ISP_VAC_S = 440.0;
    private static final double SPARK_ISP_SL_S = 360.0;

    /** Gyroscope torque about each body axis. */
    private static final double GYRO_TORQUE_NM = 200_000.0;

    /** Thruster data. Thrust at constant mass flow scales with Isp: thrust(p) = thrustVacN * isp(p) / ispVac. */
    public record ThrusterSpec(Direction exhaust, double thrustVacN, double ispVac, double ispSl) {
        /** Isp in seconds at pressure fraction p/p0 (0 = vacuum, 1 = sea level), linear interpolation. */
        public double isp(double pressureFraction) {
            double p = Math.max(0, Math.min(1, pressureFraction));
            return ispVac - (ispVac - ispSl) * p;
        }

        public double thrustN(double pressureFraction) {
            return thrustVacN * isp(pressureFraction) / ispVac;
        }
    }

    /** True if flood-fill assembly may include this block (our ship blocks plus the #apoapsis:ship_blocks tag). */
    public static boolean isShipBlock(BlockState state) {
        return state.is(GridBlocks.SHIP_BLOCKS);
    }

    /** Mass of one block in kg, excluding fuel. */
    public static double massKg(BlockState state) {
        if (state.is(GridBlocks.HULL_PLATE.get())) {
            return HULL_PLATE_MASS_KG;
        }
        if (state.is(GridBlocks.COCKPIT.get())) {
            return COCKPIT_MASS_KG;
        }
        if (state.is(GridBlocks.GYROSCOPE.get())) {
            return GYROSCOPE_MASS_KG;
        }
        if (state.is(GridBlocks.FUEL_TANK.get())) {
            return FUEL_TANK_DRY_MASS_KG;
        }
        if (state.is(GridBlocks.SPARK_THRUSTER.get())) {
            return SPARK_THRUSTER_MASS_KG;
        }
        if (state.is(GridBlocks.NOSE_CONE.get())) {
            return NOSE_CONE_MASS_KG;
        }
        if (state.is(GridBlocks.FIN.get())) {
            return FIN_MASS_KG;
        }
        if (state.is(GridBlocks.WINDOW.get())) {
            return WINDOW_MASS_KG;
        }
        if (isGlass(state)) {
            return GLASS_MASS_KG;
        }
        if (state.is(Blocks.IRON_BLOCK) || state.is(BlockTags.COPPER)) {
            return METAL_BLOCK_MASS_KG;
        }
        if (state.is(BlockTags.LANTERNS) || state.is(Blocks.REDSTONE_LAMP) || state.is(Blocks.SEA_LANTERN)) {
            return LAMP_MASS_KG;
        }
        return DEFAULT_MASS_KG;
    }

    public static boolean isCockpit(BlockState state) {
        return state.is(GridBlocks.COCKPIT.get());
    }

    /** Propellant capacity in kg; 0 if the block is not a tank. */
    public static double tankCapacityKg(BlockState state) {
        return state.is(GridBlocks.FUEL_TANK.get()) ? FuelTankBlockEntity.CAPACITY_KG : 0;
    }

    /** Thruster data with the exhaust direction in the block's (unrotated, as-placed) frame; null if not a thruster. */
    public static ThrusterSpec thruster(BlockState state) {
        if (!state.is(GridBlocks.SPARK_THRUSTER.get())) {
            return null;
        }
        return new ThrusterSpec(state.getValue(SparkThrusterBlock.FACING), SPARK_THRUST_VAC_N, SPARK_ISP_VAC_S, SPARK_ISP_SL_S);
    }

    /** Gyroscope torque in N*m; 0 if not a gyroscope. */
    public static double gyroTorqueNm(BlockState state) {
        return state.is(GridBlocks.GYROSCOPE.get()) ? GYRO_TORQUE_NM : 0;
    }

    private static boolean isGlass(BlockState state) {
        return state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                || state.getBlock() instanceof StainedGlassBlock || state.getBlock() instanceof StainedGlassPaneBlock;
    }
}
