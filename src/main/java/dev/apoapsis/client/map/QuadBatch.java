package dev.apoapsis.client.map;

/**
 * A batch of screen-space quads for the map's picture-in-picture layers (4 vertices each, ARGB per vertex, coordinates in
 * UI units from the view's top left), with the vector shapes the maps need built on it: anti-aliased lines, dashes,
 * rings, discs and fading annuli. The picture in picture has no multisampling, so every edge is feathered: a line is a
 * solid core with a strip either side whose alpha falls to zero, and a polyline is one continuous strip with mitred
 * joins, so a translucent line has no bright spots where its segments meet. Widths are in UI units and so stay the same
 * at any zoom and any GUI scale. Pure maths, no game state. Client only.
 */
public final class QuadBatch {
    /** Capacity in vertices. */
    public static final int MAX_VERTS = 1 << 18;
    /** Longest chord of a drawn circle, UI units: keeps a ring round at the sizes the maps use. */
    private static final double CHORD = 3.0;
    /** The most a mitre may stretch a join, as a multiple of the half width. */
    private static final double MITRE_LIMIT = 2.5;

    public final float[] x = new float[MAX_VERTS];
    public final float[] y = new float[MAX_VERTS];
    public final int[] argb = new int[MAX_VERTS];
    /** Vertices in use; always a multiple of four. */
    public int count;

    /** The view in UI units, for clipping circles. */
    private double viewW = 640;
    private double viewH = 360;
    private double[] px = new double[64];
    private double[] py = new double[64];

    /** Empties the batch and sets the view's size. */
    public void begin(double viewWidth, double viewHeight) {
        count = 0;
        viewW = viewWidth;
        viewH = viewHeight;
    }

    public double viewWidth() {
        return viewW;
    }

    public double viewHeight() {
        return viewH;
    }

    private static int clear(int argb) {
        return argb & 0x00FFFFFF;
    }

    /** One vertex; dropped when the batch is full. */
    private void put(double vx, double vy, int c) {
        x[count] = (float) vx;
        y[count] = (float) vy;
        argb[count] = c;
        count++;
    }

    private boolean room(int quads) {
        return count + 4 * quads <= MAX_VERTS;
    }

    /** A quad with a colour per corner, in order round its edge. */
    public void quad(double x0, double y0, int c0, double x1, double y1, int c1, double x2, double y2, int c2,
            double x3, double y3, int c3) {
        if (!room(1)) {
            return;
        }
        put(x0, y0, c0);
        put(x1, y1, c1);
        put(x2, y2, c2);
        put(x3, y3, c3);
    }

    /** A triangle (a quad with a repeated corner). */
    public void triangle(double x0, double y0, int c0, double x1, double y1, int c1, double x2, double y2, int c2) {
        quad(x0, y0, c0, x1, y1, c1, x2, y2, c2, x2, y2, c2);
    }

    /** A flat axis-aligned rectangle. */
    public void rect(double x0, double y0, double x1, double y1, int c) {
        quad(x0, y0, c, x1, y0, c, x1, y1, c, x0, y1, c);
    }

    /** Number of segments that keep a circle of {@code r} UI units round. */
    public static int segmentsFor(double r) {
        return (int) Math.max(24, Math.min(512, Math.ceil(2 * Math.PI * r / CHORD)));
    }

    // ---- lines ----

    /** One straight line from (ax, ay) to (bx, by). {@code halfW} is half the core width, {@code feather} the soft edge. */
    public void line(double ax, double ay, double bx, double by, double halfW, double feather, int c) {
        ensure(2);
        px[0] = ax;
        py[0] = ay;
        px[1] = bx;
        py[1] = by;
        strip(2, halfW, feather, c, false);
    }

    /** An open polyline of the first {@code n} points. A NaN or infinite point breaks it into separate runs. */
    public void polyline(double[] xs, double[] ys, int n, double halfW, double feather, int c) {
        int start = 0;
        for (int i = 0; i <= n; i++) {
            boolean valid = i < n && Double.isFinite(xs[i]) && Double.isFinite(ys[i]);
            if (!valid) {
                if (i - start >= 2) {
                    run(xs, ys, start, i, halfW, feather, c);
                }
                start = i + 1;
            }
        }
    }

