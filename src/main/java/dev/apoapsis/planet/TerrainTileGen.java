package dev.apoapsis.planet;

import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.TerrainTilePayload;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Builds the far-terrain tiles ({@link TerrainTilePayload}): the ground height and flat colour of an N x N grid of
 * columns around a centre, at the level's vertex spacing. Heights come from {@link TerrainSampler}, colours from
 * {@link SurfaceColors}, the same as the planet maps. Pure computation on the calling thread; it only reads the
 * generator, so it may run on any worker.
 */
public final class TerrainTileGen {
    /**
     * Centre snap of each level, blocks: a multiple of the next coarser level's vertex spacing, so a finer tile's edge
     * lies on a coarser tile's grid lines and the coarse tile can leave out exactly the cells the fine one covers.
     */
    private static final int[] SNAP = {32, 128, 512};

    private TerrainTileGen() {}

    /** The tile centre for a request near ({@code x}, {@code z}): each coordinate rounded to the level's snap. */
    public static int snap(int level, int coordinate) {
        int s = SNAP[level];
        return Math.floorDiv(coordinate + s / 2, s) * s;
    }

    /**
     * Generates the tile of {@code level} centred on ({@code cx}, {@code cz}), which should already be snapped.
     * Returns null if the thread was interrupted part way.
     */
    public static TerrainTilePayload generate(TerrainSampler sampler, ChunkGenerator generator,
            RandomState randomState, String body, int level, int cx, int cz) {
        int spacing = TerrainTilePayload.spacingOf(level);
        int size = TerrainTilePayload.sizeOf(level);
        int half = (size - 1) / 2;
        SurfaceColumns columns = SurfaceColumns.create(sampler, generator, randomState, body.equals(BodyId.PALE.id),
                true);
        short[] heights = new short[size * size];
        byte[] rgb = new byte[3 * size * size];
        for (int j = 0; j < size; j++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            int z = cz + (j - half) * spacing;
            for (int i = 0; i < size; i++) {
                int x = cx + (i - half) * spacing;
                columns.at(x, z);
                int h = columns.height();
                int c = columns.colour();
                int k = j * size + i;
                heights[k] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, h));
                rgb[3 * k] = (byte) (c >> 16);
                rgb[3 * k + 1] = (byte) (c >> 8);
                rgb[3 * k + 2] = (byte) c;
            }
        }
        return new TerrainTilePayload(body, level, cx, cz, spacing, size, heights, rgb);
    }
}
