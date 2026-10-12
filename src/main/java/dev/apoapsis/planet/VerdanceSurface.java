package dev.apoapsis.planet;

import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.Noise;

/**
 * What Verdance's ground looks like from above, column by column, without generating a chunk: a cheap model of the
 * vanilla overworld surface rules and of the snow and ice that decoration lays over cold ground and water. From the
 * ground height and the biome it picks the top block (grass, or the biome's own: sand in deserts and on beaches, banded
 * terracotta in badlands, bare stone on stony peaks and shores, gravel patches, podzol, mud, mycelium, snow and ice up
 * high and in the cold), using the same noises as the rules where a rule is a noise patch, and colours it with the mean
 * colour of that block's texture; grass takes the biome's tint. Water, anywhere the ground lies below sea level, is
 * drawn by its depth, or as ice where it would freeze. The GameTests compare the result with real chunks column by
 * column, see {@code AccuracyTests}.
 *
 * <p>What it leaves out because it costs more than it shows from a distance: the one-block-scale steepness test of the
 * rules, except on frozen peaks where it decides between snow and packed ice; where the rules put stone on steep faces
 * in the other cold mountain biomes, the snow layer decoration covers it anyway, so it shows white there. Not thread
 * safe: each {@link SurfaceColumns} cursor has its own.
 */
final class VerdanceSurface {
    private static final int OCEAN_SHALLOW = 0x3F76E4;
    private static final int OCEAN_DEEP = 0x0E2A6B;
    /** Ice over water and ice sheets: lighter than the ice texture, which is translucent. */
    private static final int ICE = 0xC9E4F5;
    // The block colours are the mean colour of the texture of the block the ground really is, so a far tile matches the
    // real chunks it meets; the accuracy GameTests hold them to 3 percent where they are a block's own.
    private static final int SNOW = 0xF9FEFE;
    private static final int PACKED_ICE = 0x8EB4FA;
    private static final int SAND = 0xDBCFA3;
    private static final int RED_SAND = 0xBF6721;
    /** The mean of the badlands' terracotta bands: mostly plain terracotta, a quarter orange, a few other colours. */
    private static final int TERRACOTTA_BANDS = 0x9B5E42;
    private static final int STONE = 0x7E7E7E;
    private static final int CALCITE = 0xDFE0DD;
    private static final int GRAVEL = 0x847F7F;
    private static final int COARSE_DIRT = 0x77563B;
    private static final int PODZOL = 0x5C3F18;
    private static final int MYCELIUM = 0x6F6365;
    private static final int MUD = 0x3C393D;
    /** The mean of the grass block's top texture, a grey: the biome's grass colour multiplies it. */
    private static final int GRASS_TEXTURE = 0x93;

    /** Ocean depth at which the water is fully deep blue, in blocks. */
    private static final double DEEP_BLOCKS = 40.0;
    /** The surface noise value the vanilla rules compare in units of this (their thresholds are written in eighths). */
    private static final double SURFACE_UNIT = 8.25;
    /** Above this height, in blocks over sea level, the wooded badlands turn to grass and bands of coarse dirt. */
    private static final int WOODED_BADLANDS_TOP = 36;
    /** The badlands show terracotta bands from this height over sea level up, red sand under it, in blocks. */
    private static final int BADLANDS_BANDS = 13;

    /** The surface rule a biome follows. */
    private enum Rule {
        GRASS, FROZEN_PEAKS, STONY_PEAKS, STONY_SHORE, WINDSWEPT_HILLS, WINDSWEPT_SAVANNA, WINDSWEPT_GRAVELLY_HILLS,
        OLD_GROWTH_TAIGA, SAND, BADLANDS, WOODED_BADLANDS, MUD, MYCELIUM, DAPPLED_FOREST, SNOW_BLOCK
    }

    /** What the cursor knows about one biome: the rule it follows and its grass colour. */
    private record Profile(Rule rule, int grassBase, GrassColorModifier modifier) {}

    private final int seaLevel;
    private final TerrainSampler.Worker worker;
    private final Noise surface;
    private final Noise gravel;
    private final Noise calcite;
    private final Noise packedIce;
    private final Noise ice;
    private final Noise smallPatch;
    private final IdentityHashMap<Biome, Profile> profiles = new IdentityHashMap<>();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    VerdanceSurface(RandomState randomState, TerrainSampler.Worker worker, int seaLevel) {
        this.seaLevel = seaLevel;
        this.worker = worker;
        this.surface = randomState.getOrCreateNoise(Noises.SURFACE);
        this.gravel = randomState.getOrCreateNoise(Noises.GRAVEL);
        this.calcite = randomState.getOrCreateNoise(Noises.CALCITE);
        this.packedIce = randomState.getOrCreateNoise(Noises.PACKED_ICE);
        this.ice = randomState.getOrCreateNoise(Noises.ICE);
        this.smallPatch = randomState.getOrCreateNoise(Noises.SMALL_PATCH);
    }

