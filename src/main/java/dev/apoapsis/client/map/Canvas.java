package dev.apoapsis.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Vector shapes on top of {@link GuiGraphicsExtractor}, which has no line or circle primitive in 26.3. Everything is
 * drawn as fills: a line is a run of horizontal or vertical spans (consecutive pixels in the same row or column
 * merge into one fill), a disc is one span per row. Coordinates are GUI pixels; lines are clipped to the view before
 * rasterising, so a far-off point costs nothing. Counts its fills so the map can budget a frame. Client only.
 */
final class Canvas {
    private static final int MIN_CIRCLE_POINTS = 48;
    private static final int MAX_CIRCLE_POINTS = 1024;

    private final GuiGraphicsExtractor g;
    final int width;
    final int height;
    final Font font;
    private int fills;

    Canvas(GuiGraphicsExtractor g) {
        this.g = g;
        this.width = g.guiWidth();
        this.height = g.guiHeight();
        this.font = Minecraft.getInstance().font;
    }

    /** Fills drawn through this canvas so far. */
    int fills() {
        return fills;
    }

    /** A filled rectangle, x1 and y1 exclusive. */
    void rect(int x0, int y0, int x1, int y1, int argb) {
        fill(x0, y0, x1, y1, argb);
    }

    /** A one-pixel line between two points. Clips to the view and rasterises with merged runs. */
    void line(double ax, double ay, double bx, double by, int argb) {
        if (!(Double.isFinite(ax) && Double.isFinite(ay) && Double.isFinite(bx) && Double.isFinite(by))) {
            return;
        }
        double[] seg = clip(ax, ay, bx, by);
        if (seg == null) {
            return;
        }
        int x0 = (int) Math.round(seg[0]);
        int y0 = (int) Math.round(seg[1]);
        int x1 = (int) Math.round(seg[2]);
        int y1 = (int) Math.round(seg[3]);
        int dx = x1 - x0;
        int dy = y1 - y0;
        if (Math.abs(dx) >= Math.abs(dy)) {
            // Walk columns; a run is consecutive columns sharing a row.
            int steps = Math.abs(dx);
            if (steps == 0) {
                fill(x0, y0, x0 + 1, y0 + 1, argb);
                return;
            }
            int sx = Integer.signum(dx);
            int runStart = x0;
            int runY = y0;
            for (int i = 1; i <= steps; i++) {
                int x = x0 + i * sx;
                int y = y0 + (int) Math.round((double) dy * i / steps);
                if (y != runY) {
                    span(runStart, x - sx, runY, argb);
                    runStart = x;
                    runY = y;
                }
            }
            span(runStart, x1, runY, argb);
        } else {
            // Walk rows; a run is consecutive rows sharing a column.
            int steps = Math.abs(dy);
            int sy = Integer.signum(dy);
            int runStart = y0;
            int runX = x0;
            for (int i = 1; i <= steps; i++) {
                int y = y0 + i * sy;
                int x = x0 + (int) Math.round((double) dx * i / steps);
                if (x != runX) {
                    column(runStart, y - sy, runX, argb);
                    runStart = y;
                    runX = x;
                }
            }
            column(runStart, y1, runX, argb);
        }
    }

    /** Joins consecutive points of a polyline. {@code n} is the point count. */
    void polyline(double[] xs, double[] ys, int n, int argb) {
        for (int k = 1; k < n; k++) {
            line(xs[k - 1], ys[k - 1], xs[k], ys[k], argb);
        }
    }

    /** A closed circle outline, one pixel thick. */
    void ring(double cx, double cy, double r, int argb) {
        dashedRing(cx, cy, r, argb, 0, MAX_CIRCLE_POINTS);
    }

    /** A closed circle outline with at most {@code maxSegments} segments. */
    void ring(double cx, double cy, double r, int argb, int maxSegments) {
        dashedRing(cx, cy, r, argb, 0, maxSegments);
    }

    /** A circle outline. With {@code dashPx} above zero it draws dashes of that length with equal gaps. */
    void dashedRing(double cx, double cy, double r, int argb, double dashPx) {
        dashedRing(cx, cy, r, argb, dashPx, MAX_CIRCLE_POINTS);
    }

    private void dashedRing(double cx, double cy, double r, int argb, double dashPx, int maxSegments) {
        if (!(r > 0) || !Double.isFinite(r) || !Double.isFinite(cx) || !Double.isFinite(cy)) {
            return;
        }
        int n = circlePoints(r, maxSegments);
        double segLen = 2 * Math.PI * r / n;
        double px = cx + r;
        double py = cy;
        for (int i = 1; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            double qx = cx + r * Math.cos(a);
            double qy = cy + r * Math.sin(a);
            boolean on = dashPx <= 0 || ((int) ((i - 1) * segLen / dashPx)) % 2 == 0;
            if (on) {
                line(px, py, qx, qy, argb);
            }
            px = qx;
            py = qy;
        }
    }

