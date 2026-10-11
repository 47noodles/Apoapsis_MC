package dev.apoapsis.planet;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.PlanetMapPayload;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Server side of the planet surface maps. Each body's map is loaded from the world save or generated off the server
 * thread, kept, cached, and sent to clients as a {@link PlanetMapPayload}. Never blocks the server thread.
 */
public final class PlanetMaps {
    /** Cache format version; bump it when the colouring or a body's terrain changes (3: biome colours, smooth relief). */
    static final int CACHE_VERSION = 3;
    private static final int POOL_THREADS = 2;
    private static final List<Spec> SPECS = List.of(
            new Spec(BodyId.VERDANCE, 512, 256),
            new Spec(BodyId.PALE, 256, 128));

    /** Server thread only. Non-null while a server runs. */
    private static @Nullable MinecraftServer server;
    /** Server thread only. Non-null while a server runs. */
    private static @Nullable ExecutorService pool;
    /** Server thread only. */
    private static final Map<BodyId, PlanetMapPayload> READY = new EnumMap<>(BodyId.class);

    private PlanetMaps() {}

    /** One body's map: which body, and its size in texels. */
    private record Spec(BodyId body, int width, int height) {}

    /** Registers the lifecycle listeners. Call once from the mod constructor. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onServerStarted);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PlanetMaps::onServerStopping);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer s = event.getServer();
        server = s;
        ExecutorService exec = Executors.newFixedThreadPool(POOL_THREADS, daemonThreads());
        pool = exec;
        for (Spec spec : SPECS) {
            String name = spec.body().id;
            ServerLevel level = s.getLevel(FlightDimensions.surfaceDimension(spec.body()));
            if (level == null) {
                Apoapsis.LOG.info("Planet map {} skipped: its dimension is not loaded", name);
                continue;
            }
            Path file = s.getWorldPath(LevelResource.ROOT).resolve("data/apoapsis/planet_map_" + name + ".bin");
            exec.execute(() -> build(s, spec, level, file));
        }
    }

    /** Worker thread: load the cached map, or generate it. Hands the result back to the server thread. */
    private static void build(MinecraftServer s, Spec spec, ServerLevel level, Path file) {
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

    private static void send(ServerPlayer player, PlanetMapPayload map) {
        if (player.connection != null && player.connection.hasChannel(PlanetMapPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, map);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ExecutorService exec = pool;
        pool = null;
        server = null;
        if (exec != null) {
            exec.shutdownNow();
        }
        READY.clear();
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
}
