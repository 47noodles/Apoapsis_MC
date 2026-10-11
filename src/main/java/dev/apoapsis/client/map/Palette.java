package dev.apoapsis.client.map;

/** The UI palette (ARGB). Client only. Steel is the ground, ink the text, the accents mark state. */
public final class Palette {
    private Palette() {}

    public static final int STEEL_950 = 0xFF0D1013;
    public static final int STEEL_700 = 0xFF2A323B;
    public static final int STEEL_500 = 0xFF56616D;
    public static final int INK = 0xFFE8E2D4;
    public static final int INK_MUTED = 0xFFA39C8E;
    public static final int BRASS = 0xFFC9A25A;
    public static final int COPPER = 0xFFC46F3E;
    public static final int SIGNAL = 0xFF3AD1C1;
    public static final int SIGNAL_EDGE = 0xFF1F4F4A;
    public static final int HYDROGEN = 0xFF5AA9F5;
    public static final int GO = 0xFF74D465;
    public static final int ALERT = 0xFFEC5B45;

    /** Verdance's surface tone on the shadow side (muted blue-green), and the lit side. */
    public static final int VERDANCE_GROUND = 0xFF2F6F63;
    public static final int VERDANCE_LIT = 0xFF6FBBA8;
    /** Pale's surface tone on the shadow side (grey), and the lit side. */
    public static final int PALE_GROUND = 0xFF5C636B;
    public static final int PALE_LIT = 0xFFB8BEC4;

    /** The same colour with a new alpha (0..255). */
    public static int alpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
