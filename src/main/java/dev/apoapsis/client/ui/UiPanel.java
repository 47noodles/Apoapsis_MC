package dev.apoapsis.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Plates and rules. A plate is one layer: dark steel, a one-unit steel edge, a brass line along the top and a rivet in
 * each corner. Nothing is drawn inside it but content; separate groups with {@link #rule} or space, not with boxes.
 * Coordinates are UI units between {@link UiScale#begin} and {@link UiScale#end}. Client only.
 */
public final class UiPanel {
    private UiPanel() {}

    /** A steel plate over x0..x1 by y0..y1 (x1 and y1 exclusive). */
    public static void plate(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, UiPalette.alpha(UiPalette.STEEL_900, 0xEB));
        edge(g, x0, y0, x1, y1, UiPalette.STEEL_700);
        g.fill(x0 + 1, y0, x1 - 1, y0 + 1, UiPalette.BRASS);
        int inset = 4;
        int rivet = UiPalette.BRASS_DEEP;
        g.fill(x0 + inset, y0 + inset, x0 + inset + 1, y0 + inset + 1, rivet);
        g.fill(x1 - inset - 1, y0 + inset, x1 - inset, y0 + inset + 1, rivet);
        g.fill(x0 + inset, y1 - inset - 1, x0 + inset + 1, y1 - inset, rivet);
        g.fill(x1 - inset - 1, y1 - inset - 1, x1 - inset, y1 - inset, rivet);
    }

    /** A one-unit outline just inside the rectangle. */
    public static void edge(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1, y0 + 1, argb);
        g.fill(x0, y1 - 1, x1, y1, argb);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, argb);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, argb);
    }

    /** A hairline across x0..x1 at y. */
    public static void rule(GuiGraphicsExtractor g, int x0, int x1, int y, int argb) {
        g.fill(x0, y, x1, y + 1, argb);
    }

    /** Short corner brackets, {@code arm} units long, around the rectangle; the focus mark. */
    public static void brackets(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int arm, int argb) {
        g.fill(x0, y0, x0 + arm, y0 + 1, argb);
        g.fill(x0, y0, x0 + 1, y0 + arm, argb);
        g.fill(x1 - arm, y0, x1, y0 + 1, argb);
        g.fill(x1 - 1, y0, x1, y0 + arm, argb);
        g.fill(x0, y1 - 1, x0 + arm, y1, argb);
        g.fill(x0, y1 - arm, x0 + 1, y1, argb);
        g.fill(x1 - arm, y1 - 1, x1, y1, argb);
        g.fill(x1 - 1, y1 - arm, x1, y1, argb);
    }

    /** A dashed outline, for a control that is switched off. */
    public static void dashed(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int argb) {
        for (int x = x0; x < x1; x += 4) {
            int e = Math.min(x + 2, x1);
            g.fill(x, y0, e, y0 + 1, argb);
            g.fill(x, y1 - 1, e, y1, argb);
        }
        for (int y = y0 + 2; y < y1 - 1; y += 4) {
            int e = Math.min(y + 2, y1 - 1);
            g.fill(x0, y, x0 + 1, e, argb);
            g.fill(x1 - 1, y, x1, e, argb);
        }
    }
}
