package dev.apoapsis.client.ui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Keycap hints: a small key with the label of the key the player actually has bound, then a caption. Pass a
 * {@link KeyMapping} so a rebind shows up here; use the string form only for keys that cannot be rebound (Enter, Esc,
 * the arrows). Coordinates are UI units between {@link UiScale#begin} and {@link UiScale#end}. Client only.
 */
public final class UiKeycap {
    /** Height of a keycap, in UI units. */
    public static final int HEIGHT = 13;
    private static final int PAD = 4;
    private static final int GAP = 5;

    private UiKeycap() {}

    /** The label of the key bound to {@code mapping}, as the player's keyboard names it. */
    public static String label(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    /**
     * Whether the label's characters are all in the kit typefaces. Evenly spaced (smooth) text is drawn from a font three
     * times the size under a smaller pose, which would shrink a glyph the game draws from its fallback font (an arrow), so
     * those labels use the plain type.
     */
    private static boolean smooth(String label) {
        return label.chars().allMatch(c -> c < 0x2000);
    }

    private static float textWidth(String label) {
        return smooth(label) ? UiTextSmooth.width(UiText.Type.LABEL, label) : UiText.width(UiText.Type.LABEL, label);
    }

    /** Width of a keycap showing {@code label}. */
    public static int width(String label) {
        return Math.max(HEIGHT, Math.round(textWidth(label)) + PAD * 2);
    }

    /** Draws a keycap with its top left at ({@code x}, {@code y}); returns its width. */
    public static int draw(GuiGraphicsExtractor g, int x, int y, String label) {
        int w = width(label);
        g.fill(x, y, x + w, y + HEIGHT, UiPalette.STEEL_800);
        UiPanel.edge(g, x, y, x + w, y + HEIGHT, UiPalette.STEEL_500);
        g.fill(x + 1, y + HEIGHT - 2, x + w - 1, y + HEIGHT - 1, UiPalette.STEEL_700);
        int capTop = y + (HEIGHT - UiText.Type.LABEL.capHeight()) / 2 - 1;
        if (smooth(label)) {
            UiTextSmooth.drawCentered(g, UiText.Type.LABEL, label, x + w / 2f, capTop, UiPalette.INK);
        } else {
            UiText.drawCentered(g, UiText.Type.LABEL, label, x + w / 2f, capTop, UiPalette.INK);
        }
        return w;
    }

    /** A keycap for a bound key, then {@code caption}; returns the total width. */
    public static int hint(GuiGraphicsExtractor g, int x, int y, KeyMapping mapping, String caption) {
        return hint(g, x, y, label(mapping), caption);
    }

    /** A keycap for a fixed key, then {@code caption}; returns the total width. */
    public static int hint(GuiGraphicsExtractor g, int x, int y, String keyLabel, String caption) {
        int w = draw(g, x, y, keyLabel);
        int capTop = y + (HEIGHT - UiText.Type.LABEL.capHeight()) / 2 - 1;
        UiTextSmooth.draw(g, UiText.Type.LABEL, caption, x + w + GAP, capTop, UiPalette.INK_MUTED);
        return w + GAP + Math.round(UiTextSmooth.width(UiText.Type.LABEL, caption));
    }

    /** Width {@link #hint(GuiGraphicsExtractor, int, int, String, String)} would take. */
    public static int hintWidth(String keyLabel, String caption) {
        return width(keyLabel) + GAP + Math.round(UiTextSmooth.width(UiText.Type.LABEL, caption));
    }
}
