package dev.apoapsis.planet;

import dev.apoapsis.planet.terrain.PaleSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;

/**
 * A cursor over the ground columns of one body: {@link #at} moves it to a block column and {@link #height} and
 * {@link #colour} then give the ground height and flat colour there. The far-terrain tiles and the planet maps both read
 * their columns through it, so the two agree. On Pale the height includes the craters and the colour follows the block
 * really on top (basalt or regolith, with the craters' floors, rims and ejecta), through {@link PaleSurface}; Verdance's
 * colour comes from the biome and height, through {@link SurfaceColors}. Not thread safe: each thread takes its own.
 */
public final class SurfaceColumns {
    private final TerrainSampler.Worker worker;
    private final BiomeResolver biomes;
    private final boolean pale;
    private final int seaLevel;
    private final @Nullable PaleSurface.Cursor paleSurface;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private int height;
    private int colour;

    private SurfaceColumns(TerrainSampler sampler, ChunkGenerator generator, RandomState randomState, boolean pale,
            boolean craters) {
        this.worker = sampler.worker();
        this.biomes = generator.getBiomeSource().createCachingResolver(randomState);
        this.pale = pale;
        this.seaLevel = sampler.seaLevel();
        PaleSurface surface = pale ? sampler.paleSurface() : null;
        this.paleSurface = surface == null ? null : surface.cursor(craters);
    }

    /**
     * A cursor for a body's sampler and generator; {@code pale} picks Pale's colours over Verdance's. With
     * {@code craters} off, Pale's craters are left out of both height and colour: a planet map's texel is far larger than
     * a crater.
     */
    public static SurfaceColumns create(TerrainSampler sampler, ChunkGenerator generator, RandomState randomState,
            boolean pale, boolean craters) {
        return new SurfaceColumns(sampler, generator, randomState, pale, craters);
    }

    /** Moves to the column ({@code x}, {@code z}). */
    public void at(int x, int z) {
        int ground = Math.round(worker.ground(x, z));
        if (paleSurface != null) {
            paleSurface.at(x, z, ground);
            height = ground + paleSurface.heightDelta();
            colour = SurfaceColors.pale(paleSurface.regolith());
            return;
        }
        Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(ground),
                QuartPos.fromBlock(z));
        height = ground;
        colour = SurfaceColors.at(pale, biome, pos.set(x, ground, z), ground, seaLevel);
    }

    /** Ground height of the current column, blocks: the y of the top face of its top solid block. */
    public int height() {
        return height;
    }

    /** Flat colour of the current column, 0xRRGGBB. */
    public int colour() {
        return colour;
    }
}
