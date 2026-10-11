package dev.apoapsis.planet.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.apoapsis.grid.GridBlocks;
import dev.apoapsis.planet.terrain.CraterField.Crater;
import dev.apoapsis.planet.terrain.CraterField.Effect;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Craters on Pale: a bowl dug into the surface, a raised regolith rim and a light regolith ejecta ring. Radius runs from
 * 4 to 40 blocks, smaller craters more common. The config is the chance that a grid cell holds a crater, one for
 * highlands and one for maria.
 *
 * <p>Where the craters are and what each does to a column is {@link CraterField}, decided by the world seed alone.
 * Every chunk paints the craters that reach it into its own columns, so a crater that crosses a chunk edge comes out
 * whole. The feature belongs to every Pale biome, so it runs in every chunk.
 */
public record CraterFeature(float chance, float mariaChance) implements Feature {
    /** {@code chance} is the per-cell probability of a crater whose centre is in highlands, {@code maria_chance} in maria. */
    public static final MapCodec<CraterFeature> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(CraterFeature::chance),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("maria_chance").forGetter(CraterFeature::mariaChance))
                    .apply(i, CraterFeature::new));

    @Override
    public MapCodec<CraterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        // The biome of a crater's centre is a noise lookup that does not depend on height on Pale; y 0 will do.
        CraterField field = new CraterField(level.getSeed(), this, (cx, cz) -> level
                .getUncachedNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(0), QuartPos.fromBlock(cz))
                .is(PaleFeatures.MARIA));
        List<Crater> craters = field.reaching(x0 - CraterField.REACH, z0 - CraterField.REACH,
                x0 + 15 + CraterField.REACH, z0 + 15 + CraterField.REACH);
        if (craters.isEmpty()) {
            return false;
        }
        BlockState basalt = Blocks.BASALT.defaultBlockState();
        BlockState regolith = GridBlocks.REGOLITH.get().defaultBlockState();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                Effect effect = CraterField.effect(CraterField.nearest(craters, x, z), x, z);
                if (effect == null) {
                    continue;
                }
                // The surface is the top solid block; the heightmap reports the air above it.
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                switch (effect.kind()) {
                    case FLOOR -> {
                        for (int y = top; y > top - effect.blocks(); y--) {
                            put(level, x, y, z, Blocks.AIR.defaultBlockState());
                        }
                        put(level, x, top - effect.blocks(), z, basalt);
                    }
                    case RIM -> {
                        for (int y = top + 1; y <= top + effect.blocks(); y++) {
                            put(level, x, y, z, regolith);
                        }
                    }
                    case EJECTA -> put(level, x, top, z, regolith);
                }
            }
        }
        return true;
    }

    /** Places a block in the column this chunk owns. Flag 2 sends the change to clients without neighbour updates. */
    private static void put(WorldGenLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }
}
