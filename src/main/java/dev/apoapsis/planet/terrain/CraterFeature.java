package dev.apoapsis.planet.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.grid.GridBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A crater on Pale: a bowl dug into the surface, a raised regolith rim and a light regolith ejecta ring.
 * Radius runs from 4 to 40 blocks, smaller craters more common. The config is the chance that a grid cell holds a crater.
 *
 * <p>Craters come from a fixed grid of {@link #CELL} blocks, decided by the world seed alone. Every chunk paints
 * the craters that reach it into its own columns, so a crater that crosses a chunk edge comes out whole.
 */
public record CraterFeature(float chance) implements Feature {
    /** Grid cell size, blocks. Larger than twice the ejecta reach, so the cells next to a chunk cover every crater that reaches it. */
    static final int CELL = 128;
    /** Farthest ejecta reaches from a crater centre, blocks: 1.5 times the largest radius. */
    static final int REACH = 60;
    static final int MIN_RADIUS = 4;
    static final int MAX_RADIUS = 40;

    /** Chance is the per-cell probability of a crater, so 0.8 gives roughly one crater per 128 by 128 blocks. */
    public static final MapCodec<CraterFeature> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(CraterFeature::chance))
                    .apply(i, CraterFeature::new));

    @Override
    public MapCodec<CraterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        List<Crater> craters = craters(level.getSeed(), x0, z0);
        if (craters.isEmpty()) {
            return false;
        }
        BlockState basalt = Blocks.BASALT.defaultBlockState();
        BlockState regolith = GridBlocks.REGOLITH.get().defaultBlockState();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                Crater c = nearest(craters, x, z);
                double d = Math.hypot(x + 0.5 - c.x(), z + 0.5 - c.z());
                double r = c.radius();
                if (d >= 1.5 * r) {
                    continue;
                }
                // The surface is the top solid block; the heightmap reports the air above it.
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                double rimWidth = Math.max(2.0, 0.15 * r);
                if (d < r) {
                    int depth = (int) Math.round(0.2 * r * (1.0 - (d / r) * (d / r)));
                    if (depth > 0) {
                        for (int y = top; y > top - depth; y--) {
                            put(level, x, y, z, Blocks.AIR.defaultBlockState());
                        }
                        put(level, x, top - depth, z, basalt);
                    }
                } else if (d < r + rimWidth) {
                    int rise = (int) Math.round(Math.max(1.0, 0.1 * r) * (1.0 - (d - r) / rimWidth));
                    for (int y = top + 1; y <= top + rise; y++) {
                        put(level, x, y, z, regolith);
                    }
                } else {
                    put(level, x, top, z, regolith);
                }
            }
        }
        return true;
    }

    /** Places a block in the column this chunk owns. Flag 2 sends the change to clients without neighbour updates. */
    private static void put(WorldGenLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    /** The crater whose centre is nearest in units of its own radius, so overlapping craters meet at their rims. */
    private static Crater nearest(List<Crater> craters, int x, int z) {
        Crater best = craters.get(0);
        double bestRatio = Double.MAX_VALUE;
        for (Crater c : craters) {
            double ratio = Math.hypot(x + 0.5 - c.x(), z + 0.5 - c.z()) / c.radius();
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = c;
            }
        }
        return best;
    }

    /** The craters of the grid cells that can reach the chunk at (x0, z0). Each cell draws the same values for a given seed. */
    private List<Crater> craters(long seed, int x0, int z0) {
        List<Crater> out = new ArrayList<>();
        int gx0 = Math.floorDiv(x0 - REACH, CELL);
        int gx1 = Math.floorDiv(x0 + 15 + REACH, CELL);
        int gz0 = Math.floorDiv(z0 - REACH, CELL);
        int gz1 = Math.floorDiv(z0 + 15 + REACH, CELL);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                RandomSource r = RandomSource.create(seed ^ (gx * 0x9E3779B97F4A7C15L) ^ (gz * 0xC2B2AE3D27D4EB4FL));
                boolean exists = r.nextFloat() < chance;
                int cx = gx * CELL + r.nextInt(CELL);
                int cz = gz * CELL + r.nextInt(CELL);
                float u = r.nextFloat();
                // Cubing the uniform draw makes small craters common and large ones rare.
                int radius = MIN_RADIUS + Math.round((MAX_RADIUS - MIN_RADIUS) * u * u * u);
                if (exists) {
                    out.add(new Crater(cx, cz, radius));
                }
            }
        }
        return out;
    }

    /** One crater: centre in block coordinates and radius in blocks. */
    record Crater(int x, int z, int radius) {}
}
