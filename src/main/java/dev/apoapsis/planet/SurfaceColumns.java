package dev.apoapsis.planet;

import dev.apoapsis.planet.terrain.PaleSurface;
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
 * colour comes from the biome and height, through {@link VerdanceSurface}, which also names the {@link #kind} of ground.
 * Not thread safe: each thread takes its own.
 */
public final class SurfaceColumns {
    private final TerrainSampler.Worker worker;
    private final BiomeResolver biomes;
    private final @Nullable PaleSurface.Cursor paleSurface;
    private final @Nullable VerdanceSurface verdance;
    private final int seaLevel;
    private int height;
    private int colour;
    private SurfaceKind kind = SurfaceKind.GRASS;

    private SurfaceColumns(TerrainSampler sampler, ChunkGenerator generator, RandomState randomState, boolean pale,
            boolean craters) {
        this.worker = sampler.worker();
        this.seaLevel = sampler.seaLevel();
        this.biomes = generator.getBiomeSource().createCachingResolver(randomState);
        PaleSurface surface = pale ? sampler.paleSurface() : null;
        this.paleSurface = surface == null ? null : surface.cursor(craters);
        this.verdance = pale ? null : new VerdanceSurface(randomState, worker, sampler.seaLevel());
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
            set(SurfaceColors.pale(paleSurface.regolith()));
            return;
        }
        // Water that freezes does so by the biome at its surface, not at the sea floor under it.
        int biomeY = verdance != null && ground < seaLevel ? seaLevel - 1 : ground;
        Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(biomeY),
                QuartPos.fromBlock(z));
        height = ground;
        set(verdance == null ? SurfaceColors.pale(biome) : verdance.at(biome, x, z, ground));
    }

    private void set(int packed) {
        kind = SurfaceColors.kindOf(packed);
        colour = packed & 0xFFFFFF;
    }

    /** Ground height of the current column, blocks: the y of the top face of its top solid block. */
    public int height() {
        return height;
    }

    /** Flat colour of the current column, 0xRRGGBB. */
    public int colour() {
        return colour;
    }

    /** What the current column is made of, as far as its colour goes. */
    public SurfaceKind kind() {
        return kind;
    }
}
