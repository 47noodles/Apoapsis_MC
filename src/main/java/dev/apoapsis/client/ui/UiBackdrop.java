package dev.apoapsis.client.ui;

import com.mojang.blaze3d.platform.Window;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The calm full-screen ground of the menus: near-black steel with a field of fixed stars, drawn in device pixels so
 * the stars stay one pixel at any GUI scale. Positions come from a fixed seed as fractions of the window, so the sky
 * does not jump on a resize. Client only.
 */
public final class UiBackdrop {
    private static final int STARS = 520;
    private static final float[] SX = new float[STARS];
    private static final float[] SY = new float[STARS];
    private static final int[] SA = new int[STARS];
    private static final int[] SIZE = new int[STARS];
    private static final boolean[] WARM = new boolean[STARS];

    static {
        Random rnd = new Random(0xA90A515L);
        for (int i = 0; i < STARS; i++) {
            SX[i] = rnd.nextFloat();
            SY[i] = rnd.nextFloat();
            float b = rnd.nextFloat();
            SA[i] = 40 + (int) (b * b * b * 190);
            SIZE[i] = b > 0.93f ? 2 : 1;
            WARM[i] = rnd.nextInt(7) == 0;
        }
    }

    private UiBackdrop() {}

    /** Fills the screen with steel and stars. */
    public static void draw(GuiGraphicsExtractor g) {
        Window win = Minecraft.getInstance().getWindow();
        int dw = win.getWidth();
        int dh = win.getHeight();
        float inv = 1f / Math.max(1, win.getGuiScale());
        g.pose().pushMatrix();
        g.pose().scale(inv, inv);
        g.fill(0, 0, dw, dh, UiPalette.STEEL_950);
        for (int i = 0; i < STARS; i++) {
            int x = (int) (SX[i] * (dw - 2));
            int y = (int) (SY[i] * (dh - 2));
            int c = UiPalette.alpha(WARM[i] ? UiPalette.BRASS : UiPalette.INK, SA[i]);
            g.fill(x, y, x + SIZE[i], y + SIZE[i], c);
        }
        g.pose().popMatrix();
    }

    /** A dark veil over a rendered scene, strongest at the left where the menu sits, for menus over the world. */
    public static void veil(GuiGraphicsExtractor g, float leftAlpha, float rightAlpha) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int strips = 32;
        for (int i = 0; i < strips; i++) {
            float t = (float) i / (strips - 1);
            float a = leftAlpha + (rightAlpha - leftAlpha) * t;
            g.fill(w * i / strips, 0, w * (i + 1) / strips, h, UiPalette.alpha(UiPalette.STEEL_950, Math.round(a * 255)));
        }
    }
}
