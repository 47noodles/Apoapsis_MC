package dev.apoapsis.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The navball as a lit glass sphere in a brass bezel. It is painted per device pixel into a {@link HudSprite} whenever
 * the ship's attitude changes: sky and ground shaded like a ball, a horizon line, a pitch ladder (parallels every 15
 * degrees, the 30 and 60 stronger), heading ticks along the horizon, the prograde and retrograde markers, and the fixed
 * nose symbol. Lines are measured in screen pixels from the sphere's own gradient, so they keep one width across the
 * ball. Sizes are UI units, so the ball is the same share of the view at every window size. Client only.
 */
public final class NavballView {
    /** Width of the brass bezel, UI units. */
    public static final int BEZEL = 3;

    private static final int SKY_ZENITH = 0xFF173F63;
    private static final int SKY_HORIZON = 0xFF3F8DB0;
    private static final int GROUND_HORIZON = 0xFF9A6034;
    private static final int GROUND_NADIR = 0xFF402818;
    private static final int LINE = UiPalette.INK;
    private static final int MARKER = UiPalette.SIGNAL;
    private static final int NOSE = 0xFFE9894C;
    /** Direction to the light, ball coordinates (right, up, towards the viewer), normalised in the static block. */
    private static final double[] LIGHT = normalise(-0.42, 0.55, 0.72);
    private static final double[] HALF = normalise(LIGHT[0], LIGHT[1], LIGHT[2] + 1.0);

    /** Sines of the ladder's parallels: 15, 30, 45, 60 and 75 degrees; the 30 and 60 are the stronger lines. */
    private static final double[] PARALLEL_SIN = {Math.sin(Math.toRadians(15)), Math.sin(Math.toRadians(30)),
            Math.sin(Math.toRadians(45)), Math.sin(Math.toRadians(60)), Math.sin(Math.toRadians(75))};

    private static final HudSprite SPRITE = new HudSprite("navball");
    private static final double[] LAST = new double[16];
    private static boolean haveLast;

    private NavballView() {}

    private static double[] normalise(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[] {x / l, y / l, z / l};
    }

    /**
     * Draws the ball with its centre at ({@code cx}, {@code cy}) UI units and ball radius {@code radius} UI units; the
     * bezel is added outside it. Must be called between {@link UiScale#begin} and {@link UiScale#end}.
     *
     * @param axes the ship's axes from {@link Navball#axes}
     * @param up the local vertical in dimension coordinates, unit length
     * @param vel the velocity direction in dimension coordinates, unit length, or null when too slow
     */
    public static void draw(GuiGraphicsExtractor g, UiScale s, int cx, int cy, int radius, double[] axes, double[] up,
                            double[] vel) {
        int unit = s.unit();
        int rb = radius * unit;
        int rt = (radius + BEZEL) * unit;
        int size = 2 * rt;
        double[] upB = new double[3];
        Navball.toBall(axes, up[0], up[1], up[2], upB);
        double[] nh = new double[6];
        Navball.horizon(up[0], up[1], up[2], nh);
        double[] northB = new double[3];
        double[] eastB = new double[3];
        Navball.toBall(axes, nh[0], nh[1], nh[2], northB);
        Navball.toBall(axes, nh[3], nh[4], nh[5], eastB);
        double[] velB = null;
        if (vel != null) {
            velB = new double[3];
            Navball.toBall(axes, vel[0], vel[1], vel[2], velB);
        }
        double[] key = {upB[0], upB[1], upB[2], northB[0], northB[1], northB[2], velB == null ? 9 : velB[0],
                velB == null ? 9 : velB[1], velB == null ? 9 : velB[2], unit, radius, 0, 0, 0, 0, 0};
        if (!SPRITE.has(size, size) || !same(key)) {
            paint(SPRITE.pixels(size, size), size, rb, rt, unit, upB, northB, eastB, velB);
            SPRITE.upload();
            System.arraycopy(key, 0, LAST, 0, key.length);
            haveLast = true;
        }
        int r = radius + BEZEL;
        g.blit(SPRITE.id(), cx - r, cy - r, cx + r, cy + r, 0f, 1f, 0f, 1f);
    }

    private static boolean same(double[] key) {
        if (!haveLast) {
            return false;
        }
        for (int i = 0; i < key.length; i++) {
            if (Math.abs(key[i] - LAST[i]) > 2e-4) {
                return false;
            }
        }
        return true;
    }