    /** A polyline dashed by arc length: {@code dash} drawn, {@code gap} skipped, from the first point. */
    public void dashedPolyline(double[] xs, double[] ys, int n, double halfW, double feather, double dash, double gap,
            int c) {
        double period = dash + gap;
        double arc = 0;
        int fill = 0;
        boolean inDash = false;
        for (int i = 0; i < n; i++) {
            if (!(Double.isFinite(xs[i]) && Double.isFinite(ys[i]))) {
                flushDash(fill, halfW, feather, c);
                fill = 0;
                inDash = false;
                continue;
            }
            if (i == 0 || !(Double.isFinite(xs[i - 1]) && Double.isFinite(ys[i - 1]))) {
                // A fresh run starts a fresh pattern.
                arc = 0;
                ensure(2);
                inDash = true;
                fill = 0;
                px[fill] = xs[i];
                py[fill] = ys[i];
                fill++;
                continue;
            }
            double ax = xs[i - 1];
            double ay = ys[i - 1];
            double len = Math.hypot(xs[i] - ax, ys[i] - ay);
            if (len < 1e-9) {
                continue;
            }
            double done = 0;
            while (done < len) {
                double phase = arc % period;
                boolean on = phase < dash;
                double toEdge = on ? dash - phase : period - phase;
                double step = Math.min(toEdge, len - done);
                double t0 = done / len;
                double t1 = (done + step) / len;
                if (on) {
                    if (!inDash || fill == 0) {
                        fill = 0;
                        ensure(2);
                        px[0] = ax + (xs[i] - ax) * t0;
                        py[0] = ay + (ys[i] - ay) * t0;
                        fill = 1;
                        inDash = true;
                    }
                    ensure(fill + 1);
                    px[fill] = ax + (xs[i] - ax) * t1;
                    py[fill] = ay + (ys[i] - ay) * t1;
                    fill++;
                    if (step >= toEdge - 1e-9) {
                        flushDash(fill, halfW, feather, c);
                        fill = 0;
                        inDash = false;
                    }
                } else if (inDash) {
                    flushDash(fill, halfW, feather, c);
                    fill = 0;
                    inDash = false;
                }
                done += step;
                arc += step;
            }
        }
        flushDash(fill, halfW, feather, c);
    }

    private void flushDash(int fill, double halfW, double feather, int c) {
        if (fill >= 2) {
            strip(fill, halfW, feather, c, false);
        }
    }

    private void run(double[] xs, double[] ys, int from, int to, double halfW, double feather, int c) {
        int n = to - from;
        ensure(n);
        int k = 0;
        for (int i = from; i < to; i++) {
            if (k > 0 && Math.abs(xs[i] - px[k - 1]) < 1e-6 && Math.abs(ys[i] - py[k - 1]) < 1e-6) {
                continue;
            }
            px[k] = xs[i];
            py[k] = ys[i];
            k++;
        }
        if (k >= 2) {
            strip(k, halfW, feather, c, false);
        }
    }

    private void ensure(int n) {
        if (px.length < n) {
            int len = Math.max(n, px.length * 2);
            px = java.util.Arrays.copyOf(px, len);
            py = java.util.Arrays.copyOf(py, len);
        }
    }

