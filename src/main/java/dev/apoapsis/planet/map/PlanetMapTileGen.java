package dev.apoapsis.planet.map;

import dev.apoapsis.planet.SurfaceColumns;
import dev.apoapsis.planet.SurfaceKind;
import dev.apoapsis.planet.TerrainSampler;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Renders one {@link MapLevels#TILE} tile of a refined planet map. Every texel is the same as the texel that
 * {@code PlanetMapGen} renders for the whole map at the same level: the same supersampling, the same height and colour
 * averages, the same 3x3 relief smoothing and north-west hillshade. The tile is computed inside a region with an apron
 * of {@link #APRON} texels on each side, so the shading at the tile edges reads the neighbouring texels and has no seam.
 * Longitude wraps and latitude clamps, as in {@code PlanetMapGen}. Pure computation with no static mutable state, so
 * tiles can be rendered on any thread, in any order.
 */
public final class PlanetMapTileGen {
    /** Apron texels on each side of the tile. */
    private static final int APRON = 2;
    /** Region edge, texels: the tile plus an apron on each side. */
    private static final int REGION = MapLevels.TILE + 2 * APRON;
    /** Hillshade brightness change per block of height difference, capped at 15 percent either way. */
    private static final double SHADE_PER_BLOCK = 0.04;
    private static final double SHADE_CAP = 0.15;

    private PlanetMapTileGen() {}

    /**
     * Generates tile ({@code tx}, {@code ty}) of {@code level} as raw RGB: {@code TILE * TILE * 3} bytes, three per
     * texel, row-major. Returns null if the calling thread was interrupted part way.
     *
     * @param sampler the terrain sampler of the body
     * @param generator the chunk generator of the body's dimension
     * @param randomState the random state of that generator
     * @param pale whether the body is Pale (selects its surface colours)
     * @param radius the body's radius in blocks, which maps texels to longitude and latitude
     * @param level the map level; its width and height are whole multiples of the tile size
     * @param tx the tile's column, from 0 to {@link MapLevels.Level#tilesX()} minus 1
     * @param ty the tile's row, from 0 to {@link MapLevels.Level#tilesY()} minus 1
     */
    public static byte[] generate(TerrainSampler sampler, ChunkGenerator generator, RandomState randomState,
            boolean pale, double radius, MapLevels.Level level, int tx, int ty) {
        int tile = MapLevels.TILE;
        int width = level.width();
        int height = level.height();
        int ss = level.supersample();
        int samples = ss * ss;
        SurfaceColumns columns = SurfaceColumns.create(sampler, generator, randomState, pale, false);

        // Ground height and colour of every region texel, sampled at its global texel.
        int[] ground = new int[REGION * REGION];
        int[] colors = new int[REGION * REGION];
        for (int rv = 0; rv < REGION; rv++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            int gv = clamp(ty * tile - APRON + rv, 0, height - 1);
            for (int ru = 0; ru < REGION; ru++) {
                int gu = Math.floorMod(tx * tile - APRON + ru, width);
                long r = 0, g = 0, b = 0;
                double hSum = 0;
                for (int sv = 0; sv < ss; sv++) {
                    double lat = Math.PI / 2 - Math.PI * (gv + (sv + 0.5) / ss) / height;
                    int z = (int) Math.round(-lat * radius);
                    for (int su = 0; su < ss; su++) {
                        double lon = -Math.PI + 2 * Math.PI * (gu + (su + 0.5) / ss) / width;
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
                int idx = rv * REGION + ru;
                ground[idx] = (int) Math.round(hSum / samples);
                colors[idx] = (int) (r / samples) << 16 | (int) (g / samples) << 8 | (int) (b / samples);
            }
        }

        // Relief: the ground height averaged over 3x3 region texels, for every texel that has all eight neighbours.
        int[] smooth = new int[REGION * REGION];
        for (int rv = 1; rv < REGION - 1; rv++) {
            for (int ru = 1; ru < REGION - 1; ru++) {
                int sum = 0;
                for (int dv = -1; dv <= 1; dv++) {
                    for (int du = -1; du <= 1; du++) {
                        sum += ground[(rv + dv) * REGION + ru + du];
                    }
                }
                smooth[rv * REGION + ru] = sum / 9;
            }
        }

        byte[] rgb = new byte[tile * tile * 3];
        for (int j = 0; j < tile; j++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            int rv = j + APRON;
            // PlanetMapGen clamps the north-west row to 0, so on the north edge it uses the texel's own row.
            boolean northEdge = ty * tile + j == 0;
            for (int i = 0; i < tile; i++) {
                int ru = i + APRON;
                int own = rv * REGION + ru;
                int nw = (northEdge ? rv : rv - 1) * REGION + ru - 1;
                double relief = clamp((smooth[own] - smooth[nw]) * SHADE_PER_BLOCK, -SHADE_CAP, SHADE_CAP);
                int argb = shade(colors[own], 1 + relief);
                int p = (j * tile + i) * 3;
                rgb[p] = (byte) (argb >> 16);
                rgb[p + 1] = (byte) (argb >> 8);
                rgb[p + 2] = (byte) argb;
            }
        }
        return rgb;
    }

    /** Scales each channel of an RGB colour by f, clamped to 0..255. Same as {@code SurfaceColors.shade}. */
    private static int shade(int c, double f) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ch = (c >> shift) & 0xFF;
            out |= clamp((int) Math.round(ch * f), 0, 255) << shift;
        }
        return out;
    }

    /** Same as {@code SurfaceColors.clamp}, which is not visible from this package. */
    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Same as {@code SurfaceColors.clamp}, which is not visible from this package. */
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
