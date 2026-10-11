package dev.apoapsis.planet;

import dev.apoapsis.planet.terrain.PaleFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * The flat colour of a body's ground at one point, from the biome and the ground height: water by depth, sand, snow
 * above a snow line, stone on high ground, grass by biome on Verdance; dark basalt maria and light regolith highlands
 * on Pale. Shared by the planet maps and the far-terrain tiles, so the two agree.
 */
final class SurfaceColors {
    private static final int OCEAN_SHALLOW = 0x3F76E4;
    private static final int OCEAN_DEEP = 0x0E2A6B;
    private static final int ICE = 0xC9E4F5;
    // The rock, sand and snow colours are the mean colour of the texture of the block the ground really is (snow block,
    // sand, stone, the top of basalt, our regolith), so a far tile matches the real chunks it meets. The accuracy
    // GameTests hold them to 3 percent. Terracotta stands for badlands, whose top is red sand over terracotta bands.
    private static final int SNOW = 0xF9FEFE;
    private static final int SAND = 0xDBCFA3;
    private static final int TERRACOTTA = 0xAB6232;
    private static final int STONE = 0x7E7E7E;
    private static final int PLAINS = 0x8DB360;
    private static final int FOREST = 0x6E9A44;
    private static final int JUNGLE = 0x4E9E2E;
    private static final int TAIGA = 0x7A9C7A;
    private static final int SAVANNA = 0xB5AD57;
    private static final int BASALT = 0x515156;
    private static final int REGOLITH = 0x979793;
    private static final Identifier PALE_MARIA = PaleFeatures.MARIA;

    /** Ground this far above sea level is stone grey, in blocks. */
    private static final int STONE_LINE = 50;
    /** Ground this far above sea level is snow, in blocks. */
    private static final int SNOW_LINE = 80;
    /** Ocean depth at which the water is fully deep blue, in blocks. */
    private static final double DEEP_BLOCKS = 40.0;

    private SurfaceColors() {}

    /** The colour (0xRRGGBB) at {@code pos} with ground height {@code h}, for Pale or Verdance. */
    static int at(boolean pale, Holder<Biome> biome, BlockPos pos, int h, int seaLevel) {
        return pale ? pale(biome) : verdance(biome, pos, h, seaLevel);
    }

    /** Verdance surface colour: the ground height and the biome found there. */
    static int verdance(Holder<Biome> biome, BlockPos pos, int h, int seaLevel) {
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
    static int pale(Holder<Biome> biome) {
        return biome.is(PALE_MARIA) ? BASALT : REGOLITH;
    }

    /** Pale's ground by the block on top: regolith or basalt. */
    static int pale(boolean regolith) {
        return regolith ? REGOLITH : BASALT;
    }

    /** Blends two RGB colours; t = 0 gives a, t = 1 gives b. */
    static int mix(int a, int b, double t) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ca = (a >> shift) & 0xFF;
            int cb = (b >> shift) & 0xFF;
            out |= clamp((int) Math.round(ca + (cb - ca) * t), 0, 255) << shift;
        }
        return out;
    }

    /** Scales each channel of an RGB colour by f, clamped to 0..255. */
    static int shade(int c, double f) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ch = (c >> shift) & 0xFF;
            out |= clamp((int) Math.round(ch * f), 0, 255) << shift;
        }
        return out;
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
