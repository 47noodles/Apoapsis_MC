package dev.apoapsis.client.map3d;

import java.util.Locale;

/**
 * Number formats for the 3D map's labels: spaced caps for names, a distance in m or km, and a clock. The 2D map's
 * formatter is package-private to its own package, so these are the small subset this view needs. Client only.
 */
final class Fmt3d {
    private Fmt3d() {}

    /** Upper case with a space between letters, and three between words, as the map's names are set. */
    static String spaced(String s) {
        String up = s.toUpperCase(Locale.ROOT);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < up.length(); i++) {
            char ch = up.charAt(i);
            if (ch == ' ') {
                b.append("   ");
                continue;
            }
            b.append(ch);
            if (i + 1 < up.length() && up.charAt(i + 1) != ' ') {
                b.append(' ');
            }
        }
        return b.toString();
    }

    /** A distance in metres, as "850 m" under a kilometre and "12.3 km" above. */
    static String distance(double m) {
        if (!Double.isFinite(m)) {
            return "—";
        }
        if (Math.abs(m) < 1000) {
            return String.format(Locale.ROOT, "%.0f m", m);
        }
        return String.format(Locale.ROOT, "%.1f km", m / 1000);
    }

    /** A time span in seconds as "H:MM:SS", with a leading minus when negative. */
    static String clock(double seconds) {
        if (!Double.isFinite(seconds)) {
            return "—";
        }
        long total = Math.round(Math.abs(seconds));
        String sign = seconds < 0 ? "-" : "";
        return String.format(Locale.ROOT, "%s%d:%02d:%02d", sign, total / 3600, (total / 60) % 60, total % 60);
    }
}
