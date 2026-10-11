package dev.apoapsis.ship;

import dev.apoapsis.grid.ShipParts;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The blocks of a ship in its own local grid (block positions relative to the assembly origin), plus per-tank fuel.
 * STUB from S0 (in-memory parts work; save/load and the network codec are node SHIP's job). Signatures are fixed.
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

    /** Writes the structure into entity save data. */
    public void save(ValueOutput out) {
    }

    /** Reads a structure written by {@link #save}. */
    public static ShipStructure load(ValueInput in) {
        return new ShipStructure();
    }

    /** Network form, used in the ship's spawn data. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipStructure> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> buf.writeVarInt(0),
            buf -> {
                buf.readVarInt();
                return new ShipStructure();
            });
}