    /** The kind and colour (see {@link SurfaceColors#pack}) of the column ({@code x}, {@code z}) with this ground height. */
    int at(Holder<Biome> holder, int x, int z, int ground) {
        Biome biome = holder.value();
        if (ground < seaLevel) {
            // Water over the ground. It freezes where the biome is cold: the ice is what shows.
            if (!biome.warmEnoughToRain(pos.set(x, seaLevel - 1, z), seaLevel)) {
                return SurfaceColors.pack(SurfaceKind.SNOW, ICE);
            }
            double deep = SurfaceColors.clamp((seaLevel - ground) / DEEP_BLOCKS, 0, 1);
            return SurfaceColors.pack(SurfaceKind.WATER, SurfaceColors.mix(OCEAN_SHALLOW, OCEAN_DEEP, deep));
        }
        Profile profile = profiles.get(biome);
        if (profile == null) {
            profile = profile(holder, biome);
            profiles.put(biome, profile);
        }
        boolean snowy = biome.hasPrecipitation() && biome.coldEnoughToSnow(pos.set(x, ground, z), seaLevel);
        if (snowy) {
            // Snow lies on whatever is there, except on ice: the packed ice of frozen peaks stays bare.
            if (profile.rule == Rule.FROZEN_PEAKS && bareIce(x, z)) {
                return SurfaceColors.pack(SurfaceKind.SNOW, PACKED_ICE);
            }
            return SurfaceColors.pack(SurfaceKind.SNOW, SNOW);
        }
        return top(profile, x, z, ground);
    }

    /** The top block of a column that is not under water and not snowy, by its biome's rule. */
    private int top(Profile profile, int x, int z, int ground) {
        switch (profile.rule) {
            case STONY_PEAKS:
                return calcite.get(x, 0, z) >= -0.0125F && calcite.get(x, 0, z) <= 0.0125F
                        ? SurfaceColors.pack(SurfaceKind.STONE, CALCITE) : SurfaceColors.pack(SurfaceKind.STONE, STONE);
            case STONY_SHORE:
                return Math.abs(gravel.get(x, 0, z)) <= 0.05F
                        ? SurfaceColors.pack(SurfaceKind.GRAVEL, GRAVEL) : SurfaceColors.pack(SurfaceKind.STONE, STONE);
            case WINDSWEPT_HILLS:
                if (surfaceAbove(x, z, 1.0)) {
                    return SurfaceColors.pack(SurfaceKind.STONE, STONE);
                }
                break;
            case WINDSWEPT_SAVANNA:
                if (surfaceAbove(x, z, 1.75)) {
                    return SurfaceColors.pack(SurfaceKind.STONE, STONE);
                }
                if (surfaceAbove(x, z, -0.5)) {
                    return SurfaceColors.pack(SurfaceKind.GRASS, COARSE_DIRT);
                }
                break;
            case WINDSWEPT_GRAVELLY_HILLS:
                if (surfaceAbove(x, z, 2.0)) {
                    return SurfaceColors.pack(SurfaceKind.GRAVEL, GRAVEL);
                }
                if (surfaceAbove(x, z, 1.0)) {
                    return SurfaceColors.pack(SurfaceKind.STONE, STONE);
                }
                if (!surfaceAbove(x, z, -1.0)) {
                    return SurfaceColors.pack(SurfaceKind.GRAVEL, GRAVEL);
                }
                break;
            case OLD_GROWTH_TAIGA:
                if (surfaceAbove(x, z, 1.75)) {
                    return SurfaceColors.pack(SurfaceKind.GRASS, COARSE_DIRT);
                }
                if (surfaceAbove(x, z, -0.95)) {
                    return SurfaceColors.pack(SurfaceKind.GRASS, PODZOL);
                }
                break;
            case SAND:
                return SurfaceColors.pack(SurfaceKind.SAND, SAND);
            case BADLANDS:
                return badlands(ground);
            case WOODED_BADLANDS:
                if (ground - 1 < seaLevel + WOODED_BADLANDS_TOP) {
                    return badlands(ground);
                }
                break;
            case MUD:
                return SurfaceColors.pack(SurfaceKind.GRASS, MUD);
            case MYCELIUM:
                return SurfaceColors.pack(SurfaceKind.GRASS, MYCELIUM);
            case DAPPLED_FOREST:
                if (smallPatch.get(x, 0, z) >= 1.2F) {
                    return SurfaceColors.pack(SurfaceKind.GRASS, COARSE_DIRT);
                }
                break;
            case SNOW_BLOCK:
                return SurfaceColors.pack(SurfaceKind.SNOW, SNOW);
            case FROZEN_PEAKS:
                return bareIce(x, z) ? SurfaceColors.pack(SurfaceKind.SNOW, PACKED_ICE)
                        : SurfaceColors.pack(SurfaceKind.SNOW, SNOW);
            default:
                break;
        }
        return SurfaceColors.pack(SurfaceKind.GRASS, grass(profile, x, z));
    }

    /** Badlands: terracotta bands high up, red sand lower. */
    private int badlands(int ground) {
        return ground - 1 >= seaLevel + BADLANDS_BANDS ? SurfaceColors.pack(SurfaceKind.RED_ROCK, TERRACOTTA_BANDS)
                : SurfaceColors.pack(SurfaceKind.RED_ROCK, RED_SAND);
    }

