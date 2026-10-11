package dev.apoapsis.client.map;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.hud.HudProjection;
import dev.apoapsis.client.hud.Navball;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The flight HUD after the cockpit concept. In the bottom centre in first person, or the bottom left in third person so
 * the ship stays clear, a compact cluster: a telemetry column (ALT, AP, PE, ORB or SPD, V/S), a navball, and throttle
 * and fuel gauges. Above it at the left, status pills (SAS, warp, fuel) and
 * keycap hints for the pilot controls. At the top right, the regime. In first person only, world-space markers: the
 * prograde and retrograde reticles, a horizon bracket, and a bracket around Pale with its distance. Drawn only while
 * the local player rides a ship, with no screen open and the HUD not hidden. Client only.
 */
public final class FlightHud {
    /**
     * Cluster bottom, GUI px above the bottom edge, in survival. Vanilla's overlay message (action bar) glyphs run h-72..h-64
     * (Hud.java:382, 391), and the chat's four lowest lines run h-76..h-40 (chat bottom h-40, 9 px lines). The status
     * rows and the selected item name sit at h-59 and below. So the cluster stays above h-84, the band that clears the
     * action bar and the chat's lowest lines, with 2 px to spare. The hotbar sits below h-22 and is never reached.
     */
    private static final int BOTTOM_SURVIVAL = 86;
    /** Cluster bottom in creative, where the selected item name sits at h-45 and there are no status rows. */
    private static final int BOTTOM_CREATIVE = 48;
    private static final int PAD = 10;
    private static final int LINE = 9;
    /** The cluster takes at most this share of the GUI width. Its width is fixed, so the scale is arithmetic. */
    private static final double CLUSTER_MAX_FRACTION = 0.6;
    /** The smallest scale the cluster and pills step down to. */
    private static final double MIN_SCALE = 0.6;

    /** Cluster geometry at scale 1, GUI px. Telemetry, navball and gauges, left to right. */
    private static final int TEL_W = 76;
    private static final int GAP = 8;
    private static final int BALL_R = 22;
    private static final int GAUGE_W = 46;
    private static final int HEAD_H = 9;
    private static final int LABEL_H = 9;
    private static final int CLUSTER_W = TEL_W + GAP + 2 * BALL_R + GAP + GAUGE_W;
    private static final int CLUSTER_H = HEAD_H + 2 * BALL_R + LABEL_H;
    private static final int BAR_W = 6;
    /**
     * Gauge bars sit at these x offsets inside the gauge column, at scale 1. The bar centres are 28 px apart: the THR
     * label is about 18 px wide and the FUEL label about 24 px, so their halves (9 + 12) leave 7 px between the labels.
     */
    private static final int THR_BAR_X = 2;
    private static final int FUEL_BAR_X = 30;

    private static final int PILL_PAD = 4;
    private static final int PILL_H = 10;
    /** The background behind a status pill. */
    private static final int PILL_FILL = Palette.alpha(Palette.STEEL_950, 0xB8);
    /** A throttle bar with no fuel: the setting is shown, dimmed. */
    private static final int DIMMED_BRASS = Palette.alpha(Palette.BRASS, 0x55);
    /** The marker accent: the reticles, the horizon bracket and the target bracket. */
    private static final int ACCENT = Palette.SIGNAL;
    private static final int HORIZON = Palette.alpha(Palette.SIGNAL, 0xB0);
    /** Below this speed the velocity markers are hidden, m/s. */
    private static final double MARKER_MIN_SPEED = 1.0;
    /** The horizon bracket: the gap from its centre and its length, GUI px. */
    private static final double HORIZON_GAP = 8;
    private static final double HORIZON_LEN = 10;
    /** The target bracket's half size and corner leg, GUI px. */
    private static final double TARGET_HALF = 9;
    private static final double TARGET_LEG = 4;

    /** The top-right and telemetry values, read once per frame. */
    private record Apsides(double apoapsisAlt, double periapsisAlt, boolean apInside) {}

