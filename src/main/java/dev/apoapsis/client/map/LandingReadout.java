package dev.apoapsis.client.map;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Landing;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The landing readout beside the bottom cluster: the radar altitude, and while descending the cue for when to start a
 * full-thrust retro burn. Shown only near the ground or on a fast descent, so the HUD stays quiet in the air. Client
 * only.
 */
final class LandingReadout {
    private static final int PILL_FILL = Palette.alpha(Palette.STEEL_950, 0xB8);
    private static final int LINE = 9;
    private static final int GAP = 8;
    private static final double MAX_RADAR_M = 1500;
    private static final double NEAR_GROUND_M = 25;
    private static final double DESCENT_MPS = 2.0;
    private static final double ALERT_RADAR_M = 30;
    private static final double ALERT_VS_MPS = -6;
    private static final double SOON_S = 10;

    private LandingReadout() {}

    /**
     * Draws the radar row, and the burn cue row while descending, as a pill whose bottom edge sits at {@code bottomY}
     * and whose left edge is {@code x}. Draws nothing unless the ship is near the ground or descending fast, and not
     * landed.
     */
    static void draw(Canvas c, ShipEntity ship, double radar, double vs, CelestialBody body, double altAboveSeaM, int x,
                     int bottomY, double scale) {
        if (!Double.isFinite(radar) || ship.isLanded() || radar > MAX_RADAR_M
                || (vs > -DESCENT_MPS && radar > NEAR_GROUND_M)) {
            return;
        }
        Component cue = null;
        int cueArgb = Palette.INK_MUTED;
        double vDown = -vs;
        if (vDown > DESCENT_MPS && ship.fuelKg() > 0) {
            // Ground pressure is the worst case: rocket thrust falls as the ambient pressure rises.
            double pf = Bodies.pressureFraction(body, altAboveSeaM - radar);
            double thrustAccel = ship.maxThrustN(pf) / ship.massKg();
            double g = body.gravity(body.radius + altAboveSeaM - radar);
            double t = Landing.burnStartIn(radar, vDown, g, thrustAccel);
            if (Double.isNaN(t)) {
                cue = Component.translatable("apoapsis.hud.burn.low");
                cueArgb = Palette.ALERT;
            } else if (t == 0) {
                cue = Component.translatable("apoapsis.hud.burn.now");
                cueArgb = Palette.ALERT;
            } else if (!Double.isInfinite(t)) {
                cue = Component.translatable("apoapsis.hud.burn.in", String.format(Locale.ROOT, "%.1f s", t));
                cueArgb = t <= SOON_S ? Palette.BRASS : Palette.INK_MUTED;
            }
        }

        float fs = (float) scale;
        int lineH = px(LINE, scale);
        int rows = cue == null ? 1 : 2;
        int rowTop = bottomY - rows * lineH;
        Component label = Component.translatable("apoapsis.hud.label.radar");
        Component value = Component.literal(Fmt.distance(radar));
        int valueArgb = radar < ALERT_RADAR_M && vs < ALERT_VS_MPS ? Palette.ALERT : Palette.INK;
        int labelW = (int) Math.round(c.textWidth(label) * scale);
        int valueW = (int) Math.round(c.textWidth(value) * scale);
        int width = labelW + px(GAP, scale) + valueW;
        if (cue != null) {
            width = Math.max(width, (int) Math.round(c.textWidth(cue) * scale));
        }
        int right = x + width;
        c.rect(x - 3, rowTop - 1, right + 3, bottomY, PILL_FILL);
        c.text(label, x, rowTop, fs, Palette.INK_MUTED, true);
        c.text(value, right - valueW, rowTop, fs, valueArgb, true);
        if (cue != null) {
            c.text(cue, x, rowTop + lineH, fs, cueArgb, true);
        }
    }

    /** A unit-scale length at scale {@code s}, rounded to GUI px. */
    private static int px(int v, double s) {
        return (int) Math.round(v * s);
    }
}
