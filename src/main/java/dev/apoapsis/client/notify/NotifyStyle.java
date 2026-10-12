package dev.apoapsis.client.notify;

import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import java.util.ArrayList;
import java.util.List;

/** Colours, wrapping and timing curves shared by the three notification kinds. Client only. */
final class NotifyStyle {
    /** The plate behind a line of text: dark steel, mostly opaque. */
    static final int PLATE = UiPalette.alpha(UiPalette.STEEL_950, 0xC0);
    static final int TEXT = UiPalette.INK;
    static final int TEXT_MUTED = UiPalette.INK_MUTED;
    static final int RULE = UiPalette.BRASS;
    static final int RULE_WARNING = UiPalette.ALERT;
    static final int RULE_FIRST = UiPalette.SIGNAL;

    private NotifyStyle() {}

    /** The colour with its alpha scaled by {@code fade} (0..1), never below 4 (the text renderer reads less as opaque). */
    static int faded(int argb, double fade) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0, Math.min(1.0, fade)));
        return UiPalette.alpha(argb, Math.max(alpha, 4));
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

    /**
     * Word-wraps {@code text} on spaces so that each row is at most {@code maxWidth} UI units wide in {@code type}. A
     * single word wider than that stays on its own row. Always returns at least one row.
     */
    static List<String> wrap(UiText.Type type, String text, float maxWidth) {
        List<String> rows = new ArrayList<>();
        String row = "";
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String next = row.isEmpty() ? word : row + " " + word;
            if (!row.isEmpty() && UiTextSmooth.width(type, next) > maxWidth) {
                rows.add(row);
                row = word;
            } else {
                row = next;
            }
        }
        rows.add(row);
        return rows;
    }
}
