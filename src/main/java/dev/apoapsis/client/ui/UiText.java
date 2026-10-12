package dev.apoapsis.client.ui;

import dev.apoapsis.Apoapsis;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * The one place the kit draws text. Each {@link Type} names a font definition under {@code assets/apoapsis/font/}
 * (Barlow Condensed for UI text, Chakra Petch for headings and the wordmark), so changing a typeface or a size is a
 * change to a JSON file, not to screens. Positions are in UI units (see {@link UiScale}); {@code y} is the top of the
 * capital letters, so rows of different types line up on their caps. Client only.
 */
public final class UiText {
    private UiText() {}

    /** The type styles. Sizes are font pixels per em in UI units; the font JSON must carry the same size. */
    public enum Type {
        /** Small tracked capitals: captions, hints, keycaps. */
        LABEL("label", 8f, true, 1.0f),
        /** Running text and descriptions. */
        BODY("body", 11f, false, 0f),
        /** Menu items and buttons: tracked capitals. */
        TITLE("title", 16f, true, 2f),
        /** Screen headings, in the title typeface: tracked capitals. */
        HEADING("heading", 13f, true, 2f),
        /** The wordmark only. */
        DISPLAY("display", 30f, true, 6f);

        private final FontDescription font;
        private final float size;
        private final boolean caps;
        private final float tracking;

        Type(String fontName, float size, boolean caps, float tracking) {
            this.font = new FontDescription.Resource(Identifier.fromNamespaceAndPath(Apoapsis.MODID, fontName));
            this.size = size;
            this.caps = caps;
            this.tracking = tracking;
        }

        /** Height of the capital letters, in UI units. */
        public int capHeight() {
            return Math.round(size * 0.7f);
        }
    }

    /** Where the font's baseline sits below the origin a text is drawn from, in font pixels (the renderer's fixed ascent). */
    private static final int ASCENT = 7;

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static MutableComponent styled(Type type, String text) {
        return Component.literal(text).withStyle(s -> s.withFont(type.font));
    }

    private static String prepare(Type type, String text) {
        return type.caps ? text.toUpperCase(Locale.ROOT) : text;
    }

    /** Width of {@code text} in {@code type}, in UI units. */
    public static float width(Type type, String text) {
        String s = prepare(type, text);
        if (type.tracking == 0f) {
            return font().getSplitter().stringWidth(styled(type, s));
        }
        float w = 0;
        int n = s.codePointCount(0, s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += font().getSplitter().stringWidth(styled(type, new String(Character.toChars(cp))));
            i += Character.charCount(cp);
        }
        return w + Math.max(0, n - 1) * type.tracking;
    }

    /** Draws {@code text} with its left edge at {@code x} and the top of its capitals at {@code y}. */
    public static void draw(GuiGraphicsExtractor g, Type type, String text, float x, int y, int argb) {
        String s = prepare(type, text);
        int oy = y + type.capHeight() - ASCENT;
        if (type.tracking == 0f) {
            g.text(font(), styled(type, s), Math.round(x), oy, argb, false);
            return;
        }
        float cx = x;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            MutableComponent c = styled(type, ch);
            g.text(font(), c, Math.round(cx), oy, argb, false);
            cx += font().getSplitter().stringWidth(c) + type.tracking;
            i += Character.charCount(cp);
        }
    }

    /** As {@link #draw} for a component's plain text. */
    public static void draw(GuiGraphicsExtractor g, Type type, Component text, float x, int y, int argb) {
        draw(g, type, text.getString(), x, y, argb);
    }

    /** Draws {@code text} centred on {@code cx}. */
    public static void drawCentered(GuiGraphicsExtractor g, Type type, String text, float cx, int y, int argb) {
        draw(g, type, text, cx - width(type, text) / 2f, y, argb);
    }

    /** Draws {@code text} with its right edge at {@code right}. */
    public static void drawRight(GuiGraphicsExtractor g, Type type, String text, float right, int y, int argb) {
        draw(g, type, text, right - width(type, text), y, argb);
    }
}