    /**
     * Emits the strip along the first {@code n} points of the scratch arrays: four vertices a point (outer left, inner
     * left, inner right, outer right), three quads a segment. When {@code closed} the last point repeats the first and
     * the join at the seam is mitred like any other.
     */
    private void strip(int n, double halfW, double feather, int c, boolean closed) {
        if (n < 2 || !room(3 * (n - 1))) {
            return;
        }
        int solid = c;
        int soft = clear(c);
        double outer = halfW + feather;
        double lx = 0, ly = 0, ax = 0, ay = 0, bx = 0, by = 0, cx = 0, cy = 0, dx = 0, dy = 0;
        double pnx = 0, pny = 0;
        for (int k = 0; k < n; k++) {
            // Normal of the segment before and after this point.
            double sx = 0, sy = 0;
            if (k + 1 < n) {
                double ex = px[k + 1] - px[k], ey = py[k + 1] - py[k];
                double len = Math.hypot(ex, ey);
                sx = -ey / len;
                sy = ex / len;
            }
            double mx, my, miter = 1;
            if (k == 0) {
                if (closed) {
                    double ex = px[n - 1] - px[n - 2], ey = py[n - 1] - py[n - 2];
                    double len = Math.hypot(ex, ey);
                    double qx = -ey / len, qy = ex / len;
                    mx = qx + sx;
                    my = qy + sy;
                    double ml = Math.hypot(mx, my);
                    if (ml < 1e-6) {
                        mx = sx;
                        my = sy;
                    } else {
                        mx /= ml;
                        my /= ml;
                        miter = Math.min(MITRE_LIMIT, 1 / Math.max(1e-6, mx * sx + my * sy));
                    }
                } else {
                    mx = sx;
                    my = sy;
                }
            } else if (k == n - 1) {
                mx = pnx;
                my = pny;
                if (closed) {
                    // The seam: mitre with the first segment, as point 0 did.
                    double ex = px[1] - px[0], ey = py[1] - py[0];
                    double len = Math.hypot(ex, ey);
                    double qx = -ey / len, qy = ex / len;
                    double bxm = pnx + qx, bym = pny + qy;
                    double ml = Math.hypot(bxm, bym);
                    if (ml >= 1e-6) {
                        mx = bxm / ml;
                        my = bym / ml;
                        miter = Math.min(MITRE_LIMIT, 1 / Math.max(1e-6, mx * pnx + my * pny));
                    }
                }
            } else {
                mx = pnx + sx;
                my = pny + sy;
                double ml = Math.hypot(mx, my);
                if (ml < 1e-6) {
                    mx = sx;
                    my = sy;
                } else {
                    mx /= ml;
                    my /= ml;
                    miter = Math.min(MITRE_LIMIT, 1 / Math.max(1e-6, mx * sx + my * sy));
                }
            }
            double o = outer * miter;
            double i = halfW * miter;
            double nax = px[k] + mx * o, nay = py[k] + my * o;
            double nbx = px[k] + mx * i, nby = py[k] + my * i;
            double ncx = px[k] - mx * i, ncy = py[k] - my * i;
            double ndx = px[k] - mx * o, ndy = py[k] - my * o;
            if (k > 0) {
                put(ax, ay, soft);
                put(bx, by, solid);
                put(nbx, nby, solid);
                put(nax, nay, soft);
                put(bx, by, solid);
                put(cx, cy, solid);
                put(ncx, ncy, solid);
                put(nbx, nby, solid);
                put(cx, cy, solid);
                put(dx, dy, soft);
                put(ndx, ndy, soft);
                put(ncx, ncy, solid);
            }
            ax = nax;
            ay = nay;
            bx = nbx;
            by = nby;
            cx = ncx;
            cy = ncy;
            dx = ndx;
            dy = ndy;
            pnx = sx;
            pny = sy;
            lx = px[k];
            ly = py[k];
        }
    }

    // ---- circles ----

