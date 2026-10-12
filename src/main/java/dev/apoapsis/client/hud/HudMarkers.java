package dev.apoapsis.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The flight markers drawn in the world view: the prograde and retrograde reticles, the target bracket and the horizon
 * bracket. The reticles and the bracket are small anti-aliased sprites, painted once per window size and placed at a
 * device pixel; the horizon bracket is two rotated bars. Labels are kit text. Positions come in as GUI pixels, the
 * coordinates {@link HudProjection} gives. Client only.
 */
public final class HudMarkers {
    /** Half the sprite size, UI units. */
    private static final int HALF = 10;
    private static final double RING = 4.2;
    private static final int ACCENT = UiPalette.SIGNAL;
    private static final int HORIZON = UiPalette.alpha(UiPalette.SIGNAL, 0xB0);

    private static final HudSprite PROGRADE = new HudSprite("marker_prograde");
    private static final HudSprite RETROGRADE = new HudSprite("marker_retrograde");
    private static final HudSprite TARGET = new HudSprite("marker_target");
    private static int paintedUnit;

    private HudMarkers() {}

    /** The prograde reticle centred at GUI pixel (x, y), with its label when {@code labelled}. */
    public static void prograde(GuiGraphicsExtractor g, UiScale s, double x, double y, String label) {
        sprite(g, s, PROGRADE, x, y, label);
    }

    /** The retrograde reticle centred at GUI pixel (x, y), with its label. */
    public static void retrograde(GuiGraphicsExtractor g, UiScale s, double x, double y, String label) {
        sprite(g, s, RETROGRADE, x, y, label);
    }

    /** The target bracket centred at GUI pixel (x, y), with its label to the right. */
    public static void target(GuiGraphicsExtractor g, UiScale s, double x, double y, String label) {
        sprite(g, s, TARGET, x, y, label);
    }

    private static void ensure(int unit) {
        if (paintedUnit == unit && PROGRADE.has(2 * HALF * unit, 2 * HALF * unit)) {
            return;
        }
        int size = 2 * HALF * unit;
        paint(PROGRADE.pixels(size, size), size, unit, 0);
        PROGRADE.upload();
        paint(RETROGRADE.pixels(size, size), size, unit, 1);
        RETROGRADE.upload();
        paint(TARGET.pixels(size, size), size, unit, 2);
        TARGET.upload();
        paintedUnit = unit;
    }

    private static void sprite(GuiGraphicsExtractor g, UiScale s, HudSprite sprite, double x, double y, String label) {
        ensure(s.unit());
        double gs = Minecraft.getInstance().getWindow().getGuiScale();
        int size = sprite.width();
        int dx = (int) Math.round(x * gs) - size / 2;
        int dy = (int) Math.round(y * gs) - size / 2;
        g.pose().pushMatrix();
        g.pose().scale((float) (1.0 / gs), (float) (1.0 / gs));
        g.blit(sprite.id(), dx, dy, dx + size, dy + size, 0f, 1f, 0f, 1f);
        g.pose().popMatrix();
        if (label != null) {
            s.begin(g, 0, 0);
            float ux = (float) (x / s.scale());
            float uy = (float) (y / s.scale());
            int capTop = Math.round(uy) - UiText.Type.LABEL.capHeight() / 2;
            float off = sprite == TARGET ? 13f : (float) (RING + 6);
            UiTextSmooth.draw(g, UiText.Type.LABEL, label, ux + off, capTop, ACCENT);
            s.end(g);
        }
    }

    /**
     * The horizon bracket: two short bars either side of GUI pixel (x, y), along direction (dirX, dirY) (unit length).
     * The gap and length are in UI units.
     */
    public static void horizon(GuiGraphicsExtractor g, UiScale s, double x, double y, double dirX, double dirY) {
        double gs = Minecraft.getInstance().getWindow().getGuiScale();
        int unit = s.unit();
        int thick = Math.max(1, Math.round(0.35f * unit));
        int gap = 10 * unit;
        int len = 12 * unit;
        g.pose().pushMatrix();
        g.pose().scale((float) (1.0 / gs), (float) (1.0 / gs));
        g.pose().translate((float) (x * gs), (float) (y * gs));
        g.pose().rotate((float) Math.atan2(dirY, dirX));
        g.fill(gap, -thick / 2, gap + len, -thick / 2 + thick, HORIZON);
        g.fill(-gap - len, -thick / 2, -gap, -thick / 2 + thick, HORIZON);
        g.pose().popMatrix();
    }

    private static void paint(NativeImage img, int size, int unit, int kind) {
        double c = size / 2.0;
        double r = RING * unit;
        double hw = 0.3 * unit;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double x = px + 0.5 - c;
                double y = py + 0.5 - c;
                float cov = switch (kind) {
                    case 0 -> HudSprite.stroke(Math.min(Math.abs(Math.hypot(x, y) - r), Math.min(
                            HudSprite.segment(x, y, -r - 3.4 * unit, 0, -r - 0.8 * unit, 0), Math.min(
                                    HudSprite.segment(x, y, r + 0.8 * unit, 0, r + 3.4 * unit, 0),
                                    HudSprite.segment(x, y, 0, -r - 3.4 * unit, 0, -r - 0.8 * unit)))), hw);
                    case 1 -> {
                        double cross = r * 0.62;
                        double d = Math.min(HudSprite.segment(x, y, -cross, -cross, cross, cross),
                                HudSprite.segment(x, y, -cross, cross, cross, -cross));
                        yield HudSprite.stroke(Math.min(Math.abs(Math.hypot(x, y) - r), d), hw);
                    }
                    default -> {
                        double h = 8.5 * unit;
                        double leg = 3.5 * unit;
                        double ax = Math.abs(x);
                        double ay = Math.abs(y);
                        double d = Math.min(HudSprite.segment(ax, ay, h, h, h - leg, h),
                                HudSprite.segment(ax, ay, h, h, h, h - leg));
                        yield HudSprite.stroke(d, hw);
                    }
                };
                // A faint dark halo keeps the thin lines readable over a bright planet.
                img.setPixel(px, py, cov <= 0 ? 0 : (Math.round(cov * 255) << 24) | (ACCENT & 0xFFFFFF));
            }
        }
    }
}
