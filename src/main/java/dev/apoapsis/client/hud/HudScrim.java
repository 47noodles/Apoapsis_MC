package dev.apoapsis.client.hud;

import dev.apoapsis.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A soft dark backing for a group of readouts that sits on the screen edge: darkest at the edge, fading out upwards and
 * along the free side, with no outline, so the text reads over a bright planet or sky without a box around it. UI
 * units, between {@code UiScale.begin} and {@code end}. Client only.
 */
final class HudScrim {
    private static final int EDGE_ALPHA = 0xA0;
    /** Height of the ramp from clear to full, UI units. */
    private static final int RAMP = 16;
    /** Strips across the free side, each this wide, UI units. */
    private static final int STRIPS = 12;
    private static final int STRIP_W = 2;

    private HudScrim() {}

    /**
     * A scrim over x0..x1 by y0..y1 whose bottom edge is the screen edge. {@code fadeRight} says which side is free: the
     * right side for a group at the left of the screen, the left side otherwise.
     */
    static void draw(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, boolean fadeRight) {
        strip(g, x0, y0, x1, y1, 1f);
        for (int j = 0; j < STRIPS; j++) {
            float t = 1f - (j + 0.5f) / STRIPS;
            float k = t * t * (3 - 2 * t);
            int sx = fadeRight ? x1 + j * STRIP_W : x0 - (j + 1) * STRIP_W;
            strip(g, sx, y0, sx + STRIP_W, y1, k);
        }
    }

    /** One column of the scrim: a ramp from clear over {@link #RAMP} units, then steady down to the edge. */
    private static void strip(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float k) {
        int steel = UiPalette.STEEL_950;
        int a = Math.round(EDGE_ALPHA * k);
        int ramp = Math.min(RAMP, y1 - y0);
        g.fillGradient(x0, y0, x1, y0 + ramp, UiPalette.alpha(steel, 0), UiPalette.alpha(steel, a));
        g.fill(x0, y0 + ramp, x1, y1, UiPalette.alpha(steel, a));
    }
}