    /** A filled disc with a feathered rim: solid to {@code r}, fading to nothing by {@code r + feather}. */
    public void disc(double cx, double cy, double r, double feather, int c) {
        int n = segmentsFor(r + feather);
        int soft = clear(c);
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            triangle(cx, cy, c, cx + r * c0, cy + r * s0, c, cx + r * c1, cy + r * s1, c);
            if (feather > 0) {
                quad(cx + r * c0, cy + r * s0, c, cx + r * c1, cy + r * s1, c, cx + (r + feather) * c1,
                        cy + (r + feather) * s1, soft, cx + (r + feather) * c0, cy + (r + feather) * s0, soft);
            }
        }
    }

    /** An annulus from {@code r0} to {@code r1} whose colour runs from {@code c0} inside to {@code c1} outside. */
    public void annulus(double cx, double cy, double r0, double r1, int c0, int c1) {
        int n = segmentsFor(r1);
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
            double co0 = Math.cos(a0), si0 = Math.sin(a0), co1 = Math.cos(a1), si1 = Math.sin(a1);
            quad(cx + r0 * co0, cy + r0 * si0, c0, cx + r0 * co1, cy + r0 * si1, c0, cx + r1 * co1, cy + r1 * si1, c1,
                    cx + r1 * co0, cy + r1 * si0, c1);
        }
    }

    /**
     * A circle outline of centre ({@code cx}, {@code cy}) and radius {@code r}, clipped to the view so a ring far larger
     * than the screen is still drawn round where it crosses it. With {@code dash} above zero it is dashed by angle, the
     * pattern fixed to the circle so it does not crawl as the view moves.
     */
    public void ring(double cx, double cy, double r, double halfW, double feather, int c, double dash, double gap) {
        if (!(r > 0) || !Double.isFinite(r) || !Double.isFinite(cx) || !Double.isFinite(cy)) {
            return;
        }
        double m = halfW + feather + 4;
        double nearX = Math.max(-m, Math.max(cx - viewW - m, 0)) ;
        // Distance from the centre to the nearest and farthest point of the view (grown by the margin).
        double dxn = cx < -m ? -m - cx : cx > viewW + m ? cx - viewW - m : 0;
        double dyn = cy < -m ? -m - cy : cy > viewH + m ? cy - viewH - m : 0;
        double nearest = Math.hypot(dxn, dyn);
        double dxf = Math.max(Math.abs(cx + m), Math.abs(cx - viewW - m));
        double dyf = Math.max(Math.abs(cy + m), Math.abs(cy - viewH - m));
        double farthest = Math.hypot(dxf, dyf);
        if (r < nearest || r > farthest) {
            return;
        }
        boolean whole = cx - r >= -m && cx + r <= viewW + m && cy - r >= -m && cy + r <= viewH + m;
        if (whole) {
            arc(cx, cy, r, 0, 2 * Math.PI, halfW, feather, c, dash, gap, true);
            return;
        }
        // Coarse samples find where the circle is on screen; each visible run is then drawn finely.
        int coarse = (int) Math.max(180, Math.min(40000, Math.ceil(2 * Math.PI * r / 150)));
        double step = 2 * Math.PI / coarse;
        double grow = m + r * step;
        boolean prev = false;
        double runStart = 0;
        boolean firstInside = inside(cx + r, cy, grow);
        boolean any = false;
        for (int i = 1; i <= coarse; i++) {
            double a = step * i;
            boolean in = inside(cx + r * Math.cos(a), cy + r * Math.sin(a), grow);
            if (in && !prev) {
                runStart = step * (i - 1);
            }
            if (!in && prev) {
                arc(cx, cy, r, runStart, step * i, halfW, feather, c, dash, gap, false);
                any = true;
            }
            prev = in;
        }
        if (prev) {
            // A run that reaches the end of the circle joins the one that began at angle zero.
            arc(cx, cy, r, firstInside ? runStart - 0 : runStart, 2 * Math.PI, halfW, feather, c, dash, gap, false);
            any = true;
        }
        if (!any && firstInside) {
            arc(cx, cy, r, 0, step, halfW, feather, c, dash, gap, false);
        }
    }

    private boolean inside(double vx, double vy, double grow) {
        return vx >= -grow && vx <= viewW + grow && vy >= -grow && vy <= viewH + grow;
    }

    /** The arc of a circle from angle {@code a0} to {@code a1}, dashed by angle when {@code dash} is above zero. */
    private void arc(double cx, double cy, double r, double a0, double a1, double halfW, double feather, int c,
            double dash, double gap, boolean closed) {
        double da = Math.min(0.5, CHORD / r);
        if (dash <= 0) {
            int n = (int) Math.max(3, Math.ceil((a1 - a0) / da)) + 1;
            ensure(n);
            for (int i = 0; i < n; i++) {
                double a = i == n - 1 ? a1 : a0 + (a1 - a0) * i / (n - 1);
                px[i] = cx + r * Math.cos(a);
                py[i] = cy + r * Math.sin(a);
            }
            if (closed) {
                px[n - 1] = px[0];
                py[n - 1] = py[0];
            }
            strip(n, halfW, feather, c, closed);
            return;
        }
        double period = (dash + gap) / r;
        double on = dash / r;
        for (double s = Math.floor(a0 / period) * period; s < a1; s += period) {
            double b0 = Math.max(a0, s);
            double b1 = Math.min(a1, s + on);
            if (b1 - b0 < 1e-9) {
                continue;
            }
            int n = (int) Math.max(2, Math.ceil((b1 - b0) / da)) + 1;
            ensure(n);
            for (int i = 0; i < n; i++) {
                double a = b0 + (b1 - b0) * i / (n - 1);
                px[i] = cx + r * Math.cos(a);
                py[i] = cy + r * Math.sin(a);
            }
            strip(n, halfW, feather, c, false);
        }
    }

    /** A rectangle whose colour fades to nothing over {@code f} units outside its edge: a soft scrim behind text. */
    public void softRect(double x0, double y0, double x1, double y1, double f, int c) {
        int s = clear(c);
        rect(x0, y0, x1, y1, c);
        quad(x0, y0 - f, s, x1, y0 - f, s, x1, y0, c, x0, y0, c);
        quad(x0, y1, c, x1, y1, c, x1, y1 + f, s, x0, y1 + f, s);
        quad(x0 - f, y0, s, x0, y0, c, x0, y1, c, x0 - f, y1, s);
        quad(x1, y0, c, x1 + f, y0, s, x1 + f, y1, s, x1, y1, c);
        quad(x0 - f, y0 - f, s, x0, y0 - f, s, x0, y0, c, x0 - f, y0, s);
        quad(x1, y0 - f, s, x1 + f, y0 - f, s, x1 + f, y0, s, x1, y0, c);
        quad(x0 - f, y1, s, x0, y1, c, x0, y1 + f, s, x0 - f, y1 + f, s);
        quad(x1, y1, c, x1 + f, y1, s, x1 + f, y1 + f, s, x1, y1 + f, s);
    }

    /** A soft glow along a polyline: a wide strip of {@code c} (its own alpha) that fades to nothing at both edges. */
    public void glow(double[] xs, double[] ys, int n, double width, int c) {
        polyline(xs, ys, n, 0, width, c);
    }
}
