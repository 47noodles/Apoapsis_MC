package dev.apoapsis.client.planet;

import dev.apoapsis.flight.FlightDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

/**
 * Per-planet colour rules shared by the planet tint classes (client only).
 * Verdance feels lush and a little cooler: greens lean teal, water runs deep teal and the air is faintly teal.
 * Pale feels dead and airless: no haze at all, and its regolith is a touch cool and dust-pale.
 * Colours are pulled from the Verdance ground tone {@code 0x2F6F63} and the Pale ground tone {@code 0x7D838A}
 * (see {@code client/map/Palette}, package-private there) and docs/design/13-art-direction.md.
 */
final class PlanetTints {
    /** Grass target: the teal-green family of the Verdance ground tone, lighter. Blended in at {@link #VERDANCE_GRASS_AMOUNT}. */
    static final int VERDANCE_GRASS = 0x5FA58A;
    static final float VERDANCE_GRASS_AMOUNT = 0.30F;
    /** Foliage target: the Verdance ground tone pushed toward teal. */
    static final int VERDANCE_FOLIAGE = 0x2F8F7A;
    static final float VERDANCE_FOLIAGE_AMOUNT = 0.35F;
    /** Water target: a deeper teal than vanilla's blue. */
    static final int VERDANCE_WATER = 0x1A6E74;
    static final float VERDANCE_WATER_AMOUNT = 0.50F;
    /** Fog target for Verdance air: a pale teal, blended lightly into the vanilla fog. */
    static final int VERDANCE_AIR = 0x7FC7B8;
    static final float VERDANCE_AIR_AMOUNT = 0.25F;
    /** Multiplier for Pale regolith: slightly cool, slightly pale. */
    static final int PALE_REGOLITH = 0xFFD6E0EA;

    private PlanetTints() {}

    /** The dimension of the level a tint is read in, or of the client level when the reader is a render region. */
    private static ResourceKey<Level> dimension(BlockAndTintGetter getter) {
        Level level = getter instanceof Level l ? l : Minecraft.getInstance().level;
        return level == null ? null : level.dimension();
    }

    /** True in the Verdance overworld, false in every other dimension. */
    static boolean isVerdance(BlockAndTintGetter getter) {
        return Level.OVERWORLD.equals(dimension(getter));
    }

    /** True in Pale's dimension. */
    static boolean isPale(BlockAndTintGetter getter) {
        return FlightDimensions.PALE.equals(dimension(getter));
    }

    /** Pulls an ARGB colour toward {@code target} by {@code amount} (0 = unchanged, 1 = target). The "no tint" value -1 passes through. */
    static int shift(int argb, int target, float amount) {
        if (argb == -1) {
            return -1;
        }
        int r = mix((argb >> 16) & 0xFF, (target >> 16) & 0xFF, amount);
        int g = mix((argb >> 8) & 0xFF, (target >> 8) & 0xFF, amount);
        int b = mix(argb & 0xFF, target & 0xFF, amount);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Blends two 0..1 channel values. */
    static float mixFloat(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static int mix(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

    /** Wraps a vanilla tint source: shifts its colour toward {@code target} in Verdance, leaves it alone everywhere else. */
    static BlockTintSource verdanceShift(BlockTintSource base, int target, float amount) {
        return new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return base.color(state);
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                int c = base.colorInWorld(state, level, pos);
                return isVerdance(level) ? shift(c, target, amount) : c;
            }

            @Override
            public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                int c = base.colorAsTerrainParticle(state, level, pos);
                return isVerdance(level) ? shift(c, target, amount) : c;
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return base.relevantProperties();
            }
        };
    }
}
