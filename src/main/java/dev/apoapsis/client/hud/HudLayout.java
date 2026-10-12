package dev.apoapsis.client.hud;

import dev.apoapsis.client.ui.UiScale;
import net.minecraft.client.Minecraft;

/**
 * The one safe-area helper for everything the mod draws over the flight view. Layout is in UI units (see
 * {@link UiScale}), so it looks the same at every window size and GUI scale. Elements hug the screen edges by
 * {@link #MARGIN}; the middle of the view stays free except for the flight markers.
 *
 * <p>Survival keeps vanilla's health, armour and hunger rows at the bottom centre, drawn in GUI pixels, so how many UI
 * units they take depends on the GUI scale. {@link #bottomFor} gives the lowest y an element may use over a span of x:
 * the bottom margin, or just above those rows when the span reaches them. Client only.
 */
public final class HudLayout {
    /** Distance from the screen edge, UI units. */
    public static final int MARGIN = 10;
    /** The top margin, UI units: clear of the canopy frame's top strut and its brass line in first person. */
    public static final int TOP_MARGIN = 15;
    /** The bottom margin of elements that sit on the bottom edge, UI units. */
    public static final int BOTTOM_MARGIN = 8;
    /** The y where a block under the regime label at the top right starts, UI units. */
    public static final int BELOW_REGIME = TOP_MARGIN + 16;
    /** Space kept between an element and the vanilla rows it clears, UI units. */
    private static final int CLEAR = 4;
    /** Vanilla's status rows: half the width of the hunger and health rows from the centre, and the height with armour. */
    private static final int BARS_HALF_WIDTH_GUI = 91;
    private static final int BARS_HEIGHT_GUI = 52;

    private final UiScale scale;
    private final boolean survival;

    private HudLayout(UiScale scale, boolean survival) {
        this.scale = scale;
        this.survival = survival;
    }

    /** The layout for the current window, GUI scale and game mode. */
    public static HudLayout current() {
        Minecraft mc = Minecraft.getInstance();
        boolean survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        return new HudLayout(UiScale.current(), survival);
    }

    /** The kit's grid for this window. */
    public UiScale scale() {
        return scale;
    }

    /** Whether vanilla's health and hunger rows are on screen. */
    public boolean survival() {
        return survival;
    }

    public int width() {
        return scale.width();
    }

    public int height() {
        return scale.height();
    }

    public int left() {
        return MARGIN;
    }

    public int right() {
        return width() - MARGIN;
    }

    public int top() {
        return TOP_MARGIN;
    }

    /** The bottom edge of an element that sits on the bottom edge. */
    public int bottom() {
        return height() - BOTTOM_MARGIN;
    }

    /**
     * The canopy frame (first person) cuts each top corner with a diagonal gusset; its size is a share of the screen,
     * mirrored here from the frame so top-row elements stay clear of it: the side post, the top strut and the gusset leg.
     */
    private static final double CANOPY_SIDE = 0.011;
    private static final double CANOPY_TOP = 0.030;
    private static final double CANOPY_LEG = 0.070;
    private static final int CANOPY_GAP = 3;

    /** How far in from a side edge the gusset leaves room for an element whose top edge is at {@code y}, UI units. */
    private int cornerInset(int y) {
        double len = CANOPY_LEG * height() - (y - CANOPY_TOP * height());
        return (int) Math.ceil(CANOPY_SIDE * width() + Math.max(0, len) + CANOPY_GAP);
    }

    /** The left edge for an element whose top edge is at {@code y}: the margin, or clear of the canopy gusset. */
    public int leftAt(int y) {
        return Math.max(left(), cornerInset(y));
    }

    /** The right edge for an element whose top edge is at {@code y}: the margin, or clear of the canopy gusset. */
    public int rightAt(int y) {
        return Math.min(right(), width() - cornerInset(y));
    }

    public int centreX() {
        return width() / 2;
    }

    /** The left edge of the middle third of the view. */
    public int centreLeft() {
        return width() / 3;
    }

    /** The right edge of the middle third of the view. */
    public int centreRight() {
        return width() * 2 / 3;
    }

    /** The top of vanilla's status rows, UI units; the screen bottom when they are not shown. */
    public int barsTop() {
        return survival ? height() - Math.round(BARS_HEIGHT_GUI / scale.scale()) : height();
    }

    private int barsHalfWidth() {
        return Math.round(BARS_HALF_WIDTH_GUI / scale.scale());
    }

    /**
     * The right limit for an element on a row whose bottom is {@code yBottom}: stops short of vanilla's status rows when
     * the row reaches them, else the centre third's left edge.
     */
    public int rowRightLimit(int yBottom) {
        int limit = centreLeft() - 8;
        if (survival && yBottom > barsTop() - CLEAR) {
            limit = Math.min(limit, centreX() - barsHalfWidth() - CLEAR - 4);
        }
        return limit;
    }

    /**
     * The lowest y, UI units, for an element spanning x0..x1: {@link #bottom} unless the span reaches vanilla's status
     * rows, then just above them.
     */
    public int bottomFor(int x0, int x1) {
        if (!survival) {
            return bottom();
        }
        boolean reaches = x1 > centreX() - barsHalfWidth() - CLEAR && x0 < centreX() + barsHalfWidth() + CLEAR;
        return reaches ? Math.min(bottom(), barsTop() - CLEAR) : bottom();
    }
}
