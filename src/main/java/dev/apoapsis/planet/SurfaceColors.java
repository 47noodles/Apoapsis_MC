package dev.apoapsis.planet;

import dev.apoapsis.planet.terrain.PaleFeatures;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

/**
 * Colours of a body's ground that are not Verdance's (that is {@link VerdanceSurface}) and the helpers both share: dark
 * basalt maria and light regolith highlands on Pale, and the packing of a {@link SurfaceKind} with its colour into one
 * int. Used by the planet maps and the far-terrain tiles through {@link SurfaceColumns}, so the two agree.
 */
final class SurfaceColors {
    // The basalt and regolith colours are the mean colour of the texture of the block the ground really is (the top of
    // basalt, our regolith), so a far tile matches the real chunks it meets. The accuracy GameTests hold them to 3
    // percent.
    private static final int BASALT = 0x515156;
    private static final int REGOLITH = 0x979793;
    private static final Identifier PALE_MARIA = PaleFeatures.MARIA;

    private SurfaceColors() {}

    /** Packs a kind and a colour into one int: the kind in the top byte, the colour 0xRRGGBB below. */
    static int pack(SurfaceKind kind, int rgb) {
        return kind.ordinal() << 24 | rgb;
    }

    /** The kind of a packed value from {@link #pack}. */
    static SurfaceKind kindOf(int packed) {
        return SurfaceKind.values()[packed >>> 24];
    }

    /** Pale without its surface model has no grass or water: dark basalt maria, light regolith highlands. */
    static int pale(Holder<Biome> biome) {
        return pale(!biome.is(PALE_MARIA));
    }

    /** Pale's ground by the block on top, kind and colour (see {@link #pack}): regolith, or basalt in the maria. */
    static int pale(boolean regolith) {
        return regolith ? pack(SurfaceKind.REGOLITH, REGOLITH) : pack(SurfaceKind.BASALT, BASALT);
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
