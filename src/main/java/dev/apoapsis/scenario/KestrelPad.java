package dev.apoapsis.scenario;

import dev.apoapsis.grid.FuelTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Builds a 7 x 7 smooth-stone pad and sets a blueprint's blocks on it as real, fuelled blocks. */
public final class KestrelPad {
    /** Half-width of the pad: the pad is 2 * HALF + 1 blocks square. */
    public static final int HALF = 3;
    private static final int CLEAR_HEIGHT = 12;
    private static final int MAX_SUPPORT_DEPTH = 6;

    private KestrelPad() {}

    /**
     * Builds the pad centred on column (cx, cz) at the surface, clears the air above it, and places {@code blueprint}
     * so its bottom sits on the pad. Returns false if the column has no room.
     */
    public static boolean place(ServerLevel level, int cx, int cz, Blueprint blueprint) {
        return placeCockpit(level, cx, cz, blueprint) != null;
    }

    /**
     * As {@link #place}, and returns the world position of the blueprint's cockpit (its origin), or null if the column
     * has no room.
     */
    public static BlockPos placeCockpit(ServerLevel level, int cx, int cz, Blueprint blueprint) {
        int padY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1;
        if (padY <= level.getMinY() + MAX_SUPPORT_DEPTH || padY + CLEAR_HEIGHT >= level.getMaxY()) {
            return null;
        }
        BlockState stone = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                level.setBlock(new BlockPos(x, padY, z), stone, Block.UPDATE_ALL);
                for (int y = padY - 1; y >= padY - MAX_SUPPORT_DEPTH; y--) {
                    BlockPos below = new BlockPos(x, y, z);
                    if (level.getBlockState(below).isSolid()) {
                        break;
                    }
                    level.setBlock(below, stone, Block.UPDATE_ALL);
                }
                for (int y = padY + 1; y <= padY + CLEAR_HEIGHT; y++) {
                    BlockPos above = new BlockPos(x, y, z);
                    if (!level.getBlockState(above).isAir()) {
                        level.setBlock(above, air, Block.UPDATE_ALL);
                    }
                }
            }
        }
        // The blueprint's lowest layer is at local y = -3, so it sits on the pad.
        int baseY = padY + 1 - minY(blueprint);
        for (Blueprint.Entry e : blueprint.entries()) {
            BlockPos pos = new BlockPos(cx + e.pos().getX(), baseY + e.pos().getY(), cz + e.pos().getZ());
            level.setBlock(pos, e.state(), Block.UPDATE_ALL);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FuelTankBlockEntity tank) {
                tank.setFuelKg(FuelTankBlockEntity.CAPACITY_KG);
            }
        }
        return new BlockPos(cx, baseY, cz);
    }

    private static int minY(Blueprint blueprint) {
        int min = Integer.MAX_VALUE;
        for (Blueprint.Entry e : blueprint.entries()) {
            min = Math.min(min, e.pos().getY());
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }
}
