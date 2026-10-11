package dev.apoapsis.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds a fuel tank's hydrolox. STUB from S0; node BLK implements it (registration, save/load, sync).
 * Signatures are fixed: SHIP copies fuel in and out on assembly, SCN fills tanks.
 */
public class FuelTankBlockEntity extends BlockEntity {
    public static final double CAPACITY_KG = 1200.0;

    private double fuelKg;

    public FuelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public double getFuelKg() {
        return fuelKg;
    }

    /** Sets the fuel, clamped to 0..CAPACITY_KG, and marks the block entity changed. */
    public void setFuelKg(double kg) {
        fuelKg = Math.max(0, Math.min(CAPACITY_KG, kg));
        setChanged();
    }
}
