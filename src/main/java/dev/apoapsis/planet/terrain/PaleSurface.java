package dev.apoapsis.planet.terrain;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.planet.terrain.CraterField.Crater;
import dev.apoapsis.planet.terrain.CraterField.Effect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.Nullable;

/**
 * What Pale's surface looks like from above, column by column, without generating a chunk: the height the craters
 * change and which of the two surface materials, basalt or regolith, lies on top. It reproduces three things of the
 * world generator: the surface rules of {@code apoapsis:pale} (regolith over the highlands and over the band of the
 * maria noise that rings each mare; basalt elsewhere), the craters of {@link CraterField}, and the per-block biome
 * lookup the surface rules use ({@link BiomeManager}, with the world seed's zoom). The GameTests compare all of it with
 * real chunks.
 *
 * <p>Shared between threads; each thread takes its own {@link Cursor}.
 */
public final class PaleSurface {
    /** The noise settings of the Pale dimension. */
    public static final ResourceKey<NoiseGeneratorSettings> SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS,
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale"));
    private static final ResourceKey<NormalNoise> MARIA_NOISE = ResourceKey.create(Registries.NOISE,
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale/maria"));
    /** The band of the maria noise that the surface rules of {@code noise_settings/pale.json} cover with regolith. */
    private static final double BAND_MIN = -0.15;
    private static final double BAND_MAX = -0.05;
    /** A cursor forgets its crater cells when it holds this many. */
    private static final int MAX_CACHED_CELLS = 512;

    private final ChunkGenerator generator;
    private final RandomState randomState;
    private final CraterFeature craters;
    private final Noise mariaNoise;
    private final long zoomSeed;

    private PaleSurface(ChunkGenerator generator, RandomState randomState, CraterFeature craters) {
        this.generator = generator;
        this.randomState = randomState;
        this.craters = craters;
        this.mariaNoise = randomState.getOrCreateNoise(MARIA_NOISE);
        this.zoomSeed = BiomeManager.obfuscateSeed(randomState.seed());
    }

    /**
     * The model of Pale for a generator and random state, or null when the generator is not Pale's. The crater chances
     * come from the {@code apoapsis:crater} feature in {@code registries}, so the data stays the one source.
     */
    public static @Nullable PaleSurface create(ChunkGenerator generator, RandomState randomState,
            HolderLookup.Provider registries) {
        if (!(generator instanceof NoiseBasedChunkGenerator noise) || !noise.stable(SETTINGS)) {
            return null;
        }
        try {
            Feature feature = registries.lookupOrThrow(Registries.FEATURE).getOrThrow(PaleFeatures.CRATER).value();
            if (feature instanceof CraterFeature crater) {
                return new PaleSurface(generator, randomState, crater);
            }
        } catch (RuntimeException e) {
            Apoapsis.LOG.warn("Pale's far terrain has no craters: the crater feature is missing", e);
        }
        return null;
    }

    /**
     * A per-thread cursor: {@link #at} moves it to a column and the accessors read what lies there. With
     * {@code withCraters} off it leaves craters out (a planet map's texel is far bigger than one).
     */
    public Cursor cursor(boolean withCraters) {
        return new Cursor(withCraters);
    }

    /** Per-thread state: the biome lookup caches and the crater cells seen so far. */
    public final class Cursor {
        private final boolean withCraters;
        private final BiomeResolver biomes = generator.getBiomeSource().createCachingResolver(randomState);
        private final BiomeManager biomeManager = new BiomeManager(biomes, zoomSeed);
        private final CraterField field = new CraterField(randomState.seed(), craters,
                (x, z) -> biomes.getNoiseBiome(QuartPos.fromBlock(x), 0, QuartPos.fromBlock(z)).is(PaleFeatures.MARIA));
        private final Map<Long, Crater> cells = new HashMap<>();
        private final List<Crater> near = new ArrayList<>(4);
        private int delta;
        private boolean regolith;

        private Cursor(boolean withCraters) {
            this.withCraters = withCraters;
        }

        /** Moves to the column ({@code x}, {@code z}) whose uncratered ground height is {@code ground}. */
        public void at(int x, int z, int ground) {
            Effect effect = withCraters ? crater(x, z) : null;
            if (effect == null) {
                delta = 0;
                regolith = plainRegolith(x, z, ground);
                return;
            }
            switch (effect.kind()) {
                case FLOOR -> {
                    delta = -effect.blocks();
                    regolith = false;
                }
                case RIM -> {
                    delta = effect.blocks();
                    regolith = true;
                }
                case EJECTA -> {
                    delta = 0;
                    regolith = true;
                }
            }
        }

        /** How far the craters move the current column's top, blocks: below zero in a bowl, above on a rim. */
        public int heightDelta() {
            return delta;
        }

        /** True when the current column's top block is regolith, false when it is basalt. */
        public boolean regolith() {
            return regolith;
        }

        /** The surface rules without craters: regolith on the highlands and on the maria noise's band. */
        private boolean plainRegolith(int x, int z, int ground) {
            // The rules ask for the biome at the top block, whose y is one below the ground height; the lookup's
            // random zoom can pick a different neighbour cell for a different y.
            Holder<Biome> biome = biomeManager.getBiome(x, ground - 1, z);
            if (biome.is(PaleFeatures.HIGHLANDS)) {
                return true;
            }
            double n = mariaNoise.get(x, 0.0, z);
            return n >= BAND_MIN && n <= BAND_MAX;
        }

        private @Nullable Effect crater(int x, int z) {
            near.clear();
            for (int gx = Math.floorDiv(x - CraterField.REACH, CraterField.CELL);
                    gx <= Math.floorDiv(x + CraterField.REACH, CraterField.CELL); gx++) {
                for (int gz = Math.floorDiv(z - CraterField.REACH, CraterField.CELL);
                        gz <= Math.floorDiv(z + CraterField.REACH, CraterField.CELL); gz++) {
                    Crater c = cell(gx, gz);
                    if (c != null) {
                        near.add(c);
                    }
                }
            }
            return CraterField.effect(CraterField.nearest(near, x, z), x, z);
        }

        private @Nullable Crater cell(int gx, int gz) {
            long key = (long) gx << 32 | (gz & 0xFFFFFFFFL);
            if (cells.size() >= MAX_CACHED_CELLS) {
                cells.clear();
            }
            Crater c = cells.get(key);
            if (c == null && !cells.containsKey(key)) {
                c = field.cell(gx, gz);
                cells.put(key, c);
            }
            return c;
        }
    }
}
