package dev.apoapsis.client.map3d;

import java.util.Locale;

/**
 * The 3D map's clock format: a time span in seconds as H:MM:SS. The other label formats are the 2D map's {@code Fmt},
 * which this view reuses. Client only.
 */
final class Fmt3d {
    private Fmt3d() {}

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
