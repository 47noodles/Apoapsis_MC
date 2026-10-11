package dev.apoapsis.planet;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.TerrainTilePayload;
import dev.apoapsis.ship.ShipEntity;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

/**
 * Server side of the far terrain. A pilot's client asks for the tile of one level of detail around its sub-ship point
 * ({@link TerrainTilePayload.Request}); the tile is generated off the server thread by {@link TerrainTileGen} and
 * sent back as a {@link TerrainTilePayload}. The flight state (and so the sub-ship point of a ship in orbit) is not
 * readable from here, which is why the client, which already tracks it, does the asking.
 *
 * <p>Who may ask: only the pilot of a ship, or a player in the orbit dimension ({@link #mayRequest}); a request from
 * anyone else is dropped without a reply or a log line.
 *
 * <p>Limits: at most one request per level and player is kept (a newer one replaces an older one still waiting); a
 * player may send one request per level every {@link #MIN_GAP_TICKS} ticks; at most {@link #MAX_QUEUED} jobs wait in
 * all, finest level first; finished tiles are kept in a small cache so a repeated request costs nothing; the request's
 * centre is snapped to the level's grid and must lie within the body's map. Never blocks the server thread.
 */
@EventBusSubscriber(modid = Apoapsis.MODID)
public final class TerrainTiles {
    private static final int POOL_THREADS = 2;
    /** Smallest gap between two accepted requests of one player and level, server ticks. */
    private static final int MIN_GAP_TICKS = 5;
    /** Most jobs waiting for a worker; a request beyond it is dropped. */
    private static final int MAX_QUEUED = 12;
    /** Finished tiles kept for repeat requests. */
    private static final int CACHE_TILES = 16;

    /** Server thread only. Non-null while a server runs. */
    private static @Nullable MinecraftServer server;
    private static @Nullable ThreadPoolExecutor pool;
    /** Per body: the level and its generator handles. Server thread only. */
    private static final Map<BodyId, Ctx> CONTEXTS = new EnumMap<>(BodyId.class);
    /** Per player: the newest request of each level, and when each last got through. Server thread only. */
    private static final Map<UUID, PlayerState> PLAYERS = new java.util.HashMap<>();
    /** Finished tiles, least recently used first. Server thread only. */
    private static final Map<Key, TerrainTilePayload> CACHE = new LinkedHashMap<>(32, 0.75f, true);
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private TerrainTiles() {}

    /** One body's surface level with the handles a worker needs. */
    private record Ctx(ServerLevel level, TerrainSampler sampler, ChunkGenerator generator, RandomState randomState) {}

    private record Key(String body, int level, int cx, int cz) {}

    private static final class PlayerState {
        final Job[] latest = new Job[TerrainTilePayload.LEVELS];
        final long[] lastTick = {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2};
    }

    /** A tile to generate for a player. Ordered finest level first, then oldest first. */
    private static final class Job implements Runnable, Comparable<Job> {
        final UUID player;
        final Ctx ctx;
        final Key key;
        final long sequence = SEQUENCE.incrementAndGet();
        volatile boolean superseded;

        Job(UUID player, Ctx ctx, Key key) {
            this.player = player;
            this.ctx = ctx;
            this.key = key;
        }

        @Override
        public int compareTo(Job other) {
            int c = Integer.compare(key.level(), other.key.level());
            return c != 0 ? c : Long.compare(sequence, other.sequence);
        }