    private static void paint(NativeImage img, int size, int rb, int rt, int unit, double[] u, double[] north,
                              double[] east, double[] vel) {
        double lineHw = 0.2 * unit;
        double[][] ticks = tickPlanes(north, east);
        double mR = 3.6 * unit;
        double mStroke = 0.28 * unit;
        for (int py = 0; py < size; py++) {
            double dy = py + 0.5 - rt;
            for (int px = 0; px < size; px++) {
                double dx = px + 0.5 - rt;
                double rr = Math.sqrt(dx * dx + dy * dy);
                if (rr > rt + 0.5) {
                    img.setPixel(px, py, 0);
                    continue;
                }
                double nx = dx / rb;
                double ny = -dy / rb;
                int colour = 0;
                if (rr < rb + 1.0) {
                    colour = ball(nx, ny, u, ticks, rb, lineHw);
                    colour = markers(colour, dx, dy, vel, rb, unit, mR, mStroke);
                }
                if (rr > rb - 1.0) {
                    int bezel = bezel(dx, dy, rr, rb, rt, unit);
                    float k = (float) Math.max(0.0, Math.min(1.0, rr - (rb - 0.5)));
                    colour = rr < rb - 0.5 ? colour : HudSprite.over(colour, bezel & 0xFFFFFF, k);
                }
                float outer = (float) Math.max(0.0, Math.min(1.0, rt + 0.5 - rr));
                int a = Math.round(((colour >>> 24) & 0xFF) * outer);
                img.setPixel(px, py, (a << 24) | (colour & 0xFFFFFF));
            }
        }
    }

    /** The 36 heading planes through the pole axis: for each, (mx, my, mz) with the ball-coordinate normal. */
    private static double[][] tickPlanes(double[] north, double[] east) {
        double[][] planes = new double[36][3];
        for (int k = 0; k < 36; k++) {
            double a = Math.toRadians(k * 10.0);
            double c = Math.cos(a);
            double sn = Math.sin(a);
            for (int i = 0; i < 3; i++) {
                planes[k][i] = c * east[i] - sn * north[i];
            }
        }
        return planes;
    }

    private static int ball(double nx, double ny, double[] u, double[][] ticks, int rb, double lineHw) {
        double r2 = Math.min(nx * nx + ny * ny, 0.9999);
        double d = Math.sqrt(1 - r2);
        double dd = Math.max(d, 0.08);
        double f = u[0] * nx + u[1] * ny + u[2] * d;
        double gx = u[0] - u[2] * nx / dd;
        double gy = u[1] - u[2] * ny / dd;
        double grad = Math.max(0.02, Math.hypot(gx, gy));
        double pxPerF = rb / grad;

        int base;
        if (f >= 0) {
            base = UiPalette.mix(SKY_HORIZON, SKY_ZENITH, (float) Math.sqrt(Math.min(1, f)));
        } else {
            base = UiPalette.mix(GROUND_HORIZON, GROUND_NADIR, (float) Math.sqrt(Math.min(1, -f)));
        }
        double lam = Math.max(0, nx * LIGHT[0] + ny * LIGHT[1] + d * LIGHT[2]);
        float rim = (float) (0.6 + 0.4 * Math.sqrt(d));
        int colour = HudSprite.shade(base, (float) ((0.58 + 0.5 * lam) * rim));
        double h1 = Math.max(0, nx * HALF[0] + ny * HALF[1] + d * HALF[2]);
        double h2 = h1 * h1;
        double h4 = h2 * h2;
        double h8 = h4 * h4;
        double h16 = h8 * h8;
        double spec = h16 * h16 * h4 * 0.3;
        colour = UiPalette.mix(colour, 0xFFFFFFFF, (float) spec);

        float lineAlpha = rim * (float) Math.max(0.0, Math.min(1.0, (d - 0.07) / 0.28));
        // The horizon, and the pitch ladder either side of it.
        float cov = HudSprite.stroke(f * pxPerF, lineHw * 1.5);
        if (cov > 0) {
            colour = HudSprite.over(colour, LINE, cov * lineAlpha);
        }
        for (int i = 0; i < PARALLEL_SIN.length; i++) {
            double sinP = PARALLEL_SIN[i];
            float strength = i % 2 == 1 ? 0.5f : 0.22f;
            float c2 = Math.max(HudSprite.stroke((f - sinP) * pxPerF, lineHw),
                    HudSprite.stroke((f + sinP) * pxPerF, lineHw));
            if (c2 > 0) {
                colour = HudSprite.over(colour, LINE, c2 * strength * lineAlpha);
            }
        }
        // Heading ticks across the horizon: 10 degrees short, 30 longer, the cardinals longest.
        if (Math.abs(f) < 0.2) {
            double px = nx;
            double py = ny;
            for (int k = 0; k < 36; k++) {
                double reach = k % 9 == 0 ? 0.2 : k % 3 == 0 ? 0.12 : 0.065;
                if (Math.abs(f) > reach) {
                    continue;
                }
                double[] m = ticks[k];
                double h = m[0] * px + m[1] * py + m[2] * d;
                double hgx = m[0] - m[2] * nx / dd;
                double hgy = m[1] - m[2] * ny / dd;
                double hg = Math.max(0.02, Math.hypot(hgx, hgy));
                float c3 = HudSprite.stroke(h * rb / hg, lineHw);
                if (c3 > 0) {
                    colour = HudSprite.over(colour, LINE, c3 * 0.85f * lineAlpha);
                }
            }
        }
        return colour;
    }

