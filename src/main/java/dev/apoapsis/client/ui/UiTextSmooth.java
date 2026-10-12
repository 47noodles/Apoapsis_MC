package dev.apoapsis.client.ui;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ui.UiText.Type;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * {@link UiText} with fine placement, for text that sits on a map or moves with it. The game rounds every glyph's
 * advance to a whole font pixel, and {@code UiText} rounds each glyph to a whole UI unit, which at 8 to 13 pixels an em
 * leaves tracked capitals unevenly spaced ("AT MOSPHERE") and makes moving labels step. This draws the same typefaces
 * from copies of the font definitions at three times the size ({@code map_label}, {@code map_body},
 * {@code map_heading}) under a pose scaled down by three, so advances round to a third of a font pixel, and places each
 * glyph at its exact position. Sizes, tracking and caps match {@link UiText.Type}; {@link #width} is the width this
 * draws. Only the label, body and heading types are available. Client only.
 */
public final class UiTextSmooth {
    /** How much larger the fonts are than their type's size. */
    private static final int BIG = 3;
    /** The renderer's fixed ascent: the origin sits this many font pixels above the baseline. */
    private static final int ASCENT = 7;

    private UiTextSmooth() {}

    private static FontDescription font(Type type) {
        String name = switch (type) {
            case LABEL -> "map_label";
            case BODY -> "map_body";
            case HEADING -> "map_heading";
            default -> throw new IllegalArgumentException("no smooth font for " + type);
        };
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(Apoapsis.MODID, name));
    }

    /** Tracking between glyphs, UI units, as {@link UiText.Type}: zero for running text. */
    private static float tracking(Type type) {
        return type == Type.LABEL ? 1.0f : type == Type.HEADING ? 2f : 0f;
    }

    private static String prepare(Type type, String text) {
        return type == Type.BODY ? text : text.toUpperCase(Locale.ROOT);
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static MutableComponent styled(FontDescription f, String text) {
        return Component.literal(text).withStyle(s -> s.withFont(f));
    }

    /** Width of {@code text} in {@code type}, in UI units. */
    public static float width(Type type, String text) {
        FontDescription f = font(type);
        String s = prepare(type, text);
        float track = tracking(type);
        if (track == 0f) {
            return font().getSplitter().stringWidth(styled(f, s)) / BIG;
        }
        float w = 0;
        int n = s.codePointCount(0, s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += font().getSplitter().stringWidth(styled(f, new String(Character.toChars(cp)))) / BIG;
            i += Character.charCount(cp);
        }
        return w + Math.max(0, n - 1) * track;
    }

    /** Draws {@code text} with its left edge at {@code x} and the top of its capitals at {@code y}, both exact. */
    public static void draw(GuiGraphicsExtractor g, Type type, String text, float x, float y, int argb) {
        FontDescription f = font(type);
        String s = prepare(type, text);
        float track = tracking(type);
        // Work in big font pixels: the pose is scaled by 1/BIG and moved to the exact origin.
        float baseline = type.capHeight() * BIG;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(1f / BIG, 1f / BIG);
        int oy = Math.round(baseline) - ASCENT;
        if (track == 0f) {
            g.text(font(), styled(f, s), 0, oy, argb, false);
        } else {
            float cx = 0;
            for (int i = 0; i < s.length(); ) {
                int cp = s.codePointAt(i);
                MutableComponent c = styled(f, new String(Character.toChars(cp)));
                g.text(font(), c, Math.round(cx), oy, argb, false);
                cx += font().getSplitter().stringWidth(c) + track * BIG;
                i += Character.charCount(cp);
            }
        }
        g.pose().popMatrix();
    }
}
