package dev.apoapsis.client.map;

import dev.apoapsis.client.ui.UiKeycap;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText.Type;
import dev.apoapsis.client.ui.UiTextSmooth;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The light overlays both maps share, drawn over the full-bleed view in UI units with a thin dark halo and no panels:
 * the breadcrumb top left, the universal time top right, a short list of figures for the selected thing down the left
 * edge, one quiet row of key hints on the bottom edge (the rest behind a key), the warp step beside it, a card of lines
 * bottom right for a transfer or a maneuver node, and the labels the maps place with {@link Labels}. Each overlay
 * reserves its box in the solver as it is laid out, so no label lands on it; the drawing itself is queued and runs in
 * {@link #flush}, after the picture-in-picture layers, so text always sits on top. Three type sizes: {@code LABEL} for
 * captions, hints and labels, {@code BODY} for figures, {@code HEADING} for the crumb, the clock and the one big
 * number. Client only.
 */
public final class MapChrome {
    /** Margin from the screen edge, UI units. */
    public static final int MARGIN = 16;
    /** Row step of the figures list. */
    private static final int ROW = 13;
    private static final int LABEL_COL = 66;

    /** A figure with its caption: "ALTITUDE" "16.0 km", and an optional muted note after the value. */
    public record Stat(String label, String value, String note, int valueColor) {
        public Stat(String label, String value) {
            this(label, value, null, UiPalette.INK);
        }
    }

    /** A key hint: one or more key labels, then what they do. */
    public record Hint(String[] keys, String caption) {
        public static Hint of(String key, String caption) {
            return new Hint(new String[] {key}, caption);
        }

        public static Hint of(KeyMapping key, String caption) {
            return new Hint(new String[] {MapKeys.label(key)}, caption);
        }

        public static Hint of(String[] keys, String caption) {
            return new Hint(keys, caption);
        }
    }

    /** One line of a card: text, size and colour. */
    public record Line(String text, Type type, int color) {}

    private final List<Runnable> queue = new ArrayList<>();
    private final List<float[]> scrims = new ArrayList<>();
    private GuiGraphicsExtractor g;
    private UiScale s;
    private float halo;
    private float w;
    private float h;

    /** Starts a frame: reads the scale and the size of the view in UI units, and empties the queue. */
    public void begin(GuiGraphicsExtractor graphics) {
        g = graphics;
        s = UiScale.current();
        halo = 1f / s.unit();
        w = graphics.guiWidth() / s.scale();
        h = graphics.guiHeight() / s.scale();
        queue.clear();
        scrims.clear();
    }

    /** Adds soft dark patches behind the figures and the card to {@code front}, so text reads over a body or a line. */
    public void scrims(QuadBatch front) {
        for (float[] b : scrims) {
            front.softRect(b[0], b[1], b[2], b[3], 14, UiPalette.alpha(UiPalette.STEEL_950, (int) b[4]));
        }
    }

    /** Draws everything queued, in UI units. Call after the picture-in-picture layers are submitted. */
    public void flush() {
        scrims.clear();
        s.begin(g, 0, 0);
        for (Runnable r : queue) {
            r.run();
        }
        s.end(g);
        queue.clear();
    }

    public UiScale scale() {
        return s;
    }

    /** Width of the view, UI units. */
    public float width() {
        return w;
    }

    /** Height of the view, UI units. */
    public float height() {
        return h;
    }

    /** The y of the top of the keycaps on the bottom row. */
    public float bottomRowY() {
        return h - MARGIN - UiKeycap.HEIGHT;
    }

    // ---- text ----

    /** Queues text with a one-device-pixel dark halo so it reads over a line, a body or the stars. */
    public void text(Type type, String text, float x, float y, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        queue.add(() -> drawText(type, text, x, y, color));
    }

    private void drawText(Type type, String text, float x, float y, int color) {
        int shade = UiPalette.alpha(UiPalette.STEEL_950, Math.min(0xC0, (color >>> 24) * 3 / 4));
        for (int k = 0; k < 4; k++) {
            float dx = (k & 1) == 0 ? -halo : halo;
            float dy = (k & 2) == 0 ? -halo : halo;
            UiTextSmooth.draw(g, type, text, x + dx, y + dy, shade);
        }
        UiTextSmooth.draw(g, type, text, x, y, color);
    }

    /** Text with its right edge at {@code right}. */
    public void textRight(Type type, String text, float right, float y, int color) {
        text(type, text, right - UiTextSmooth.width(type, text), y, color);
    }

    /** Text centred on {@code cx}. */
    public void textCentered(Type type, String text, float cx, float y, int color) {
        text(type, text, cx - UiTextSmooth.width(type, text) / 2f, y, color);
    }

    // ---- labels ----

    /** Names are tracked capitals; anything with a figure (and so a unit) is running text, so "km" stays "km". */
    private static Type typeFor(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                return Type.BODY;
            }
        }
        return Type.LABEL;
    }

    /**
     * A one-line label beside the anchor ({@code ax}, {@code ay}), placed by {@code labels}: the best of the sides in
     * {@code prefer}, clear of the lines, the overlays and the other labels. Returns whether it was drawn; a label
     * that is {@code force}d is drawn even over a line. {@code clearance} keeps it that far from the anchor (a body's
     * radius).
     */
    public boolean label(Labels labels, String text, int color, double ax, double ay, double clearance, boolean force,
            Labels.Side... prefer) {
        Type type = typeFor(text);
        float tw = UiTextSmooth.width(type, text);
        float th = type.capHeight();
        Labels.Spot spot = labels.place(ax, ay, tw, th, clearance, force, prefer);
        if (spot == null) {
            return false;
        }
        text(type, text, spot.x(), spot.y(), color);
        return true;
    }

    /** A two-line label: a name over a muted detail (a countdown, a gravity). */
    public boolean label2(Labels labels, String name, int color, String detail, double ax, double ay, double clearance,
            boolean force, Labels.Side... prefer) {
        Type nameType = typeFor(name);
        float tw = Math.max(UiTextSmooth.width(nameType, name), UiTextSmooth.width(Type.BODY, detail));
        float line = nameType.capHeight() + 5;
        Labels.Spot spot = labels.place(ax, ay, tw, line + Type.BODY.capHeight(), clearance, force, prefer);
        if (spot == null) {
            return false;
        }
        boolean right = spot.side() == Labels.Side.LEFT || spot.side() == Labels.Side.ABOVE_LEFT
                || spot.side() == Labels.Side.BELOW_LEFT;
        if (right) {
            textRight(nameType, name, spot.x() + tw, spot.y(), color);
            textRight(Type.BODY, detail, spot.x() + tw, spot.y() + line, UiPalette.INK_MUTED);
        } else {
            text(nameType, name, spot.x(), spot.y(), color);
            text(Type.BODY, detail, spot.x(), spot.y() + line, UiPalette.INK_MUTED);
        }
        return true;
    }

    // ---- the overlays ----

    /** Top left: the breadcrumb in the heading type, "MERIDIAN · VERDANCE". */
    public void crumb(Labels labels, String system, String focus) {
        float x = MARGIN;
        float y = MARGIN - 2;
        text(Type.HEADING, system, x, y, UiPalette.INK_MUTED);
        float sep = x + UiTextSmooth.width(Type.HEADING, system) + 7;
        text(Type.HEADING, "·", sep, y, UiPalette.INK_DIM);
        float fx = sep + UiTextSmooth.width(Type.HEADING, "·") + 7;
        text(Type.HEADING, focus, fx, y, UiPalette.INK);
        float crumbRight = fx + UiTextSmooth.width(Type.HEADING, focus) + 4;
        labels.reserve(x - 3, y - 4, crumbRight, y + Type.HEADING.capHeight() + 6);
        scrims.add(new float[] {x - 4, y - 4, crumbRight, y + Type.HEADING.capHeight() + 6, 0x40});
    }

    /** Top right: the caption over the clock. */
    public void time(Labels labels, String caption, String clock) {
        float right = w - MARGIN;
        float cw = UiTextSmooth.width(Type.HEADING, clock);
        float capw = UiTextSmooth.width(Type.LABEL, caption);
        float y = MARGIN - 2;
        textRight(Type.LABEL, caption, right, y, UiPalette.INK_MUTED);
        textRight(Type.HEADING, clock, right, y + Type.LABEL.capHeight() + 5, UiPalette.INK);
        labels.reserve(right - Math.max(cw, capw) - 4, y - 4, right + 3,
                y + Type.LABEL.capHeight() + 5 + Type.HEADING.capHeight() + 6);
        scrims.add(new float[] {right - Math.max(cw, capw) - 6, y - 4, right + 4,
                y + Type.LABEL.capHeight() + 5 + Type.HEADING.capHeight() + 6, 0x40});
    }

    /** Down the left edge under the crumb: captions in a muted column, figures beside them. */
    public void stats(Labels labels, List<Stat> stats) {
        if (stats.isEmpty()) {
            return;
        }
        float x = MARGIN;
        float y = MARGIN + 30;
        float right = x;
        for (Stat st : stats) {
            text(Type.LABEL, st.label(), x, y + 2, UiPalette.INK_DIM);
            float vx = x + LABEL_COL;
            text(Type.BODY, st.value(), vx, y, st.valueColor());
            float end = vx + UiTextSmooth.width(Type.BODY, st.value());
            if (st.note() != null) {
                float nx = end + 6;
                text(Type.LABEL, st.note(), nx, y + 2, UiPalette.INK_MUTED);
                end = nx + UiTextSmooth.width(Type.LABEL, st.note());
            }
            right = Math.max(right, end);
            y += ROW;
        }
        labels.reserve(x - 3, MARGIN + 24, right + 4, y + 2);
        scrims.add(new float[] {x - 6, MARGIN + 22, right + 6, y + 2, 0x58});
    }

    /** A card of lines at the bottom right, its last line's baseline area on {@code bottom}. Returns the y of its top. */
    public float card(Labels labels, List<Line> lines, float bottom) {
        if (lines.isEmpty()) {
            return bottom;
        }
        float right = w - MARGIN;
        float y = bottom;
        float widest = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line l = lines.get(i);
            y -= l.type().capHeight();
            textRight(l.type(), l.text(), right, y, l.color());
            widest = Math.max(widest, UiTextSmooth.width(l.type(), l.text()));
            y -= 6;
        }
        labels.reserve(right - widest - 4, y + 2, right + 3, bottom + 4);
        scrims.add(new float[] {right - widest - 8, y + 2, right + 6, bottom + 4, 0x48});
        return y;
    }

    /** The y a card's last line should end at, above the bottom row. */
    public float cardBottom() {
        return bottomRowY() - 10;
    }

    /**
     * The bottom row: key hints in a centred run, the warp beside it at the right edge. {@code warp} is the step text
     * ("x1") or null when the player does not pilot.
     */
    public void bottomRow(Labels labels, List<Hint> hints, String warpCaption, String warp) {
        int y = Math.round(bottomRowY());
        int gap = 14;
        float total = 0;
        for (Hint hint : hints) {
            total += hintWidth(hint) + gap;
        }
        total -= gap;
        float x = (w - total) / 2f;
        float left = x;
        for (Hint hint : hints) {
            float at = Math.round(x);
            drawHint(hint, (int) at, y);
            x += hintWidth(hint) + gap;
        }
        labels.reserve(left - 6, y - 4, left + total + 6, y + UiKeycap.HEIGHT + 4);
        if (warp != null) {
            float right = w - MARGIN;
            float vw = UiTextSmooth.width(Type.HEADING, warp);
            float cap = UiTextSmooth.width(Type.LABEL, warpCaption);
            float vy = y + (UiKeycap.HEIGHT - Type.HEADING.capHeight()) / 2f - 1;
            text(Type.HEADING, warp, right - vw, vy, UiPalette.SIGNAL);
            text(Type.LABEL, warpCaption, right - vw - 7 - cap, y + (UiKeycap.HEIGHT - Type.LABEL.capHeight()) / 2f - 1,
                    UiPalette.INK_MUTED);
            labels.reserve(right - vw - cap - 11, y - 4, right + 3, y + UiKeycap.HEIGHT + 4);
        }
    }

    /** The extra key list, bottom left above the row, faded by {@code alpha} (0..1). */
    public void help(Labels labels, List<Hint> hints, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        int rowStep = UiKeycap.HEIGHT + 4;
        int top = Math.round(bottomRowY()) - 12 - hints.size() * rowStep;
        int x = MARGIN;
        float widest = 0;
        int y = top;
        for (Hint hint : hints) {
            int at = x;
            for (String k : hint.keys()) {
                final int keyX = at;
                final int keyY = y;
                queue.add(() -> UiKeycap.draw(g, keyX, keyY, k));
                at += UiKeycap.width(k) + 3;
            }
            int capY = y + (UiKeycap.HEIGHT - Type.LABEL.capHeight()) / 2 - 1;
            text(Type.LABEL, hint.caption(), at + 3, capY, UiPalette.fade(UiPalette.INK_MUTED, alpha));
            widest = Math.max(widest, at + 3 + UiTextSmooth.width(Type.LABEL, hint.caption()) - x);
            y += rowStep;
        }
        labels.reserve(x - 4, top - 4, x + widest + 6, Math.round(bottomRowY()) - 4);
    }

    /** A line of text centred above the bottom row, for overlay messages. */
    public void notice(Labels labels, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float y = bottomRowY() - 22;
        textCentered(Type.BODY, text, w / 2f, y, UiPalette.INK);
        float tw = UiTextSmooth.width(Type.BODY, text);
        labels.reserve(w / 2f - tw / 2 - 4, y - 3, w / 2f + tw / 2 + 4, y + Type.BODY.capHeight() + 5);
    }

    private float hintWidth(Hint hint) {
        float width = 0;
        for (String k : hint.keys()) {
            width += UiKeycap.width(k) + 3;
        }
        return width + 3 + UiTextSmooth.width(Type.LABEL, hint.caption());
    }

    private void drawHint(Hint hint, int x, int y) {
        int at = x;
        for (String k : hint.keys()) {
            final int keyX = at;
            queue.add(() -> UiKeycap.draw(g, keyX, y, k));
            at += UiKeycap.width(k) + 3;
        }
        int capY = y + (UiKeycap.HEIGHT - Type.LABEL.capHeight()) / 2 - 1;
        text(Type.LABEL, hint.caption(), at + 3, capY, UiPalette.INK_MUTED);
    }
}
