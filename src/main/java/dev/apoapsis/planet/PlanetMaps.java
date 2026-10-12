package dev.apoapsis.planet;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.PlanetMapPayload;
import dev.apoapsis.net.PlanetMapRequest;
import dev.apoapsis.net.PlanetMapTilePayload;
import dev.apoapsis.planet.map.MapLevels;
import dev.apoapsis.planet.map.PlanetMapStore;
import dev.apoapsis.planet.map.PlanetMapTileGen;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

/**
 * Server side of the planet surface maps. Each body's coarse map (level 0) is loaded from the world save or generated
 * off the server thread, kept, cached, and sent to clients as a {@link PlanetMapPayload} on login. The finer levels are
 * then refined in the background, one level at a time on a low-priority thread with its own tile pool, cached per
 * level, and streamed as {@link PlanetMapTilePayload}s to the players who ask for them. Never blocks the server thread.
 */
public final class PlanetMaps {
    /** Cache format version; bump it when the colouring or terrain changes (6: flat water in the relief shading). */
    static final int CACHE_VERSION = 6;
    private static final int POOL_THREADS = 2;
    /** Tile generators for the refined levels: a few, and fewer than half the cores. */
    private static final int LEVEL_THREADS = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 2 - 1));
    /** Refined tiles sent in total per server tick, from the head stream of each player. */
    private static final int TILES_PER_TICK = 2;
    /** Server ticks timed after the last level is ready, as the idle baseline. */
    private static final int IDLE_TICKS = 200;
    private static final List<Spec> SPECS = List.of(
            new Spec(BodyId.VERDANCE, 512, 256),
            new Spec(BodyId.PALE, 512, 256));

    /** Server thread only. Non-null while a server runs. */
    private static @Nullable MinecraftServer server;
    /** Server thread only. Non-null while a server runs. */
    private static @Nullable ExecutorService pool;
    /** Server thread only. Non-null while a server runs. */
    private static @Nullable ExecutorService tilePool;
    /** Server thread only. The refiner thread while a server runs. */
    private static @Nullable Thread refiner;
    /** Server thread only. */
    private static final Map<BodyId, PlanetMapPayload> READY = new EnumMap<>(BodyId.class);
    /** Server thread only: the generation inputs of each body, built in onServerStarted. */
    private static final Map<BodyId, Ctx> CONTEXTS = new EnumMap<>(BodyId.class);
    /** Server thread only: the refined levels of each body, indexed by level; level 0 is not kept here. */
    private static final Map<BodyId, PlanetMapStore.Stored[]> LEVELS = new EnumMap<>(BodyId.class);
    /** Server thread only: the streams under way, per player. */
    private static final Map<UUID, ArrayDeque<Stream>> STREAMS = new HashMap<>();
    /** Server thread only: the levels each player asked for before they were ready, as "body:level". */
    private static final Map<UUID, Set<String>> PENDING = new HashMap<>();
    /** Server thread only: the levels each player has been sent or is streaming, as "body:level". */
    private static final Map<UUID, Set<String>> SENT = new HashMap<>();
    /** True from the moment the refiner starts generating until the last level is ready. */
    private static volatile boolean refining;
    /** Server thread only: the start of the tick being timed, or 0 when it is not timed. */
    private static long tickStart;
    /** Server thread only: the timed ticks since the last reset. */
    private static int tickCount;
    private static long tickSum;
    private static long tickMax;
    /** Server thread only: the ticks still to time for the idle baseline. */
    private static int idleLeft;

    private PlanetMaps() {}

    /** One body's map: which body, and its size in texels. */
    private record Spec(BodyId body, int width, int height) {}

    /** What the workers need to generate one body's levels: the level, the terrain sampler and generator, the radius. */
    private record Ctx(BodyId body, ServerLevel level, TerrainSampler sampler, ChunkGenerator generator,
            RandomState randomState, boolean pale, double radius) {}

    /** One level being streamed to one player; {@code next} is the index of the next tile to send. */
    private static final class Stream {
        final BodyId body;
        final MapLevels.Level level;
        final PlanetMapStore.Stored stored;
        int next;

        Stream(BodyId body, MapLevels.Level level, PlanetMapStore.Stored stored) {
            this.body = body;
            this.level = level;
            this.stored = stored;
        }
    }

    /** Registers the lifecycle listeners. Call once from the mod constructor. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onServerStarted);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onServerStopping);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onServerTickPre);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onServerTickPost);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer s = event.getServer();
        server = s;
        ExecutorService exec = Executors.newFixedThreadPool(POOL_THREADS, daemonThreads());
        pool = exec;
        ExecutorService tiles = Executors.newFixedThreadPool(LEVEL_THREADS, lowPriorityThreads("apoapsis-planet-map-tile-"));
        tilePool = tiles;
        CountDownLatch level0Done = new CountDownLatch(SPECS.size());
        List<Ctx> bodies = new ArrayList<>();
        for (Spec spec : SPECS) {
            String name = spec.body().id;
            ServerLevel level = s.getLevel(FlightDimensions.surfaceDimension(spec.body()));
            if (level == null) {
                Apoapsis.LOG.info("Planet map {} skipped: its dimension is not loaded", name);
                level0Done.countDown();
                continue;
            }
            Ctx ctx = new Ctx(spec.body(), level, TerrainSampler.of(level), level.getChunkSource().getGenerator(),
                    level.getChunkSource().randomState(), spec.body() == BodyId.PALE,
                    FlightDimensions.system(spec.body()).radius);
            CONTEXTS.put(spec.body(), ctx);
            bodies.add(ctx);
            Path file = s.getWorldPath(LevelResource.ROOT).resolve("data/apoapsis/planet_map_" + name + ".bin");
            exec.execute(() -> build(s, spec, level, file, level0Done));
        }
        Path root = s.getWorldPath(LevelResource.ROOT);
        List<Ctx> ordered = List.copyOf(bodies);
        Thread t = new Thread(() -> refineAll(s, ordered, root, level0Done, tiles), "apoapsis-planet-map-refine");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        refiner = t;
        t.start();
    }

    /** Worker thread: load the cached map, or generate it. Hands the result back to the server thread. */
    private static void build(MinecraftServer s, Spec spec, ServerLevel level, Path file, CountDownLatch level0Done) {
        try {
            String name = spec.body().id;
            long t0 = System.nanoTime();
            PlanetMapPayload cached = readCache(file, spec);
            PlanetMapPayload map = cached;
            if (map == null) {
                map = PlanetMapGen.generate(level, name, FlightDimensions.system(spec.body()).radius,
                        spec.width(), spec.height());
                if (map == null) {
                    return;
                }
            }
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            PlanetMapPayload done = map;
            boolean fromCache = cached != null;
            s.execute(() -> finish(s, spec, done, file, fromCache, ms));
        } catch (RuntimeException e) {
            Apoapsis.LOG.error("Planet map {} failed", spec.body().id, e);
        } finally {
            level0Done.countDown();
        }
    }

    /** Server thread: keep the map, write the cache if it was generated, and send it to everyone. */
    private static void finish(MinecraftServer s, Spec spec, PlanetMapPayload map, Path file, boolean fromCache,
            long ms) {
        if (server != s) {
            return;
        }
        String name = spec.body().id;
        READY.put(spec.body(), map);
        if (fromCache) {
            Apoapsis.LOG.info("Planet map {} loaded from the cache ({}x{})", name, map.width(), map.height());
        } else {
            Apoapsis.LOG.info("Planet map {} generated in {} ms ({}x{})", name, ms, map.width(), map.height());
            writeCache(file, map);
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            send(p, map);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (server == null || !(player instanceof ServerPlayer p)) {
            return;
        }
        for (PlanetMapPayload map : READY.values()) {
            send(p, map);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        STREAMS.remove(id);
        PENDING.remove(id);
        SENT.remove(id);
    }

    private static void send(ServerPlayer player, PlanetMapPayload map) {
        if (player.connection != null && player.connection.hasChannel(PlanetMapPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, map);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ExecutorService exec = pool;
        pool = null;
        ExecutorService tiles = tilePool;
        tilePool = null;
        Thread r = refiner;
        refiner = null;
        server = null;
        refining = false;
        if (exec != null) {
            exec.shutdownNow();
        }
        if (tiles != null) {
            tiles.shutdownNow();
        }
        if (r != null) {
            r.interrupt();
        }
        READY.clear();
        CONTEXTS.clear();
        LEVELS.clear();
        STREAMS.clear();
        PENDING.clear();
        SENT.clear();
        tickStart = 0L;
        idleLeft = 0;
        resetTicks();
    }

    // ------------------------------------------------------------------------------------------ refinement

    /** Refiner thread: waits for both level-0 builds, then refines each level of each body, coarse levels first. */
    private static void refineAll(MinecraftServer s, List<Ctx> bodies, Path root, CountDownLatch level0Done,
            ExecutorService tiles) {
        try {
            level0Done.await();
            int max = 0;
            for (Ctx ctx : bodies) {
                max = Math.max(max, MapLevels.maxLevel(ctx.body()));
            }
            if (max == 0) {
                return;
            }
            refining = true;
            for (int lv = 1; lv <= max; lv++) {
                for (Ctx ctx : bodies) {
                    if (lv <= MapLevels.maxLevel(ctx.body())
                            && !refine(s, ctx, MapLevels.of(ctx.body()).get(lv), root, tiles)) {
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            Apoapsis.LOG.error("Planet map refinement failed", e);
        }
    }

    /** Refiner thread: one level of one body, from the cache or generated. False when stopped or failed. */
    private static boolean refine(MinecraftServer s, Ctx ctx, MapLevels.Level level, Path root, ExecutorService tiles) {
        String name = ctx.body().id;
        Path file = root.resolve("data/apoapsis/planet_map_" + name + "_L" + level.index() + "_s" + level.supersample() + ".bin");
        try {
            long t0 = System.nanoTime();
            PlanetMapStore.Stored cached = PlanetMapStore.read(file, level);
            PlanetMapStore.Stored stored = cached;
            if (stored == null) {
                stored = generate(ctx, level, tiles);
                if (stored == null) {
                    return false;
                }
                PlanetMapStore.write(file, stored);
            }
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            PlanetMapStore.Stored done = stored;
            boolean fromCache = cached != null;
            s.execute(() -> levelReady(s, ctx.body(), done, fromCache, ms));
            return true;
        } catch (RuntimeException e) {
            Apoapsis.LOG.error("Planet map {} level {} failed", name, level.index(), e);
            return false;
        }
    }

    /** Generates every tile of a level on the tile pool, in index order. Null if interrupted or a tile came back empty. */
    private static PlanetMapStore.@Nullable Stored generate(Ctx ctx, MapLevels.Level level, ExecutorService tiles) {
        int n = level.tileCount();
        List<Future<byte[]>> jobs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int tx = i % level.tilesX();
            int ty = i / level.tilesX();
            jobs.add(tiles.submit(() -> {
                byte[] raw = PlanetMapTileGen.generate(ctx.sampler(), ctx.generator(), ctx.randomState(), ctx.pale(),
                        ctx.radius(), level, tx, ty);
                return raw == null ? null : PlanetMapStore.deflate(raw);
            }));
        }
        List<byte[]> out = new ArrayList<>(n);
        try {
            for (Future<byte[]> job : jobs) {
                byte[] deflated = job.get();
                if (deflated == null) {
                    tiles.shutdownNow();
                    return null;
                }
                out.add(deflated);
            }
        } catch (InterruptedException e) {
            tiles.shutdownNow();
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            tiles.shutdownNow();
            throw new IllegalStateException("Planet map tile failed", e.getCause());
        }
        return new PlanetMapStore.Stored(level.index(), level.width(), level.height(), out);
    }

    /** Server thread: keep a refined level, log it, start the streams waiting for it, and time the idle baseline after the last. */
    private static void levelReady(MinecraftServer s, BodyId body, PlanetMapStore.Stored stored, boolean fromCache,
            long ms) {
        if (server != s) {
            return;
        }
        int lv = stored.level();
        PlanetMapStore.Stored[] levels = LEVELS.computeIfAbsent(body,
                b -> new PlanetMapStore.Stored[MapLevels.maxLevel(b) + 1]);
        levels[lv] = stored;
        long deflated = 0L;
        for (byte[] tile : stored.tiles()) {
            deflated += tile.length;
        }
        Apoapsis.LOG.info("Planet map {} level {} {} in {} ms ({}x{}, {} tiles, {} KiB deflated)", body.id, lv,
                fromCache ? "loaded from the cache" : "generated", ms, stored.width(), stored.height(),
                stored.tiles().size(), deflated / 1024L);
        if (!fromCache) {
            Apoapsis.LOG.info("[planet map] server ticks while {} level {} was generated: {} ticks, mean {} ms, max {} ms",
                    body.id, lv, tickCount, ms(tickCount == 0 ? 0L : tickSum / tickCount), ms(tickMax));
        }
        resetTicks();

        String key = key(body, lv);
        MapLevels.Level level = MapLevels.of(body).get(lv);
        for (Iterator<Map.Entry<UUID, Set<String>>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Set<String>> entry = it.next();
            if (entry.getValue().remove(key)) {
                startStream(entry.getKey(), key, new Stream(body, level, stored));
            }
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }

        if (allLevelsReady()) {
            refining = false;
            idleLeft = IDLE_TICKS;
        }
    }

    /** Whether every refined level of every body that has a context is ready. Server thread only. */
    private static boolean allLevelsReady() {
        for (BodyId body : CONTEXTS.keySet()) {
            PlanetMapStore.Stored[] levels = LEVELS.get(body);
            if (levels == null) {
                return false;
            }
            for (int lv = 1; lv < levels.length; lv++) {
                if (levels[lv] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------ streaming

    /**
     * Payload entry point, server thread. A player who may fly to the far terrain (or is in the Pale dimension) asks for
     * one refined level of a body: it is streamed now if ready, or as soon as it is. Each level is sent once per player.
     */
    public static void handleRequest(PlanetMapRequest request, IPayloadContext context) {
        MinecraftServer s = server;
        if (s == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BodyId body = bodyOf(request.body());
        int lv = request.level();
        if (body == null || lv < 1 || lv > MapLevels.maxLevel(body) || !mayRequestMap(player)) {
            return;
        }
        UUID id = player.getUUID();
        String key = key(body, lv);
        if (SENT.getOrDefault(id, Set.of()).contains(key) || PENDING.getOrDefault(id, Set.of()).contains(key)) {
            return;
        }
        PlanetMapStore.Stored[] levels = LEVELS.get(body);
        PlanetMapStore.Stored ready = levels == null ? null : levels[lv];
        if (ready != null) {
            startStream(id, key, new Stream(body, MapLevels.of(body).get(lv), ready));
        } else {
            PENDING.computeIfAbsent(id, k -> new HashSet<>()).add(key);
        }
    }

    private static void startStream(UUID id, String key, Stream stream) {
        SENT.computeIfAbsent(id, k -> new HashSet<>()).add(key);
        STREAMS.computeIfAbsent(id, k -> new ArrayDeque<>()).add(stream);
    }

    /** Server thread, end of tick: sends up to {@link #TILES_PER_TICK} tiles of each player's head stream. */
    private static void drainStreams(MinecraftServer s) {
        for (Iterator<Map.Entry<UUID, ArrayDeque<Stream>>> it = STREAMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, ArrayDeque<Stream>> entry = it.next();
            ServerPlayer player = s.getPlayerList().getPlayer(entry.getKey());
            ArrayDeque<Stream> queue = entry.getValue();
            if (player == null || queue.isEmpty()) {
                it.remove();
                continue;
            }
            if (player.connection == null || !player.connection.hasChannel(PlanetMapTilePayload.TYPE)) {
                continue;
            }
            for (int budget = TILES_PER_TICK; budget > 0 && !queue.isEmpty(); budget--) {
                Stream st = queue.peek();
                int index = st.next++;
                int tx = index % st.level.tilesX();
                int ty = index / st.level.tilesX();
                PacketDistributor.sendToPlayer(player, new PlanetMapTilePayload(st.body.id, st.level.index(),
                        st.stored.width(), st.stored.height(), tx, ty, PlanetMapTilePayload.RAW_LENGTH,
                        st.stored.tiles().get(index)));
                if (st.next >= st.stored.tiles().size()) {
                    queue.poll();
                    break;
                }
            }
            if (queue.isEmpty()) {
                it.remove();
            }
        }
    }

    /** Whether a player may ask for refined maps: the same players as the far terrain, or anyone in the Pale dimension. */
    public static boolean mayRequestMap(Player player) {
        return TerrainTiles.mayRequest(player) || player.level().dimension().equals(FlightDimensions.PALE);
    }

    // ------------------------------------------------------------------------------------------ tick timing

    private static void onServerTickPre(ServerTickEvent.Pre event) {
        tickStart = refining || idleLeft > 0 ? System.nanoTime() : 0L;
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        if (tickStart != 0L) {
            long took = System.nanoTime() - tickStart;
            tickStart = 0L;
            tickCount++;
            tickSum += took;
            tickMax = Math.max(tickMax, took);
            if (idleLeft > 0 && --idleLeft == 0) {
                Apoapsis.LOG.info("[planet map] server ticks after generation: {} ticks, mean {} ms, max {} ms",
                        tickCount, ms(tickSum / tickCount), ms(tickMax));
                resetTicks();
            }
        }
        MinecraftServer s = server;
        if (s != null) {
            drainStreams(s);
        }
    }

    private static void resetTicks() {
        tickCount = 0;
        tickSum = 0L;
        tickMax = 0L;
    }

    /** Nanoseconds as milliseconds to one decimal place. */
    private static double ms(long nanos) {
        return Math.round(nanos / 1e5) / 10.0;
    }

    // ------------------------------------------------------------------------------------------ helpers

    private static String key(BodyId body, int level) {
        return body.id + ":" + level;
    }

    private static @Nullable BodyId bodyOf(String id) {
        for (BodyId b : BodyId.values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    /** The cached map if the file exists, has the current version and size, and is complete; otherwise null. */
    private static @Nullable PlanetMapPayload readCache(Path file, Spec spec) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != CACHE_VERSION || in.readInt() != spec.width() || in.readInt() != spec.height()) {
                return null;
            }
            byte[] rgb = new byte[spec.width() * spec.height() * 3];
            in.readFully(rgb);
            return new PlanetMapPayload(spec.body().id, spec.width(), spec.height(), rgb);
        } catch (IOException e) {
            Apoapsis.LOG.warn("Planet map cache {} unreadable, regenerating", file, e);
            return null;
        }
    }

    /** Writes the map cache: version, width, height, then the RGB bytes. Written to a temp file, then moved. */
    private static void writeCache(Path file, PlanetMapPayload map) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(CACHE_VERSION);
                out.writeInt(map.width());
                out.writeInt(map.height());
                out.write(map.rgb());
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Apoapsis.LOG.warn("Could not write planet map cache {}", file, e);
        }
    }

    private static ThreadFactory daemonThreads() {
        AtomicInteger count = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "apoapsis-planet-map-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    /** Daemon threads at the lowest priority, named {@code prefix} and a number. */
    private static ThreadFactory lowPriorityThreads(String prefix) {
        AtomicInteger count = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + count.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        };
    }
}
