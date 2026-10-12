package dev.apoapsis.client.hud;

import dev.apoapsis.client.ui.UiNumber;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The flight instruments, laid along the bottom edge in three groups and no boxes: the position and speed readouts at
 * the left, the navball and the SAS and warp status at the bottom centre, and the throttle, fuel and delta-v at the
 * right. Numbers are tabular with the unit small after them; warnings are colour on the number or the bar, plus one small
 * word (low fuel, no fuel, impact), never a banner. Positions come from {@link HudLayout}. Client only.
 */
public final class FlightInstruments {
    /** What the instruments show, read once per frame by the flight HUD. */
    public record Telemetry(double alt, double speed, double vs, double fuel, double deltaV, boolean dry, boolean orbit,
                            boolean hasApsides, boolean apInside, double apAlt, double peAlt, float throttle,
                            String sasKey, boolean sasActive, float warp) {}

    private static final int ROW = 13;
    private static final int LABEL_W = 20;
    private static final int VALUE_W = 42;
    private static final int UNIT_GAP = 3;
    private static final int COLUMN_W = LABEL_W + VALUE_W + UNIT_GAP + 18;
    private static final int COLUMN_GAP = 16;
    private static final int BAR_W = 56;
    private static final int BAR_H = 3;
    private static final int BALL_R = 21;
    private static final int MIN_BALL_R = 15;
    private static final int LOW_FUEL = 15;
    private static final int LABEL_CAP = UiText.Type.LABEL.capHeight();
    /** The faint backing behind a small label that sits over the view. */
    static final int PLATE = UiPalette.alpha(UiPalette.STEEL_950, 0x99);

    private FlightInstruments() {}

    /** The ball's centre y and the radius it was drawn with, so callers can place things beside it. */
    public record BallSpot(int cx, int cy, int half, int radius) {}

    /** The bottom-left readouts' top y (capitals), for hints that sit above them. */
    public static int leftTop(HudLayout lay, Telemetry t) {
        return lay.bottomFor(lay.left(), lay.left() + leftWidth(t)) - UiNumber.CAP_HEIGHT - 2 * ROW;
    }

    private static int leftWidth(Telemetry t) {
        return columns(t) * COLUMN_W + (columns(t) - 1) * COLUMN_GAP;
    }

    private static int columns(Telemetry t) {
        return t.orbit() ? 2 : 1;
    }

    /** Where the navball goes: centred, its bottom on the lowest free edge. */
    public static BallSpot ballSpot(HudLayout lay) {
        int cx = lay.centreX();
        int reach = BALL_R + NavballView.BEZEL;
        int bottom = lay.bottomFor(cx - reach, cx + reach);
        // Over vanilla's status rows the ball may shrink a little so it stays out of the middle third of the view.
        int room = bottom - lay.height() * 2 / 3;
        int radius = Math.max(MIN_BALL_R, Math.min(BALL_R, room / 2 - NavballView.BEZEL));
        int half = radius + NavballView.BEZEL;
        return new BallSpot(cx, bottom - half, half, radius);
    }

    /** Draws the left and right groups. Call between {@code UiScale.begin} and {@code end}. */
    public static void drawSides(GuiGraphicsExtractor g, HudLayout lay, Telemetry t) {
        drawLeft(g, lay, t);
        drawRight(g, lay, t);
    }

