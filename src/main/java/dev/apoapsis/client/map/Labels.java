package dev.apoapsis.client.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the maps' labels apart, for both maps. Everything is in UI units. Each frame starts empty: the overlays reserve
 * their boxes, the lines and bodies mark where they run, then each label asks for a place beside its anchor point and
 * gets the best of eight spots: never on a reserved box or another label, and as far from the lines' busiest parts and
 * from other bodies as it can be. A label with no acceptable spot is dropped, so callers place the important labels
 * first. Client only.
 */
public final class Labels {
    /** Occupancy grid cell, UI units. */
    private static final int CELL = 4;
    /** Gap between an anchor and its label, UI units. */
    private static final double GAP = 5;
    /** A spot costing more than this is refused: it would sit on a busy line. */
    private static final double REFUSE = 14;

    /** The eight places round an anchor, in the order of preference a caller can reorder through {@code prefer}. */
    public enum Side { RIGHT, ABOVE, BELOW, LEFT, ABOVE_RIGHT, BELOW_RIGHT, ABOVE_LEFT, BELOW_LEFT }

    /** A label's top-left in UI units, or what a caller needs to draw it. */
    public record Spot(float x, float y, Side side) {}

    private final List<float[]> boxes = new ArrayList<>();
    private int gridW;
    private int gridH;
    private byte[] grid = new byte[0];
    private double width;
    private double height;

    /** Starts a frame for a view of {@code w} by {@code h} UI units. */
    public void begin(double w, double h) {
        width = w;
        height = h;
        boxes.clear();
        gridW = (int) Math.ceil(w / CELL) + 1;
        gridH = (int) Math.ceil(h / CELL) + 1;
        if (grid.length < gridW * gridH) {
            grid = new byte[gridW * gridH];
        } else {
            java.util.Arrays.fill(grid, 0, gridW * gridH, (byte) 0);
        }
    }

    /** Width of the view this frame, UI units. */
    public double width() {
        return width;
    }

    /** Height of the view this frame, UI units. */
    public double height() {
        return height;
    }

    /** Claims a box that labels must not cover (an overlay, a marker). */
    public void reserve(double x0, double y0, double x1, double y1) {
        boxes.add(new float[] {(float) x0, (float) y0, (float) x1, (float) y1});
    }

    /** True when the box meets a reserved box or a placed label. */
    public boolean blocked(double x0, double y0, double x1, double y1) {
        for (float[] b : boxes) {
            if (x0 < b[2] && b[0] < x1 && y0 < b[3] && b[1] < y1) {
                return true;
            }
        }
        return false;
    }