    /** A filled disc: one span per visible row. */
    void disc(double cx, double cy, double r, int argb) {
        if (!(r > 0) || !Double.isFinite(r) || !Double.isFinite(cx) || !Double.isFinite(cy)) {
            return;
        }
        int yFrom = Math.max(0, (int) Math.floor(cy - r));
        int yTo = Math.min(height - 1, (int) Math.ceil(cy + r));
        for (int y = yFrom; y <= yTo; y++) {
            double dy = (y + 0.5) - cy;
            double d2 = r * r - dy * dy;
            if (d2 < 0) {
                continue;
            }
            double half = Math.sqrt(d2);
            int xa = Math.max(0, (int) Math.round(cx - half));
            int xb = Math.min(width - 1, (int) Math.round(cx + half));
            if (xa <= xb) {
                fill(xa, y, xb + 1, y + 1, argb);
            }
        }
    }

    /** A filled triangle by scanline. */
    void triangle(double[] px, double[] py, int argb) {
        double minY = Math.min(py[0], Math.min(py[1], py[2]));
        double maxY = Math.max(py[0], Math.max(py[1], py[2]));
        if (!Double.isFinite(minY) || !Double.isFinite(maxY)) {
            return;
        }
        int yFrom = Math.max(0, (int) Math.floor(minY));
        int yTo = Math.min(height - 1, (int) Math.ceil(maxY));
        for (int y = yFrom; y <= yTo; y++) {
            double yc = y + 0.5;
            double xl = Double.POSITIVE_INFINITY;
            double xr = Double.NEGATIVE_INFINITY;
            for (int e = 0; e < 3; e++) {
                int f = (e + 1) % 3;
                double ay = py[e];
                double by = py[f];
                if ((ay <= yc && by > yc) || (by <= yc && ay > yc)) {
                    double x = px[e] + (yc - ay) * (px[f] - px[e]) / (by - ay);
                    xl = Math.min(xl, x);
                    xr = Math.max(xr, x);
                }
            }
            if (xl <= xr) {
                int a = Math.max(0, (int) Math.round(xl));
                int b = Math.min(width - 1, (int) Math.round(xr));
                if (a <= b) {
                    fill(a, y, b + 1, y + 1, argb);
                }
            }
        }
    }

    /** Text at the given size, where {@code scale} 1 is the default font. Shadow off reads cleaner on the map. */
    void text(Component c, int x, int y, float scale, int argb, boolean shadow) {
        if (scale == 1f) {
            g.text(font, c, x, y, argb, shadow);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, c, 0, 0, argb, shadow);
        g.pose().popMatrix();
    }

    /** Width of a line of text at scale 1, GUI px. */
    int textWidth(Component c) {
        return font.width(c);
    }

    private void fill(int x0, int y0, int x1, int y1, int argb) {
        fills++;
        g.fill(x0, y0, x1, y1, argb);
    }

    private void span(int a, int b, int y, int argb) {
        fill(Math.min(a, b), y, Math.max(a, b) + 1, y + 1, argb);
    }

    private void column(int a, int b, int x, int argb) {
        fill(x, Math.min(a, b), x + 1, Math.max(a, b) + 1, argb);
    }

    private static int circlePoints(double r, int maxSegments) {
        double want = Math.PI * r;
        int cap = Math.min(MAX_CIRCLE_POINTS, maxSegments);
        return (int) Math.min(cap, Math.max(MIN_CIRCLE_POINTS, want));
    }

    /** Liang-Barsky clip to the view. Returns the clipped endpoints, or null when nothing is visible. */
    private double[] clip(double x0, double y0, double x1, double y1) {
        double xmin = 0;
        double xmax = width - 1;
        double ymin = 0;
        double ymax = height - 1;
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - xmin, xmax - x0, y0 - ymin, ymax - y0};
        double t0 = 0;
        double t1 = 1;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return null;
                }
            } else {
                double r = q[i] / p[i];
                if (p[i] < 0) {
                    if (r > t1) {
                        return null;
                    }
                    t0 = Math.max(t0, r);
                } else {
                    if (r < t0) {
                        return null;
                    }
                    t1 = Math.min(t1, r);
                }
            }
        }
        return new double[] {x0 + t0 * dx, y0 + t0 * dy, x0 + t1 * dx, y0 + t1 * dy};
    }
}
