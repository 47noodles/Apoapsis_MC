package dev.apoapsis.client.notify;

import dev.apoapsis.client.map.Palette;

/** Colours and timing curves shared by the three notification kinds. Client only. */
final class NotifyStyle {
    /** The plate behind a line of text: dark steel, mostly opaque. */
    static final int PLATE = Palette.alpha(Palette.STEEL_950, 0xC0);
    static final int TEXT = Palette.INK;
    static final int TEXT_MUTED = Palette.INK_MUTED;
    static final int RULE = Palette.BRASS;
    static final int RULE_WARNING = Palette.ALERT;
    static final int RULE_FIRST = Palette.SIGNAL;
    /** GUI px from the screen edge, as the flight HUD keeps. */
    static final int EDGE = 10;

    private NotifyStyle() {}

    /** The colour with its alpha scaled by {@code fade} (0..1), never below 4 (the text renderer reads less as opaque). */
    static int faded(int argb, double fade) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0, Math.min(1.0, fade)));
        return Palette.alpha(argb, Math.max(alpha, 4));
    }

    /**
     * A 0..1 envelope for a message {@code age} ticks old that lasts {@code total}: up over {@code in}, flat, down over
     * {@code out}.
     */
    static double envelope(double age, int in, int total, int out) {
        double up = in <= 0 ? 1.0 : age / in;
        double down = out <= 0 ? 1.0 : (total - age) / out;
        return Math.max(0.0, Math.min(1.0, Math.min(up, down)));
    }
}
