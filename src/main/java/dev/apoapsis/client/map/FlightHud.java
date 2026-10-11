package dev.apoapsis.client.map;

import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The flight HUD: one compact row above the hotbar and a small block at the top right. Drawn only while the local
 * player rides a ship, with no screen open and the HUD not hidden. Nothing is drawn in the middle third of the view.
 * Client only.
 */
public final class FlightHud {
    private static final int SLOT_GAP = 10;
    private static final int SLOT_THR = 58;
    private static final int SLOT_ALT = 80;
    private static final int SLOT_SPD = 88;
    private static final int SLOT_VS = 76;
    private static final int SLOT_FUEL = 92;
    private static final int SLOT_DV = 84;
    private static final int SLOT_TWR = 48;
    /** Bar offsets within a slot (after the label) and bar width. */
    private static final int BAR_X_THR = 22;
    private static final int BAR_X_FUEL = 26;
    private static final int BAR_W = 36;
    private static final int BAR_H = 3;
    /** Row distance above the bottom edge: clear of the status rows (hearts, armour, air) in survival. */
    private static final int ROW_BAND_SURVIVAL = 58;
    private static final int ROW_BAND_CREATIVE = 38;
    private static final int PAD = 10;
    private static final int LINE = 10;
    private static final int WARP_GAP = 8;

    private FlightHud() {}

    /** The GUI layer. Registered above the hotbar by {@link dev.apoapsis.client.MapClient}. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.gui.hud.isHidden()) {
            return;
        }
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) {
            return;
        }
        OrbitStatePayload p = OrbitClock.current();
        if (p == null) {
            return;
        }
        Canvas c = new Canvas(graphics);
        CelestialBody body = Bodies.of(p);
        double r = Math.sqrt(p.rx() * p.rx() + p.ry() * p.ry() + p.rz() * p.rz());
        double speed = Math.sqrt(p.vx() * p.vx() + p.vy() * p.vy() + p.vz() * p.vz());
        double vs = r > 0 ? (p.vx() * p.rx() + p.vy() * p.ry() + p.vz() * p.rz()) / r : 0;
        double alt = r - body.radius;
        double gravity = body.gravity(r);
        double pf = Bodies.pressureFraction(body, alt);
        double mass = ship.massKg();
        double twr = mass > 0 && gravity > 0 ? ship.maxThrustN(pf) / (mass * gravity) : 0;
        double capacity = ship.fuelCapacityKg();
        double fuel = capacity > 0 ? ship.fuelKg() / capacity : 0;

        drawRow(c, ship, alt, speed, vs, fuel, twr, pf);
        drawTopRight(c, p, body, ship.isSasOn());
    }

    private static void drawRow(Canvas c, ShipEntity ship, double alt, double speed, double vs, double fuel,
                                double twr, double pf) {
        boolean survival = Minecraft.getInstance().gameMode != null
                && Minecraft.getInstance().gameMode.canHurtPlayer();
        int y = c.height - (survival ? ROW_BAND_SURVIVAL : ROW_BAND_CREATIVE);
        int total = SLOT_THR + SLOT_ALT + SLOT_SPD + SLOT_VS + SLOT_FUEL + SLOT_DV + SLOT_TWR + 6 * SLOT_GAP;
        int x = (c.width - total) / 2;

        label(c, "apoapsis.hud.label.throttle", x, y);
        bar(c, x + BAR_X_THR, y + 3, ship.getThrottle(), Palette.BRASS);
        x += SLOT_THR + SLOT_GAP;

        x = slot(c, x, y, SLOT_ALT, "apoapsis.hud.label.alt", Fmt.distance(alt));
        x = slot(c, x, y, SLOT_SPD, "apoapsis.hud.label.speed", Fmt.speed(speed));
        x = slot(c, x, y, SLOT_VS, "apoapsis.hud.label.vs", Fmt.signedSpeed(vs));

        label(c, "apoapsis.hud.label.fuel", x, y);
        bar(c, x + BAR_X_FUEL, y + 3, fuel, Palette.HYDROGEN);
        value(c, x + SLOT_FUEL, y, Fmt.percent(fuel));
        x += SLOT_FUEL + SLOT_GAP;

        x = slot(c, x, y, SLOT_DV, "apoapsis.hud.label.dv", Fmt.speed(ship.deltaV(pf)));
        slot(c, x, y, SLOT_TWR, "apoapsis.hud.label.twr", Fmt.ratio(twr));
    }

    private static void drawTopRight(Canvas c, OrbitStatePayload p, CelestialBody body, boolean sas) {
        int right = c.width - PAD;
        int y = PAD;
        boolean orbit = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
        Component regime = Component.translatable("apoapsis.hud.regime",
                Component.translatable(MapScreen.regimeKey(p.regime())),
                Component.translatable("apoapsis.map.body." + body.id));
        rightAligned(c, regime, right, y, Palette.INK_MUTED);
        y += LINE;

        if (orbit) {
            Elements el = Elements.of(body.mu, Bodies.stateOf(p));
            if (el.eccentricity() < 1) {
                Component apsides = Component.translatable("apoapsis.hud.apsides",
                        Fmt.distance(el.apoapsisRadius() - body.radius),
                        Fmt.distance(el.periapsisRadius() - body.radius));
                rightAligned(c, apsides, right, y, Palette.INK_MUTED);
                y += LINE;
            }
        }

        // The bottom line: warp (when above 1), then the SAS lamp at the right edge.
        int x = right;
        if (p.warp() > 1) {
            Component warp = Component.literal(Fmt.warp(p.warp()));
            x -= c.textWidth(warp);
            c.text(warp, x, y, 1f, Palette.INK, true);
            x -= WARP_GAP;
        }
        Component lamp = Component.translatable("apoapsis.hud.label.sas");
        x -= c.textWidth(lamp);
        c.text(lamp, x, y, 1f, sas ? Palette.GO : Palette.INK_MUTED, true);
    }

    /** A slot: label at the left, value right-aligned at the slot's right edge. Returns the next x. */
    private static int slot(Canvas c, int x, int y, int width, String labelKey, String value) {
        label(c, labelKey, x, y);
        value(c, x + width, y, value);
        return x + width + SLOT_GAP;
    }

    private static void label(Canvas c, String key, int x, int y) {
        c.text(Component.translatable(key), x, y, 1f, Palette.INK_MUTED, true);
    }

    private static void value(Canvas c, int rightEdge, int y, String value) {
        Component v = Component.literal(value);
        c.text(v, rightEdge - c.textWidth(v), y, 1f, Palette.INK, true);
    }

    private static void rightAligned(Canvas c, Component text, int right, int y, int argb) {
        c.text(text, right - c.textWidth(text), y, 1f, argb, true);
    }

    private static void bar(Canvas c, int x, int y, double fraction, int fill) {
        double f = Math.max(0, Math.min(1, fraction));
        c.rect(x, y, x + BAR_W, y + BAR_H, Palette.STEEL_700);
        int w = (int) Math.round(BAR_W * f);
        if (w > 0) {
            c.rect(x, y, x + w, y + BAR_H, fill);
        }
    }
}
