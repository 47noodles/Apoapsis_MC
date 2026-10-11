package dev.apoapsis.client.map;

import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The flight HUD: two readout clusters hugging the bottom corners, above the hotbar and clear of the status rows, and
 * a block at the top right. Drawn only while the local player rides a ship, with no screen open and the HUD not
 * hidden. Each cluster is at most {@link #CLUSTER_MAX_FRACTION} of the GUI width. Client only.
 */
public final class FlightHud {
    /**
     * Text top, GUI px above the bottom edge, in survival. The status rows reach up to y = h-59 and the selected item
     * name sits at y = h-59, so the row goes above both.
     */
    private static final int ROW_BAND_SURVIVAL = 70;
    /**
     * Text top in creative, where there are no status rows. The selected item name sits at y = h-45 and the row's
     * bottom edge is 4 px above the hotbar top (h-22).
     */
    private static final int ROW_BAND_CREATIVE = 35;
    private static final int PAD = 10;
    private static final int LINE = 10;
    private static final int WARP_GAP = 8;
    /** Each cluster takes at most this share of the GUI width, measured from its corner. */
    private static final double CLUSTER_MAX_FRACTION = 0.45;
    /** Below this GUI width the row steps down one size before it is fitted to its clusters. */
    private static final int NARROW_GUI = 400;
    private static final double NARROW_SCALE = 0.8;
    /** The smallest text scale the row is fitted down to. */
    private static final float MIN_SCALE = 0.5f;
    private static final int BAR_W = 36;
    private static final int BAR_H = 3;
    private static final int LABEL_GAP = 3;
    private static final int BAR_GAP = 4;
    private static final int READOUT_GAP = 8;
    /** A throttle bar with no fuel: the setting is shown, dimmed. */
    private static final int DIMMED_BRASS = Palette.alpha(Palette.BRASS, 0x55);
    /** Below this speed the nose cue is hidden, m/s. */
    private static final double NOSE_MIN_SPEED = 1.0;

    /** A piece of a cluster, laid out left to right. Widths are at scale 1, GUI px. */
    private sealed interface Part permits Text, Bar, Gap {}

    private record Text(Component text, int argb) implements Part {}

    private record Bar(double fraction, int fill) implements Part {}

    private record Gap(int px) implements Part {}

    /** The nose's angle from prograde or from retrograde, whichever is nearer, degrees. */
    private record NoseCue(double degrees, boolean prograde) {}

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
        boolean orbit = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
        // In orbit the state is advanced to this frame; on the surface the payload's own state is already smooth.
        StateVector s = orbit
                ? OrbitNow.stateNow(p, deltaTracker.getGameTimeDeltaPartialTick(false))
                : Bodies.stateOf(p);
        double r = s.r().length();
        double speed = s.v().length();
        double vs = r > 0 ? s.r().dot(s.v()) / r : 0;
        double alt = r - body.radius;
        double gravity = body.gravity(r);
        double pf = Bodies.pressureFraction(body, alt);
        double mass = ship.massKg();
        double twr = mass > 0 && gravity > 0 ? ship.maxThrustN(pf) / (mass * gravity) : 0;
        double capacity = ship.fuelCapacityKg();
        double fuel = capacity > 0 ? ship.fuelKg() / capacity : 0;

        drawRow(c, ship, alt, speed, vs, fuel, twr, pf);
        drawTopRight(c, p, body, orbit, ship.getSasMode(), noseCue(ship, s, orbit));
    }

    /** The two clusters on the bottom corners: the throttle, fuel and delta-v on the left, altitude and flight on the right. */
    private static void drawRow(Canvas c, ShipEntity ship, double alt, double speed, double vs, double fuel,
                                double twr, double pf) {
        boolean survival = Minecraft.getInstance().gameMode != null
                && Minecraft.getInstance().gameMode.canHurtPlayer();
        int top = c.height - (survival ? ROW_BAND_SURVIVAL : ROW_BAND_CREATIVE);
        boolean dry = ship.fuelKg() <= 0;
        double dv = ship.deltaV(pf);

        List<Part> left = List.of(
                label("apoapsis.hud.label.throttle"), gap(LABEL_GAP),
                new Bar(ship.getThrottle(), dry ? DIMMED_BRASS : Palette.BRASS), gap(READOUT_GAP),
                label("apoapsis.hud.label.fuel"), gap(LABEL_GAP),
                new Bar(fuel, dry ? Palette.ALERT : Palette.HYDROGEN), gap(BAR_GAP),
                value(Fmt.percent(fuel), dry ? Palette.ALERT : Palette.INK), gap(READOUT_GAP),
                label("apoapsis.hud.label.dv"), gap(LABEL_GAP),
                value(Fmt.speed(dv), dry ? Palette.ALERT : Palette.INK));
        List<Part> right = List.of(
                label("apoapsis.hud.label.alt"), gap(LABEL_GAP), value(Fmt.distance(alt), Palette.INK), gap(READOUT_GAP),
                label("apoapsis.hud.label.speed"), gap(LABEL_GAP), value(Fmt.speed(speed), Palette.INK), gap(READOUT_GAP),
                label("apoapsis.hud.label.vs"), gap(LABEL_GAP), value(Fmt.signedSpeed(vs), Palette.INK), gap(READOUT_GAP),
                label("apoapsis.hud.label.twr"), gap(LABEL_GAP), value(Fmt.ratio(twr), Palette.INK));

        float scale = rowScale(c, left, right);
        int rightWidth = width(c, right, scale);
        layout(c, left, PAD, top, scale);
        layout(c, right, c.width - PAD - rightWidth, top, scale);
    }

    /**
     * The row's text scale: one step down below {@link #NARROW_GUI}, and smaller only as far as the widest cluster needs
     * to fit {@link #CLUSTER_MAX_FRACTION} of the GUI width.
     */
    private static float rowScale(Canvas c, List<Part> left, List<Part> right) {
        double budget = c.width * CLUSTER_MAX_FRACTION;
        double widest = Math.max(width(c, left, 1f), width(c, right, 1f));
        double wanted = Math.min(c.width < NARROW_GUI ? NARROW_SCALE : 1.0, budget / widest);
        float scale = (float) Math.max(MIN_SCALE, wanted);
        while (scale > MIN_SCALE && Math.max(width(c, left, scale), width(c, right, scale)) > budget) {
            scale -= 0.01f;
        }
        return scale;
    }

    /** Lays a cluster out from ({@code x}, {@code y}) to the right. Label and value sit together, no slots. */
    private static void layout(Canvas c, List<Part> parts, int x, int y, float scale) {
        for (Part part : parts) {
            int w = (int) Math.round(partWidth(c, part) * scale);
            if (part instanceof Text t) {
                c.text(t.text(), x, y, scale, t.argb(), true);
            } else if (part instanceof Bar b) {
                bar(c, x, y + 3, w, b.fraction(), b.fill());
            }
            x += w;
        }
    }

    private static int width(Canvas c, List<Part> parts, float scale) {
        int w = 0;
        for (Part part : parts) {
            w += (int) Math.round(partWidth(c, part) * scale);
        }
        return w;
    }

    private static double partWidth(Canvas c, Part part) {
        return switch (part) {
            case Text t -> c.textWidth(t.text());
            case Bar b -> BAR_W;
            case Gap g -> g.px();
        };
    }

    private static Part label(String key) {
        return new Text(Component.translatable(key), Palette.INK_MUTED);
    }

    private static Part value(String text, int argb) {
        return new Text(Component.literal(text), argb);
    }

    private static Part gap(int px) {
        return new Gap(px);
    }

    private static void drawTopRight(Canvas c, OrbitStatePayload p, CelestialBody body, boolean orbit, SasMode sas,
                                     NoseCue nose) {
        int right = c.width - PAD;
        int y = PAD;
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

        // The SAS mode, with the warp factor to its left when time is running fast.
        Component sasText = Component.translatable(sasKey(sas));
        int x = right - c.textWidth(sasText);
        c.text(sasText, x, y, 1f, sas == SasMode.OFF ? Palette.INK_MUTED : Palette.GO, true);
        if (p.warp() > 1) {
            Component warp = Component.literal(Fmt.warp(p.warp()));
            x -= WARP_GAP + c.textWidth(warp);
            c.text(warp, x, y, 1f, Palette.INK, true);
        }
        y += LINE;

        if (nose != null) {
            Component line = Component.translatable(
                    nose.prograde() ? "apoapsis.hud.nose.prograde" : "apoapsis.hud.nose.retrograde",
                    Fmt.degrees(nose.degrees()));
            rightAligned(c, line, right, y, Palette.INK_MUTED);
        }
    }

    /**
     * The angle between the nose and the velocity, from prograde or retrograde whichever is nearer. The nose is the
     * orientation applied to the thrust direction. The velocity is in the ship's dimension axes: in orbit the
     * inertial velocity mapped to (X, Z, -Y), on the surface the ship's own. Null below {@link #NOSE_MIN_SPEED}.
     */
    private static NoseCue noseCue(ShipEntity ship, StateVector s, boolean orbit) {
        Vec3 t = ship.thrustDirectionBody();
        Vector3f nose = ship.getOrientation().transform(new Vector3f((float) t.x, (float) t.y, (float) t.z));
        double vx;
        double vy;
        double vz;
        if (orbit) {
            vx = s.v().x();
            vy = s.v().z();
            vz = -s.v().y();
        } else {
            Vec3 v = ship.getVelocity();
            vx = v.x;
            vy = v.y;
            vz = v.z;
        }
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double noseLength = nose.length();
        if (!(speed >= NOSE_MIN_SPEED) || !(noseLength > 0)) {
            return null;
        }
        double cos = (nose.x * vx + nose.y * vy + nose.z * vz) / (noseLength * speed);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
        return angle <= 90 ? new NoseCue(angle, true) : new NoseCue(180 - angle, false);
    }

    private static String sasKey(SasMode mode) {
        return switch (mode) {
            case OFF -> "apoapsis.hud.sas.off";
            case HOLD -> "apoapsis.hud.sas.hold";
            case PROGRADE -> "apoapsis.hud.sas.prograde";
            case RETROGRADE -> "apoapsis.hud.sas.retrograde";
        };
    }

    private static void rightAligned(Canvas c, Component text, int right, int y, int argb) {
        c.text(text, right - c.textWidth(text), y, 1f, argb, true);
    }

    private static void bar(Canvas c, int x, int y, int w, double fraction, int fill) {
        double f = Math.max(0, Math.min(1, fraction));
        c.rect(x, y, x + w, y + BAR_H, Palette.STEEL_700);
        int filled = (int) Math.round(w * f);
        if (filled > 0) {
            c.rect(x, y, x + filled, y + BAR_H, fill);
        }
    }
}
