package dev.apoapsis.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * The full-screen system map (M). Full-bleed, no panel, does not pause the game. Shows the piloted ship's orbit and
 * predicted coast over Verdance and Pale. Client only.
 */
public final class MapScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    private static final int STAR_COUNT = 160;
    private static final double SYSTEM_HALF_EXTENT_M = 360_000.0;
    private static final double ORBIT_FIT_MARGIN = 1.3;
    private static final double MARGIN_PX = 24;
    private static final float TITLE_SCALE = 1.5f;
    private static final int LINE_STEP = 12;
    private static final long STAR_SEED = 0x41504F41L;

    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final int[] STAR_ALPHA = new int[STAR_COUNT];

    static {
        Random rng = new Random(STAR_SEED);
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = rng.nextFloat();
            STAR_Y[i] = rng.nextFloat();
            STAR_ALPHA[i] = 0x38 + rng.nextInt(0x70);
        }
    }

    private final MapView view = new MapView();
    private final double[] bufX = new double[MapPrediction.MAX_SAMPLES + 1];
    private final double[] bufY = new double[MapPrediction.MAX_SAMPLES + 1];
    private MapPrediction prediction;
    private long frame;
    private long lastComputeFrame = -RECOMPUTE_FRAMES;
    private boolean fitted;

    /** Opened by {@link dev.apoapsis.client.MapClient#openMap()}. */
    public MapScreen() {
        super(Component.translatable("screen.apoapsis.map"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Full-bleed steel with a sparse, fixed star field. Replaces the vanilla panorama and blur. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        graphics.fill(0, 0, w, h, Palette.STEEL_950);
        for (int i = 0; i < STAR_COUNT; i++) {
            int x = (int) (STAR_X[i] * (w - 1));
            int y = (int) (STAR_Y[i] * (h - 1));
            graphics.fill(x, y, x + 1, y + 1, Palette.alpha(Palette.INK, STAR_ALPHA[i]));
        }
        Minecraft.getInstance().gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        frame++;
        Canvas c = new Canvas(graphics);
        view.setSize(c.width, c.height);
        drawHint(c);

        OrbitStatePayload p = OrbitClock.current();
        if (p == null) {
            Component msg = Component.translatable("apoapsis.map.board");
            c.text(msg, (c.width - c.textWidth(msg)) / 2, c.height / 2 - 4, 1f, Palette.INK_MUTED, false);
            return;
        }
        CelestialBody body = Bodies.of(p);
        if (!fitted) {
            fitted = true;
            view.follow = MapView.Follow.SHIP;
            view.fit(initialHalfExtent(p, body));
        }
        double nowUt = OrbitClock.nowUt(p, partialTick);
        if (prediction == null || (prediction.source != p && frame - lastComputeFrame >= RECOMPUTE_FRAMES)) {
            prediction = MapPrediction.compute(p, view.scale());
            lastComputeFrame = frame;
        }
        resolveCentre(nowUt);
        drawSystem(c, nowUt);
        drawTopLeft(c, p, body, nowUt);
    }

    /** Escape closes (vanilla), M closes, 1 2 3 choose what the view follows. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.input();
        if (key == InputConstants.KEY_M) {
            onClose();
            return true;
        }
        if (key == InputConstants.KEY_1) {
            view.follow = MapView.Follow.VERDANCE;
            return true;
        }
        if (key == InputConstants.KEY_2) {
            view.follow = MapView.Follow.PALE;
            return true;
        }
        if (key == InputConstants.KEY_3) {
            view.follow = MapView.Follow.SHIP;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        view.pan(dx, dy);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        view.zoom(mouseX, mouseY, scrollY);
        return true;
    }

    /** Half the extent to fit on first open: the ship's apoapsis when it is in an ellipse, else the whole system. */
    private static double initialHalfExtent(OrbitStatePayload p, CelestialBody body) {
        Elements el = Elements.of(body.mu, Bodies.stateOf(p));
        if (el.eccentricity() < 1 && Double.isFinite(el.apoapsisRadius())) {
            return Math.max(el.apoapsisRadius() * ORBIT_FIT_MARGIN, body.radius * 2.5);
        }
        return SYSTEM_HALF_EXTENT_M;
    }

    /** Sets the view centre from the follow target, or leaves it where a pan put it. */
    private void resolveCentre(double nowUt) {
        switch (view.follow) {
            case VERDANCE -> {
                view.centreX = 0;
                view.centreY = 0;
            }
            case PALE -> {
                Vec3d q = Bodies.rootState(MeridianSystem.PALE, nowUt).r();
                view.centreX = q.x();
                view.centreY = q.y();
            }
            case SHIP -> {
                if (prediction.patchCount() > 0) {
                    Vec3d q = prediction.rootState(prediction.indexAt(nowUt), nowUt).r();
                    view.centreX = q.x();
                    view.centreY = q.y();
                }
            }
            default -> {
            }
        }
    }

    private void drawSystem(Canvas c, double nowUt) {
        CelestialBody verdance = MeridianSystem.VERDANCE;
        CelestialBody pale = MeridianSystem.PALE;
        Vec3d palePos = Bodies.rootState(pale, nowUt).r();
        double vx = view.sx(0);
        double vy = view.sy(0);
        double px = view.sx(palePos.x());
        double py = view.sy(palePos.y());

        // Pale's orbit, its sphere of influence, and the two bodies.
        c.ring(vx, vy, view.px(pale.orbitRadius), Palette.STEEL_500);
        c.dashedRing(px, py, view.px(pale.soiRadius()), Palette.SIGNAL_EDGE, 6);
        c.ring(vx, vy, view.px(verdance.radius + verdance.atmosphereTop),
                Palette.alpha(Palette.HYDROGEN, 0xB0));
        c.disc(vx, vy, view.px(verdance.radius), Palette.VERDANCE_GROUND);
        c.disc(px, py, Math.max(1.5, view.px(pale.radius)), Palette.PALE_GROUND);
        c.text(Component.translatable("apoapsis.map.body.verdance"),
                (int) Math.round(vx + Math.max(2, view.px(verdance.radius)) + 4), (int) Math.round(vy - 4),
                1f, Palette.INK_MUTED, false);
        c.text(Component.translatable("apoapsis.map.body.pale"),
                (int) Math.round(px + Math.max(2, view.px(pale.radius)) + 4), (int) Math.round(py - 4),
                1f, Palette.INK_MUTED, false);

        if (prediction.patchCount() == 0) {
            return;
        }
        // The predicted coast: later patches first so the current one is drawn on top.
        int cur = prediction.indexAt(nowUt);
        for (int i = prediction.patchCount() - 1; i >= cur; i--) {
            double[] xs = prediction.xs(i);
            double[] ys = prediction.ys(i);
            int n = Math.min(xs.length, bufX.length);
            for (int k = 0; k < n; k++) {
                bufX[k] = view.sx(xs[k]);
                bufY[k] = view.sy(ys[k]);
            }
            c.polyline(bufX, bufY, n, i == cur ? Palette.SIGNAL : Palette.HYDROGEN);
        }

        for (MapPrediction.Mark m : prediction.marks()) {
            if (m.ut() < nowUt) {
                continue;
            }
            drawMark(c, m, nowUt);
        }

        // Ship marker: a small triangle pointing along the velocity (screen y is -Y).
        StateVector ship = prediction.rootState(cur, nowUt);
        double sx = view.sx(ship.r().x());
        double sy = view.sy(ship.r().y());
        double dx = ship.v().x();
        double dy = -ship.v().y();
        double len = Math.hypot(dx, dy);
        double ux = len > 0 ? dx / len : 0;
        double uy = len > 0 ? dy / len : -1;
        double nx = -uy;
        double ny = ux;
        c.triangle(
                new double[] {sx + ux * 7, sx - ux * 4 + nx * 4, sx - ux * 4 - nx * 4},
                new double[] {sy + uy * 7, sy - uy * 4 + ny * 4, sy - uy * 4 - ny * 4},
                Palette.SIGNAL);
    }

    private void drawMark(Canvas c, MapPrediction.Mark m, double nowUt) {
        double mx = view.sx(m.x());
        double my = view.sy(m.y());
        if (mx < -MARGIN_PX || my < -MARGIN_PX || mx > c.width + MARGIN_PX || my > c.height + MARGIN_PX) {
            return;
        }
        switch (m.kind()) {
            case APOAPSIS, PERIAPSIS -> {
                double bx = view.sx(m.cx());
                double by = view.sy(m.cy());
                double dx = mx - bx;
                double dy = my - by;
                double len = Math.hypot(dx, dy);
                if (len < 1e-9) {
                    dx = 0;
                    dy = -1;
                } else {
                    dx /= len;
                    dy /= len;
                }
                c.line(mx + dx * 2, my + dy * 2, mx + dx * 7, my + dy * 7, Palette.BRASS);
                String key = m.kind() == MapPrediction.Kind.APOAPSIS ? "apoapsis.map.mark.ap" : "apoapsis.map.mark.pe";
                Component label = Component.translatable(key, Fmt.mono(Fmt.distance(m.altM())));
                int lx = (int) Math.round(mx + dx * 10);
                int ly = (int) Math.round(my + dy * 10 - 4);
                int x = dx < -0.3 ? lx - c.textWidth(label) : lx;
                c.text(label, x, ly, 1f, Palette.INK_MUTED, false);
            }
            case ENCOUNTER -> {
                c.rect((int) Math.round(mx) - 1, (int) Math.round(my) - 1,
                        (int) Math.round(mx) + 2, (int) Math.round(my) + 2, Palette.INK);
                Component label = Component.translatable("apoapsis.map.mark.encounter",
                        Component.translatable("apoapsis.map.body." + m.body()),
                        Fmt.mono(Fmt.clock(m.ut() - nowUt)));
                c.text(label, (int) Math.round(mx + 6), (int) Math.round(my - 4), 1f, Palette.INK_MUTED, false);
            }
            case ATMOSPHERE, IMPACT -> {
                c.rect((int) Math.round(mx) - 2, (int) Math.round(my) - 2,
                        (int) Math.round(mx) + 3, (int) Math.round(my) + 3, Palette.COPPER);
                String key = m.kind() == MapPrediction.Kind.ATMOSPHERE
                        ? "apoapsis.map.mark.atmosphere" : "apoapsis.map.mark.impact";
                Component label = Component.translatable(key, Fmt.mono(Fmt.clock(m.ut() - nowUt)));
                c.text(label, (int) Math.round(mx + 7), (int) Math.round(my - 4), 1f, Palette.COPPER, false);
            }
            default -> {
            }
        }
    }

    /** Top left, no box: body and regime, then altitude, speed, apsides and the time to apoapsis. Five lines at most. */
    private void drawTopLeft(Canvas c, OrbitStatePayload p, CelestialBody body, double nowUt) {
        int x = 16;
        int y = 14;
        Component bodyName = Component.translatable("apoapsis.map.body." + body.id);
        Component regime = Component.translatable(regimeKey(p.regime()));
        Component title = p.warp() > 1
                ? Component.translatable("apoapsis.map.title.warp", bodyName, regime, Fmt.warp(p.warp()))
                : Component.translatable("apoapsis.map.title", bodyName, regime);
        c.text(title, x, y, TITLE_SCALE, Palette.INK, false);

        int lineY = y + 22;
        double r = Math.sqrt(p.rx() * p.rx() + p.ry() * p.ry() + p.rz() * p.rz());
        double speed = Math.sqrt(p.vx() * p.vx() + p.vy() * p.vy() + p.vz() * p.vz());
        c.text(Component.translatable("apoapsis.map.top.alt", Fmt.mono(Fmt.distance(r - body.radius))),
                x, lineY, 1f, Palette.INK, false);
        lineY += LINE_STEP;
        c.text(Component.translatable("apoapsis.map.top.speed", Fmt.mono(Fmt.speed(speed))),
                x, lineY, 1f, Palette.INK, false);
        lineY += LINE_STEP;

        Elements el = Elements.of(body.mu, Bodies.stateOf(p));
        if (el.eccentricity() < 1) {
            c.text(Component.translatable("apoapsis.map.top.apsides",
                            Fmt.mono(Fmt.distance(el.apoapsisRadius() - body.radius)),
                            Fmt.mono(Fmt.distance(el.periapsisRadius() - body.radius))),
                    x, lineY, 1f, Palette.INK, false);
            lineY += LINE_STEP;
            double toAp = el.timeToApoapsis() - (nowUt - p.ut());
            c.text(Component.translatable("apoapsis.map.top.ap_in", Fmt.mono(Fmt.clock(toAp))),
                    x, lineY, 1f, Palette.INK, false);
        } else {
            c.text(Component.translatable("apoapsis.map.top.periapsis",
                            Fmt.mono(Fmt.distance(el.periapsisRadius() - body.radius))),
                    x, lineY, 1f, Palette.INK, false);
        }
    }

    /** Lang key for a regime ordinal: ORBIT or SURFACE. */
    static String regimeKey(int ordinal) {
        return FlightRegime.byOrdinal(ordinal) == FlightRegime.ORBIT
                ? "apoapsis.map.regime.orbit" : "apoapsis.map.regime.surface";
    }

    private void drawHint(Canvas c) {
        c.text(Component.translatable("apoapsis.map.hint"), 16, c.height - 16, 1f, Palette.INK_MUTED, false);
    }
}
