package dev.apoapsis.client.hud;

import dev.apoapsis.client.ui.UiKeycap;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiPanel;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Keycaps for the HUD: the kit's {@link UiKeycap} look, but able to fade, since the hints come and go. A key is always
 * the player's bound key. UI units, between {@code UiScale.begin} and {@code end}. Client only.
 */
public final class HudKeycap {
    private static final int GAP = 5;

    private HudKeycap() {}

    /** Draws one keycap at (x, y) with every colour scaled by {@code fade} (0..1); returns its width. */
    public static int cap(GuiGraphicsExtractor g, int x, int y, String label, float fade) {
        int w = UiKeycap.width(label);
        int h = UiKeycap.HEIGHT;
        g.fill(x, y, x + w, y + h, UiPalette.fade(UiPalette.alpha(UiPalette.STEEL_800, 0xE6), fade));
        UiPanel.edge(g, x, y, x + w, y + h, UiPalette.fade(UiPalette.STEEL_500, fade));
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, UiPalette.fade(UiPalette.STEEL_700, fade));
        int capTop = y + (h - UiText.Type.LABEL.capHeight()) / 2 - 1;
        UiTextSmooth.drawCentered(g, UiText.Type.LABEL, label, x + w / 2f, capTop, UiPalette.fade(UiPalette.INK, fade));
        return w;
    }

    /**
     * One or two keycaps (touching), then a caption in {@code captionArgb}; returns the width drawn.
     */
    public static int hint(GuiGraphicsExtractor g, int x, int y, KeyMapping[] keys, String caption, int captionArgb,
                           float fade) {
        int total = hintWidth(keys, caption);
        g.fill(x - 3, y - 2, x + total + 4, y + UiKeycap.HEIGHT + 2,
                UiPalette.fade(UiPalette.alpha(UiPalette.STEEL_950, 0x99), fade));
        int cx = x;
        for (int i = 0; i < keys.length; i++) {
            cx += cap(g, cx, y, UiKeycap.label(keys[i]), fade) + (i + 1 < keys.length ? 2 : 0);
        }
        int capTop = y + (UiKeycap.HEIGHT - UiText.Type.LABEL.capHeight()) / 2 - 1;
        UiTextSmooth.draw(g, UiText.Type.LABEL, caption, cx + GAP, capTop, UiPalette.fade(captionArgb, fade));
        return cx + GAP + Math.round(UiTextSmooth.width(UiText.Type.LABEL, caption)) - x;
    }

    /** The width {@link #hint} would take. */
    public static int hintWidth(KeyMapping[] keys, String caption) {
        int w = 0;
        for (int i = 0; i < keys.length; i++) {
            w += UiKeycap.width(UiKeycap.label(keys[i])) + (i + 1 < keys.length ? 2 : 0);
        }
        return w + GAP + Math.round(UiTextSmooth.width(UiText.Type.LABEL, caption));
    }
}
