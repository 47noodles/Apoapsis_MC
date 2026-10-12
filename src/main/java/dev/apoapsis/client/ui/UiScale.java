package dev.apoapsis.client.ui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The kit's own grid. Menus are laid out in "UI units" instead of GUI pixels, so a screen looks the same at every GUI
 * scale: one UI unit is {@code unit} device pixels, chosen from the window height (about 360 UI units tall: 2 device
 * pixels per unit at 720p, 3 at 1080p, 4 at 1440p). {@link #scale()} converts to GUI pixels, the coordinates widgets
 * and the mouse use. Draw in UI units between {@link #begin} and {@link #end}. Client only.
 */
public final class UiScale {
    /** Target height of the virtual canvas, in UI units. */
    private static final int TARGET_HEIGHT = 400;

    private final int unit;
    private final float scale;
    private final int guiWidth;
    private final int guiHeight;

    private UiScale(int unit, float scale, int guiWidth, int guiHeight) {
        this.unit = unit;
        this.scale = scale;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
    }

    /** The grid for the current window and GUI scale setting. */
    public static UiScale current() {
        Window w = Minecraft.getInstance().getWindow();
        int unit = Math.max(1, Math.round((float) w.getHeight() / TARGET_HEIGHT));
        int guiScale = Math.max(1, w.getGuiScale());
        return new UiScale(unit, (float) unit / guiScale, w.getGuiScaledWidth(), w.getGuiScaledHeight());
    }

    /** Device pixels per UI unit. */
    public int unit() {
        return unit;
    }

    /** GUI pixels per UI unit. */
    public float scale() {
        return scale;
    }

    /** Width of the screen in UI units. */
    public int width() {
        return Math.round(guiWidth / scale);
    }

    /** Height of the screen in UI units. */
    public int height() {
        return Math.round(guiHeight / scale);
    }

    /** UI units to GUI pixels, rounded. */
    public int gui(float ui) {
        return Math.round(ui * scale);
    }

    /** GUI pixels to UI units. */
    public float ui(float gui) {
        return gui / scale;
    }

    /** Starts drawing in UI units with the origin at GUI pixel ({@code guiX}, {@code guiY}). Pair with {@link #end}. */
    public void begin(GuiGraphicsExtractor g, float guiX, float guiY) {
        g.pose().pushMatrix();
        g.pose().translate(guiX, guiY);
        g.pose().scale(scale, scale);
    }

    /** Ends a {@link #begin}. */
    public void end(GuiGraphicsExtractor g) {
        g.pose().popMatrix();
    }
}
