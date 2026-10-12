package dev.apoapsis.client.map;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.hud.FlightInstruments;
import dev.apoapsis.client.hud.HintRow;
import dev.apoapsis.client.hud.HudLayout;
import dev.apoapsis.client.hud.HudMarkers;
import dev.apoapsis.client.hud.HudProjection;
import dev.apoapsis.client.hud.Navball;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
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
 * The flight HUD after the cockpit concept, made calm. Everything hugs the screen edges and the middle of the view stays
 * clear: the position and speed readouts at the bottom left, the navball with the SAS and warp status at the bottom
 * centre, the throttle, fuel and delta-v at the bottom right, the regime small at the top right, a row of keycap hints
 * only while it helps, and the radar altitude beside the ball near the ground. In first person only, world-space
 * markers: the prograde and retrograde reticles, a horizon bracket, and a bracket around Pale with its distance. Drawn
 * only while the local player rides a ship, with no screen open and the HUD not hidden. The drawing lives in
 * {@code client/hud}; this class reads the flight state. Client only.
 */
public final class FlightHud {
    /** Below this speed the velocity markers are hidden, m/s. */
    private static final double MARKER_MIN_SPEED = 1.0;
    /** The horizon bracket looks for a horizon point this near the view centre or it is not drawn. */
    private static final double MIN_HORIZON_LEN = 0.1;

    /** The apsides, read once per frame in orbit. */
    private record Apsides(double apoapsisAlt, double periapsisAlt, boolean apInside) {}

    private FlightHud() {}

