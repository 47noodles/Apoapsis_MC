package dev.apoapsis.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.OverlayMessages;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Transfers;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The full-screen system map (the System map key, M by default). Full-bleed, no panel, does not pause the game. Shows
 * the piloted ship's orbit and predicted coast over Verdance and Pale. Client only.
 *
 * <p>Frame rule: the current patch and its marks are drawn at their body's position now (the current extrapolated
 * time), so a Pale patch the ship is in sits around Pale's disc. A future patch around a non-root body (Pale) is drawn
 * at that body's position when the ship enters it, so the predicted line meets the encounter's white dot. The encounter
 * shows a faint ghost of Pale where it will be at that time: its outline, a dashed sphere of influence and brackets.
 *
 * <p>Look: the current patch is a clean one-pixel line, future patches are dashed, Pale's orbit a quiet thin ring. Bodies
 * are shaded discs lit from the upper left, textured when the server has sent a surface map. Labels are drawn after the
 * bodies and the paths, on a dark backdrop. Chrome sits in the four corners and reserves its boxes before any label is
 * placed, so nothing lands on it. The bottom row holds the keycaps on the left and the warp strip on the right.
 */
public final class MapScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    /** Zoom change, as a ratio either way, that re-samples the polylines. */
    private static final double RESAMPLE_ZOOM_RATIO = 1.5;
    private static final int STAR_COUNT = 160;
    private static final int PALE_ORBIT_SEGMENTS = 256;
    private static final int ATMOSPHERE_SEGMENTS = 256;
    private static final double SYSTEM_HALF_EXTENT_M = 360_000.0;
    private static final double ORBIT_FIT_MARGIN = 1.3;
    private static final double SOI_FIT_MARGIN = 1.1;
    private static final double MARGIN_PX = 24;
    private static final int LINE_STEP = 12;
    private static final int LABEL_H = 9;
    /** Light comes from the upper left of the screen (screen y grows downward). */
    private static final double LIGHT_X = -0.6;
    private static final double LIGHT_Y = -0.8;
    private static final double STANDARD_GRAVITY = 9.80665;
    /** Dash length of a future patch, px. */
    private static final double DASH_PX = 6;
    /** The bottom row sits this far above the bottom edge, px. The transfer card stands 8 px above it. */
    private static final int BOTTOM_INSET = 22;
    /** Clear space between the keycaps and the warp strip, px. */
    private static final int BOTTOM_GAP = 12;
    /** Below this GUI width the scroll, drag and focus hints are left out, so the warp keys fit, px. */
    private static final int NARROW_GUI_PX = 376;
    /** Within this many degrees of the Hohmann lead, the window is open now. */
    private static final double WINDOW_NOW_DEG = 2;
    /** Eccentricity below which the Pale window cue applies. */
    private static final double WINDOW_MAX_ECC = 0.1;
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
    /** Pilot keys whose press the map has acted on and that are still held: their key-repeats are swallowed. */
    private final Set<Integer> heldPilotKeys = new HashSet<>();
    private MapPrediction prediction;
    /** The view scale the prediction's polylines were sampled at. */
    private double sampledScale = 1e-3;
    /** The body the ship was in when last seen; null before the first payload. */
    private CelestialBody shownBody;
    private long frame;
    private long lastComputeFrame = -RECOMPUTE_FRAMES;
    private long lastSampleFrame = -RECOMPUTE_FRAMES;

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
        Labels labels = new Labels();
        OrbitStatePayload p = ClientState.orbit;
        int bottomY = c.height - BOTTOM_INSET;
        // The bottom row claims its box first so no label lands on it; it is drawn last so it sits on the paths.
        labels.reserve(8, bottomY - 6, c.width - 8, bottomY + LABEL_H + 6);
        Component overlay = OverlayMessages.currentOwn(Minecraft.getInstance());
        if (p == null) {
            Component msg = Component.translatable("apoapsis.map.board");
            c.text(msg, (c.width - c.textWidth(msg)) / 2, c.height / 2 - 4, 1f, Palette.INK_MUTED, false);
            drawBottom(c, null, bottomY, overlay);
            return;
        }
        CelestialBody body = Bodies.of(p);
        followBody(p, body);
        ensurePrediction(p);
        double nowUt = OrbitNow.utNow(p, partialTick);
        resolveCentre(nowUt);
        drawTopLeft(c, labels, p, body, nowUt);
        drawTime(c, labels, nowUt);
        Card card = transferCard(c, p, body, nowUt);
        if (card != null) {
            labels.reserve(card.x0() - 2, card.y0() - 2, card.x1() + 2, card.y1() + 2);
        }
        drawSystem(c, labels, p, nowUt);
        drawBottom(c, p, bottomY, overlay);
        if (card != null) {
            drawCard(c, card);
        }
    }

    /** Escape closes (vanilla), the System map key closes, 1 2 3 choose what the view follows, the pilot keys act. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (PilotKeys.SYSTEM_MAP.matches(event)) {
            onClose();
            return true;
        }
        // With a screen open vanilla does not act on the pilot keys, so the map sends their presses itself.
        if (PilotKeys.WARP_UP.matches(event)
                && pilotKey(event, ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_WARP_UP)) {
            return true;
        }
        if (PilotKeys.WARP_DOWN.matches(event)
                && pilotKey(event, ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_WARP_DOWN)) {
            return true;
        }
        if (PilotKeys.THROTTLE_CUT.matches(event)
                && pilotKey(event, ShipInputPayload.THROTTLE_CUT, (byte) 0)) {
            return true;
        }
        int key = event.input();
        if (key == InputConstants.KEY_TAB) {
            dev.apoapsis.client.MapClient.switchMap();
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
    public boolean keyReleased(KeyEvent event) {
        heldPilotKeys.remove(event.input());
        return super.keyReleased(event);
    }

    /**
     * A pilot key sends one input to the ship on its initial press only: a key-repeat of a key still held is swallowed.
     * False when the player rides no ship, so the key falls through to the screen.
     */
    private boolean pilotKey(KeyEvent event, byte throttle, byte flag) {
        int key = event.input();
        if (heldPilotKeys.contains(key)) {
            return true;
        }
        if (!sendToShip(throttle, flag)) {
            return false;
        }
        heldPilotKeys.add(key);
        return true;
    }

    /** Sends one pilot input to the ship the player rides. False when the player rides no ship. */
    private static boolean sendToShip(byte throttle, byte flag) {
        Minecraft mc = Minecraft.getInstance();
        Entity vehicle = mc.player == null ? null : mc.player.getVehicle();
        if (!(vehicle instanceof ShipEntity ship) || mc.getConnection() == null) {
            return false;
        }
        ClientPacketDistributor.sendToServer(new ShipInputPayload(ship.getId(), (byte) 0, (byte) 0, (byte) 0,
                throttle, flag));
        return true;
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

        // Pale's orbit is a quiet thin ring; its sphere of influence dashed; the bodies, then Verdance's thin air ring.
        c.ring(vx, vy, view.px(pale.orbitRadius), Palette.alpha(Palette.STEEL_500, 0xA0), PALE_ORBIT_SEGMENTS);
        c.dashedRing(px, py, view.px(pale.soiRadius()), Palette.SIGNAL_EDGE, 6);
        drawBody(c, PlanetTextures.texture(verdance.id), vx, vy, verdanceR, Palette.VERDANCE_LIT,
                Palette.VERDANCE_GROUND);
        c.ring(vx, vy, view.px(verdance.radius + verdance.atmosphereTop),
                Palette.alpha(Palette.HYDROGEN, 0x70), ATMOSPHERE_SEGMENTS);
        drawBody(c, PlanetTextures.texture(pale.id), px, py, paleR, Palette.PALE_LIT, Palette.PALE_GROUND);
        if (prediction.patchCount() > 0) {
            drawCoast(c, labels, orbit, nowUt);
        }
        // The body names come last, over the bodies and the paths.
        bodyLabel(c, labels, verdance, "apoapsis.map.body.verdance", vx, vy, verdanceR);
        bodyLabel(c, labels, pale, "apoapsis.map.body.pale", px, py, paleR);
    }

    /** The predicted coast, its marks and the ship marker. Labels on the marks are drawn over the paths. */
    private void drawCoast(Canvas c, Labels labels, boolean orbit, double nowUt) {
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
            if (i == cur) {
                // The current patch is one clean pixel line: a halo read as a thick, jagged line.
                c.glowPolyline(bufX, bufY, n, 0, Palette.SIGNAL, 0);
            } else {
                // Future patches are plain dashes: a halo turns short dashes into blobs.
                c.glowPolyline(bufX, bufY, n, DASH_PX, Palette.HYDROGEN, 0);
            }
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
     * Off: the surface maps come from base terrain columns (stone and water, before surface rules), so a textured disc
     * reads as grey and blue static, and its square mask covers the chrome under it. Bodies use the shaded disc.
     */
    private static final boolean TEXTURED_BODIES = false;

    /**
     * A body as a disc. With its surface map (and {@link #TEXTURED_BODIES}), the map textured and shaded; else a lit disc
     * in the body's palette colours. Under three pixels a disc has no shading to show, so it is one flat colour.
     */
    private static void drawBody(Canvas c, Identifier texture, double cx, double cy, double r, int lit, int dark) {
        if (r < 3) {
            c.disc(cx, cy, r, lit);
        } else if (TEXTURED_BODIES && texture != null) {
            c.texturedDisc(texture, cx, cy, r, LIGHT_X, LIGHT_Y, Palette.STEEL_950,
                    Palette.alpha(Palette.STEEL_950, 0xB0));
        } else {
            c.shadedDisc(cx, cy, r, LIGHT_X, LIGHT_Y, lit, dark);
        }
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
                c.line(mx + dx * 2, my + dy * 2, mx + dx * 6, my + dy * 6, Palette.BRASS);
                String key = m.kind() == MapPrediction.Kind.APOAPSIS ? "apoapsis.map.mark.ap" : "apoapsis.map.mark.pe";
                Component label = Component.translatable(key, Fmt.distance(m.altM()));
                int lx = (int) Math.round(mx + dx * 9);
                int ly = (int) Math.round(my + dy * 9 - 4);
                int x = dx < -0.3 ? lx - c.textWidth(label) : lx;
                label(c, labels, label, x, ly, Palette.INK_MUTED);
            }
            case ENCOUNTER -> {
                c.rect((int) Math.round(mx) - 1, (int) Math.round(my) - 1,
                        (int) Math.round(mx) + 2, (int) Math.round(my) + 2, Palette.INK);
                if (m.body().isEmpty()) {
                    return;
                }
                // The entered body where it will be at the encounter: a faint outline and sphere of influence, bracketed.
                CelestialBody entered = MeridianSystem.byId(m.body());
                Vec3d ghost = Bodies.rootState(entered, m.ut()).r();
                double gx = view.sx(ghost.x());
                double gy = view.sy(ghost.y());
                double gr = Math.max(2, view.px(entered.radius));
                c.ring(gx, gy, gr, Palette.STEEL_500);
                c.dashedRing(gx, gy, view.px(entered.soiRadius()), Palette.alpha(Palette.SIGNAL_EDGE, 0x80), 6);
                double half = Math.max(gr + 5, 9);
                c.corners(gx - half, gy - half, gx + half, gy + half, 4, Palette.INK);
                // Two lines beside the brackets: the name of the encounter, then the countdown to it.
                Component title = Component.translatable("apoapsis.map.mark.encounter",
                        Fmt.spaced(Component.translatable("apoapsis.map.body." + m.body()).getString()));
                Component countdown = Component.translatable("apoapsis.map.mark.countdown",
                        Fmt.clock(m.ut() - nowUt));
                int lx = (int) Math.round(gx + half + 6);
                int ly = (int) Math.round(gy - half);
                if (!label(c, labels, title, lx, ly, Palette.INK)) {
                    lx = (int) Math.round(gx - c.textWidth(title) / 2.0);
                    ly = (int) Math.round(gy - half - LABEL_H - 4);
                    label(c, labels, title, lx, ly, Palette.INK);
                    label(c, labels, countdown, (int) Math.round(gx - c.textWidth(countdown) / 2.0),
                            (int) Math.round(gy + half + 4), Palette.INK_MUTED);
                    return;
                }
                label(c, labels, countdown, lx, ly + LABEL_H + 3, Palette.INK_MUTED);
            }
            case ESCAPE -> {
                c.rect((int) Math.round(mx) - 1, (int) Math.round(my) - 1,
                        (int) Math.round(mx) + 2, (int) Math.round(my) + 2, Palette.INK);
                Component label = Component.translatable("apoapsis.map.mark.escape",
                        Component.translatable("apoapsis.map.body." + m.anchor().id),
                        Fmt.countdown(m.ut() - nowUt));
                label(c, labels, label, (int) Math.round(mx + 7), (int) Math.round(my - 4), Palette.INK_MUTED);
            }
            case ATMOSPHERE, IMPACT -> {
                c.rect((int) Math.round(mx) - 2, (int) Math.round(my) - 2,
                        (int) Math.round(mx) + 3, (int) Math.round(my) + 3, Palette.COPPER);
                String key = m.kind() == MapPrediction.Kind.ATMOSPHERE
                        ? "apoapsis.map.mark.atmosphere" : "apoapsis.map.mark.impact";
                Component label = Component.translatable(key, Fmt.countdown(m.ut() - nowUt));
                label(c, labels, label, (int) Math.round(mx + 7), (int) Math.round(my - 4), Palette.COPPER);
            }
            default -> {
            }
        }
    }

    /**
     * A body's name in spaced caps above its disc, with its surface gravity beneath, or below the disc when the spot
     * above is taken.
     */
    private static void bodyLabel(Canvas c, Labels labels, CelestialBody body, String key, double cx, double cy,
                                  double radiusPx) {
        Component name = Component.literal(Fmt.spaced(Component.translatable(key).getString()));
        Component sub = Component.literal(Fmt.gees(body.gravity(body.radius) / STANDARD_GRAVITY));
        int x = (int) Math.round(cx - c.textWidth(name) / 2.0);
        int subX = (int) Math.round(cx - c.textWidth(sub) / 2.0);
        int above = (int) Math.round(cy - Math.max(2, radiusPx) - 2 * LABEL_H - 8);
        if (label(c, labels, name, x, above, Palette.INK)) {
            label(c, labels, sub, subX, above + LABEL_H + 3, Palette.INK_MUTED);
        } else {
            int below = (int) Math.round(cy + radiusPx + 3);
            label(c, labels, name, x, below, Palette.INK);
            label(c, labels, sub, subX, below + LABEL_H + 3, Palette.INK_MUTED);
        }
    }

    /**
     * Draws {@code text} at {@code x}, {@code y} when it is on screen and clear of every label already drawn. It sits
     * on a dark backdrop so it reads over a disc or a path.
     */
    private static boolean label(Canvas c, Labels labels, Component text, int x, int y, int argb) {
        int w = c.textWidth(text);
        if (x + w < 0 || y + LABEL_H < 0 || x > c.width || y > c.height) {
            return false;
        }
        if (!labels.claim(x - 1, y - 1, x + w + 1, y + LABEL_H + 1)) {
            return false;
        }
        c.rect(x - 1, y - 1, x + w + 1, y + LABEL_H + 1, Palette.alpha(Palette.STEEL_950, 0xB0));
        c.text(text, x, y, 1f, argb, false);
        return true;
    }

    /**
     * Top left, no box round the figures: the breadcrumb (system and body) in a bracketed frame, then altitude and
     * speed, then apsides for an orbit that stays in its sphere of influence (else periapsis).
     */
    private void drawTopLeft(Canvas c, Labels labels, OrbitStatePayload p, CelestialBody body, double nowUt) {
        int x = 16;
        int y = 14;
        Component crumb = Component.translatable("apoapsis.map.crumb",
                Fmt.spaced(Component.translatable("apoapsis.map.system").getString()),
                Fmt.spaced(Component.translatable("apoapsis.map.body." + body.id).getString()));
        int crumbW = c.textWidth(crumb);
        c.corners(x, y, x + crumbW + 14, y + LABEL_H + 8, 5, Palette.STEEL_500);
        c.text(crumb, x + 7, y + 4, 1f, Palette.INK, false);
        int blockRight = x + crumbW + 14;

        double r = Math.sqrt(p.rx() * p.rx() + p.ry() * p.ry() + p.rz() * p.rz());
        double speed = Math.sqrt(p.vx() * p.vx() + p.vy() * p.vy() + p.vz() * p.vz());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("apoapsis.map.top.alt", Fmt.distance(r - body.radius)));
        lines.add(Component.translatable("apoapsis.map.top.speed", Fmt.speed(speed)));
        if (FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT) {
            Elements el = Elements.of(body.mu, Bodies.stateOf(p));
            if (MapPrediction.boundWithinSoi(el, body)) {
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
        int lineY = y + LABEL_H + 8 + 12;
        for (Component line : lines) {
            c.text(line, x, lineY, 1f, Palette.INK, false);
            blockRight = Math.max(blockRight, x + c.textWidth(line));
            lineY += LINE_STEP;
        }
        labels.reserve(x - 2, y - 2, blockRight + 2, lineY);
    }

    /** Top right: the universal time in a bracketed frame. */
    private static void drawTime(Canvas c, Labels labels, double nowUt) {
        Component time = Component.translatable("apoapsis.map.time", Fmt.universalTime(nowUt));
        int w = c.textWidth(time);
        int x1 = c.width - 16;
        int x0 = x1 - w - 14;
        int y = 14;
        c.corners(x0, y, x1, y + LABEL_H + 8, 5, Palette.STEEL_500);
        c.text(time, x0 + 7, y + 4, 1f, Palette.INK, false);
        labels.reserve(x0 - 2, y - 2, x1 + 2, y + LABEL_H + 10);
    }

    /** The transfer card's lines and box, bottom right, standing just above the bottom row. */
    private record Card(Component title, Component timer, Component burn, int x0, int y0, int x1, int y1) {}

    /**
     * The transfer card: "TRANSFER · VERDANCE TO PALE", then the encounter countdown once one is on the path, else the
     * departure time of the Pale window, with the burn's delta-v for that transfer. Null when no window or encounter applies.
     */
    private Card transferCard(Canvas c, OrbitStatePayload p, CelestialBody body, double nowUt) {
        Component timer;
        Component burn = null;
        double encounterUt = encounterUt(nowUt);
        if (!Double.isNaN(encounterUt)) {
            timer = Component.translatable("apoapsis.map.card.encounter", Fmt.clock(encounterUt - nowUt));
        } else {
            double windowIn = paleWindowIn(p, body, nowUt);
            if (Double.isNaN(windowIn)) {
                return null;
            }
            timer = windowIn == 0
                    ? Component.translatable("apoapsis.map.card.now")
                    : Component.translatable("apoapsis.map.card.depart", Fmt.clock(windowIn));
            int cur = prediction.indexAt(nowUt);
            StateVector s = prediction.relativeState(cur, nowUt);
            double dv = TransferMath.hohmannDeltaV(body.mu, s.r().length(), s.v().length(),
                    MeridianSystem.PALE.orbitRadius);
            if (Double.isFinite(dv)) {
                burn = Component.translatable("apoapsis.map.card.burn", Fmt.speed(dv));
            }
        }
        Component title = Component.translatable("apoapsis.map.card.title",
                Component.translatable("apoapsis.map.body.verdance"), Component.translatable("apoapsis.map.body.pale"));
        int w = Math.max(c.textWidth(title), c.textWidth(timer));
        if (burn != null) {
            w = Math.max(w, c.textWidth(burn));
        }
        int x1 = c.width - 16;
        int y1 = c.height - BOTTOM_INSET - 8;
        int h = 3 * LINE_STEP + 10;
        return new Card(title, timer, burn, x1 - w - 20, y1 - h, x1, y1);
    }

    private static void drawCard(Canvas c, Card card) {
        c.rect(card.x0(), card.y0(), card.x1(), card.y1(), Palette.alpha(Palette.STEEL_950, 0xC0));
        c.corners(card.x0(), card.y0(), card.x1() - 1, card.y1() - 1, 6, Palette.STEEL_500);
        int tx = card.x0() + 10;
        int ty = card.y0() + 6;
        c.text(card.title(), tx, ty, 1f, Palette.INK_MUTED, false);
        c.text(card.timer(), tx, ty + LINE_STEP, 1f, Palette.SIGNAL, false);
        if (card.burn() != null) {
            c.text(card.burn(), tx, ty + 2 * LINE_STEP, 1f, Palette.INK, false);
        }
    }

    /**
     * The Hohmann window cue: on a near-circular, prograde orbit of Verdance with no encounter predicted, the time until
     * Pale is where a transfer from this orbit meets it. Zero when the window is open now (within
     * {@link #WINDOW_NOW_DEG}), NaN when the cue does not apply. A retrograde orbit has no such window.
     */
    private double paleWindowIn(OrbitStatePayload p, CelestialBody body, double nowUt) {
        if (body != MeridianSystem.VERDANCE || FlightRegime.byOrdinal(p.regime()) != FlightRegime.ORBIT
                || prediction.patchCount() == 0 || !Double.isNaN(encounterUt(nowUt))) {
            return Double.NaN;
        }
        int cur = prediction.indexAt(nowUt);
        StateVector s = prediction.relativeState(cur, nowUt);
        if (!(Elements.of(body.mu, s).eccentricity() < WINDOW_MAX_ECC)) {
            return Double.NaN;
        }
        // Angular momentum about the system's +Z: positive is prograde.
        double hz = s.r().x() * s.v().y() - s.r().y() * s.v().x();
        if (!(hz > 0)) {
            return Double.NaN;
        }
        double r1 = Math.hypot(s.r().x(), s.r().y());
        StateVector pale = Bodies.rootState(MeridianSystem.PALE, nowUt);
        double lead = Math.atan2(pale.r().y(), pale.r().x()) - Math.atan2(s.r().y(), s.r().x());
        double wanted = Transfers.hohmannLead(body.mu, r1, MeridianSystem.PALE.orbitRadius,
                MeridianSystem.PALE.orbitPeriod());
        double error = Math.IEEEremainder(lead - wanted, 2 * Math.PI);
        if (Math.abs(error) < Math.toRadians(WINDOW_NOW_DEG)) {
            return 0;
        }
        double omegaShip = hz / (r1 * r1);
        double omegaPale = 2 * Math.PI / MeridianSystem.PALE.orbitPeriod();
        return Transfers.windowSeconds(error, omegaPale - omegaShip);
    }

    /** The universal time of the first encounter at or after {@code ut}, or NaN when none is predicted. */
    private double encounterUt(double ut) {
        for (MapPrediction.Mark m : prediction.marks()) {
            if (m.kind() == MapPrediction.Kind.ENCOUNTER && m.ut() >= ut) {
                return m.ut();
            }
        }
        return Double.NaN;
    }

    /** Lang key for a regime ordinal: ORBIT or SURFACE. */
    static String regimeKey(int ordinal) {
        return FlightRegime.byOrdinal(ordinal) == FlightRegime.ORBIT
                ? "apoapsis.map.regime.orbit" : "apoapsis.map.regime.surface";
    }

    /**
     * Bottom row: the controls as keycaps from the left, the warp strip at the right edge. Hints that do not fit before
     * the strip are left out, scroll and drag first (on narrow GUI widths they and the focus keys are not tried at all, so
     * faster and slower fit at GUI width 320). The warp hints show
     * only while piloting. While a vanilla overlay message is showing (warp refusals, sphere of influence entry, hard
     * landing) it takes the keycaps' place, in full ink, so it stays readable.
     */
    private static void drawBottom(Canvas c, OrbitStatePayload p, int y, Component overlay) {
        String step = p == null ? null : Fmt.warp(p.warp());
        int stripLeft = step == null ? c.width - 16 : c.width - 32 - warpTotal(c, step);
        if (overlay != null) {
            c.text(overlay, 16, y, 1f, Palette.INK, false);
        } else {
            int limit = stripLeft - BOTTOM_GAP;
            int x = 16;
            if (c.width >= NARROW_GUI_PX) {
                x = hint(c, x, y, limit, Component.translatable("apoapsis.map.key.zoom"), Component.literal("SCROLL"));
                x = hint(c, x, y, limit, Component.translatable("apoapsis.map.key.pan"), Component.literal("DRAG"));
                x = hint(c, x, y, limit, Component.translatable("apoapsis.map.key.focus"),
                        Component.literal("1"), Component.literal("2"), Component.literal("3"));
            }
            if (p != null) {
                x = hint(c, x, y, limit, Component.translatable("apoapsis.map.key.faster"),
                        PilotKeys.WARP_UP.getTranslatedKeyMessage());
                x = hint(c, x, y, limit, Component.translatable("apoapsis.map.key.slower"),
                        PilotKeys.WARP_DOWN.getTranslatedKeyMessage());
            }
            hint(c, x, y, limit, Component.translatable("apoapsis.map.key.close"),
                    PilotKeys.SYSTEM_MAP.getTranslatedKeyMessage());
        }
        if (step != null) {
            drawWarp(c, step, y);
        }
    }

    /**
     * Keycaps for one control, then what they do, drawn only when they end by {@code limit}. Returns the x after the
     * hint, or {@code x} unchanged when it does not fit.
     */
    private static int hint(Canvas c, int x, int y, int limit, Component what, Component... keys) {
        int w = 3 + c.textWidth(what) + 18;
        for (Component key : keys) {
            w += c.textWidth(key) + 8 + 3;
        }
        if (x + w > limit) {
            return x;
        }
        int at = x;
        for (Component key : keys) {
            at += keycap(c, at, y, key) + 3;
        }
        c.text(what, at + 3, y + 2, 1f, Palette.INK_MUTED, false);
        return x + w;
    }

    /** One keycap: a small raised box with its key name. Returns its width. */
    private static int keycap(Canvas c, int x, int y, Component key) {
        int w = c.textWidth(key) + 8;
        int h = LABEL_H + 5;
        c.rect(x, y - 2, x + w, y - 2 + h, Palette.STEEL_700);
        c.rect(x, y - 2, x + w, y - 1, Palette.STEEL_500);
        c.rect(x, y - 2 + h - 1, x + w, y - 2 + h, Palette.STEEL_500);
        c.rect(x, y - 2, x + 1, y - 2 + h, Palette.STEEL_500);
        c.rect(x + w - 1, y - 2, x + w, y - 2 + h, Palette.STEEL_500);
        c.text(key, x + 4, y, 1f, Palette.INK, false);
        return w;
    }

    /** Width of the warp strip's label and step cell for {@code step}, GUI px. */
    private static int warpTotal(Canvas c, String step) {
        return c.textWidth(Component.translatable("apoapsis.map.warp")) + 10 + c.textWidth(Component.literal(step)) + 12;
    }

    /** The warp strip: "WARP" and the current step in a bracketed cell, at the bottom right edge. */
    private static void drawWarp(Canvas c, String step, int y) {
        Component label = Component.translatable("apoapsis.map.warp");
        int labelW = c.textWidth(label);
        int cellW = c.textWidth(Component.literal(step)) + 12;
        int total = warpTotal(c, step);
        int x0 = c.width - 24 - total;
        c.corners(x0 - 8, y - 6, x0 + total + 8, y + LABEL_H + 5, 6, Palette.STEEL_500);
        c.text(label, x0, y, 1f, Palette.INK_MUTED, false);
        int cx = x0 + labelW + 10;
        c.rect(cx, y - 3, cx + cellW, y + LABEL_H + 3, Palette.alpha(Palette.SIGNAL_EDGE, 0xA0));
        c.text(Component.literal(step), cx + 6, y, 1f, Palette.SIGNAL, false);
    }
}
