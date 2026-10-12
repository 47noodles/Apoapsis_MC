package dev.apoapsis.client.map;

import dev.apoapsis.client.map3d.TexturedSphere;
import dev.apoapsis.client.ui.UiPalette;

/**
 * The geometry of one frame of either map: the two quad layers around the bodies, the bodies' sphere meshes, and the
 * look the maps share (the sun, the atmosphere, line widths). The 3D map projects through a camera and the 2D map
 * straight from above, but both put what they draw here, so a body, an orbit and a marker look the same in both.
 * Coordinates are UI units from the top left of the view. Client only.
 */
public final class MapScene {
    /** The sun's direction in the root frame (a unit vector): a body is lit from +X. */
    public static final double SUN_X = 1.0;
    /** Brightness of the unlit side of a body, 0..1: enough to show its outline. */
    public static final double AMBIENT = 0.16;
    /** How far past the terminator the light wraps, so the edge of the lit side is soft. */
    private static final double WRAP = 0.14;

    /** Half width of a current orbit's core, UI units (1.4 device pixels at 1080p). */
    public static final double ORBIT_HALF = 0.28;
    /** Half width of a quiet line (a grid ring, a sphere of influence). */
    public static final double THIN_HALF = 0.16;
    /** The soft edge of every line, UI units. */
    public static final double FEATHER = 0.34;

    /** Atmosphere colour: pale hydrogen blue. */
    public static final int HAZE_RGB = UiPalette.HYDROGEN & 0xFFFFFF;
    /** Haze alpha at the limb on the lit side, 0..255. */
    private static final int HAZE_ALPHA = 0x70;

    /** Behind the bodies. */
    public final QuadBatch back = new QuadBatch();
    /** In front of the bodies. */
    public final QuadBatch front = new QuadBatch();
    /** The textured bodies' meshes by body (0 Verdance, 1 Pale), and the bodies to draw, farthest first. */
    public final TexturedSphere[] mesh = {new TexturedSphere(), new TexturedSphere()};
    public final int[] order = new int[2];
    public int meshCount;

    private double viewW = 640;
    private double viewH = 360;

    /** Starts a frame for a view of {@code w} by {@code h} UI units. */
    public void begin(double w, double h) {
        viewW = w;
        viewH = h;
        back.begin(w, h);
        front.begin(w, h);
        meshCount = 0;
    }

    public double viewWidth() {
        return viewW;
    }

    public double viewHeight() {
        return viewH;
    }

    /**
     * {@code rgb} lit by the sun on a surface point: {@code sunDot} is its normal's x component (the sun is on +X) and
     * {@code viewDot} the cosine of the angle between the normal and the way to the viewer. A cool blue shadow, a soft
     * terminator and a slight darkening toward the limb, as the menus' planet. Opaque.
     */
    public static int shade(int rgb, double sunDot, double viewDot) {
        double t = Math.max(0, Math.min(1, (sunDot + WRAP) / (1 + WRAP)));
        double day = Math.pow(t, 0.85);
        double limb = 0.74 + 0.26 * Math.sqrt(Math.max(0, Math.min(1, viewDot)));
        double lr = (AMBIENT * 0.55 + (1 - AMBIENT * 0.55) * day) * limb;
        double lg = (AMBIENT * 0.75 + (1 - AMBIENT * 0.75) * day) * limb;
        double lb = (AMBIENT + (1 - AMBIENT) * day) * limb;
        int r = (int) Math.round(((rgb >> 16) & 0xFF) * lr);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) * lg);
        int b = (int) Math.round((rgb & 0xFF) * lb);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * Adds a body's atmosphere and limb to the front layer: a soft haze from the disc out to {@code topPx}, brightest on
     * the side toward the sun, and a thin bright rim just inside the limb on that side, so the lit edge reads like
     * air. {@code sunX} and {@code sunY} are the sun's direction on screen (any length; zero means the sun is behind
     * the viewer and the haze is even); {@code lit} is 0..1, how much of the sun's light the near side gets.
     */
    public void atmosphere(double cx, double cy, double rPx, double topPx, double sunX, double sunY, double lit) {
        double len = Math.hypot(sunX, sunY);
        double ux = len > 1e-9 ? sunX / len : 0;
        double uy = len > 1e-9 ? sunY / len : 0;
        // How one-sided the light is on screen: a sun off to the side (len 1) shades the far limb; a sun behind us does not.
        double side = Math.min(1, len);
        int segments = QuadBatch.segmentsFor(topPx);
        for (int i = 0; i < segments; i++) {
            double a0 = 2 * Math.PI * i / segments;
            double a1 = 2 * Math.PI * (i + 1) / segments;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            int h0 = hazeAlpha(c0 * ux + s0 * uy, side, lit);
            int h1 = hazeAlpha(c1 * ux + s1 * uy, side, lit);
            if (topPx > rPx) {
                front.quad(cx + rPx * c0, cy + rPx * s0, h0 << 24 | HAZE_RGB, cx + rPx * c1, cy + rPx * s1,
                        h1 << 24 | HAZE_RGB, cx + topPx * c1, cy + topPx * s1, HAZE_RGB, cx + topPx * c0,
                        cy + topPx * s0, HAZE_RGB);
            }
            // The rim inside the limb: a short fade inward from the edge.
            double inner = rPx - Math.max(1.2, rPx * 0.05);
            int r0 = (int) (h0 * 0.9);
            int r1 = (int) (h1 * 0.9);
            front.quad(cx + inner * c0, cy + inner * s0, HAZE_RGB, cx + inner * c1, cy + inner * s1, HAZE_RGB,
                    cx + rPx * c1, cy + rPx * s1, r1 << 24 | HAZE_RGB, cx + rPx * c0, cy + rPx * s0,
                    r0 << 24 | HAZE_RGB);
        }
    }

    private static int hazeAlpha(double facing, double side, double lit) {
        double k = (0.35 + 0.65 * Math.max(0, facing)) * side + (1 - side);
        return (int) Math.round(HAZE_ALPHA * k * (0.55 + 0.45 * lit));
    }

    /** A thin pale ring on a body's limb in the front layer: shows the dark side's outline against the stars. */
    public void limb(double cx, double cy, double rPx) {
        front.ring(cx, cy, rPx, THIN_HALF, FEATHER, UiPalette.alpha(UiPalette.STEEL_500, 0x70), 0, 0);
    }
}
