package dev.apoapsis.planet;

import dev.apoapsis.net.PlanetMapPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;

/**
 * Renders a body's surface as a Minecraft map: the top block's map colour, water shaded by depth and terrain shaded by
 * height difference to the texel to the north, with the steps and offsets of vanilla {@code MapItem}. Deterministic for
 * a seed. Runs off the server thread; it only reads the generator.
 */
final class PlanetMapGen {
    private PlanetMapGen() {}

    /** Generates the map, or returns null if the calling thread was interrupted. */
    static PlanetMapPayload generate(ServerLevel level, String body, double radius, int width, int height) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        int n = width * height;
        int[] top = new int[n];
        int[] waterDepth = new int[n];
        MapColor[] colors = new MapColor[n];

        for (int v = 0; v < height; v++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            double lat = Math.PI / 2 - Math.PI * (v + 0.5) / height;
            int z = (int) Math.round(-lat * radius);
            for (int u = 0; u < width; u++) {
                double lon = -Math.PI + 2 * Math.PI * (u + 0.5) / width;
                int x = (int) Math.round(lon * radius);
                NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
                int i = v * width + u;

                // Top block with a map colour, as MapItem does: air and other NONE blocks are skipped.
                int y = maxY;
                BlockState state = null;
                MapColor color = MapColor.NONE;
                while (y >= minY) {
                    state = column.getBlock(y);
                    color = state.getMapColor(level, BlockPos.ZERO);
                    if (color != MapColor.NONE) {
                        break;
                    }
                    y--;
                }
                if (y < minY) {
                    y = minY;
                }
                top[i] = y;
                colors[i] = color;

                // Water: count the fluid column below the surface, as vanilla does.
                if (color == MapColor.WATER && y > minY && state != null && !state.getFluidState().isEmpty()) {
                    int depth = 0;
                    int by = y - 1;
                    BlockState below;
                    do {
                        below = column.getBlock(by--);
                        depth++;
                    } while (by > minY && !below.getFluidState().isEmpty());
                    waterDepth[i] = depth;
                }
            }
        }

        byte[] rgb = new byte[n * 3];
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                int i = v * width + u;
                MapColor color = colors[i];
                if (color == MapColor.NONE) {
                    continue;
                }
                int checker = (u + v) & 1;
                MapColor.Brightness brightness;
                if (color == MapColor.WATER) {
                    double diff = waterDepth[i] * 0.1 + checker * 0.2;
                    brightness = diff < 0.5 ? MapColor.Brightness.HIGH
                            : diff > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                } else {
                    int north = v > 0 ? top[i - width] : top[i];
                    // Scale 1 map: vanilla's 4 / (scale + 4) height factor, plus the checker offset.
                    double diff = (top[i] - north) * 0.8 + (checker - 0.5) * 0.4;
                    brightness = diff > 0.6 ? MapColor.Brightness.HIGH
                            : diff < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                }
                int argb = color.calculateARGBColor(brightness);
                rgb[3 * i] = (byte) (argb >> 16);
                rgb[3 * i + 1] = (byte) (argb >> 8);
                rgb[3 * i + 2] = (byte) argb;
            }
        }
        return new PlanetMapPayload(body, width, height, rgb);
    }
}
