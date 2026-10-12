package dev.apoapsis.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.stream.IntStream;

/**
 * Turns the coarse surface map from the server into a finer one for the planet cap, so a texel never reads as a square
 * from orbit. The map is magnified {@link #FACTOR} times with a bicubic filter after its sampling point is warped by a
 * smooth noise (so the borders between biomes wander like coasts instead of following the grid), and a faint fine grain
 * is added to land. Everything is computed from the map and fixed noise, so the same map gives the same picture.
 * Client only.
 */
final class PlanetDetail {
    /** Output pixels per source texel, along each axis. */
    static final int FACTOR = 4;
    /** Warp lattice spacing in output pixels (a divisor of the output width, so longitude wraps), per octave. */
    private static final int WARP_CELL_1 = 16;
    private static final int WARP_CELL_2 = 8;
    /** Warp amplitude in source texels, per octave. */
    private static final double WARP_1 = 0.85;
    private static final double WARP_2 = 0.35;
    /** Land grain: brightness change either way, and the lattice spacing in output pixels. */
    private static final double GRAIN = 0.07;
    private static final int GRAIN_CELL = 2;

    private PlanetDetail() {}

    /** The magnified map for {@code rgb} (3 bytes per texel, row-major from the north edge) of {@code w} x {@code h}. */
    static NativeImage magnify(byte[] rgb, int w, int h) {
        int ow = w * FACTOR;
        int oh = h * FACTOR;
        float[] src = new float[w * h * 3];
        for (int i = 0; i < src.length; i++) {
            src[i] = rgb[i] & 0xFF;
        }
        int[] out = new int[ow * oh];
        IntStream.range(0, oh).parallel().forEach(y -> {
            for (int x = 0; x < ow; x++) {
                // Source texel coordinates of this output pixel's centre, then warped.
                double u = (x + 0.5) / FACTOR - 0.5
                        + WARP_1 * (noise(x, y, WARP_CELL_1, ow, 11) - 0.5) * 2
                        + WARP_2 * (noise(x, y, WARP_CELL_2, ow, 23) - 0.5) * 2;
                double v = (y + 0.5) / FACTOR - 0.5
                        + WARP_1 * (noise(x, y, WARP_CELL_1, ow, 37) - 0.5) * 2
                        + WARP_2 * (noise(x, y, WARP_CELL_2, ow, 53) - 0.5) * 2;
                float[] c = cubic(src, w, h, u, v);
                // Grain on land only: water is blue-dominant and stays smooth.
                double water = c[2] > c[1] * 1.15 && c[2] > c[0] * 1.5 ? 0.25 : 1.0;
                double grain = 1 + GRAIN * water * (noise(x, y, GRAIN_CELL, ow, 71) - 0.5) * 2;
                int r = clamp(c[0] * grain);
                int g = clamp(c[1] * grain);
                int b = clamp(c[2] * grain);
                out[y * ow + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        });
        NativeImage image = new NativeImage(ow, oh, false);
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                image.setPixel(x, y, out[y * ow + x]);
            }
        }
        return image;
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    /** Catmull-Rom interpolation of the source at texel coordinates (u, v): longitude wraps, latitude clamps. */
    private static float[] cubic(float[] src, int w, int h, double u, double v) {
        int iu = (int) Math.floor(u);
        int iv = (int) Math.floor(v);
        double[] wu = weights(u - iu);
        double[] wv = weights(v - iv);
        float[] sum = new float[3];
        for (int j = 0; j < 4; j++) {
            int yy = Math.max(0, Math.min(h - 1, iv - 1 + j));
            for (int i = 0; i < 4; i++) {
                int xx = Math.floorMod(iu - 1 + i, w);
                double k = wu[i] * wv[j];
                int o = 3 * (yy * w + xx);
                sum[0] += (float) (src[o] * k);
                sum[1] += (float) (src[o + 1] * k);
                sum[2] += (float) (src[o + 2] * k);
            }
        }
        return sum;
    }

    /** Catmull-Rom weights of the four taps at fraction {@code t}. */
    private static double[] weights(double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new double[] {
            0.5 * (-t3 + 2 * t2 - t),
            0.5 * (3 * t3 - 5 * t2 + 2),
            0.5 * (-3 * t3 + 4 * t2 + t),
            0.5 * (t3 - t2)
        };
    }

    /** Value noise 0..1 on a lattice of {@code cell} pixels; the column wraps at {@code width} (a multiple of cell). */
    private static double noise(int x, int y, int cell, int width, int seed) {
        int cx = x / cell;
        int cy = y / cell;
        double fx = (x % cell) / (double) cell;
        double fy = (y % cell) / (double) cell;
        int cols = width / cell;
        double a = hash(Math.floorMod(cx, cols), cy, seed);
        double b = hash(Math.floorMod(cx + 1, cols), cy, seed);
        double c = hash(Math.floorMod(cx, cols), cy + 1, seed);
        double d = hash(Math.floorMod(cx + 1, cols), cy + 1, seed);
        double sx = fx * fx * (3 - 2 * fx);
        double sy = fy * fy * (3 - 2 * fy);
        double top = a + (b - a) * sx;
        double bottom = c + (d - c) * sx;
        return top + (bottom - top) * sy;
    }

    private static double hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }
}
