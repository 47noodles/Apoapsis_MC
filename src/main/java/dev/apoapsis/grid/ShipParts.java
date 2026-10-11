package dev.apoapsis.grid;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What each block means to a ship: mass, thrust, torque, fuel. STUB from S0; node BLK implements it.
 * Signatures are fixed (see docs/dev/prototype-contract.md).
 */
public final class ShipParts {
    private ShipParts() {}

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
        return false;
    }

    /** Mass of one block in kg, excluding fuel. */
    public static double massKg(BlockState state) {
        return 1000.0;
    }

    public static boolean isCockpit(BlockState state) {
        return false;
    }

    /** Propellant capacity in kg; 0 if the block is not a tank. */
    public static double tankCapacityKg(BlockState state) {
        return 0;
    }

    /** Thruster data with the exhaust direction in the block's (unrotated, as-placed) frame; null if not a thruster. */
    public static ThrusterSpec thruster(BlockState state) {
        return null;
    }

    /** Gyroscope torque in N*m; 0 if not a gyroscope. */
    public static double gyroTorqueNm(BlockState state) {
        return 0;
    }
}
