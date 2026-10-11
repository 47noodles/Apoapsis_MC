package dev.apoapsis.client.map;

/** The UI palette (ARGB). Client only. Steel is the ground, ink the text, the accents mark state. */
final class Palette {
    private Palette() {}

    static final int STEEL_950 = 0xFF0D1013;
    static final int STEEL_700 = 0xFF2A323B;
    static final int STEEL_500 = 0xFF56616D;
    static final int INK = 0xFFE8E2D4;
    static final int INK_MUTED = 0xFFA39C8E;
    static final int BRASS = 0xFFC9A25A;
    static final int COPPER = 0xFFC46F3E;
    static final int SIGNAL = 0xFF3AD1C1;
    static final int SIGNAL_EDGE = 0xFF1F4F4A;
    static final int HYDROGEN = 0xFF5AA9F5;
    static final int GO = 0xFF74D465;
    static final int ALERT = 0xFFEC5B45;

    /** Verdance's surface tone (muted blue-green). */
    static final int VERDANCE_GROUND = 0xFF2F6F63;
    /** Pale's surface tone (grey). */
    static final int PALE_GROUND = 0xFF7D838A;

    /** The same colour with a new alpha (0..255). */
    static int alpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
