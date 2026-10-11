package dev.apoapsis.client.map;

/**
 * The map camera: a centre in the root body's inertial XY plane (metres) and a scale in pixels per metre. Screen x
 * is X and screen y is -Y. Client only.
 */
final class MapView {
    /** What the camera follows. NONE means the centre is free (after a pan). */
    enum Follow { NONE, VERDANCE, PALE, SHIP }

    /** The view's half-extent (centre to the nearer edge, scaled to the view) at the smallest and largest zoom, m. */
    static final double MIN_HALF_EXTENT_M = 100.0;
    static final double MAX_HALF_EXTENT_M = 2_000_000.0;
    /** A fit puts the subject at this fraction of the shorter side from the centre. */
    private static final double FIT_FRACTION = 0.42;
    private static final double ZOOM_PER_STEP = 1.25;

    Follow follow = Follow.NONE;
    /** Centre of the view, root-frame metres. Overwritten each frame while following. */
    double centreX;
    double centreY;

    private double scale = 1e-3;
    private int width = 1;
    private int height = 1;

    void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    /** Pixels per metre. */
    double scale() {
        return scale;
    }

    /** Screen x of root-frame X, in GUI pixels. */
    double sx(double x) {
        return width / 2.0 + (x - centreX) * scale;
    }

    /** Screen y of root-frame Y, in GUI pixels (screen y grows downward). */
    double sy(double y) {
        return height / 2.0 - (y - centreY) * scale;
    }

    /** A length in metres as pixels. */
    double px(double metres) {
        return metres * scale;
    }

    /** Sets the scale so that {@code halfExtentM} metres reach {@link #FIT_FRACTION} of the shorter side. */
    void fit(double halfExtentM) {
        double h = Double.isFinite(halfExtentM) ? halfExtentM : MAX_HALF_EXTENT_M;
        scale = clampScale(FIT_FRACTION * Math.min(width, height) / clampHalfExtent(h));
    }

    /** Zooms by {@code steps} notches (positive in), keeping the point under the cursor fixed. */
    void zoom(double mouseX, double mouseY, double steps) {
        double worldX = centreX + (mouseX - width / 2.0) / scale;
        double worldY = centreY - (mouseY - height / 2.0) / scale;
        scale = clampScale(scale * Math.pow(ZOOM_PER_STEP, steps));
        centreX = worldX - (mouseX - width / 2.0) / scale;
        centreY = worldY + (mouseY - height / 2.0) / scale;
    }

    /** Pans by a drag of {@code dx}, {@code dy} GUI pixels. Frees the centre. */
    void pan(double dx, double dy) {
        follow = Follow.NONE;
        centreX -= dx / scale;
        centreY += dy / scale;
    }

    private double clampScale(double s) {
        double minScale = FIT_FRACTION * Math.min(width, height) / MAX_HALF_EXTENT_M;
        double maxScale = FIT_FRACTION * Math.min(width, height) / MIN_HALF_EXTENT_M;
        return Math.min(maxScale, Math.max(minScale, s));
    }

    private static double clampHalfExtent(double h) {
        return Math.min(MAX_HALF_EXTENT_M, Math.max(MIN_HALF_EXTENT_M, h));
    }
}