    /** The surface noise at or above the vanilla rules' threshold of {@code eighths} (they are written in eighths). */
    private boolean surfaceAbove(int x, int z, double eighths) {
        return surface.get(x, 0, z) >= eighths / SURFACE_UNIT;
    }

    /**
     * Whether a frozen peaks column is packed ice instead of snow: on a steep face, or in the patches of the two ice
     * noises. Steep is the vanilla test: the ground a block south or west is 4 or more above the one a block north or east.
     */
    private boolean bareIce(int x, int z) {
        float p = packedIce.get(x, 0, z);
        if (p >= 0.0F && p <= 0.2F) {
            return true;
        }
        float i = ice.get(x, 0, z);
        if (i >= 0.0F && i <= 0.025F) {
            return true;
        }
        int south = Math.round(worker.ground(x, z + 1));
        int north = Math.round(worker.ground(x, z - 1));
        if (south - north >= 4) {
            return true;
        }
        int west = Math.round(worker.ground(x - 1, z));
        int east = Math.round(worker.ground(x + 1, z));
        return west - east >= 4;
    }

    /** The grass block's top in a biome at a column: its grass colour times the texture. */
    private int grass(Profile profile, int x, int z) {
        int tint = profile.modifier.modifyColor(x, z, profile.grassBase);
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            out |= (((tint >> shift) & 0xFF) * GRASS_TEXTURE + 127) / 255 << shift;
        }
        return out;
    }

    private static Profile profile(Holder<Biome> holder, Biome biome) {
        Rule rule = Rule.GRASS;
        if (holder.is(Biomes.FROZEN_PEAKS)) {
            rule = Rule.FROZEN_PEAKS;
        } else if (holder.is(Biomes.SNOWY_SLOPES) || holder.is(Biomes.JAGGED_PEAKS) || holder.is(Biomes.GROVE)
                || holder.is(Biomes.ICE_SPIKES)) {
            rule = Rule.SNOW_BLOCK;
        } else if (holder.is(Biomes.STONY_PEAKS)) {
            rule = Rule.STONY_PEAKS;
        } else if (holder.is(Biomes.STONY_SHORE)) {
            rule = Rule.STONY_SHORE;
        } else if (holder.is(Biomes.WINDSWEPT_HILLS)) {
            rule = Rule.WINDSWEPT_HILLS;
        } else if (holder.is(Biomes.BEACH) || holder.is(Biomes.SNOWY_BEACH) || holder.is(Biomes.WARM_OCEAN)
                || holder.is(Biomes.DESERT)) {
            rule = Rule.SAND;
        } else if (holder.is(Biomes.MANGROVE_SWAMP)) {
            rule = Rule.MUD;
        } else if (holder.is(Biomes.WINDSWEPT_SAVANNA)) {
            rule = Rule.WINDSWEPT_SAVANNA;
        } else if (holder.is(Biomes.WINDSWEPT_GRAVELLY_HILLS)) {
            rule = Rule.WINDSWEPT_GRAVELLY_HILLS;
        } else if (holder.is(Biomes.OLD_GROWTH_PINE_TAIGA) || holder.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            rule = Rule.OLD_GROWTH_TAIGA;
        } else if (holder.is(Biomes.MUSHROOM_FIELDS)) {
            rule = Rule.MYCELIUM;
        } else if (holder.is(Biomes.DAPPLED_FOREST)) {
            rule = Rule.DAPPLED_FOREST;
        } else if (holder.is(Biomes.BADLANDS) || holder.is(Biomes.ERODED_BADLANDS)) {
            rule = Rule.BADLANDS;
        } else if (holder.is(Biomes.WOODED_BADLANDS)) {
            rule = Rule.WOODED_BADLANDS;
        }
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        int base = biome.getSpecialEffects().grassColorOverride()
                .orElseGet(() -> grassColourMap(climate.temperature(), climate.downfall()));
        return new Profile(rule, base, biome.getSpecialEffects().grassColorModifier());
    }

    /**
     * The grass colour the vanilla colour map gives a temperature and downfall (both clamped to 0..1), as a quadratic
     * fitted to the 256 by 256 map: within 0.6 percent of it everywhere.
     */
    static int grassColourMap(float temperature, float downfall) {
        double t = SurfaceColors.clamp(temperature, 0, 1);
        double r = SurfaceColors.clamp(downfall, 0, 1) * t;
        int red = quadratic(t, r, 128.0500, 62.8868, -22.9198, -0.4664, 0.9783, -97.8114);
        int green = quadratic(t, r, 180.5556, 3.1635, 4.5150, -0.2252, -0.1757, 17.1541);
        int blue = quadratic(t, r, 150.7022, -66.9109, -6.5126, 1.3092, 0.2070, -28.1888);
        return red << 16 | green << 8 | blue;
    }

    private static int quadratic(double t, double r, double c0, double ct, double cr, double ctt, double crr,
            double ctr) {
        return SurfaceColors.clamp((int) Math.round(c0 + ct * t + cr * r + ctt * t * t + crr * r * r + ctr * t * r), 0, 255);
    }
}