    private static void drawLeft(GuiGraphicsExtractor g, HudLayout lay, Telemetry t) {
        int x = lay.left();
        int width = leftWidth(t);
        int bottom = lay.bottomFor(x, x + width);
        int top = bottom - UiNumber.CAP_HEIGHT - 2 * ROW;
        HudScrim.draw(g, 0, top - 14, x + width + 6, bottom + HudLayout.BOTTOM_MARGIN, true);
        int y0 = top;
        reading(g, x, y0, "apoapsis.hud.label.alt", HudFormat.distance(t.alt()), UiPalette.INK);
        reading(g, x, y0 + ROW, t.orbit() ? "apoapsis.hud.label.orb" : "apoapsis.hud.label.speed",
                HudFormat.speed(t.speed()), UiPalette.INK);
        reading(g, x, y0 + 2 * ROW, "apoapsis.hud.label.vs", HudFormat.signedSpeed(t.vs()), UiPalette.INK);
        if (t.orbit()) {
            int x2 = x + COLUMN_W + COLUMN_GAP;
            if (t.hasApsides() && t.apInside()) {
                reading(g, x2, y0, "apoapsis.hud.label.ap", HudFormat.distance(t.apAlt()), UiPalette.INK);
            } else if (t.hasApsides() && t.peAlt() <= 0) {
                word(g, x2, y0, "apoapsis.hud.label.ap", "apoapsis.hud.impact", UiPalette.COPPER);
            } else if (t.hasApsides()) {
                word(g, x2, y0, "apoapsis.hud.label.ap", "apoapsis.hud.escape", UiPalette.INK_MUTED);
            } else {
                word(g, x2, y0, "apoapsis.hud.label.ap", "apoapsis.hud.none", UiPalette.INK_DIM);
            }
            if (t.hasApsides()) {
                int pe = t.peAlt() <= 0 ? UiPalette.COPPER : UiPalette.INK;
                reading(g, x2, y0 + ROW, "apoapsis.hud.label.pe", HudFormat.distance(t.peAlt()), pe);
            } else {
                word(g, x2, y0 + ROW, "apoapsis.hud.label.pe", "apoapsis.hud.none", UiPalette.INK_DIM);
            }
        }
    }

    /** One readout: caption, tabular number right-aligned to a fixed edge, small unit after it. */
    private static void reading(GuiGraphicsExtractor g, int x, int y, String labelKey, String[] value, int argb) {
        caption(g, x, y, labelKey);
        UiNumber.drawRight(g, value[0], x + LABEL_W + VALUE_W, y, argb);
        if (!value[1].isEmpty()) {
            UiNumber.drawUnit(g, value[1], x + LABEL_W + VALUE_W + UNIT_GAP, y, UiPalette.INK_MUTED);
        }
    }

    /** A readout whose value is a word, set small and right-aligned where the number would end. */
    private static void word(GuiGraphicsExtractor g, int x, int y, String labelKey, String wordKey, int argb) {
        caption(g, x, y, labelKey);
        String text = Component.translatable(wordKey).getString();
        UiTextSmooth.drawRight(g, UiText.Type.LABEL, text, x + LABEL_W + VALUE_W, y + UiNumber.CAP_HEIGHT - LABEL_CAP, argb);
    }

    private static void caption(GuiGraphicsExtractor g, int x, int y, String labelKey) {
        UiTextSmooth.draw(g, UiText.Type.LABEL, Component.translatable(labelKey).getString(), x,
                y + UiNumber.CAP_HEIGHT - LABEL_CAP, UiPalette.INK_MUTED);
    }

