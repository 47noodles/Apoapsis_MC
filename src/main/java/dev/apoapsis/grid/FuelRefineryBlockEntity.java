package dev.apoapsis.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Fuel refinery state: stored water buckets, furnace heat as burn ticks, the refining cycle, and hydrolox waiting to
 * be pushed into adjacent fuel tanks. Saved as {@code water_buckets}, {@code heat_ticks}, {@code progress} and
 * {@code hydrolox_kg}.
 */
public class FuelRefineryBlockEntity extends BlockEntity {
    /** Hydrolox made per refining cycle: the same 300 kg as one canister. */
    public static final double KG_PER_CYCLE = FuelTankBlock.CANISTER_KG;
    /** Ticks per refining cycle. */
    public static final int CYCLE_TICKS = 200;
    /** Hydrolox the refinery itself stores before it stops refining. */
    public static final double CAPACITY_KG = 2400.0;
    /** Water buckets stored. */
    public static final int MAX_WATER = 8;
    /** Eight coal's worth of burn time: coal burns for 1,600 ticks. */
    public static final int MAX_HEAT_TICKS = 8 * 1600;

    private static final int SOURCE_CHECK_INTERVAL = 20;
    private static final int PUSH_INTERVAL = 20;
    private static final int SMOKE_INTERVAL = 10;

    private static final String WATER_KEY = "water_buckets";
    private static final String HEAT_KEY = "heat_ticks";
    private static final String PROGRESS_KEY = "progress";
    private static final String HYDROLOX_KEY = "hydrolox_kg";

    public static final BlockEntityTicker<FuelRefineryBlockEntity> SERVER_TICKER = FuelRefineryBlockEntity::serverTick;
    public static final BlockEntityTicker<FuelRefineryBlockEntity> CLIENT_TICKER = FuelRefineryBlockEntity::clientTick;

    private int waterBuckets;
    private int heatTicks;
    private int progress;
    private double hydrolox;
    /** Cached result of the side check, refreshed every {@link #SOURCE_CHECK_INTERVAL} ticks. */
    private boolean sourceAdjacent;
    private boolean sourceKnown;

    public FuelRefineryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getWaterBuckets() {
        return waterBuckets;
    }

    /** Sets the stored water, clamped to 0..MAX_WATER. */
    public void setWaterBuckets(int buckets) {
        waterBuckets = clampInt(buckets, MAX_WATER);
        setChanged();
    }

    public int getHeatTicks() {
        return heatTicks;
    }

    /** Sets the burn time left, clamped to 0..MAX_HEAT_TICKS. */
    public void setHeatTicks(int ticks) {
        heatTicks = clampInt(ticks, MAX_HEAT_TICKS);
        setChanged();
    }

    public double getHydroloxKg() {
        return hydrolox;
    }

    /** True when any of the six sides of {@code pos} is a water source block. */
    static boolean hasSourceWater(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            var fluid = level.getFluidState(pos.relative(direction));
            if (fluid.getType() == Fluids.WATER && fluid.isSource()) {
                return true;
            }
        }
        return false;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FuelRefineryBlockEntity refinery) {
        refinery.tickServer(level, pos, state);
    }

    /** Smoke above the refinery every {@link #SMOKE_INTERVAL} ticks while it is lit. Client only. */
    static void clientTick(Level level, BlockPos pos, BlockState state, FuelRefineryBlockEntity refinery) {
        if (state.getValue(BlockStateProperties.LIT) && level.getGameTime() % SMOKE_INTERVAL == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        long time = level.getGameTime();
        if (!sourceKnown || time % SOURCE_CHECK_INTERVAL == 0) {
            sourceAdjacent = hasSourceWater(level, pos);
            sourceKnown = true;
        }

        boolean refining;
        if (progress == 0) {
            // A cycle starts only with heat, water (or a source), and room in the store for its 300 kg.
            refining = heatTicks > 0 && (sourceAdjacent || waterBuckets > 0) && hydrolox + KG_PER_CYCLE <= CAPACITY_KG;
            if (refining && !sourceAdjacent) {
                waterBuckets--;
            }
        } else {
            // A running cycle has its water already; it only needs heat to keep going.
            refining = heatTicks > 0;
        }
        if (refining) {
            heatTicks--;
            progress++;
            if (progress >= CYCLE_TICKS) {
                progress = 0;
                hydrolox = Math.min(CAPACITY_KG, hydrolox + KG_PER_CYCLE);
            }
            setChanged();
        }
        if (state.getValue(BlockStateProperties.LIT) != refining) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, refining), Block.UPDATE_ALL);
        }
        if (hydrolox > 0 && time % PUSH_INTERVAL == 0) {
            pushToTanks(level, pos);
        }
    }

    /** Moves hydrolox into adjacent fuel tanks, up to each tank's capacity. */
    private void pushToTanks(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (hydrolox <= 0) {
                return;
            }
            if (level.getBlockEntity(pos.relative(direction)) instanceof FuelTankBlockEntity tank) {
                double moved = Math.min(FuelTankBlockEntity.CAPACITY_KG - tank.getFuelKg(), hydrolox);
                if (moved > 0) {
                    tank.setFuelKg(tank.getFuelKg() + moved);
                    hydrolox = Math.max(0, hydrolox - moved);
                    setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(WATER_KEY, waterBuckets);
        output.putInt(HEAT_KEY, heatTicks);
        output.putInt(PROGRESS_KEY, progress);
        output.putDouble(HYDROLOX_KEY, hydrolox);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        waterBuckets = clampInt(input.getIntOr(WATER_KEY, 0), MAX_WATER);
        heatTicks = clampInt(input.getIntOr(HEAT_KEY, 0), MAX_HEAT_TICKS);
        progress = clampInt(input.getIntOr(PROGRESS_KEY, 0), CYCLE_TICKS);
        hydrolox = clampKg(input.getDoubleOr(HYDROLOX_KEY, 0.0));
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    /** Clamps to 0..max; negative values become 0. */
    private static int clampInt(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    /** Clamps to 0..CAPACITY_KG; NaN and negative values become 0. */
    private static double clampKg(double kg) {
        if (!(kg > 0)) {
            return 0;
        }
        return Math.min(CAPACITY_KG, kg);
    }
}
