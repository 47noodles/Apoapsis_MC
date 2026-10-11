package dev.apoapsis.ship;

import dev.apoapsis.grid.ShipParts;
import io.netty.handler.codec.DecoderException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The blocks of a ship in its own local grid (block positions relative to the assembly origin), plus per-tank fuel.
 * Save/load and the network codec are implemented here; signatures are fixed by the contract.
 */
public final class ShipStructure {
    public static final int MAX_BLOCKS = 4096;

    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<BlockPos, Double> fuel = new LinkedHashMap<>();

    public void put(BlockPos local, BlockState state) {
        blocks.put(local.immutable(), state);
    }

    public BlockState get(BlockPos local) {
        return blocks.getOrDefault(local, Blocks.AIR.defaultBlockState());
    }

    public Map<BlockPos, BlockState> blocks() {
        return Collections.unmodifiableMap(blocks);
    }

    public int size() {
        return blocks.size();
    }

    public void setFuel(BlockPos local, double kg) {
        fuel.put(local.immutable(), kg);
    }

    public double getFuel(BlockPos local) {
        return fuel.getOrDefault(local, 0.0);
    }

    public Map<BlockPos, Double> fuel() {
        return Collections.unmodifiableMap(fuel);
    }

    /** Local position of the first cockpit block, or null. */
    public BlockPos cockpit() {
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            if (ShipParts.isCockpit(e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    public double dryMassKg() {
        double m = 0;
        for (BlockState s : blocks.values()) {
            m += ShipParts.massKg(s);
        }
        return m;
    }

    public double fuelKg() {
        double f = 0;
        for (double v : fuel.values()) {
            f += v;
        }
        return f;
    }

    public double fuelCapacityKg() {
        double c = 0;
        for (BlockState s : blocks.values()) {
            c += ShipParts.tankCapacityKg(s);
        }
        return c;
    }

    /** Centre of mass in local block coordinates (a block's centre is pos + 0.5). Includes fuel. */
    public Vec3 centerOfMassLocal() {
        double m = 0, x = 0, y = 0, z = 0;
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            double bm = ShipParts.massKg(e.getValue()) + getFuel(e.getKey());
            BlockPos p = e.getKey();
            m += bm;
            x += bm * (p.getX() + 0.5);
            y += bm * (p.getY() + 0.5);
            z += bm * (p.getZ() + 0.5);
        }
        return m <= 0 ? Vec3.ZERO : new Vec3(x / m, y / m, z / m);
    }

    public AABB localBounds() {
        if (blocks.isEmpty()) {
            return new AABB(0, 0, 0, 1, 1, 1);
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (BlockPos p : blocks.keySet()) {
            x0 = Math.min(x0, p.getX());
            y0 = Math.min(y0, p.getY());
            z0 = Math.min(z0, p.getZ());
            x1 = Math.max(x1, p.getX() + 1);
            y1 = Math.max(y1, p.getY() + 1);
            z1 = Math.max(z1, p.getZ() + 1);
        }
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    public ShipStructure copy() {
        ShipStructure s = new ShipStructure();
        s.blocks.putAll(blocks);
        s.fuel.putAll(fuel);
        return s;
    }

    /** Writes the structure into entity save data: one entry per block with its position, state and fuel. */
    public void save(ValueOutput out) {
        ValueOutput.ValueOutputList list = out.childrenList("blocks");
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            ValueOutput entry = list.addChild();
            entry.putLong("pos", e.getKey().asLong());
            entry.store("state", BlockState.CODEC, e.getValue());
            entry.putDouble("fuel", getFuel(e.getKey()));
        }
    }

    /** Reads a structure written by {@link #save}. Entries with an unreadable state are skipped. */
    public static ShipStructure load(ValueInput in) {
        ShipStructure s = new ShipStructure();
        for (ValueInput entry : in.childrenListOrEmpty("blocks").stream().toList()) {
            Optional<BlockState> state = entry.read("state", BlockState.CODEC);
            if (state.isEmpty()) {
                continue;
            }
            BlockPos pos = BlockPos.of(entry.getLongOr("pos", 0L));
            s.put(pos, state.get());
            double kg = entry.getDoubleOr("fuel", 0.0);
            if (kg > 0) {
                s.setFuel(pos, kg);
            }
        }
        return s;
    }

    /** Network form, used in the ship's spawn data. Decoding rejects oversized or malformed data. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipStructure> STREAM_CODEC =
            StreamCodec.of(ShipStructure::encodeStream, ShipStructure::decodeStream);

    private static void encodeStream(RegistryFriendlyByteBuf buf, ShipStructure s) {
        buf.writeVarInt(s.blocks.size());
        for (Map.Entry<BlockPos, BlockState> e : s.blocks.entrySet()) {
            buf.writeBlockPos(e.getKey());
            buf.writeVarInt(Block.getId(e.getValue()));
        }
        buf.writeVarInt(s.fuel.size());
        for (Map.Entry<BlockPos, Double> e : s.fuel.entrySet()) {
            buf.writeBlockPos(e.getKey());
            buf.writeDouble(e.getValue());
        }
    }

    private static ShipStructure decodeStream(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_BLOCKS) {
            throw new DecoderException("Ship has " + count + " blocks, the limit is " + MAX_BLOCKS);
        }
        ShipStructure s = new ShipStructure();
        int airId = Block.getId(Blocks.AIR.defaultBlockState());
        for (int i = 0; i < count; i++) {
            BlockPos pos = checkedPos(buf.readBlockPos());
            int id = buf.readVarInt();
            BlockState state = Block.stateById(id);
            if (state.isAir() && id != airId) {
                throw new DecoderException("Unknown block state id " + id);
            }
            s.blocks.put(pos, state);
        }
        int fuelCount = buf.readVarInt();
        if (fuelCount < 0 || fuelCount > MAX_BLOCKS) {
            throw new DecoderException("Ship has " + fuelCount + " fuel entries, the limit is " + MAX_BLOCKS);
        }
        for (int i = 0; i < fuelCount; i++) {
            BlockPos pos = checkedPos(buf.readBlockPos());
            double kg = buf.readDouble();
            if (!(kg >= 0) || Double.isInfinite(kg)) {
                throw new DecoderException("Invalid fuel amount " + kg);
            }
            s.fuel.put(pos, kg);
        }
        return s;
    }

    private static BlockPos checkedPos(BlockPos pos) {
        if (Math.abs(pos.getX()) > MAX_BLOCKS || Math.abs(pos.getY()) > MAX_BLOCKS
                || Math.abs(pos.getZ()) > MAX_BLOCKS) {
            throw new DecoderException("Block position out of range: " + pos);
        }
        return pos.immutable();
    }
}