    /** The prograde and retrograde markers, and the fixed nose symbol, over {@code colour}. */
    private static int markers(int colour, double dx, double dy, double[] vel, int rb, int unit, double mR,
                               double mStroke) {
        if (vel != null && vel[2] > 0) {
            double mx = vel[0] * rb;
            double my = -vel[1] * rb;
            colour = marker(colour, dx - mx, dy - my, mR, mStroke, unit, false);
        }
        if (vel != null && vel[2] < 0) {
            // The retrograde point is on the near side when the prograde one is on the far side.
            double mx = -vel[0] * rb;
            double my = vel[1] * rb;
            colour = marker(colour, dx - mx, dy - my, mR, mStroke, unit, true);
        }
        // The nose: a small ring with short wings, fixed at the centre of the glass.
        double ring = 1.5 * unit;
        float c = HudSprite.stroke(Math.hypot(dx, dy) - ring, mStroke * 1.2);
        double wing = Math.min(
                HudSprite.segment(Math.abs(dx), dy, 4.0 * unit, 0, 10.0 * unit, 0),
                HudSprite.segment(Math.abs(dx), dy, 10.0 * unit, 0, 10.0 * unit, 1.8 * unit));
        c = Math.max(c, HudSprite.stroke(wing, mStroke * 1.4));
        return HudSprite.over(colour, NOSE & 0xFFFFFF, c);
    }

    private static int marker(int colour, double x, double y, double r, double stroke, int unit, boolean retro) {
        if (Math.abs(x) > r + 4.5 * unit || Math.abs(y) > r + 4.5 * unit) {
            return colour;
        }
        float c = HudSprite.stroke(Math.hypot(x, y) - r, stroke);
        if (retro) {
            double cross = r * 0.62;
            double dist = Math.min(HudSprite.segment(x, y, -cross, -cross, cross, cross),
                    HudSprite.segment(x, y, -cross, cross, cross, -cross));
            c = Math.max(c, HudSprite.stroke(dist, stroke));
        } else {
            double ticks = Math.min(HudSprite.segment(x, y, -r - 3.2 * unit, 0, -r - 0.8 * unit, 0),
                    Math.min(HudSprite.segment(x, y, r + 0.8 * unit, 0, r + 3.2 * unit, 0),
                            HudSprite.segment(x, y, 0, -r - 3.2 * unit, 0, -r - 0.8 * unit)));
            c = Math.max(c, HudSprite.stroke(ticks, stroke));
        }
        return HudSprite.over(colour, MARKER & 0xFFFFFF, c);
    }

    /** The brass bezel: a bevel lit from the upper left, a dark line at each edge and eight rivets. */
    private static int bezel(double dx, double dy, double rr, int rb, int rt, int unit) {
        double t = (rr - rb) / (rt - rb);
        t = Math.max(0, Math.min(1, t));
        double dirx = dx / Math.max(rr, 1e-6);
        double diry = -dy / Math.max(rr, 1e-6);
        double light = 0.5 + 0.5 * (dirx * -0.6 + diry * 0.8);
        double profile = 1.0 - Math.abs(2 * t - 1) * 0.55;
        float k = (float) (0.5 + 0.75 * light * profile);
        int colour = HudSprite.shade(UiPalette.BRASS, k);
        double edge = Math.min(rr - rb, rt - rr);
        if (edge < 1.0) {
            colour = UiPalette.mix(colour, UiPalette.STEEL_950, (float) (1.0 - Math.max(0, edge)) * 0.8f);
        }
        double mid = (rb + rt) / 2.0;
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(22.5 + 45 * i);
            double rdx = dx - mid * Math.cos(a);
            double rdy = dy + mid * Math.sin(a);
            double rd = Math.hypot(rdx, rdy);
            if (rd < 0.6 * unit + 1) {
                float cov = HudSprite.stroke(rd - 0.45 * unit, 0.15 * unit);
                float lit = (float) Math.max(0, -(rdx * -0.6 + rdy * 0.8)) / (float) (0.6 * unit);
                int rivet = UiPalette.mix(UiPalette.BRASS_DEEP, UiPalette.STEEL_900, 0.35f + 0.3f * (1 - lit));
                colour = UiPalette.mix(colour, rivet, Math.max(cov, rd < 0.45 * unit ? 1f : 0f) * 0.9f);
            }
        }
        return 0xFF000000 | (colour & 0xFFFFFF);
    }
}