        @Override
        public void run() {
            if (superseded) {
                return;
            }
            try {
                long t0 = System.nanoTime();
                TerrainTilePayload tile = TerrainTileGen.generate(ctx.sampler(), ctx.generator(), ctx.randomState(),
                        key.body(), key.level(), key.cx(), key.cz());
                if (tile == null) {
                    return;
                }
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                MinecraftServer s = server;
                if (s != null) {
                    s.execute(() -> deliver(this, tile, ms));
                }
            } catch (RuntimeException e) {
                Apoapsis.LOG.error("Terrain tile {} failed", key, e);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ lifecycle

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        AtomicInteger count = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "apoapsis-terrain-tile-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        pool = new ThreadPoolExecutor(POOL_THREADS, POOL_THREADS, 0, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(), factory);
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        ThreadPoolExecutor exec = pool;
        pool = null;
        server = null;
        if (exec != null) {
            exec.shutdownNow();
        }
        CONTEXTS.clear();
        PLAYERS.clear();
        CACHE.clear();
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerState state = PLAYERS.remove(event.getEntity().getUUID());
        if (state != null) {
            for (Job job : state.latest) {
                if (job != null) {
                    job.superseded = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------ requests

    /** Payload entry point; runs on the server thread. Validates, rate-limits, then queues or
     * answers from the cache. */
    public static void handleRequest(TerrainTilePayload.Request request, IPayloadContext context) {
        MinecraftServer s = server;
        ThreadPoolExecutor exec = pool;
        if (s == null || exec == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!mayRequest(player)) {
            return; // a stranger to the far terrain: nothing is generated, queued or sent, and nothing is logged
        }
        int level = request.level();
        BodyId body = bodyOf(request.body());
        if (body == null || level < 0 || level >= TerrainTilePayload.LEVELS
                || Math.abs(request.centerX()) > TerrainTilePayload.MAX_COORD
                || Math.abs(request.centerZ()) > TerrainTilePayload.MAX_COORD
                || !bodyMatchesDimension(body, player.level().dimension())) {
            return;
        }
        PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), k -> new PlayerState());
        long now = s.getTickCount();
        if (now - state.lastTick[level] < MIN_GAP_TICKS) {
            return;
        }
        state.lastTick[level] = now;
        Ctx ctx = context(s, body);
        if (ctx == null) {
            return;
        }
        Key key = new Key(body.id, level, TerrainTileGen.snap(level, request.centerX()),
                TerrainTileGen.snap(level, request.centerZ()));
        TerrainTilePayload cached = CACHE.get(key);
        if (cached != null) {
            send(player, cached);
            return;
        }
        Job old = state.latest[level];
        if (old != null && old.key.equals(key)) {
            return; // already queued or running
        }
        if (exec.getQueue().size() >= MAX_QUEUED) {
            return;
        }
        if (old != null) {
            old.superseded = true;
        }
        Job job = new Job(player.getUUID(), ctx, key);
        state.latest[level] = job;
        exec.execute(job);
    }

    /**
     * Whether a player may ask for far terrain: the pilot of a ship, or anyone in the orbit dimension (the orbit regime
     * looks down on the body). Every other client gets nothing, however politely it asks: the far terrain is for flight,
     * and a tile costs the server a few hundred milliseconds of work.
     */
    public static boolean mayRequest(Player player) {
        return mayRequest(FlightDimensions.ORBIT.equals(player.level().dimension()), player);
    }

    /** As {@link #mayRequest(Player)}, with the regime given: {@code inOrbit} is true in the orbit dimension. */
    public static boolean mayRequest(boolean inOrbit, Player player) {
        return inOrbit || player.getVehicle() instanceof ShipEntity ship && ship.getPilot() == player;
    }

    /** True when the player has got a request past the gate and the rate limit since the server started. */
    public static boolean hasRequested(UUID player) {
        return PLAYERS.containsKey(player);
    }

    private static @Nullable BodyId bodyOf(String id) {
        for (BodyId b : BodyId.values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    /** A player in a surface dimension may ask only for that body; in the orbit dimension, for either. */
    private static boolean bodyMatchesDimension(BodyId body, ResourceKey<Level> dimension) {
        if (FlightDimensions.ORBIT.equals(dimension)) {
            return true;
        }
        return (Level.OVERWORLD.equals(dimension) || FlightDimensions.PALE.equals(dimension))
                && FlightDimensions.surfaceBody(dimension) == body;
    }

    private static @Nullable Ctx context(MinecraftServer s, BodyId body) {
        Ctx ctx = CONTEXTS.get(body);
        if (ctx == null) {
            ServerLevel level = s.getLevel(FlightDimensions.surfaceDimension(body));
            if (level == null) {
                return null;
            }
            ctx = new Ctx(level, TerrainSampler.of(level), level.getChunkSource().getGenerator(),
                    level.getChunkSource().randomState());
            CONTEXTS.put(body, ctx);
        }
        return ctx;
    }

    /** Server thread: remember the finished tile and send it unless a newer request replaced this job. */
    private static void deliver(Job job, TerrainTilePayload tile, long ms) {
        MinecraftServer s = server;
        if (s == null) {
            return;
        }
        Apoapsis.LOG.debug("Terrain tile {} level {} ({}x{}) at {},{} generated in {} ms{}", tile.body(), tile.level(),
                tile.size(), tile.size(), tile.centerX(), tile.centerZ(), ms, job.superseded ? " (superseded)" : "");
        CACHE.put(job.key, tile);
        while (CACHE.size() > CACHE_TILES) {
            CACHE.remove(CACHE.keySet().iterator().next());
        }
        ServerPlayer player = s.getPlayerList().getPlayer(job.player);
        PlayerState state = PLAYERS.get(job.player);
        if (state != null && state.latest[tile.level()] == job) {
            state.latest[tile.level()] = null;
        }
        if (player != null && !job.superseded) {
            send(player, tile);
        }
    }

    private static void send(ServerPlayer player, TerrainTilePayload tile) {
        if (player.connection != null && player.connection.hasChannel(TerrainTilePayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, tile);
        }
    }

    // ------------------------------------------------------------------------------------------ debug command

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("apoapsis")
                .then(Commands.literal("terrain")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("check")
                                .executes(c -> check(c, 12))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(c -> check(c, IntegerArgumentType.getInteger(c, "count")))))));
    }

    /**
     * Debug: builds the three tiles around the player on this thread, then compares {@code count} random vertices of
     * each with the height of the real chunk there (the generated chunk's ocean-floor heightmap, which loads the chunk;
     * the world-gen one on Verdance, which leaves the trees out, the final one on Pale, which has the craters in).
     * Logs per level the generation time and the error in blocks, and tells the caller.
     */
    private static int check(CommandContext<CommandSourceStack> c, int count) {
        CommandSourceStack source = c.getSource();
        ServerLevel level = source.getLevel();
        BodyId body = FlightDimensions.surfaceBody(level.dimension());
        if (!Level.OVERWORLD.equals(level.dimension()) && !FlightDimensions.PALE.equals(level.dimension())) {
            source.sendFailure(Component.literal("Stand in the overworld or on Pale."));
            return 0;
        }
        TerrainSampler sampler = TerrainSampler.of(level);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int px = (int) Math.floor(source.getPosition().x);
        int pz = (int) Math.floor(source.getPosition().z);
        Heightmap.Types realHeights = body == BodyId.PALE ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG;
        java.util.Random rnd = new java.util.Random(7);
        for (int lv = 0; lv < TerrainTilePayload.LEVELS; lv++) {
            long t0 = System.nanoTime();
            TerrainTilePayload tile = TerrainTileGen.generate(sampler, generator, randomState, body.id, lv,
                    TerrainTileGen.snap(lv, px), TerrainTileGen.snap(lv, pz));
            double ms = (System.nanoTime() - t0) / 1e6;
            if (tile == null) {
                return 0;
            }
            int n = tile.size();
            double sum = 0, sumSq = 0, max = 0;
            int within3 = 0;
            for (int s = 0; s < count; s++) {
                int i = rnd.nextInt(n), j = rnd.nextInt(n);
                int x = tile.centerX() + (i - (n - 1) / 2) * tile.spacing();
                int z = tile.centerZ() + (j - (n - 1) / 2) * tile.spacing();
                ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
                int real = chunk.getHeight(realHeights, x & 15, z & 15);
                double err = tile.heights()[j * n + i] - real;
                sum += err;
                sumSq += err * err;
                max = Math.max(max, Math.abs(err));
                if (Math.abs(err) <= 3) {
                    within3++;
                }
            }
            String line = String.format(
                    "[terrain-check] %s level %d (%dx%d, spacing %d): generated in %.0f ms; vs real chunk"
                    + " heights over %d vertices: bias %.2f rms %.2f max %.0f blocks, %d%% within 3 blocks",
                    body.id, lv, n, n, tile.spacing(), ms, count, sum / count, Math.sqrt(sumSq / count), max,
                    100 * within3 / count);
            Apoapsis.LOG.info(line);
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