    private static void drawRight(GuiGraphicsExtractor g, HudLayout lay, Telemetry t) {
        int labelW = 22;
        int valueW = 22;
        int unitW = 18;
        int width = labelW + BAR_W + 5 + valueW + unitW;
        int right = lay.right();
        int x = right - width;
        int bottom = lay.bottomFor(x, right);
        int top = bottom - UiNumber.CAP_HEIGHT - 2 * ROW;
        HudScrim.draw(g, x - 6, top - 14, lay.width(), bottom + HudLayout.BOTTOM_MARGIN, false);

        int fuelPct = (int) Math.round(t.fuel() * 100);
        boolean low = !t.dry() && fuelPct <= LOW_FUEL;
        int fuelColour = t.dry() ? UiPalette.COPPER : low ? UiPalette.BRASS : UiPalette.HYDROGEN;
        int thrColour = t.dry() ? UiPalette.alpha(UiPalette.BRASS, 0x66) : UiPalette.BRASS;
        bar(g, x, top, labelW, "apoapsis.hud.label.throttle", t.throttle(), thrColour, UiPalette.INK,
                HudFormat.percent(t.throttle()), "%", valueW);
        bar(g, x, top + ROW, labelW, "apoapsis.hud.label.fuel", t.fuel(), fuelColour,
                t.dry() || low ? fuelColour : UiPalette.INK, HudFormat.percent(t.fuel()), "%", valueW);
        // Delta-v row: caption, number, unit, aligned with the bars' values.
        int y = top + 2 * ROW;
        caption(g, x, y, "apoapsis.hud.label.dv");
        String[] dv = HudFormat.speed(t.deltaV());
        int valueRight = x + labelW + BAR_W + 5 + valueW;
        UiNumber.drawRight(g, dv[0], valueRight, y, t.dry() ? UiPalette.INK_DIM : UiPalette.INK);
        UiNumber.drawUnit(g, dv[1], valueRight + UNIT_GAP, y, UiPalette.INK_MUTED);
        // One small word for a fuel warning, above the group at its right edge.
        if (t.dry() || low) {
            String word = Component.translatable(t.dry() ? "apoapsis.hud.fuel.empty" : "apoapsis.hud.fuel.low")
                    .getString();
            UiTextSmooth.drawRight(g, UiText.Type.LABEL, word, right, top - LABEL_CAP - 5, fuelColour);
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int labelW, String labelKey, double fraction,
                            int fill, int valueArgb, String value, String unit, int valueW) {
        caption(g, x, y, labelKey);
        int bx = x + labelW;
        int by = y + (UiNumber.CAP_HEIGHT - BAR_H) / 2;
        g.fill(bx, by, bx + BAR_W, by + BAR_H, UiPalette.alpha(UiPalette.STEEL_600, 0xA0));
        int filled = (int) Math.round(BAR_W * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            g.fill(bx, by, bx + filled, by + BAR_H, fill);
        }
        int valueRight = bx + BAR_W + 5 + valueW;
        UiNumber.drawRight(g, value, valueRight, y, valueArgb);
        UiNumber.drawUnit(g, unit, valueRight + UNIT_GAP, y, UiPalette.INK_MUTED);
    }

    /**
     * Draws the navball with the heading above it and the SAS and warp status either side. Returns the spot used.
     * Call between {@code UiScale.begin} and {@code end}.
     */
    public static BallSpot drawBall(GuiGraphicsExtractor g, HudLayout lay, Telemetry t, double[] axes, double[] up,
                                    double[] vel, double heading) {
        BallSpot b = ballSpot(lay);
        NavballView.draw(g, lay.scale(), b.cx(), b.cy(), b.radius(), axes, up, vel);
        if (!Double.isNaN(heading)) {
            String hdg = String.format("%03d", (int) Math.round(heading) % 360);
            int w = Math.round(UiNumber.width(hdg)) + 10;
            int h = UiNumber.CAP_HEIGHT + 6;
            int x0 = b.cx() - w / 2;
            int y1 = b.cy() - b.half() - 2;
            g.fill(x0, y1 - h, x0 + w, y1, UiPalette.alpha(UiPalette.STEEL_950, 0xB8));
            g.fill(x0, y1 - 1, x0 + w, y1, UiPalette.alpha(UiPalette.BRASS, 0xC0));
            UiNumber.draw(g, hdg, x0 + 5, y1 - h + 3, UiPalette.INK);
        }
        // Status beside the ball: SAS to the left, warp to the right, at the ball's centre line, each on a faint plate.
        int mid = b.cy() - LABEL_CAP / 2;
        String sas = Component.translatable(t.sasKey()).getString();
        int sasRight = b.cx() - b.half() - 8;
        int sasW = Math.round(UiTextSmooth.width(UiText.Type.LABEL, sas));
        g.fill(sasRight - sasW - 4, mid - 3, sasRight + 4, mid + LABEL_CAP + 3, PLATE);
        UiTextSmooth.drawRight(g, UiText.Type.LABEL, sas, sasRight, mid, t.sasActive() ? UiPalette.GO : UiPalette.INK_MUTED);
        if (t.warp() > 1) {
            String warp = Component.translatable("apoapsis.hud.warp").getString() + " " + formatWarp(t.warp());
            int wx = b.cx() + b.half() + 8;
            int ww = Math.round(UiTextSmooth.width(UiText.Type.LABEL, warp));
            g.fill(wx - 4, mid - 3, wx + ww + 4, mid + LABEL_CAP + 3, PLATE);
            UiTextSmooth.draw(g, UiText.Type.LABEL, warp, wx, mid, UiPalette.BRASS);
        }
        return b;
    }

    private static String formatWarp(float w) {
        return w == Math.rint(w) ? "x" + (long) w : String.format(java.util.Locale.ROOT, "x%.1f", w);
    }
}
