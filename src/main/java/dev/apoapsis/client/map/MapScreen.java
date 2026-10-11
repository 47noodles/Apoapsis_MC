package dev.apoapsis.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ship.PilotKeys;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The full-screen system map (the System map key, M by default). Full-bleed, no panel, does not pause the game. Shows
 * the piloted ship's orbit and predicted coast over Verdance and Pale. Client only.
 *
 * <p>Frame rule: the current patch and its marks are drawn at their body's position now (the current extrapolated
 * time), so a Pale patch the ship is in sits around Pale's disc. A future patch around a non-root body (Pale) is drawn
 * at that body's position when the ship enters it, so the predicted line meets the encounter's white dot. The encounter
 * shows a faint ghost of Pale where it will be at that time: its outline and a dashed sphere of influence.
 */
public final class MapScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    /** Zoom change, as a ratio either way, that re-samples the polylines. */
    private static final double RESAMPLE_ZOOM_RATIO = 1.5;
    private static final int STAR_COUNT = 160;
    /** Fills a frame may use; the star field is skipped when the next frame would go past this. */
    private static final int MAX_FRAME_FILLS = 1200;
    private static final int PALE_ORBIT_SEGMENTS = 256;
    private static final int ATMOSPHERE_SEGMENTS = 256;
    private static final double SYSTEM_HALF_EXTENT_M = 360_000.0;
    private static final double ORBIT_FIT_MARGIN = 1.3;
    private static final double SOI_FIT_MARGIN = 1.1;
    private static final double MARGIN_PX = 24;
    private static final float TITLE_SCALE = 1.5f;
    private static final int LINE_STEP = 12;
    private static final int LABEL_H = 9;
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
    /** The view scale the prediction's polylines were sampled at. */
    private double sampledScale = 1e-3;
    /** The body the ship was in when last seen; null before the first payload. */
    private CelestialBody shownBody;
    private long frame;
    private long lastComputeFrame = -RECOMPUTE_FRAMES;
    private long lastSampleFrame = -RECOMPUTE_FRAMES;
    /** Fills the last frame drew, to decide whether this frame's stars fit the budget. */
    private int lastFills;

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
        if (lastFills + 1 + STAR_COUNT <= MAX_FRAME_FILLS) {
            for (int i = 0; i < STAR_COUNT; i++) {
                int x = (int) (STAR_X[i] * (w - 1));
                int y = (int) (STAR_Y[i] * (h - 1));
                graphics.fill(x, y, x + 1, y + 1, Palette.alpha(Palette.INK, STAR_ALPHA[i]));
            }
        }
        Minecraft.getInstance().gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        frame++;
        Canvas c = new Canvas(graphics);
        view.setSize(c.width, c.height);
        Labels labels = new Labels();
        OrbitStatePayload p = OrbitClock.current();
        drawHint(c, labels, p);
        if (p == null) {
            Component msg = Component.translatable("apoapsis.map.board");
            c.text(msg, (c.width - c.textWidth(msg)) / 2, c.height / 2 - 4, 1f, Palette.INK_MUTED, false);
        } else {
            CelestialBody body = Bodies.of(p);
            followBody(p, body);
            ensurePrediction(p);
            double nowUt = OrbitClock.nowUt(p, partialTick);
            resolveCentre(nowUt);
            drawTopLeft(c, labels, p, body, nowUt);
            drawSystem(c, labels, p, nowUt);
        }
        lastFills = c.fills();
    }

    /** Escape closes (vanilla), the System map key closes, 1 2 3 choose what the view follows. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (PilotKeys.SYSTEM_MAP.matches(event)) {
            onClose();
            return true;
        }
        int key = event.input();
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

    /**
     * The view's side of the ship changing body. The first sight of the ship fits its orbit, or Pale's sphere of
     * influence when it is there. Entering Pale follows Pale and fits its sphere of influence. Later the player's
     * keys decide.
     */
    private void followBody(OrbitStatePayload p, CelestialBody body) {
        if (body == shownBody) {
            return;
        }
        boolean first = shownBody == null;
        shownBody = body;
        if (body == MeridianSystem.PALE) {
            view.follow = MapView.Follow.PALE;
            view.fit(MeridianSystem.PALE.soiRadius() * SOI_FIT_MARGIN);
        } else {
            view.follow = MapView.Follow.SHIP;
            if (first) {
                view.fit(initialHalfExtent(p, body));
            }
        }
    }

    /** Half the extent to fit on first open: the ship's apoapsis when it is in an ellipse, else the whole system. */
    private static double initialHalfExtent(OrbitStatePayload p, CelestialBody body) {
        Elements el = Elements.of(body.mu, Bodies.stateOf(p));
        if (el.eccentricity() < 1 && Double.isFinite(el.apoapsisRadius())) {
            return Math.max(el.apoapsisRadius() * ORBIT_FIT_MARGIN, body.radius * 2.5);
        }
        return SYSTEM_HALF_EXTENT_M;
    }

    /**
     * Recomputes the prediction when the payload changes, at most every {@link #RECOMPUTE_FRAMES} frames, and
     * re-samples it when the zoom has moved by more than {@link #RESAMPLE_ZOOM_RATIO}, also at most that often.
     */
    private void ensurePrediction(OrbitStatePayload p) {
        double scale = view.scale();
        if (prediction == null || (prediction.source != p && frame - lastComputeFrame >= RECOMPUTE_FRAMES)) {
            prediction = MapPrediction.compute(p, scale);
            sampledScale = scale;
            lastComputeFrame = frame;
            lastSampleFrame = frame;
        } else if (zoomChanged(scale) && frame - lastSampleFrame >= RECOMPUTE_FRAMES) {
            prediction = prediction.resampled(scale);
            sampledScale = scale;
            lastSampleFrame = frame;
        }
    }

    private boolean zoomChanged(double scale) {
        double ratio = scale / sampledScale;
        return ratio > RESAMPLE_ZOOM_RATIO || ratio < 1 / RESAMPLE_ZOOM_RATIO;
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
                    int cur = prediction.indexAt(nowUt);
                    StateVector rel = prediction.relativeState(cur, nowUt);
                    Vec3d q = Bodies.placeNow(prediction.bodyOf(cur), nowUt, rel.r().x(), rel.r().y());
                    view.centreX = q.x();
                    view.centreY = q.y();
                }
            }
            default -> {
            }
        }
    }

    private void drawSystem(Canvas c, Labels labels, OrbitStatePayload p, double nowUt) {
        boolean orbit = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
        CelestialBody verdance = MeridianSystem.VERDANCE;
        CelestialBody pale = MeridianSystem.PALE;
        Vec3d palePos = Bodies.rootState(pale, nowUt).r();
        double vx = view.sx(0);
        double vy = view.sy(0);
        double px = view.sx(palePos.x());
        double py = view.sy(palePos.y());
        double verdanceR = view.px(verdance.radius);
        double paleR = Math.max(1.5, view.px(pale.radius));

        // Pale's orbit and sphere of influence, Verdance's atmosphere, then the two bodies and their names.
        c.ring(vx, vy, view.px(pale.orbitRadius), Palette.STEEL_500, PALE_ORBIT_SEGMENTS);
        c.dashedRing(px, py, view.px(pale.soiRadius()), Palette.SIGNAL_EDGE, 6);
        c.ring(vx, vy, view.px(verdance.radius + verdance.atmosphereTop),
                Palette.alpha(Palette.HYDROGEN, 0xB0), ATMOSPHERE_SEGMENTS);
        c.disc(vx, vy, verdanceR, Palette.VERDANCE_GROUND);
        c.disc(px, py, paleR, Palette.PALE_GROUND);
        bodyLabel(c, labels, "apoapsis.map.body.verdance", vx, vy, verdanceR);
        bodyLabel(c, labels, "apoapsis.map.body.pale", px, py, paleR);

        if (prediction.patchCount() == 0) {
            return;
        }
        // The predicted coast: later patches first so the current one is drawn on top. Each patch is offset by
        // patchCentre. Known limitation, not fixed: after a flyby the next Verdance patch is drawn at its true
        // positions, so it starts away from the ghost-drawn Pale patch by Pale's motion during the flyby.
        int cur = prediction.indexAt(nowUt);
        for (int i = prediction.patchCount() - 1; i >= cur; i--) {
            Vec3d o = patchCentre(prediction, i, cur, nowUt);
            double[] xs = prediction.xs(i);
            double[] ys = prediction.ys(i);
            int n = Math.min(xs.length, bufX.length);
            for (int k = 0; k < n; k++) {
                bufX[k] = view.sx(o.x() + xs[k]);
                bufY[k] = view.sy(o.y() + ys[k]);
            }
            c.polyline(bufX, bufY, n, i == cur ? Palette.SIGNAL : Palette.HYDROGEN);
        }

        for (MapPrediction.Mark m : prediction.marks()) {
            if (m.ut() < nowUt) {
                continue;
            }
            // Apsides are an orbit's business: the surface shows none, as the HUD does.
            if (!orbit && (m.kind() == MapPrediction.Kind.APOAPSIS || m.kind() == MapPrediction.Kind.PERIAPSIS)) {
                continue;
            }
            drawMark(c, labels, prediction, m, cur, nowUt);
        }

        // Ship marker: a small triangle pointing along the velocity relative to the body it is around (screen y is -Y).
        StateVector rel = prediction.relativeState(cur, nowUt);
        Vec3d at = Bodies.placeNow(prediction.bodyOf(cur), nowUt, rel.r().x(), rel.r().y());
        double sx = view.sx(at.x());
        double sy = view.sy(at.y());
        double dx = rel.v().x();
        double dy = -rel.v().y();
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

    /**
     * The root-frame point a patch's coordinates are offset by. The current patch and Verdance's patches use their
     * body now; a future patch around another body uses that body at the patch's start, the moment the ship enters it.
     * Verdance sits at the origin, so for it both give the same point.
     */
    private static Vec3d patchCentre(MapPrediction prediction, int index, int cur, double nowUt) {
        double ut = index > cur ? prediction.startUt(index) : nowUt;
        return Bodies.rootState(prediction.bodyOf(index), ut).r();
    }

    private void drawMark(Canvas c, Labels labels, MapPrediction prediction, MapPrediction.Mark m, int cur,
                          double nowUt) {
        // The encounter's dot stays where its body is now (it lies on the Verdance line). Other marks sit on the drawn
        // line of their patch, so they use patchCentre.
        Vec3d base = m.kind() == MapPrediction.Kind.ENCOUNTER
                ? Bodies.rootState(m.anchor(), nowUt).r()
                : patchCentre(prediction, m.patch(), cur, nowUt);
        double mx = view.sx(base.x() + m.x());
        double my = view.sy(base.y() + m.y());
        if (mx < -MARGIN_PX || my < -MARGIN_PX || mx > c.width + MARGIN_PX || my > c.height + MARGIN_PX) {
            return;
        }
        switch (m.kind()) {
            case APOAPSIS, PERIAPSIS -> {
                double bx = view.sx(base.x());
                double by = view.sy(base.y());
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
                label(c, labels, label, x, ly, Palette.INK_MUTED);
            }
            case ENCOUNTER -> {
                c.rect((int) Math.round(mx) - 1, (int) Math.round(my) - 1,
                        (int) Math.round(mx) + 2, (int) Math.round(my) + 2, Palette.INK);
                if (m.body().isEmpty()) {
                    return;
                }
                // The entered body where it will be at the encounter: a faint outline and sphere of influence, labelled.
                CelestialBody entered = MeridianSystem.byId(m.body());
                Vec3d ghost = Bodies.rootState(entered, m.ut()).r();
                double gx = view.sx(ghost.x());
                double gy = view.sy(ghost.y());
                double gr = Math.max(2, view.px(entered.radius));
                c.ring(gx, gy, gr, Palette.STEEL_500);
                // The label goes outside the sphere of influence, clear of the line entering it.
                double soiPx = view.px(entered.soiRadius());
                c.dashedRing(gx, gy, soiPx, Palette.alpha(Palette.SIGNAL_EDGE, 0x80), 6);
                double clear = Math.max(gr, soiPx);
                Component label = Component.translatable("apoapsis.map.mark.encounter",
                        Component.translatable("apoapsis.map.body." + m.body()),
                        Fmt.mono(Fmt.clock(m.ut() - nowUt)));
                int lx = (int) Math.round(gx + clear + 4);
                int ly = (int) Math.round(gy - 4);
                if (!label(c, labels, label, lx, ly, Palette.INK_MUTED)) {
                    int above = (int) Math.round(gy - clear - LABEL_H - 3);
                    label(c, labels, label, (int) Math.round(gx - c.textWidth(label) / 2.0), above, Palette.INK_MUTED);
                }
            }
            case ATMOSPHERE, IMPACT -> {
                c.rect((int) Math.round(mx) - 2, (int) Math.round(my) - 2,
                        (int) Math.round(mx) + 3, (int) Math.round(my) + 3, Palette.COPPER);
                String key = m.kind() == MapPrediction.Kind.ATMOSPHERE
                        ? "apoapsis.map.mark.atmosphere" : "apoapsis.map.mark.impact";
                Component label = Component.translatable(key, Fmt.mono(Fmt.clock(m.ut() - nowUt)));
                label(c, labels, label, (int) Math.round(mx + 7), (int) Math.round(my - 4), Palette.COPPER);
            }
            default -> {
            }
        }
    }

    /** A body's name centred above its disc, or below it when that spot is taken. */
    private static void bodyLabel(Canvas c, Labels labels, String key, double cx, double cy, double radiusPx) {
        Component name = Component.translatable(key);
        int x = (int) Math.round(cx - c.textWidth(name) / 2.0);
        int above = (int) Math.round(cy - Math.max(2, radiusPx) - LABEL_H - 3);
        if (!label(c, labels, name, x, above, Palette.INK_MUTED)) {
            label(c, labels, name, x, (int) Math.round(cy + radiusPx + 3), Palette.INK_MUTED);
        }
    }

    /** Draws {@code text} at {@code x}, {@code y} when it is on screen and clear of every label already drawn. */
    private static boolean label(Canvas c, Labels labels, Component text, int x, int y, int argb) {
        int w = c.textWidth(text);
        if (x + w < 0 || y + LABEL_H < 0 || x > c.width || y > c.height) {
            return false;
        }
        if (!labels.claim(x - 1, y - 1, x + w + 1, y + LABEL_H + 1)) {
            return false;
        }
        c.text(text, x, y, 1f, argb, false);
        return true;
    }

    /** Top left, no box, one font: body and regime, then altitude and speed, then apsides in orbit only. */
    private void drawTopLeft(Canvas c, Labels labels, OrbitStatePayload p, CelestialBody body, double nowUt) {
        int x = 16;
        int y = 14;
        Component bodyName = Component.translatable("apoapsis.map.body." + body.id);
        Component regime = Component.translatable(regimeKey(p.regime()));
        Component title = p.warp() > 1
                ? Component.translatable("apoapsis.map.title.warp", bodyName, regime, Fmt.warp(p.warp()))
                : Component.translatable("apoapsis.map.title", bodyName, regime);
        c.text(title, x, y, TITLE_SCALE, Palette.INK, false);
        int right = x + Math.round(c.textWidth(title) * TITLE_SCALE);

        double r = Math.sqrt(p.rx() * p.rx() + p.ry() * p.ry() + p.rz() * p.rz());
        double speed = Math.sqrt(p.vx() * p.vx() + p.vy() * p.vy() + p.vz() * p.vz());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("apoapsis.map.top.alt", Fmt.distance(r - body.radius)));
        lines.add(Component.translatable("apoapsis.map.top.speed", Fmt.speed(speed)));
        if (FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT) {
            Elements el = Elements.of(body.mu, Bodies.stateOf(p));
            if (el.eccentricity() < 1) {
                lines.add(Component.translatable("apoapsis.map.top.apsides",
                        Fmt.distance(el.apoapsisRadius() - body.radius),
                        Fmt.distance(el.periapsisRadius() - body.radius)));
                double toAp = el.timeToApoapsis() - (nowUt - p.ut());
                lines.add(Component.translatable("apoapsis.map.top.ap_in", Fmt.clock(toAp)));
            } else {
                lines.add(Component.translatable("apoapsis.map.top.periapsis",
                        Fmt.distance(el.periapsisRadius() - body.radius)));
            }
        }
        int lineY = y + 22;
        int blockRight = right;
        for (Component line : lines) {
            c.text(line, x, lineY, 1f, Palette.INK, false);
            blockRight = Math.max(blockRight, x + c.textWidth(line));
            lineY += LINE_STEP;
        }
        labels.reserve(x - 2, y - 2, blockRight + 2, lineY);
    }

    /** Lang key for a regime ordinal: ORBIT or SURFACE. */
    static String regimeKey(int ordinal) {
        return FlightRegime.byOrdinal(ordinal) == FlightRegime.ORBIT
                ? "apoapsis.map.regime.orbit" : "apoapsis.map.regime.surface";
    }

    /** Bottom left: the controls. The warp keys and the close key are named from the bindings. */
    private void drawHint(Canvas c, Labels labels, OrbitStatePayload p) {
        Component hint = Component.translatable("apoapsis.map.hint",
                PilotKeys.WARP_UP.getTranslatedKeyMessage(),
                PilotKeys.WARP_DOWN.getTranslatedKeyMessage(),
                p == null ? "—" : Fmt.warp(p.warp()),
                PilotKeys.SYSTEM_MAP.getTranslatedKeyMessage());
        int x = 16;
        int y = c.height - 16;
        c.text(hint, x, y, 1f, Palette.INK_MUTED, false);
        labels.reserve(x - 2, y - 2, x + c.textWidth(hint) + 2, y + LABEL_H + 2);
    }
}
