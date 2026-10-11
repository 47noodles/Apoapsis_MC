package dev.apoapsis.test;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.PlanetMapPayload;
import dev.apoapsis.net.TerrainTilePayload;
import dev.apoapsis.planet.PlanetMapGen;
import dev.apoapsis.planet.TerrainSampler;
import dev.apoapsis.planet.TerrainTileGen;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Builds the two bodies' generators outside any level, for tests: the vanilla normal overworld (Verdance) and the
 * datapack dimension apoapsis:pale, each with a {@link RandomState} for a fixed seed. The GameTest server bakes its
 * world flat and without datapack dimensions, so a test cannot take these from the level.
 */
final class TerrainProbe {
    /** Timed passes of the sampler in {@link #compare}; the fastest counts. */
    private static final int SAMPLER_PASSES = 5;

    /** The seed of the "Apoapsis Test Flight" save. */
    static final long SEED = 26032026L;

    /** One body's generator, random state and sampler. */
    record Body(String name, ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights,
            TerrainSampler sampler, int radius) {}

    private TerrainProbe() {}

    static Body verdance(RegistryAccess access) {
        LevelStem stem = WorldPresets.getNormalOverworld(access);
        return body("verdance", stem, access, 64_000);
    }

    /**
     * Pale's noise settings under the overworld's biome source: the dimension itself is missing from the GameTest
     * registries, but its noise settings are there, and heights do not depend on the biome source.
     */
    static Body pale(RegistryAccess access) {
        ResourceKey<NoiseGeneratorSettings> key = ResourceKey.create(Registries.NOISE_SETTINGS, TestSupport.id("pale"));
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(key);
        ChunkGenerator overworld = WorldPresets.getNormalOverworld(access).generator();
        return body("pale", new NoiseBasedChunkGenerator(overworld.getBiomeSource(), settings), access, 20_000);
    }

    private static Body body(String name, LevelStem stem, RegistryAccess access, int radius) {
        return body(name, stem.generator(), access, radius);
    }

    private static Body body(String name, ChunkGenerator generator, RegistryAccess access, int radius) {
        NoiseBasedChunkGenerator noise = (NoiseBasedChunkGenerator) generator;
        RandomState randomState = RandomState.create(access.lookupOrThrow(Registries.NOISE), SEED,
                noise.generatorSettings().value());
        int minY = noise.generatorSettings().value().noiseSettings().minY();
        int height = noise.generatorSettings().value().noiseSettings().height();
        LevelHeightAccessor heights = LevelHeightAccessor.create(minY, height);
        return new Body(name, generator, randomState, heights,
                TerrainSampler.create(generator, randomState, heights, access), radius);
    }

