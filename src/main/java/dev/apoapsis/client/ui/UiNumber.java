package dev.apoapsis.client.ui;

import dev.apoapsis.Apoapsis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Instrument numerals in the kit's heading typeface (Chakra Petch at 11 UI units), set tabular: every digit takes the
 * same width, so a reading that changes every frame does not shimmer and a column lines up on its last digit. Signs,
 * points and group gaps keep their natural width. Like {@link UiTextSmooth} it draws from a font at three times the size
 * ({@code map_numeral}) under a pose scaled down by three, so glyphs sit at a third of a font pixel, not a whole UI
 * unit. Units (m, km, m/s, %) are small lowercase Barlow, from {@code map_label}. Positions are in UI units; {@code y}
 * is the top of the digits, as in {@link UiText}. Client only.
 */
public final class UiNumber {
    private static final int BIG = 3;
    private static final FontDescription FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "map_numeral"));
    private static final FontDescription UNIT_FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "map_label"));
    /** The renderer's fixed ascent in font pixels, as in {@link UiText}. */
    private static final int ASCENT = 7;
    /** Height of the capitals and digits, UI units. */
    public static final int CAP_HEIGHT = Math.round(11f * 0.7f);

    private UiNumber() {}

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static MutableComponent styled(FontDescription f, String text) {
        return Component.literal(text).withStyle(s -> s.withFont(f));
    }

    /** Width of one glyph in big font pixels. */
    private static float big(FontDescription f, String ch) {
        return font().getSplitter().stringWidth(styled(f, ch));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Width of {@code text} set tabular, UI units. */
    public static float width(String text) {
        float digit = big(FONT, "0");
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += isDigit(c) ? digit : big(FONT, String.valueOf(c));
        }
        return w / BIG;
    }

    /** Draws {@code text} with its left edge at {@code x} and the top of its digits at {@code y}. */
    public static void draw(GuiGraphicsExtractor g, String text, float x, float y, int argb) {
        float digit = big(FONT, "0");
        int oy = CAP_HEIGHT * BIG - ASCENT;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(1f / BIG, 1f / BIG);
        float cx = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            MutableComponent part = styled(FONT, String.valueOf(c));
            float natural = big(FONT, String.valueOf(c));
            if (isDigit(c)) {
                g.text(font(), part, Math.round(cx + (digit - natural) / 2f), oy, argb, false);
                cx += digit;
            } else {
                if (c != ' ') {
                    g.text(font(), part, Math.round(cx), oy, argb, false);
                }
                cx += natural;
            }
        }
        g.pose().popMatrix();
    }

    /**
     * Draws a unit (m, km, m/s, %) in lowercase label type with its baseline on the digits' baseline: {@code y} is the
     * top of the digits it sits beside.
     */
    public static void drawUnit(GuiGraphicsExtractor g, String unit, float x, float y, int argb) {
        int oy = CAP_HEIGHT * BIG - ASCENT;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(1f / BIG, 1f / BIG);
        g.text(font(), styled(UNIT_FONT, unit), 0, oy, argb, false);
        g.pose().popMatrix();
    }

    /** Width of a unit set by {@link #drawUnit}, UI units. */
    public static float unitWidth(String unit) {
        return big(UNIT_FONT, unit) / BIG;
    }

    /** Draws {@code text} with its right edge at {@code right}. */
    public static void drawRight(GuiGraphicsExtractor g, String text, float right, float y, int argb) {
        draw(g, text, right - width(text), y, argb);
    }
}
