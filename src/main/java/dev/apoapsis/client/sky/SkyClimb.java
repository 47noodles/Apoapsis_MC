package dev.apoapsis.client.sky;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The climb from a surface to its orbit as one share, 0 to 1: 0 up to {@link #START_M} above the surface, 1 at the
 * transition altitude, smooth in between. The planet below, the sky darkening and the fog all follow it. Pure maths
 * and plain colours, no Minecraft types. Client only.
 */
public final class SkyClimb {
    /** Altitude above the surface where the planet below starts to fade in and the sky starts to darken, metres. */
    public static final double START_M = 1000.0;
    /** Verdance's low sky: a slight teal pull, faded out by the transition. */
    private static final Vector3f TINT = new Vector3f(0x7F / 255f, 0xC7 / 255f, 0xB8 / 255f);
    private static final float TINT_AMOUNT = 0.25f;

    private SkyClimb() {}

    /** The climb's share at {@code altitude} metres above the surface, for a transition at {@code transition} metres. */
    public static double share(double altitude, double transition) {
        double t = Math.max(0, Math.min(1, (altitude - START_M) / (transition - START_M)));
        return t * t * (3 - 2 * t);
    }

    /**
     * A surface sky colour at climb share {@code share}: Verdance's sky (when {@code verdance}) is pulled teal, then the
     * whole sky goes toward black. A new vector; {@code base} is not changed.
     */
    public static Vector3f sky(Vector3fc base, boolean verdance, double share) {
        Vector3f c = new Vector3f(base);
        if (verdance) {
            c.lerp(TINT, (float) (TINT_AMOUNT * (1 - share)));
        }
        return c.mul((float) (1 - share));
    }
}
