package dev.apoapsis.planet;

import dev.apoapsis.Apoapsis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.OverworldFunctionSet;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.op.FindTopSurfaceFunction;
import org.jspecify.annotations.Nullable;

/**
 * Ground heights of a body for the far terrain and the planet maps, cheaper than {@code getBaseHeight}.
 *
 * <p>{@code getBaseHeight} samples a whole 384-block column of the final density for every call, about half a
 * millisecond each on Verdance. This class finds the same surface (the top of the solid ground, as the ocean-floor
 * world-generation heightmap reports it) by a short vertical search on the sloped-cheese density: the function the
 * overworld final density is built from, without the caves. A first guess comes from the quantised preliminary surface
 * level, then eight-block steps find the bracket and one interpolation inside it gives a sub-block height. Terrain
 * whose settings are not one of the three overworld presets (Pale, whose density is a 2D noise and already cheap) uses
 * {@code getBaseHeight} itself, so its heights are exact.
 *
 * <p>A sampler is shared between threads; each thread takes its own {@link Worker}.
 */
public final class TerrainSampler {
    /** Vertical step of the downward search, blocks. */
    private static final int STEP = 8;
    /** How far above the first guess the search starts, blocks. */
    private static final int HEADROOM = 48;
    /** The search gives up this far below the first guess and returns the guess, blocks. */
    private static final int DEPTH_LIMIT = 80;

    private final ChunkGenerator generator;
    private final RandomState randomState;
    private final LevelHeightAccessor heightAccessor;
    private final int seaLevel;
    /** The sloped-cheese sampler, or null when the exact path is used. */
    private final @Nullable DensitySampler cheese;
    /** The (quantised) preliminary surface level sampler; non-null exactly when {@link #cheese} is. */
    private final @Nullable DensitySampler prelim;

    private TerrainSampler(ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heightAccessor,
            int seaLevel, @Nullable DensitySampler cheese, @Nullable DensitySampler prelim) {
        this.generator = generator;
        this.randomState = randomState;
        this.heightAccessor = heightAccessor;
        this.seaLevel = seaLevel;
        this.cheese = cheese;
        this.prelim = prelim;
    }

    /** The sampler for a loaded level's own generator. */
    public static TerrainSampler of(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return create(generator, level.getChunkSource().randomState(), level, level.registryAccess());
    }

    /** The sampler for a generator and its random state; {@code registries} supplies the
     * overworld density functions. */
    public static TerrainSampler create(ChunkGenerator generator, RandomState randomState,
            LevelHeightAccessor heightAccessor, HolderLookup.Provider registries) {
        DensitySampler cheese = null;
        DensitySampler prelim = null;
        if (generator instanceof NoiseBasedChunkGenerator noise) {
            OverworldFunctionSet<ResourceKey<DensityFunction>> names = overworldNames(noise);
            if (names != null) {
                try {
                    DensityFunction cheeseFn = new DensityFunctions.HolderHolder(registries
                            .lookupOrThrow(Registries.DENSITY_FUNCTION).getOrThrow(names.slopedCheese()));
                    DensityFunction prelimFn = new DensityFunctions.HolderHolder(registries
                            .lookupOrThrow(Registries.DENSITY_FUNCTION).getOrThrow(names.preliminarySurfaceLevel()));
                    if (unwrap(prelimFn) instanceof FindTopSurfaceFunction) {
                        cheese = randomState.getSampler(cheeseFn);
                        prelim = randomState.getSampler(prelimFn);
                    }
                } catch (RuntimeException e) {
                    Apoapsis.LOG.warn("Fast terrain heights unavailable, using getBaseHeight", e);
                }
            }
        }
        return new TerrainSampler(generator, randomState, heightAccessor, generator.getSeaLevel(), cheese, prelim);
    }

    /** The density function names of the overworld preset that {@code generator} uses,
     * or null for any other settings. */
    private static @Nullable OverworldFunctionSet<ResourceKey<DensityFunction>> overworldNames(
            NoiseBasedChunkGenerator generator) {
        if (generator.stable(NoiseGeneratorSettings.OVERWORLD)) {
            return NoiseRouterData.OVERWORLD_FUNCTIONS;
        } else if (generator.stable(NoiseGeneratorSettings.LARGE_BIOMES)) {
            return NoiseRouterData.LARGE_OVERWORLD_FUNCTIONS;
        } else if (generator.stable(NoiseGeneratorSettings.AMPLIFIED)) {
            return NoiseRouterData.AMPLIFIED_OVERWORLD_FUNCTIONS;
        }
        return null;
    }

    private static DensityFunction unwrap(DensityFunction f) {
        return f instanceof DensityFunctions.HolderHolder h ? unwrap(h.function().value()) : f;
    }

    /** True when heights come from the short vertical search, false when each one is a {@code getBaseHeight} call. */
    public boolean fast() {
        return cheese != null;
    }

    /** Sea level of the generator, blocks. */
    public int seaLevel() {
        return seaLevel;
    }

    /** The exact ground height at a column: {@code getBaseHeight} for the ocean-floor
     * heightmap (top solid block + 1). */
    public int exact(int x, int z) {
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
    }

    /** A per-thread sampling handle. */
    public Worker worker() {
        return new Worker();
    }

    /** Per-thread state: the sampler context with its caches. Not for sharing between threads. */
    public final class Worker {
        private final SamplerContext context = SamplerContext.builder().enableCaches().build();

        private Worker() {}

        /** The ground height at a column, blocks: sub-block on the fast path, a whole number on the exact path. */
        public float ground(int x, int z) {
            if (cheese == null || prelim == null) {
                return exact(x, z);
            }
            return search(x, z);
        }

        private float density(int x, int y, int z) {
            return cheese.sampleValue(context, x, y, z);
        }

        private float search(int x, int z) {
            int minY = heightAccessor.getMinY();
            int maxY = heightAccessor.getMaxY();
            int guess = Math.round(prelim.sampleValue(context, x, 0, z));
            int hi = Math.min(guess + HEADROOM, maxY);
            float dHi = density(x, hi, z);
            while (dHi > 0 && hi < maxY) {
                hi = Math.min(hi + 4 * STEP, maxY);
                dHi = density(x, hi, z);
            }
            int floor = Math.max(minY, guess - DEPTH_LIMIT);
            while (hi > floor) {
                int lo = Math.max(floor, hi - STEP);
                float dLo = density(x, lo, z);
                if (dLo > 0) {
                    // Solid at lo, open at hi: the zero crossing, by linear interpolation of the density between them.
                    // Blocks up to floor(crossing) are solid and the height is one above the top one: +0.5 on average.
                    float t = dLo / (dLo - dHi);
                    return lo + (hi - lo) * t + 0.5f;
                }
                hi = lo;
                dHi = dLo;
            }
            return guess;
        }
    }
}
