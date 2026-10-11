package dev.apoapsis.test;

import dev.apoapsis.test.TerrainProbe.Body;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jspecify.annotations.Nullable;

/**
 * Generates real chunks of a body for the accuracy tests, through the same generator calls the server's chunk pipeline
 * makes (biomes, terrain with surface rules and carvers, then the biome decoration that holds Pale's craters), but on
 * the calling thread and outside any level: the GameTest world is a flat overworld with no datapack dimensions, so a
 * test cannot ask it for a Pale chunk. The host level is used only for its registries and its structure manager (the
 * world is built without structures, so no structure ever reaches a chunk).
 *
 * <p>With {@code features} off a chunk stops after terrain: no trees, no craters, nothing the far terrain does not
 * model; Verdance is compared that way. With it on, the biome decoration runs on the chunk too, over a 3 by 3 area of
 * chunks that have their biomes (all the decoration reads of a neighbour).
 */
final class RealChunks {
    private final ServerLevel host;
    private final Body body;
    private final boolean features;
    private final BiomeResolver uncached;
    private final BiomeManager biomeManager;
    private final ChunkStep featuresStep = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
    /** Chunks that have their biomes, and for the ones asked for, their terrain and features. */
    private final Map<Long, ProtoChunk> chunks = new HashMap<>();
    private final Map<Long, ProtoChunk> finished = new HashMap<>();

    RealChunks(ServerLevel host, Body body, boolean features) {
        this.host = host;
        this.body = body;
        this.features = features;
        this.uncached = body.generator().getBiomeSource().createUncachedResolver(body.randomState());
        this.biomeManager = new BiomeManager(uncached, BiomeManager.obfuscateSeed(body.randomState().seed()));
    }

    /** The finished chunk at ({@code cx}, {@code cz}). */
    ChunkAccess chunk(int cx, int cz) {
        long key = ChunkPos.pack(cx, cz);
        ProtoChunk done = finished.get(key);
        if (done != null) {
            return done;
        }
        int radius = features ? 1 : 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                withBiomes(cx + dx, cz + dz);
            }
        }
        ProtoChunk chunk = chunks.get(key);
        Region region = new Region(host, centred(cx, cz, 1), featuresStep, chunk, body.randomState().seed(), uncached);
        StructureManager structures = host.structureManager().forWorldGenRegion(region);
        Set<Holder<Biome>> possible = new HashSet<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                ProtoChunk n = chunks.get(ChunkPos.pack(cx + dx, cz + dz));
                if (n != null) {
                    n.collectBiomesInPalette(possible);
                }
            }
        }
        body.generator().buildTerrain(chunk, Blender.empty(), body.randomState(), structures, biomeManager, null,
                possible).join();
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
        chunk.setPersistedStatus(ChunkStatus.TERRAIN);
        if (features) {
            body.generator().applyBiomeDecoration(region, chunk, structures);
            chunk.setPersistedStatus(ChunkStatus.FEATURES);
        }
        finished.put(key, chunk);
        return chunk;
    }

    /** The chunk at (cx, cz) with its biomes filled in, created on first use. */
    private void withBiomes(int cx, int cz) {
        chunks.computeIfAbsent(ChunkPos.pack(cx, cz), k -> {
            ProtoChunk proto = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, body.heights(),
                    host.palettedContainerFactory(), null);
            body.generator().createBiomes(body.randomState(), Blender.empty(), host.structureManager(), proto).join();
            return proto;
        });
    }

    /** A 3 by 3 area of holders centred on (cx, cz), each answering with its chunk when it has one. */
    private StaticCache2D<GenerationChunkHolder> centred(int cx, int cz, int range) {
        return StaticCache2D.create(cx, cz, range, (x, z) -> new Fixed(x, z, chunks.get(ChunkPos.pack(x, z))));
    }

    /** A chunk holder that is not part of a chunk map: it answers every status with the one chunk it was given. */
    private static final class Fixed extends GenerationChunkHolder {
        private final @Nullable ChunkAccess chunk;

        Fixed(int x, int z, @Nullable ChunkAccess chunk) {
            super(new ChunkPos(x, z));
            this.chunk = chunk;
        }

        @Override
        public @Nullable ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
            return chunk;
        }

        @Override
        protected void addSaveDependency(CompletableFuture<?> sync) {}

        @Override
        public int getTicketLevel() {
            return 0;
        }

        @Override
        public int getQueueLevel() {
            return 0;
        }
    }

    /**
     * A generation region for the body: the world seed and the biome lookups of the body's own generator in place of
     * the host level's, which is the flat test world.
     */
    private static final class Region extends WorldGenRegion {
        private final long seed;
        private final BiomeResolver uncached;

        Region(ServerLevel level, StaticCache2D<GenerationChunkHolder> cache, ChunkStep step, ChunkAccess center,
                long seed, BiomeResolver uncached) {
            super(level, cache, step, center);
            this.seed = seed;
            this.uncached = uncached;
        }

        @Override
        public long getSeed() {
            return seed;
        }

        @Override
        public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
            return uncached.getNoiseBiome(quartX, quartY, quartZ);
        }
    }
}
