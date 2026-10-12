package dev.apoapsis.client.map;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Landing;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.client.hud.HudFormat;
import dev.apoapsis.client.ui.UiNumber;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The landing readout beside the bottom cluster: the radar altitude, and while descending the cue for when to start a
 * full-thrust retro burn. Shown only near the ground or on a fast descent, so the HUD stays quiet in the air. Client
 * only.
 */
final class LandingReadout {
    /** Row pitch and the width of the label column, UI units. */
    private static final int ROW = 13;
    private static final int LABEL_W = 30;
    private static final double MAX_RADAR_M = 1500;
    private static final double NEAR_GROUND_M = 25;
    private static final double DESCENT_MPS = 2.0;
    private static final double ALERT_RADAR_M = 30;
    private static final double ALERT_VS_MPS = -6;
    private static final double SOON_S = 10;

    private LandingReadout() {}

    /**
     * Draws the radar altitude, and while descending the burn cue under it, in UI units: left edge {@code x}, the last
     * row's capitals ending at {@code bottomY}. Call between {@code UiScale.begin} and {@code end}. Draws nothing unless
     * the ship is near the ground or descending fast, and not landed. Calm accents only: the cue turns brass when the
     * burn is near and copper-red at the moment, never louder.
     */
    static void draw(GuiGraphicsExtractor g, ShipEntity ship, double radar, double vs, CelestialBody body,
                     double altAboveSeaM, int x, int bottomY) {
        if (!Double.isFinite(radar) || ship.isLanded() || radar > MAX_RADAR_M
                || (vs > -DESCENT_MPS && radar > NEAR_GROUND_M)) {
            return;
        }
        String cue = null;
        int cueArgb = UiPalette.INK_MUTED;
        double vDown = -vs;
        if (vDown > DESCENT_MPS && ship.fuelKg() > 0) {
            // Ground pressure is the worst case: rocket thrust falls as the ambient pressure rises.
            double pf = Bodies.pressureFraction(body, altAboveSeaM - radar);
            double thrustAccel = ship.maxThrustN(pf) / ship.massKg();
            double gr = body.gravity(body.radius + altAboveSeaM - radar);
            double t = Landing.burnStartIn(radar, vDown, gr, thrustAccel);
            if (Double.isNaN(t)) {
                cue = Component.translatable("apoapsis.hud.burn.low").getString();
                cueArgb = UiPalette.ALERT;
            } else if (t == 0) {
                cue = Component.translatable("apoapsis.hud.burn.now").getString();
                cueArgb = UiPalette.ALERT;
            } else if (!Double.isInfinite(t)) {
                cue = Component.translatable("apoapsis.hud.burn.in", String.format(Locale.ROOT, "%.1f s", t))
                        .getString();
                cueArgb = t <= SOON_S ? UiPalette.BRASS : UiPalette.INK_MUTED;
            }
        }
        int cap = UiNumber.CAP_HEIGHT;
        String[] parts = HudFormat.distance(radar);
        float width = LABEL_W + UiNumber.width(parts[0]) + 3 + UiNumber.unitWidth(parts[1]);
        if (cue != null) {
            width = Math.max(width, UiTextSmooth.width(UiText.Type.LABEL, cue));
        }
        int plateTop = bottomY - cap - (cue != null ? ROW : 0) - 4;
        g.fill(x - 5, plateTop, Math.round(x + width) + 6, bottomY + 4, UiPalette.alpha(UiPalette.STEEL_950, 0x99));
        int rowTop = bottomY - cap;
        if (cue != null) {
            UiTextSmooth.draw(g, UiText.Type.LABEL, cue, x, bottomY - UiText.Type.LABEL.capHeight(), cueArgb);
            rowTop -= ROW;
        }
        int valueArgb = radar < ALERT_RADAR_M && vs < ALERT_VS_MPS ? UiPalette.COPPER : UiPalette.INK;
        String label = Component.translatable("apoapsis.hud.label.radar").getString();
        UiTextSmooth.draw(g, UiText.Type.LABEL, label, x, rowTop + cap - UiText.Type.LABEL.capHeight(), UiPalette.INK_MUTED);
        int vx = x + LABEL_W;
        UiNumber.draw(g, parts[0], vx, rowTop, valueArgb);
        UiNumber.drawUnit(g, parts[1], vx + UiNumber.width(parts[0]) + 3, rowTop, UiPalette.INK_MUTED);
    }
}