    private record Readout(double alt, double speed, double vs, double fuel, double dv, boolean dry, boolean orbit,
                           Apsides apsides) {}

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
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            return;
        }
        Canvas c = new Canvas(graphics);
        CelestialBody body = Bodies.of(p);
        boolean orbit = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        // In orbit the state is advanced to this frame; on the surface the payload's own state is already smooth.
        StateVector s = orbit ? OrbitNow.stateNow(p, partialTick) : Bodies.stateOf(p);
        double r = s.r().length();
        double speed = s.v().length();
        double vs = r > 0 ? s.r().dot(s.v()) / r : 0;
        double alt = r - body.radius;
        double pf = Bodies.pressureFraction(body, alt);
        double capacity = ship.fuelCapacityKg();
        double fuel = capacity > 0 ? ship.fuelKg() / capacity : 0;
        Apsides apsides = orbit ? apsides(p, body) : null;
        Readout rd = new Readout(alt, speed, vs, fuel, ship.deltaV(pf), ship.fuelKg() <= 0, orbit, apsides);

        // The dimension axes of the local vertical and of the velocity, as the ship's orientation uses them.
        double[] up = {0, 1, 0};
        if (orbit && r > 0) {
            up = unit(dimOf(true, s.r().x(), s.r().y(), s.r().z()));
        }
        double[] vel = velocity(ship, s, orbit);

        boolean survival = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        int bottom = c.height - (survival ? BOTTOM_SURVIVAL : BOTTOM_CREATIVE);
        double scale = clusterScale(c.width);
        int top = bottom - px(CLUSTER_H, scale);
        // First person: bottom centre. Third person: bottom left, clear of the ship at the centre of the view.
        int left = firstPerson ? c.width / 2 - px(CLUSTER_W, scale) / 2 : PAD;
        drawCluster(c, ship, rd, up, vel, left, top, scale);
        drawStatus(c, ship, p, rd, top, scale);
        drawTopRight(c, p, body);

        if (firstPerson) {
            drawMarkers(c, mc, ship, p, body, orbit, s, partialTick, up, vel);
        }
    }

    /** The cluster's scale: 1 unless the fixed width would take more than {@link #CLUSTER_MAX_FRACTION} of the GUI. */
    private static double clusterScale(int width) {
        return Math.max(MIN_SCALE, Math.min(1.0, width * CLUSTER_MAX_FRACTION / CLUSTER_W));
    }

    /** A unit-scale length at scale {@code s}, rounded to GUI px. */
    private static int px(int v, double s) {
        return (int) Math.round(v * s);
    }

    /** The bottom instruments: telemetry, navball and gauges, with the heading above the ball. x0 is the left edge. */
    private static void drawCluster(Canvas c, ShipEntity ship, Readout rd, double[] up, double[] vel, int x0, int top,
                                    double s) {
        int rowTop = top + px(HEAD_H, s);
        int telRight = x0 + px(TEL_W, s);
        float fs = (float) s;
        // A faint dark backdrop keeps the muted labels readable over a lit planet limb.
        c.rect(x0 - 3, rowTop - 1, telRight + 3, rowTop + 5 * px(LINE, s), PILL_FILL);

        row(c, x0, telRight, rowTop, label("apoapsis.hud.label.alt"), Component.literal(Fmt.distance(rd.alt())), fs,
                Palette.INK);
        // Outside the body's SOI, or on a hyperbola: an impact course is alarming, an escape is not.
        Component ap = dash();
        int apArgb = Palette.INK;
        if (rd.apsides() != null && rd.apsides().apInside()) {
            ap = Component.literal(Fmt.distance(rd.apsides().apoapsisAlt()));
        } else if (rd.apsides() != null && rd.apsides().periapsisAlt() <= 0) {
            ap = label("apoapsis.hud.impact");
            apArgb = Palette.ALERT;
        } else if (rd.apsides() != null) {
            ap = Component.translatable("apoapsis.hud.escape");
        }
        Component pe = rd.apsides() == null ? dash() : Component.literal(Fmt.distance(rd.apsides().periapsisAlt()));
        row(c, x0, telRight, rowTop + px(LINE, s), label("apoapsis.hud.label.ap"), ap, fs, apArgb);
        row(c, x0, telRight, rowTop + 2 * px(LINE, s), label("apoapsis.hud.label.pe"), pe, fs, Palette.INK);
        row(c, x0, telRight, rowTop + 3 * px(LINE, s),
                label(rd.orbit() ? "apoapsis.hud.label.orb" : "apoapsis.hud.label.speed"),
                Component.literal(Fmt.speed(rd.speed())), fs, Palette.INK);
        row(c, x0, telRight, rowTop + 4 * px(LINE, s), label("apoapsis.hud.label.vs"),
                Component.literal(Fmt.signedSpeed(rd.vs())), fs, Palette.INK);

        // The navball: centre at the nose.
        int ballLeft = x0 + px(TEL_W + GAP, s);
        double radius = BALL_R * s;
        double cx = ballLeft + radius;
        double cy = top + px(HEAD_H, s) + radius;
        Vec3 t = ship.thrustDirectionBody();
        double[] axes = new double[9];
        var q = ship.getOrientation();
        Navball.axes(q.x, q.y, q.z, q.w, t.x, t.y, t.z, axes);
        drawNavball(c, axes, up, vel, cx, cy, radius);
        double heading = Navball.heading(axes[6], axes[7], axes[8], up[0], up[1], up[2]);
        Component headText = Component.literal(Double.isNaN(heading) ? "---"
                : String.format("%03d", (int) Math.round(heading) % 360));
        centred(c, headText, cx, top, fs, Palette.INK);

        // Gauges on the right of the ball: throttle, then fuel, each a vertical bar with its label under it.
        int gaugeX = ballLeft + px(2 * BALL_R, s) + px(GAP, s);
        int barTop = top + px(HEAD_H, s);
        int barH = px(2 * BALL_R, s);
        int barW = px(BAR_W, s);
        int thrX = gaugeX + px(THR_BAR_X, s);
        int fuelX = gaugeX + px(FUEL_BAR_X, s);
        int labelY = barTop + barH + 1;
        boolean dry = rd.dry();
        vbar(c, thrX, barTop, barW, barH, ship.getThrottle(), dry ? DIMMED_BRASS : Palette.BRASS);
        vbar(c, fuelX, barTop, barW, barH, rd.fuel(), dry ? Palette.ALERT : Palette.HYDROGEN);
        centred(c, label("apoapsis.hud.label.throttle"), thrX + barW / 2.0, labelY, fs, Palette.INK_MUTED);
        centred(c, label("apoapsis.hud.label.fuel"), fuelX + barW / 2.0, labelY, fs, Palette.INK_MUTED);
    }

    /** The navball: sky and ground halves with the horizon, the nose cross, the prograde marker and the rim. */
    private static void drawNavball(Canvas c, double[] axes, double[] up, double[] vel, double cx, double cy,
                                    double radius) {
        double[] upBall = new double[3];
        Navball.toBall(axes, up[0], up[1], up[2], upBall);
        int x0 = (int) Math.floor(cx - radius);
        int x1 = (int) Math.ceil(cx + radius);
        for (int py = (int) Math.floor(cy - radius); py <= Math.ceil(cy + radius); py++) {
            double sy = -((py + 0.5) - cy) / radius;
            if (Math.abs(sy) >= 1) {
                continue;
            }
            // Runs of one colour are drawn as one rectangle each.
            int run = 0;
            int runStart = x0;
            for (int px = x0; px <= x1 + 1; px++) {
                int col = px <= x1 ? Navball.classify(upBall, ((px + 0.5) - cx) / radius, sy) : -1;
                if (col != run) {
                    if (run != 0 && px > runStart) {
                        c.rect(runStart, py, px, py + 1, run);
                    }
                    run = col;
                    runStart = px;
                }
            }
        }
        c.ring(cx, cy, radius, Palette.STEEL_500);
        c.line(cx - 2, cy, cx + 2, cy, Palette.INK);
        c.line(cx, cy - 2, cx, cy + 2, Palette.INK);
        if (vel != null) {
            double[] vb = new double[3];
            Navball.toBall(axes, vel[0], vel[1], vel[2], vb);
            if (vb[2] > 0) {
                prograde(c, cx + vb[0] * radius, cy - vb[1] * radius, ACCENT, false);
            }
        }
    }

    /** One telemetry row: the label at the left, the value right-aligned to {@code right} in {@code valueArgb}. */
    private static void row(Canvas c, int x, int right, int y, Component label, Component value, float s,
                            int valueArgb) {
        c.text(label, x, y, s, Palette.INK_MUTED, true);
        int w = (int) Math.round(c.textWidth(value) * s);
        c.text(value, right - w, y, s, valueArgb, true);
    }

    /** Text centred on {@code cx}, its top at {@code y}. */
    private static void centred(Canvas c, Component text, double cx, int y, float s, int argb) {
        int w = (int) Math.round(c.textWidth(text) * s);
        c.text(text, (int) Math.round(cx) - w / 2, y, s, argb, true);
    }

    /** A vertical gauge: a dark track, filled from the bottom to {@code fraction}. */
    private static void vbar(Canvas c, int x, int y, int w, int h, double fraction, int fill) {
        double f = Math.max(0, Math.min(1, fraction));
        c.rect(x, y, x + w, y + h, Palette.STEEL_700);
        int filled = (int) Math.round(h * f);
        if (filled > 0) {
            c.rect(x, y + h - filled, x + w, y + h, fill);
        }
    }

    /**
     * The status pills at the left, stacked above the cluster: SAS, warp when fast, and fuel. Above them, the keycap
     * hints for the pilot controls. Each stack stops at the GUI's top pad.
     */
    private static void drawStatus(Canvas c, ShipEntity ship, OrbitStatePayload p, Readout rd, int top, double s) {
        float fs = (float) s;
        int pillH = px(PILL_H, s);
        int pillY = top - px(PILL_PAD, s) - pillH;
        int x = PAD;
        SasMode sas = ship.getSasMode();
        x = pill(c, x, pillY, pillH, s, label(sasKey(sas)), sas == SasMode.OFF ? Palette.INK_MUTED : Palette.GO);
        if (p.warp() > 1) {
            x = pill(c, x + px(PILL_PAD, s), pillY, pillH, s, Component.literal(Fmt.warp(p.warp())), Palette.INK);
        }
        Component fuelText = rd.dry() ? label("apoapsis.hud.pill.dry")
                : Component.translatable("apoapsis.hud.pill.fuel", Fmt.percent(rd.fuel()), Fmt.speed(rd.dv()));
        pill(c, x + px(PILL_PAD, s), pillY, pillH, s, fuelText, rd.dry() ? Palette.ALERT : Palette.INK);

        // Keycap hints, bottom up. Each is for a control the mod binds.
        Component[] hints = {
                hint(PilotKeys.SAS.getTranslatedKeyMessage(), "apoapsis.hud.hint.sas"),
                hint(Component.translatable("apoapsis.hud.keys", PilotKeys.THROTTLE_UP.getTranslatedKeyMessage(),
                        PilotKeys.THROTTLE_DOWN.getTranslatedKeyMessage()), "apoapsis.hud.hint.throttle"),
                hint(Component.translatable("apoapsis.hud.keys", PilotKeys.ROLL_LEFT.getTranslatedKeyMessage(),
                        PilotKeys.ROLL_RIGHT.getTranslatedKeyMessage()), "apoapsis.hud.hint.roll"),
                hint(Component.translatable("apoapsis.hud.keys", PilotKeys.WARP_DOWN.getTranslatedKeyMessage(),
                        PilotKeys.WARP_UP.getTranslatedKeyMessage()), "apoapsis.hud.hint.warp"),
                hint(PilotKeys.SYSTEM_MAP.getTranslatedKeyMessage(), "apoapsis.hud.hint.map"),
        };
        int y = pillY - px(PILL_PAD, s) - px(LINE, s);
        for (int i = hints.length - 1; i >= 0 && y >= PAD; i--, y -= px(LINE, s)) {
            c.text(hints[i], PAD, y, fs, Palette.INK_MUTED, true);
        }
    }

    /** A status pill: a dark rounded-ish backing and its text. Returns the x just past it. */
    private static int pill(Canvas c, int x, int y, int h, double s, Component text, int argb) {
        int textW = (int) Math.round(c.textWidth(text) * s);
        int w = textW + 2 * px(PILL_PAD, s);
        c.rect(x, y, x + w, y + h, PILL_FILL);
        c.text(text, x + px(PILL_PAD, s), y + (h - px(8, s)) / 2, (float) s, argb, true);
        return x + w;
    }

    private static Component hint(Component keys, String labelKey) {
        return Component.translatable("apoapsis.hud.hint", keys, Component.translatable(labelKey));
    }

    private static Component label(String key) {
        return Component.translatable(key);
    }

    private static Component dash() {
        return Component.translatable("apoapsis.hud.none");
    }

    /** The regime at the top right. The apsides are in the telemetry; the SAS and warp are in the status pills. */
    private static void drawTopRight(Canvas c, OrbitStatePayload p, CelestialBody body) {
        Component regime = Component.translatable("apoapsis.hud.regime",
                Component.translatable(MapScreen.regimeKey(p.regime())),
                Component.translatable("apoapsis.map.body." + body.id));
        int left = c.width - PAD - c.textWidth(regime);
        c.rect(left - 3, PAD - 2, c.width - PAD + 3, PAD + LINE - 1, PILL_FILL);
        c.text(regime, left, PAD, 1f, Palette.INK_MUTED, true);
    }

    /**
     * The apsides in orbit, from the payload's state. The apoapsis counts when the orbit is bound and the apoapsis lies
     * inside the body's sphere of influence (Verdance's is unbounded, so always). Radii are from the central body's
     * centre, as the SOI is.
     */
    private static Apsides apsides(OrbitStatePayload p, CelestialBody body) {
        Elements el = Elements.of(body.mu, Bodies.stateOf(p));
        boolean bound = el.eccentricity() < 1;
        boolean inside = bound && el.apoapsisRadius() < body.soiRadius();
        return new Apsides(el.apoapsisRadius() - body.radius, el.periapsisRadius() - body.radius, inside);
    }

    /** The ship's velocity in dimension axes, unit length, or null under {@link #MARKER_MIN_SPEED}. */
    private static double[] velocity(ShipEntity ship, StateVector s, boolean orbit) {
        double[] v;
        if (orbit) {
            v = dimOf(true, s.v().x(), s.v().y(), s.v().z());
        } else {
            Vec3 sv = ship.getVelocity();
            v = new double[] {sv.x, sv.y, sv.z};
        }
        if (Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) < MARKER_MIN_SPEED) {
            return null;
        }
        return unit(v);
    }

    /** Dimension axes of an inertial vector: in orbit (X, Z, -Y), as the velocity is mapped; on the surface as given. */
    private static double[] dimOf(boolean orbit, double x, double y, double z) {
        return orbit ? new double[] {x, z, -y} : new double[] {x, y, z};
    }

    /** Scales a vector to unit length in place and returns it. */
    private static double[] unit(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= l;
        v[1] /= l;
        v[2] /= l;
        return v;
    }

    /**
     * The first-person markers: the prograde and retrograde reticles, a horizon bracket at the local horizon, and a
     * bracket around Pale with its distance when the ship orbits Verdance. Each is hidden when off screen or behind.
     */
    private static void drawMarkers(Canvas c, Minecraft mc, ShipEntity ship, OrbitStatePayload p, CelestialBody body,
                                    boolean orbit, StateVector s, float partialTick, double[] up, double[] vel) {
        Camera cam = mc.gameRenderer.mainCamera();
        Quaternionf q = cam.rotation();
        double fov = cam.getFov();
        double[] o = new double[3];
        if (vel != null && HudProjection.project(q.x, q.y, q.z, q.w, vel[0], vel[1], vel[2], fov, c.width, c.height, o)
                && HudProjection.onScreen(o[0], o[1], c.width, c.height)) {
            prograde(c, o[0], o[1], ACCENT, true);
        }
        if (vel != null && HudProjection.project(q.x, q.y, q.z, q.w, -vel[0], -vel[1], -vel[2], fov, c.width,
                c.height, o) && HudProjection.onScreen(o[0], o[1], c.width, c.height)) {
            retrograde(c, o[0], o[1]);
        }
        drawHorizon(c, q, fov, up);
        if (orbit && !MeridianSystem.PALE.id.equals(body.id)) {
            drawTarget(c, q, fov, p, body, s, partialTick);
        }
    }

    /** The prograde reticle: a circle with ticks, and its label when {@code labelled}. */
    private static void prograde(Canvas c, double x, double y, int argb, boolean labelled) {
        c.ring(x, y, 5, argb);
        c.line(x - 12, y, x - 7, y, argb);
        c.line(x + 7, y, x + 12, y, argb);
        c.line(x, y - 12, x, y - 7, argb);
        if (labelled) {
            c.text(label("apoapsis.hud.prograde"), (int) Math.round(x + 14), (int) Math.round(y - 4), 0.7f, argb, true);
        }
    }

    /** The retrograde reticle: a circle with a cross. */
    private static void retrograde(Canvas c, double x, double y) {
        c.ring(x, y, 5, ACCENT);
        c.line(x - 4, y - 4, x + 4, y + 4, ACCENT);
        c.line(x - 4, y + 4, x + 4, y - 4, ACCENT);
        c.text(label("apoapsis.hud.retrograde"), (int) Math.round(x + 14), (int) Math.round(y - 4), 0.7f, ACCENT, true);
    }

    /**
     * The horizon bracket: two short segments either side of the horizon point nearest the view centre, along the
     * horizon. The horizon is the plane normal to the local vertical {@code up}.
     */
    private static void drawHorizon(Canvas c, Quaternionf q, double fov, double[] up) {
        double[] f = new double[3];
        HudProjection.rotate(q.x, q.y, q.z, q.w, 0, 0, -1, f, 0);
        double fd = f[0] * up[0] + f[1] * up[1] + f[2] * up[2];
        double[] h = {f[0] - fd * up[0], f[1] - fd * up[1], f[2] - fd * up[2]};
        double hl = Math.sqrt(h[0] * h[0] + h[1] * h[1] + h[2] * h[2]);
        if (hl < 0.1) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            h[i] /= hl;
        }
        // The horizon's tangent at h: up x h, a unit vector since up and h are unit and perpendicular.
        double[] tan = {up[1] * h[2] - up[2] * h[1], up[2] * h[0] - up[0] * h[2], up[0] * h[1] - up[1] * h[0]};
        double[] near = new double[3];
        double[] ahead = new double[3];
        double[] ah = {h[0] + 0.02 * tan[0], h[1] + 0.02 * tan[1], h[2] + 0.02 * tan[2]};
        if (!HudProjection.project(q.x, q.y, q.z, q.w, h[0], h[1], h[2], fov, c.width, c.height, near)
                || !HudProjection.project(q.x, q.y, q.z, q.w, ah[0], ah[1], ah[2], fov, c.width, c.height, ahead)
                || !HudProjection.onScreen(near[0], near[1], c.width, c.height)) {
            return;
        }
        double dx = ahead[0] - near[0];
        double dy = ahead[1] - near[1];
        double dl = Math.sqrt(dx * dx + dy * dy);
        if (!(dl > 1e-6)) {
            return;
        }
        dx /= dl;
        dy /= dl;
        for (int sign = -1; sign <= 1; sign += 2) {
            c.line(near[0] + sign * HORIZON_GAP * dx, near[1] + sign * HORIZON_GAP * dy,
                    near[0] + sign * (HORIZON_GAP + HORIZON_LEN) * dx, near[1] + sign * (HORIZON_GAP + HORIZON_LEN) * dy,
                    HORIZON);
        }
    }

    /**
     * The target bracket around Pale, four corners, with "Pale · distance" beside it. Pale's direction is from the ship
     * to its centre, at the current universal time, in the orbit's axes. The distance is to Pale's surface.
     */
    private static void drawTarget(Canvas c, Quaternionf q, double fov, OrbitStatePayload p, CelestialBody body,
                                   StateVector s, float partialTick) {
        CelestialBody other = MeridianSystem.PALE;
        double ut = OrbitNow.utNow(p, partialTick);
        Vec3d shipPos = Bodies.rootState(body, ut).r().add(s.r());
        Vec3d toOther = Bodies.rootState(other, ut).r().sub(shipPos);
        double[] dir = dimOf(true, toOther.x(), toOther.y(), toOther.z());
        double[] o = new double[3];
        if (!HudProjection.project(q.x, q.y, q.z, q.w, dir[0], dir[1], dir[2], fov, c.width, c.height, o)
                || !HudProjection.onScreen(o[0], o[1], c.width, c.height)) {
            return;
        }
        double x = o[0];
        double y = o[1];
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                double cx = x + sx * TARGET_HALF;
                double cy = y + sy * TARGET_HALF;
                c.line(cx, cy, cx - sx * TARGET_LEG, cy, ACCENT);
                c.line(cx, cy, cx, cy - sy * TARGET_LEG, ACCENT);
            }
        }
        Component name = Component.translatable("apoapsis.map.body." + other.id);
        Component dist = Component.literal(Fmt.distance(toOther.length() - other.radius));
        c.text(Component.translatable("apoapsis.hud.target", name, dist), (int) Math.round(x + TARGET_HALF + 4),
                (int) Math.round(y - TARGET_HALF), 0.7f, ACCENT, true);
    }

    private static String sasKey(SasMode mode) {
        return switch (mode) {
            case OFF -> "apoapsis.hud.sas.off";
            case HOLD -> "apoapsis.hud.sas.hold";
            case PROGRADE -> "apoapsis.hud.sas.prograde";
            case RETROGRADE -> "apoapsis.hud.sas.retrograde";
        };
    }
}
