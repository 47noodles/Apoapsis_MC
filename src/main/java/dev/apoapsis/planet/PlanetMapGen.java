package dev.apoapsis.planet;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.PlanetMapPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Renders a body's surface as a flat-coloured map. Each texel averages {@link #SUPERSAMPLE} x {@link #SUPERSAMPLE}
 * samples spread across it (the ground height and the biome there, coloured as {@link SurfaceColors}), so continents,
 * coasts and biome patches show instead of one scattered point per texel; then a smooth hillshade from the height
 * difference to the north-west. Heights come from {@link TerrainSampler}, a short vertical search that costs about
 * a fiftieth of {@code getBaseHeight} on Verdance. Deterministic for a seed. Runs off the server thread, rows spread
 * over a small pool of its own; it only reads the generator.
 */
public final class PlanetMapGen {
    /** Samples per texel along each axis. */
    static final int SUPERSAMPLE = 3;
    /** Hillshade brightness change per block of height difference, capped at 15 percent either way. */
    private static final double SHADE_PER_BLOCK = 0.04;
    private static final double SHADE_CAP = 0.15;
    private static final int MAX_THREADS = 6;

    private PlanetMapGen() {}

    /** Generates the map, or returns null if the calling thread was interrupted. */
    static PlanetMapPayload generate(ServerLevel level, String body, double radius, int width, int height) {
        return generate(TerrainSampler.of(level), level.getChunkSource().getGenerator(),
                level.getChunkSource().randomState(), body, radius, width, height, SUPERSAMPLE);
    }

    /** As above, from a sampler and generator directly; {@code ss} samples per texel along each axis. */
    public static PlanetMapPayload generate(TerrainSampler sampler, ChunkGenerator generator, RandomState randomState,
            String body, double radius, int width, int height, int ss) {
        boolean pale = body.equals(BodyId.PALE.id);
        int n = width * height;
        int[] ground = new int[n];
        int[] colors = new int[n];

        int threads = Math.max(1, Math.min(MAX_THREADS, Runtime.getRuntime().availableProcessors() - 1));
        AtomicInteger count = new AtomicInteger();
        ExecutorService rows = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "apoapsis-planet-map-row-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        AtomicBoolean cancelled = new AtomicBoolean();
        try {
            List<Future<?>> futures = new ArrayList<>(height);
            for (int v = 0; v < height; v++) {
                int row = v;
                futures.add(rows.submit(() -> {
                    if (!cancelled.get()) {
                        sampleRow(sampler, generator, randomState, pale, radius, width, height, ss, row,
                                ground, colors);
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            cancelled.set(true);
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            cancelled.set(true);
            throw new IllegalStateException("planet map row failed", e.getCause());
        } finally {
            rows.shutdownNow();
        }

        // Relief: the ground height averaged over 3x3 texels (longitude wraps, latitude clamps), then the height
        // difference to the north-west texel.
        int[] smooth = new int[n];
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                int sum = 0;
                for (int dv = -1; dv <= 1; dv++) {
                    int vv = SurfaceColors.clamp(v + dv, 0, height - 1);
                    for (int du = -1; du <= 1; du++) {
                        sum += ground[vv * width + Math.floorMod(u + du, width)];
                    }
                }
                smooth[v * width + u] = sum / 9;
            }
        }

        byte[] rgb = new byte[n * 3];
        for (int v = 0; v < height; v++) {
            int vn = SurfaceColors.clamp(v - 1, 0, height - 1);
            for (int u = 0; u < width; u++) {
                int i = v * width + u;
                int nw = vn * width + Math.floorMod(u - 1, width);
                double shade = SurfaceColors.clamp((smooth[i] - smooth[nw]) * SHADE_PER_BLOCK, -SHADE_CAP, SHADE_CAP);
                int argb = SurfaceColors.shade(colors[i], 1 + shade);
                rgb[3 * i] = (byte) (argb >> 16);
                rgb[3 * i + 1] = (byte) (argb >> 8);
                rgb[3 * i + 2] = (byte) argb;
            }
        }
        return new PlanetMapPayload(body, width, height, rgb);
    }

    /** One row of texels: average colour and height over the supersamples of each. */
    private static void sampleRow(TerrainSampler sampler, ChunkGenerator generator, RandomState randomState,
            boolean pale, double radius, int width, int height, int ss, int v, int[] ground,
            int[] colors) {
        SurfaceColumns columns = SurfaceColumns.create(sampler, generator, randomState, pale, false);
        int samples = ss * ss;
        for (int u = 0; u < width; u++) {
            long r = 0, g = 0, b = 0;
            double hSum = 0;
            for (int sv = 0; sv < ss; sv++) {
                double lat = Math.PI / 2 - Math.PI * (v + (sv + 0.5) / ss) / height;
                int z = (int) Math.round(-lat * radius);
                for (int su = 0; su < ss; su++) {
                    double lon = -Math.PI + 2 * Math.PI * (u + (su + 0.5) / ss) / width;
                    int x = (int) Math.round(lon * radius);
                    columns.at(x, z);
                    // Open water is flat for the relief shading: the sea floor under it must not show through as bumps.
                    int h = columns.kind() == SurfaceKind.WATER
                            ? Math.max(columns.height(), sampler.seaLevel()) : columns.height();
                    int c = columns.colour();
                    r += (c >> 16) & 0xFF;
                    g += (c >> 8) & 0xFF;
                    b += c & 0xFF;
                    hSum += h;
                }
            }
            int i = v * width + u;
            ground[i] = (int) Math.round(hSum / samples);
            colors[i] = (int) (r / samples) << 16 | (int) (g / samples) << 8 | (int) (b / samples);
        }
    }
}
