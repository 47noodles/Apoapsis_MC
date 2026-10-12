package dev.apoapsis.client.ui;

/**
 * The Brassworks palette as ARGB constants, shared by every screen. Steel is the ground, ink the text, brass the trim,
 * teal (signal) marks focus and live state, copper marks the one risky action on a screen. Client only.
 */
public final class UiPalette {
    private UiPalette() {}

    public static final int STEEL_950 = 0xFF0D1013;
    public static final int STEEL_900 = 0xFF14181D;
    public static final int STEEL_800 = 0xFF1D232A;
    public static final int STEEL_700 = 0xFF2A323B;
    public static final int STEEL_600 = 0xFF3F4852;
    public static final int STEEL_500 = 0xFF56616D;
    public static final int INK = 0xFFE8E2D4;
    public static final int INK_MUTED = 0xFFA39C8E;
    public static final int INK_DIM = 0xFF6E6A62;
    public static final int BRASS = 0xFFC9A25A;
    public static final int BRASS_DEEP = 0xFF8A6A34;
    public static final int COPPER = 0xFFC46F3E;
    public static final int SIGNAL = 0xFF3AD1C1;
    public static final int SIGNAL_GLASS = 0xFF0B1F1E;
    public static final int SIGNAL_EDGE = 0xFF1F4F4A;
    public static final int HYDROGEN = 0xFF5AA9F5;
    public static final int GO = 0xFF74D465;
    public static final int ALERT = 0xFFEC5B45;

    /** The same colour with a new alpha (0..255). */
    public static int alpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** The same colour with its alpha scaled by {@code factor} (0..1). */
    public static int fade(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Linear blend from {@code a} to {@code b} by {@code t} (0..1), alpha included. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            out |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return out;
    }
}