    /** The GUI layer. Registered above the hotbar by {@link dev.apoapsis.client.MapClient}. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        HintRow.screenOpen(mc.gui.screen());
        if (mc.gui.screen() != null || mc.gui.hud.isHidden()) {
            return;
        }
        if (mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) {
            HintRow.left();
            return;
        }
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            return;
        }
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
        SasMode sas = ship.getSasMode();
        FlightInstruments.Telemetry t = new FlightInstruments.Telemetry(alt, speed, vs, fuel, ship.deltaV(pf),
                ship.fuelKg() <= 0 || fuel < 0.005, orbit, apsides != null, apsides != null && apsides.apInside(),
                apsides == null ? 0 : apsides.apoapsisAlt(), apsides == null ? 0 : apsides.periapsisAlt(),
                ship.getThrottle(), sasKey(sas), sas != SasMode.OFF, p.warp());

        // The dimension axes of the local vertical and of the velocity, as the ship's orientation uses them.
        double[] up = {0, 1, 0};
        if (orbit && r > 0) {
            up = unit(dimOf(true, s.r().x(), s.r().y(), s.r().z()));
        }
        double[] vel = velocity(ship, s, orbit);
        double[] axes = new double[9];
        var q = ship.getOrientation();
        Vec3 th = ship.thrustDirectionBody();
        Navball.axes(q.x, q.y, q.z, q.w, th.x, th.y, th.z, axes);
        double heading = Navball.heading(axes[6], axes[7], axes[8], up[0], up[1], up[2]);

        HintRow.observe(mc, ship, orbit, p.warp());
        HudLayout lay = HudLayout.current();
        UiScale ui = lay.scale();
        ui.begin(graphics, 0, 0);
        FlightInstruments.drawSides(graphics, lay, t);
        FlightInstruments.BallSpot ball = FlightInstruments.drawBall(graphics, lay, t, axes, up, vel, heading);
        drawRegime(graphics, lay, p, body);
        int hintBottom = FlightInstruments.leftTop(lay, t) - 8;
        HintRow.draw(graphics, ship, lay.left(), hintBottom, lay.rowRightLimit(hintBottom));
        if (!orbit) {
            double radar = RadarAltitude.of(mc.level, ship);
            LandingReadout.draw(graphics, ship, radar, vs, body, alt, ball.cx() + ball.half() + 10,
                    ball.cy() + ball.half() - 2);
        }
        ui.end(graphics);

        if (mc.options.getCameraType().isFirstPerson()) {
            drawMarkers(graphics, ui, mc, ship, p, body, orbit, s, partialTick, up, vel);
        }
    }

    /** The regime at the top right, small, on a faint backing so it reads over a bright sky. */
    private static void drawRegime(GuiGraphicsExtractor g, HudLayout lay, OrbitStatePayload p, CelestialBody body) {
        String regime = Component.translatable("apoapsis.hud.regime",
                Component.translatable(MapScreen.regimeKey(p.regime())),
                Component.translatable("apoapsis.map.body." + body.id)).getString();
        int w = Math.round(UiTextSmooth.width(UiText.Type.LABEL, regime));
        int top = lay.top();
        int right = lay.rightAt(top);
        g.fill(right - w - 5, top - 3, right + 4, top + UiText.Type.LABEL.capHeight() + 4,
                UiPalette.alpha(UiPalette.STEEL_950, 0x80));
        UiTextSmooth.drawRight(g, UiText.Type.LABEL, regime, right, top, UiPalette.INK_MUTED);
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
    private static void drawMarkers(GuiGraphicsExtractor g, UiScale ui, Minecraft mc, ShipEntity ship,
                                    OrbitStatePayload p, CelestialBody body, boolean orbit, StateVector s,
                                    float partialTick, double[] up, double[] vel) {
        int width = g.guiWidth();
        int height = g.guiHeight();
        Camera cam = mc.gameRenderer.mainCamera();
        Quaternionf q = cam.rotation();
        double fov = cam.getFov();
        double[] o = new double[3];
        if (vel != null && HudProjection.project(q.x, q.y, q.z, q.w, vel[0], vel[1], vel[2], fov, width, height, o)
                && HudProjection.onScreen(o[0], o[1], width, height)) {
            HudMarkers.prograde(g, ui, o[0], o[1], Component.translatable("apoapsis.hud.prograde").getString());
        }
        if (vel != null && HudProjection.project(q.x, q.y, q.z, q.w, -vel[0], -vel[1], -vel[2], fov, width, height, o)
                && HudProjection.onScreen(o[0], o[1], width, height)) {
            HudMarkers.retrograde(g, ui, o[0], o[1], Component.translatable("apoapsis.hud.retrograde").getString());
        }
        drawHorizon(g, ui, q, fov, up, width, height);
        if (orbit && !MeridianSystem.PALE.id.equals(body.id)) {
            drawTarget(g, ui, q, fov, p, body, s, partialTick, width, height);
        }
    }

    /**
     * The horizon bracket: two short segments either side of the horizon point nearest the view centre, along the
     * horizon. The horizon is the plane normal to the local vertical {@code up}.
     */
    private static void drawHorizon(GuiGraphicsExtractor g, UiScale ui, Quaternionf q, double fov, double[] up, int width,
                                    int height) {
        double[] f = new double[3];
        HudProjection.rotate(q.x, q.y, q.z, q.w, 0, 0, -1, f, 0);
        double fd = f[0] * up[0] + f[1] * up[1] + f[2] * up[2];
        double[] h = {f[0] - fd * up[0], f[1] - fd * up[1], f[2] - fd * up[2]};
        double hl = Math.sqrt(h[0] * h[0] + h[1] * h[1] + h[2] * h[2]);
        if (hl < MIN_HORIZON_LEN) {
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
        if (!HudProjection.project(q.x, q.y, q.z, q.w, h[0], h[1], h[2], fov, width, height, near)
                || !HudProjection.project(q.x, q.y, q.z, q.w, ah[0], ah[1], ah[2], fov, width, height, ahead)
                || !HudProjection.onScreen(near[0], near[1], width, height)) {
            return;
        }
        double dx = ahead[0] - near[0];
        double dy = ahead[1] - near[1];
        double dl = Math.sqrt(dx * dx + dy * dy);
        if (!(dl > 1e-6)) {
            return;
        }
        HudMarkers.horizon(g, ui, near[0], near[1], dx / dl, dy / dl);
    }

    /**
     * The target bracket around Pale, four corners, with "Pale · distance" beside it. Pale's direction is from the ship
     * to its centre, at the current universal time, in the orbit's axes. The distance is to Pale's surface.
     */
    private static void drawTarget(GuiGraphicsExtractor g, UiScale ui, Quaternionf q, double fov, OrbitStatePayload p,
                                   CelestialBody body, StateVector s, float partialTick, int width, int height) {
        CelestialBody other = MeridianSystem.PALE;
        double ut = OrbitNow.utNow(p, partialTick);
        Vec3d shipPos = Bodies.rootState(body, ut).r().add(s.r());
        Vec3d toOther = Bodies.rootState(other, ut).r().sub(shipPos);
        double[] dir = dimOf(true, toOther.x(), toOther.y(), toOther.z());
        double[] o = new double[3];
        if (!HudProjection.project(q.x, q.y, q.z, q.w, dir[0], dir[1], dir[2], fov, width, height, o)
                || !HudProjection.onScreen(o[0], o[1], width, height)) {
            return;
        }
        Component name = Component.translatable("apoapsis.map.body." + other.id);
        Component dist = Component.literal(Fmt.distance(toOther.length() - other.radius));
        HudMarkers.target(g, ui, o[0], o[1], Component.translatable("apoapsis.hud.target", name, dist).getString());
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