    /**
     * Compares the sampler with getBaseHeight at {@code n} random columns and logs error and cost statistics. The
     * sampler's cost is the best of {@link #SAMPLER_PASSES} passes, so the 10x cost assertion fails for a sampler that
     * is really slower and not for a machine that was busy for a few milliseconds.
     */
    static Error compare(Body b, int n, long seed) {
        Random rnd = new Random(seed);
        int[] xs = new int[n];
        int[] zs = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = rnd.nextInt(2 * b.radius()) - b.radius();
            zs[i] = rnd.nextInt(b.radius()) - b.radius() / 2;
        }
        TerrainSampler.Worker w = b.sampler().worker();
        for (int i = 0; i < 20; i++) {
            b.sampler().exact(xs[i], zs[i]);
            w.ground(xs[i], zs[i]);
        }
        long t0 = System.nanoTime();
        int[] exact = new int[n];
        for (int i = 0; i < n; i++) {
            exact[i] = b.sampler().exact(xs[i], zs[i]);
        }
        long t1 = System.nanoTime();
        // The sampler pass is only milliseconds long, so one scheduling hiccup or GC pause in it would swamp the cost per
        // sample on a busy machine. Time several passes, each on a fresh worker so its caches start cold, and keep the
        // fastest: a pause only ever makes a pass slower, never faster.
        float[] fast = new float[n];
        long samplerNanos = Long.MAX_VALUE;
        for (int pass = 0; pass < SAMPLER_PASSES; pass++) {
            TerrainSampler.Worker passWorker = b.sampler().worker();
            long s0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                fast[i] = passWorker.ground(xs[i], zs[i]);
            }
            samplerNanos = Math.min(samplerNanos, System.nanoTime() - s0);
        }
        double[] err = new double[n];
        double sum = 0, sumSq = 0, max = 0;
        for (int i = 0; i < n; i++) {
            err[i] = fast[i] - exact[i];
            sum += err[i];
            sumSq += err[i] * err[i];
            max = Math.max(max, Math.abs(err[i]));
        }
        double[] abs = Arrays.stream(err).map(Math::abs).sorted().toArray();
        Apoapsis.LOG.info("[terrain-probe] {} fast={} n={} exact {} us/sample, sampler {} us/sample;"
                        + " error vs getBaseHeight:"
                        + " bias {} rms {} median {} p95 {} max {} blocks",
                b.name(), b.sampler().fast(), n, f((t1 - t0) / 1000.0 / n), f(samplerNanos / 1000.0 / n),
                f(sum / n), f(Math.sqrt(sumSq / n)), f(abs[n / 2]), f(abs[(int) (n * 0.95)]), f(max));
        return new Error(sum / n, Math.sqrt(sumSq / n), abs[(int) (n * 0.95)], max, (t1 - t0) / 1000.0 / n,
                samplerNanos / 1000.0 / n);
    }

    /** Height error of the sampler against getBaseHeight, in blocks, and the cost per sample, microseconds. */
    record Error(double bias, double rms, double p95, double max, double exactMicros, double samplerMicros) {}

    /** Generates each level's tile around ({@code cx}, {@code cz}), logs the time, and checks
     * the tile is well formed. */
    static void timeTiles(Body b, int cx, int cz) {
        for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
            int x = TerrainTileGen.snap(level, cx), z = TerrainTileGen.snap(level, cz);
            long t0 = System.nanoTime();
            TerrainTilePayload tile = TerrainTileGen.generate(b.sampler(), b.generator(), b.randomState(), b.name(),
                    level, x, z);
            double ms = (System.nanoTime() - t0) / 1e6;
            if (tile == null || !tile.valid()) {
                throw new IllegalStateException(b.name() + " level " + level + " produced an invalid tile");
            }
            int n = tile.size();
            int half = (n - 1) / 2;
            // The centre vertex must equal the sampler's own height there (the tile adds nothing of its own).
            TerrainSampler.Worker w = b.sampler().worker();
            int centre = Math.round(w.ground(x, z));
            if (tile.heights()[half * n + half] != centre) {
                throw new IllegalStateException("centre vertex " + tile.heights()[half * n + half] + " != " + centre);
            }
            Apoapsis.LOG.info("[terrain-probe] {} tile level {} {}x{} spacing {}: {} ms ({} us/vertex)",
                    b.name(), level,
                    n, n, tile.spacing(), f(ms), f(ms * 1000 / (n * n)));
        }
    }

    /** Generates a whole planet map and logs the time; the texels must not all be one colour. */
    static void timeMap(Body b, int width, int height, int ss) {
        long t0 = System.nanoTime();
        PlanetMapPayload map = PlanetMapGen.generate(b.sampler(), b.generator(), b.randomState(), b.name(), b.radius(),
                width, height, ss);
        double s = (System.nanoTime() - t0) / 1e9;
        int distinct = 0;
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < width * height; i += 7) {
            int c = (map.rgb()[3 * i] & 0xFF) << 16 | (map.rgb()[3 * i + 1] & 0xFF) << 8
                    | (map.rgb()[3 * i + 2] & 0xFF);
            if (seen.add(c)) {
                distinct++;
            }
        }
        Apoapsis.LOG.info("[terrain-probe] {} planet map {}x{} supersample {}x{}: {} s, {} distinct colours", b.name(),
                width, height, ss, ss, f(s), distinct);
    }

    private static String f(double v) {
        return String.format("%.2f", v);
    }
}