    /** Marks the cells a line passes through with {@code weight} (1 faint, 3 an orbit): labels avoid heavy cells. */
    public void markSegment(double ax, double ay, double bx, double by, int weight) {
        if (!(Double.isFinite(ax) && Double.isFinite(ay) && Double.isFinite(bx) && Double.isFinite(by))) {
            return;
        }
        double len = Math.hypot(bx - ax, by - ay);
        int steps = (int) Math.min(2000, Math.max(1, Math.ceil(len / (CELL * 0.5))));
        int last = -1;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int cx = (int) Math.floor((ax + (bx - ax) * t) / CELL);
            int cy = (int) Math.floor((ay + (by - ay) * t) / CELL);
            if (cx < 0 || cy < 0 || cx >= gridW || cy >= gridH) {
                continue;
            }
            int idx = cy * gridW + cx;
            if (idx != last) {
                grid[idx] = (byte) Math.min(8, Math.max(grid[idx], weight));
                last = idx;
            }
        }
    }

    /** Marks a polyline's segments. */
    public void markPolyline(double[] xs, double[] ys, int n, int weight) {
        for (int i = 1; i < n; i++) {
            markSegment(xs[i - 1], ys[i - 1], xs[i], ys[i], weight);
        }
    }

    /** Marks a circle's outline (a ring or a body's limb). Only the part near the view costs anything. */
    public void markCircle(double cx, double cy, double r, int weight) {
        if (!(r > 0) || !Double.isFinite(r)) {
            return;
        }
        double nearX = Math.max(0, Math.max(-cx, cx - width));
        double nearY = Math.max(0, Math.max(-cy, cy - height));
        if (Math.hypot(nearX, nearY) > r + 8) {
            return;
        }
        double farX = Math.max(Math.abs(cx), Math.abs(cx - width));
        double farY = Math.max(Math.abs(cy), Math.abs(cy - height));
        if (Math.hypot(farX, farY) < r - 8) {
            return;
        }
        int n = (int) Math.min(720, Math.max(48, Math.ceil(2 * Math.PI * r / (CELL * 0.75))));
        double px = cx + r;
        double py = cy;
        for (int i = 1; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            double qx = cx + r * Math.cos(a);
            double qy = cy + r * Math.sin(a);
            if (qx > -20 && qx < width + 20 && qy > -20 && qy < height + 20
                    || px > -20 && px < width + 20 && py > -20 && py < height + 20) {
                markSegment(px, py, qx, qy, weight);
            }
            px = qx;
            py = qy;
        }
    }

    private double lineCost(double x0, double y0, double x1, double y1) {
        int cx0 = Math.max(0, (int) Math.floor(x0 / CELL));
        int cy0 = Math.max(0, (int) Math.floor(y0 / CELL));
        int cx1 = Math.min(gridW - 1, (int) Math.floor(x1 / CELL));
        int cy1 = Math.min(gridH - 1, (int) Math.floor(y1 / CELL));
        double cost = 0;
        for (int cy = cy0; cy <= cy1; cy++) {
            for (int cx = cx0; cx <= cx1; cx++) {
                cost += grid[cy * gridW + cx];
            }
        }
        return cost;
    }

    /**
     * Finds a place for a label of {@code w} by {@code h} beside the anchor ({@code ax}, {@code ay}), keeping
     * {@code clearance} UI units from the anchor besides the usual gap (a body's radius, a marker's size). Tries the sides
     * in {@code prefer} order, which breaks ties. Claims the box on success; null when no side is acceptable and the
     * label is not {@code force}d. A forced label takes the cheapest side even over a line, but never over a reserved box
     * or another label unless every side is blocked.
     */
    public Spot place(double ax, double ay, double w, double h, double clearance, boolean force, Side... prefer) {
        Side[] order = prefer.length == 0 ? Side.values() : prefer;
        double best = Double.MAX_VALUE;
        float bx = 0;
        float by = 0;
        Side bestSide = null;
        double edge = 4;
        for (int rank = 0; rank < order.length; rank++) {
            Side s = order[rank];
            double d = clearance + GAP;
            double x;
            double y;
            switch (s) {
                case RIGHT -> {
                    x = ax + d;
                    y = ay - h / 2;
                }
                case LEFT -> {
                    x = ax - d - w;
                    y = ay - h / 2;
                }
                case ABOVE -> {
                    x = ax - w / 2;
                    y = ay - d - h;
                }
                case BELOW -> {
                    x = ax - w / 2;
                    y = ay + d;
                }
                case ABOVE_RIGHT -> {
                    x = ax + d * 0.75;
                    y = ay - d * 0.75 - h;
                }
                case ABOVE_LEFT -> {
                    x = ax - d * 0.75 - w;
                    y = ay - d * 0.75 - h;
                }
                case BELOW_RIGHT -> {
                    x = ax + d * 0.75;
                    y = ay + d * 0.75;
                }
                default -> {
                    x = ax - d * 0.75 - w;
                    y = ay + d * 0.75;
                }
            }
            if (x < edge || y < edge || x + w > width - edge || y + h > height - edge) {
                continue;
            }
            if (blocked(x - 1, y - 1, x + w + 1, y + h + 1)) {
                continue;
            }
            double cost = lineCost(x - 1, y - 1, x + w + 1, y + h + 1) + rank * 0.6;
            if (cost < best) {
                best = cost;
                bx = (float) x;
                by = (float) y;
                bestSide = s;
            }
        }
        if (bestSide == null || (!force && best > REFUSE)) {
            return null;
        }
        boxes.add(new float[] {bx - 1, by - 1, (float) (bx + w + 1), (float) (by + h + 1)});
        return new Spot(bx, by, bestSide);
    }
}
