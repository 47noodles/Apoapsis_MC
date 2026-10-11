package dev.apoapsis.client.hud;

import dev.apoapsis.client.ship.ReentryState;
import dev.apoapsis.client.ship.ReentryState.Heat;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The re-entry glow, seen in first person: a faint warm tint along the screen edges while the ship the player rides
 * heats up. Concentric frames, together a sixth of the screen deep on each axis, have an alpha that rises towards the
 * edge. The centre two thirds of the view is never touched and the strongest edge alpha is {@link #MAX_ALPHA}. Drawn
 * only while the local player rides a ship in first person, with no screen open and the HUD not hidden. Client only.
 */
public final class ReentryVignette {
    /** Concentric frames from the screen edge inwards. */
    private static final int RINGS = 24;
    /** Frame depth as a share of the screen size, on each axis. */
    private static final double BAND = 0.16;
    /** The alpha at the very edge at full intensity, before the flicker. */
    private static final double MAX_ALPHA = 0.20;
    /** Heating below this intensity draws nothing. */
    private static final float MIN_INTENSITY = 0.05f;
    /** The flicker scales the peak alpha by FLICKER_BASE + FLICKER_AMP * sin(time), time = ticks * FLICKER_RATE. */
    private static final double FLICKER_BASE = 0.92;
    private static final double FLICKER_AMP = 0.08;
    private static final double FLICKER_RATE = 0.35;
    /** The tint: orange at no intensity, blending to pale at full. Red stays at full. */
    private static final int RED = 255;
    private static final int ORANGE_GREEN = 110;
    private static final int ORANGE_BLUE = 30;
    private static final int PALE_GREEN = 200;
    private static final int PALE_BLUE = 120;

    private ReentryVignette() {}

    /** The GUI layer. Registered below the flight HUD by {@link dev.apoapsis.client.MapClient}. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
            return;
        }
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) {
            return;
        }
        Heat heat = ReentryState.of(ship);
        if (heat.intensity() < MIN_INTENSITY) {
            return;
        }
        float intensity = Math.min(1f, heat.intensity());
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        double time = ((double) mc.level.getGameTime() + partialTick) * FLICKER_RATE;
        double peak = MAX_ALPHA * intensity * (FLICKER_BASE + FLICKER_AMP * Math.sin(time));
        int rgb = tint(intensity);

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int depthX = (int) Math.round(w * BAND);
        int depthY = (int) Math.round(h * BAND);
        // Concentric frames from the outside in. Each frame is four fills of one alpha that do not overlap, so the corners
        // blend into the sides and no seam shows where a band meets its neighbour.
        for (int j = 0; j < RINGS; j++) {
            int x0 = offset(depthX, j);
            int x1 = offset(depthX, j + 1);
            int y0 = offset(depthY, j);
            int y1 = offset(depthY, j + 1);
            int argb = ringArgb(peak, j, rgb);
            graphics.fill(x0, y0, w - x0, y1, argb);
            graphics.fill(x0, h - y1, w - x0, h - y0, argb);
            graphics.fill(x0, y1, x1, h - y1, argb);
            graphics.fill(w - x1, y1, w - x0, h - y1, argb);
        }
    }

    /** The pixel offset of frame boundary {@code j} from the screen edge, for a band {@code depth} px deep. */
    private static int offset(int depth, int j) {
        return (int) Math.round((double) depth * j / RINGS);
    }

    /** The ARGB of frame {@code j} counted from the screen edge: alpha is peak * f^2, f = 1 at the edge, capped. */
    private static int ringArgb(double peak, int j, int rgb) {
        double f = (double) (RINGS - j) / RINGS;
        double alpha = Math.min(MAX_ALPHA, peak * f * f);
        return ((int) Math.round(alpha * 255) << 24) | rgb;
    }

    /** Orange at no intensity, blending to pale at full intensity. */
    private static int tint(float intensity) {
        int green = Math.round(ORANGE_GREEN + (PALE_GREEN - ORANGE_GREEN) * intensity);
        int blue = Math.round(ORANGE_BLUE + (PALE_BLUE - ORANGE_BLUE) * intensity);
        return (RED << 16) | (green << 8) | blue;
    }
}
