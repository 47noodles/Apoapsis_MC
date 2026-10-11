package dev.apoapsis.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Holds a fuel tank's hydrolox. Saved as {@code fuel_kg}; the block entity also syncs to clients on load and change.
 * Signatures are fixed: SHIP copies fuel in and out on assembly, SCN fills tanks.
 */
public class FuelTankBlockEntity extends BlockEntity {
    public static final double CAPACITY_KG = 1200.0;

    private static final String FUEL_KEY = "fuel_kg";

    private double fuelKg;

    public FuelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public double getFuelKg() {
        return fuelKg;
    }

    /** Sets the fuel, clamped to 0..CAPACITY_KG, and marks the block entity changed. */
    public void setFuelKg(double kg) {
        fuelKg = clamp(kg);
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble(FUEL_KEY, fuelKg);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelKg = clamp(input.getDoubleOr(FUEL_KEY, 0.0));
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    /** Clamps to 0..CAPACITY_KG; NaN and negative values become 0. */
    private static double clamp(double kg) {
        if (!(kg > 0)) {
            return 0;
        }
        return Math.min(CAPACITY_KG, kg);
    }
}
