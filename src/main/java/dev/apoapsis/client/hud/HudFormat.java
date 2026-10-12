package dev.apoapsis.client.hud;

import java.util.Locale;

/**
 * Instrument readings as a number and its unit, so the number can be set tabular and large and the unit small. Groups
 * of thousands are separated by a space and a minus is a true minus sign. A reading that does not exist is a dash.
 * Client only.
 */
public final class HudFormat {
    private static final String DASH = "–";
    private static final String MINUS = "−";

    private HudFormat() {}

    /** Altitude or distance: metres below 1 km, kilometres from there (one decimal under 100 km). */
    public static String[] distance(double m) {
        if (!Double.isFinite(m)) {
            return new String[] {DASH, ""};
        }
        double a = Math.abs(m);
        if (a < 1000) {
            return new String[] {integer(Math.round(m)), "m"};
        }
        if (a < 100_000) {
            return new String[] {minus(String.format(Locale.ROOT, "%.1f", m / 1000.0)), "km"};
        }
        return new String[] {integer(Math.round(m / 1000.0)), "km"};
    }

    /** A speed in m/s, whole. */
    public static String[] speed(double ms) {
        if (!Double.isFinite(ms)) {
            return new String[] {DASH, ""};
        }
        return new String[] {integer(Math.round(ms)), "m/s"};
    }

    /** A signed speed in m/s: "+45", "−9", "0". */
    public static String[] signedSpeed(double ms) {
        if (!Double.isFinite(ms)) {
            return new String[] {DASH, ""};
        }
        long whole = Math.round(ms);
        return new String[] {whole > 0 ? "+" + integer(whole) : integer(whole), "m/s"};
    }

    /** A 0..1 fraction as a whole percentage. */
    public static String percent(double fraction) {
        return Double.isFinite(fraction) ? Long.toString(Math.round(fraction * 100.0)) : DASH;
    }

    /** A whole number with a space between groups of three digits and a true minus. */
    public static String integer(long v) {
        String digits = Long.toString(Math.abs(v));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                b.append(' ');
            }
            b.append(digits.charAt(i));
        }
        return (v < 0 ? MINUS : "") + b;
    }

    private static String minus(String s) {
        return s.startsWith("-") ? MINUS + s.substring(1) : s;
    }
}
