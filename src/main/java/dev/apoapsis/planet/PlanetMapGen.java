package dev.apoapsis.planet;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.PlanetMapPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Renders a body's surface as a flat-coloured map. Each texel takes the ground height and the biome there, with no
 * column or surface-rule pass: the colour comes from the biome and height (water by depth, sand, snow above a snow
 * line, stone on high ground, grass by biome), then a smooth hillshade from the height difference to the north-west.
 * Deterministic for a seed. Runs off the server thread; it only reads the generator.
 */
final class PlanetMapGen {
    private static final int OCEAN_SHALLOW = 0x3F76E4;
    private static final int OCEAN_DEEP = 0x0E2A6B;
    private static final int ICE = 0xC9E4F5;
    private static final int SNOW = 0xF2F6FA;
    private static final int SAND = 0xE3D08A;
    private static final int TERRACOTTA = 0xB9673A;
    private static final int STONE = 0x7D7D7D;
    private static final int PLAINS = 0x8DB360;
    private static final int FOREST = 0x6E9A44;
    private static final int JUNGLE = 0x4E9E2E;
    private static final int TAIGA = 0x7A9C7A;
    private static final int SAVANNA = 0xB5AD57;
    private static final int BASALT = 0x3A3A40;
    private static final int REGOLITH = 0x9A958C;
    private static final Identifier PALE_MARIA = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pale_maria");

    /** Ground this far above sea level is stone grey, in blocks. */
    private static final int STONE_LINE = 50;
    /** Ground this far above sea level is snow, in blocks. */
    private static final int SNOW_LINE = 80;
    /** Ocean depth at which the water is fully deep blue, in blocks. */
    private static final double DEEP_BLOCKS = 40.0;
    /** Hillshade brightness change per block of height difference, capped at 15 percent either way. */
    private static final double SHADE_PER_BLOCK = 0.04;
    private static final double SHADE_CAP = 0.15;

    private PlanetMapGen() {}

    /** Generates the map, or returns null if the calling thread was interrupted. */
    static PlanetMapPayload generate(ServerLevel level, String body, double radius, int width, int height) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        BiomeResolver biomes = generator.getBiomeSource().createCachingResolver(randomState);
        int seaLevel = generator.getSeaLevel();
        boolean pale = body.equals(BodyId.PALE.id);
        int n = width * height;
        int[] ground = new int[n];
        int[] colors = new int[n];

        for (int v = 0; v < height; v++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            double lat = Math.PI / 2 - Math.PI * (v + 0.5) / height;
            int z = (int) Math.round(-lat * radius);
            for (int u = 0; u < width; u++) {
                double lon = -Math.PI + 2 * Math.PI * (u + 0.5) / width;
                int x = (int) Math.round(lon * radius);
                int h = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(h),
                        QuartPos.fromBlock(z));
                int i = v * width + u;
                ground[i] = h;
                colors[i] = pale ? paleColor(biome) : surfaceColor(biome, new BlockPos(x, h, z), h, seaLevel);
            }
        }

        // Relief: the ground height averaged over 3x3 texels (longitude wraps, latitude clamps), then the height
        // difference to the north-west texel.
        int[] smooth = new int[n];
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                int sum = 0;
                for (int dv = -1; dv <= 1; dv++) {
                    int vv = clamp(v + dv, 0, height - 1);
                    for (int du = -1; du <= 1; du++) {
                        sum += ground[vv * width + Math.floorMod(u + du, width)];
                    }
                }
                smooth[v * width + u] = sum / 9;
            }
        }

        byte[] rgb = new byte[n * 3];
        for (int v = 0; v < height; v++) {
            int vn = clamp(v - 1, 0, height - 1);
            for (int u = 0; u < width; u++) {
                int i = v * width + u;
                int nw = vn * width + Math.floorMod(u - 1, width);
                double shade = clamp((smooth[i] - smooth[nw]) * SHADE_PER_BLOCK, -SHADE_CAP, SHADE_CAP);
                int argb = shade(colors[i], 1 + shade);
                rgb[3 * i] = (byte) (argb >> 16);
                rgb[3 * i + 1] = (byte) (argb >> 8);
                rgb[3 * i + 2] = (byte) argb;
            }
        }
        return new PlanetMapPayload(body, width, height, rgb);
    }

    /** Verdance surface colour at a texel: the ground height and the biome found there. */
    private static int surfaceColor(Holder<Biome> biome, BlockPos pos, int h, int seaLevel) {
        boolean cold = biome.value().hasPrecipitation() && biome.value().coldEnoughToSnow(pos, seaLevel);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            if (cold) {
                return ICE;
            }
            return mix(OCEAN_SHALLOW, OCEAN_DEEP, clamp((seaLevel - h) / DEEP_BLOCKS, 0, 1));
        }
        int above = h - seaLevel;
        if (cold || above > SNOW_LINE) {
            return SNOW;
        }
        boolean dryAndWarm = !biome.value().hasPrecipitation() && biome.value().getBaseTemperature() >= 1.5F;
        if (biome.is(BiomeTags.IS_BEACH) || dryAndWarm && !biome.is(BiomeTags.IS_SAVANNA)) {
            return SAND;
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return TERRACOTTA;
        }
        if (above > STONE_LINE) {
            return STONE;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return JUNGLE;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return SAVANNA;
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return TAIGA;
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return FOREST;
        }
        return PLAINS;
    }

    /** Pale has no grass or water: dark basalt maria, light regolith highlands. */
    private static int paleColor(Holder<Biome> biome) {
        return biome.is(PALE_MARIA) ? BASALT : REGOLITH;
    }

    /** Blends two RGB colours; t = 0 gives a, t = 1 gives b. */
    private static int mix(int a, int b, double t) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ca = (a >> shift) & 0xFF;
            int cb = (b >> shift) & 0xFF;
            out |= clamp((int) Math.round(ca + (cb - ca) * t), 0, 255) << shift;
        }
        return out;
    }

    /** Scales each channel of an RGB colour by f, clamped to 0..255. */
    private static int shade(int c, double f) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ch = (c >> shift) & 0xFF;
            out |= clamp((int) Math.round(ch * f), 0, 255) << shift;
        }
        return out;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
