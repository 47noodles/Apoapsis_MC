package dev.apoapsis.client.map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Number formatting for the map and the HUD. Fixed locale so the decimal point never changes, and a dash for values
 * that do not exist (NaN, infinity). Client only.
 */
final class Fmt {
    private static final String NONE = "—";
    /** The unicode font is one width per glyph, so digits do not shift as values change. */
    private static final Style MONO = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.withDefaultNamespace("uniform")));

    private Fmt() {}

    /** A value in the tabular unicode font (see {@link #MONO}). */
    static Component mono(String s) {
        return Component.literal(s).setStyle(MONO);
    }

    /** A time until an event in the tabular font, the "T-" and the clock together ("T-12:04"). */
    static Component countdown(double seconds) {
        return mono("T-" + clock(seconds));
    }

    /** Altitude or distance: metres below 1 km, otherwise kilometres to one decimal ("14.2 km"). */
    static String distance(double m) {
        if (!Double.isFinite(m)) {
            return NONE;
        }
        if (Math.abs(m) < 1000) {
            return String.format(Locale.ROOT, "%d m", Math.round(m));
        }
        return String.format(Locale.ROOT, "%.1f km", m / 1000.0);
    }

    /** Speed in m/s with grouping ("2,140 m/s"). */
    static String speed(double ms) {
        if (!Double.isFinite(ms)) {
            return NONE;
        }
        return String.format(Locale.ROOT, "%,d m/s", Math.round(ms));
    }

    /** Vertical speed with a sign ("+81 m/s"). A value that rounds to zero is "0 m/s", never "-0 m/s". */
    static String signedSpeed(double ms) {
        if (!Double.isFinite(ms)) {
            return NONE;
        }
        long whole = Math.round(ms);
        if (whole == 0) {
            return "0 m/s";
        }
        return String.format(Locale.ROOT, "%+,d m/s", whole);
    }

    /** A 0..1 fraction as a whole percentage ("72%"). */
    static String percent(double fraction) {
        if (!Double.isFinite(fraction)) {
            return NONE;
        }
        return String.format(Locale.ROOT, "%d%%", Math.round(fraction * 100.0));
    }

    /** An angle in degrees, whole ("12°"). */
    static String degrees(double deg) {
        if (!Double.isFinite(deg)) {
            return NONE;
        }
        return String.format(Locale.ROOT, "%d°", Math.round(deg));
    }

    /** A dimensionless ratio to one decimal ("1.8"). */
    static String ratio(double r) {
        if (!Double.isFinite(r)) {
            return NONE;
        }
        return String.format(Locale.ROOT, "%.1f", r);
    }

    /** Time-warp factor: "x10", or "x2.5" when not whole. */
    static String warp(float w) {
        if (w == Math.rint(w)) {
            return "x" + (long) w;
        }
        return String.format(Locale.ROOT, "x%.1f", w);
    }

    /** A duration as m:ss, or h:mm:ss from an hour ("12:04", "1:05:09"). Negative values clamp to zero. */
    static String clock(double seconds) {
        if (!Double.isFinite(seconds)) {
            return NONE;
        }
        long s = Math.max(0, Math.round(seconds));
        long h = s / 3600;
        long m = (s / 60) % 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec);
        }
        return String.format(Locale.ROOT, "%d:%02d", m, sec);
    }
}
