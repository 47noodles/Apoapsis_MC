package dev.apoapsis.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.OverlayMessages;
import dev.apoapsis.client.map3d.OrbitCamera;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.client.ui.UiBackdrop;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText.Type;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The full-screen system map seen from above (Tab in the 3D map, or the System map key when 2D is chosen). Full-bleed,
 * no panel, does not pause the game. Shows the piloted ship's orbit and predicted coast over Verdance and Pale, with
 * the same look as the 3D map: textured spheres lit from the sun on +X with an atmosphere, smooth glowing orbit lines,
 * and light overlays and labels from {@link MapChrome} and {@link Labels}. Everything is in UI units, so the layout is
 * the same at any GUI scale. Client only.
 *
 * <p>Frame rule: the current patch and its marks are drawn at their body's position now (the current extrapolated
 * time), so a Pale patch the ship is in sits around Pale's disc. A future patch around a non-root body (Pale) is drawn
 * at that body's position when the ship enters it, so the predicted line meets the encounter's dot. The encounter
 * shows a faint ghost of Pale where it will be at that time: its outline, a dashed sphere of influence and brackets.
 */
public final class MapScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    /** Zoom change, as a ratio either way, that re-samples the polylines. */
    private static final double RESAMPLE_ZOOM_RATIO = 1.5;
    private static final double SYSTEM_HALF_EXTENT_M = 360_000.0;
    private static final double ORBIT_FIT_MARGIN = 1.3;
    private static final double SOI_FIT_MARGIN = 1.1;
    private static final double MARGIN = 12;
    private static final double STANDARD_GRAVITY = 9.80665;
    /** Dash and gap of a future patch, UI units. */
    private static final double DASH = 4.5;
    private static final double GAP = 3.5;
    /** Within this many degrees of the Hohmann lead, the window is open now. */
    private static final double WINDOW_NOW_DEG = 2;
    /** Eccentricity below which the Pale window cue applies. */
    private static final double WINDOW_MAX_ECC = 0.1;
    private static final double MARK_RADIUS = 2.4;
    private static final double HELP_FIRST_S = 7;
    private static final double HELP_FADE_S = 1.5;
    /** Distance of the camera that builds a body's sphere, in body radii: far enough to be flat. */
    private static final double SPHERE_CAMERA = 1000;
    private static boolean helpSeen;

    private final MapView view = new MapView();
    private final MapScene out = new MapScene();
    private final Labels labels = new Labels();
    private final MapChrome chrome = new MapChrome();
    private final OrbitCamera downCamera = OrbitCamera.topDown(SPHERE_CAMERA);
    private final double[] bufX = new double[MapPrediction.MAX_SAMPLES + 1];
    private final double[] bufY = new double[MapPrediction.MAX_SAMPLES + 1];
    private final double[] ringHelper = new double[2];
    private final Identifier[] mapIds = new Identifier[2];
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
    private float uiScale = 1;
    private boolean helpPinned;
    private long helpUntilNanos;

    /** Opened by {@link dev.apoapsis.client.MapClient#openMap()}. */
    public MapScreen() {
        super(Component.translatable("screen.apoapsis.map"));
        if (!helpSeen) {
            helpSeen = true;
            helpUntilNanos = System.nanoTime() + (long) (HELP_FIRST_S * 1e9);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Full-bleed steel with the kit's fixed star field. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiBackdrop.draw(graphics);
        Minecraft.getInstance().gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        frame++;
        uiScale = UiScale.current().scale();
        double w = graphics.guiWidth() / (double) uiScale;
        double h = graphics.guiHeight() / (double) uiScale;
        view.setSize(w, h);
        chrome.begin(graphics);
        labels.begin(w, h);
        out.begin(w, h);
        OrbitStatePayload p = ClientState.orbit;
        Component overlay = OverlayMessages.currentOwn(Minecraft.getInstance());
        if (p == null) {
            bottomRow(null);
            chrome.textCentered(Type.BODY, Component.translatable("apoapsis.map.board").getString(), (float) (w / 2),
                    (float) (h / 2 - 4), UiPalette.INK_MUTED);
            chrome.flush();
            return;
        }
        CelestialBody body = Bodies.of(p);
        followBody(p, body);
        ensurePrediction(p);
        double nowUt = OrbitNow.utNow(p, partialTick);
        resolveCentre(nowUt);

        // The overlays lay out first and claim their boxes, so no label lands under one.
        chrome.crumb(labels, Component.translatable("apoapsis.map.system").getString(), focusName(body));
        chrome.time(labels, Component.translatable("apoapsis.map.time").getString(), Fmt.universalTime(nowUt));
        chrome.stats(labels, stats(p, body, nowUt));
        chrome.card(labels, transferCard(p, body, nowUt), chrome.cardBottom());
        bottomRow(p);
        if (overlay != null) {
            chrome.notice(labels, overlay.getString());
        }

        drawSystem(p, nowUt);
        chrome.scrims(out.front);
        MapPip.submit(graphics, out, uiScale, mapIds);
        chrome.flush();
    }

    /** Follows 0 Verdance, 1 Pale or 2 the ship and fits {@code halfExtentM} metres to the view: for {@link MapScript}. */
    void focusFor(int which, double halfExtentM) {
        view.follow = MapView.Follow.values()[which + 1];
        view.fit(halfExtentM);
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
        if (key == InputConstants.KEY_H) {
            helpPinned = !helpPinned;
            return true;
        }
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
        double scale = UiScale.current().scale();
        view.pan(dx / scale, dy / scale);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double scale = UiScale.current().scale();
        view.zoom(mouseX / scale, mouseY / scale, scrollY);
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
            view.follow = MapView.Follow.VERDANCE;
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

    // ---- the system ----

    private void drawSystem(OrbitStatePayload p, double nowUt) {
        boolean orbit = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
        CelestialBody verdance = MeridianSystem.VERDANCE;
        CelestialBody pale = MeridianSystem.PALE;
        Vec3d palePos = Bodies.rootState(pale, nowUt).r();
        double vx = view.sx(0);
        double vy = view.sy(0);
        double px = view.sx(palePos.x());
        double py = view.sy(palePos.y());
        double verdanceR = view.px(verdance.radius);
        double paleR = view.px(pale.radius);
        mapIds[0] = PlanetTextures.texture(verdance.id);
        mapIds[1] = PlanetTextures.texture(pale.id);
        QuadBatch back = out.back;

        // Pale's orbit: a quiet ring with a glow; its sphere of influence dashed. Both go under the bodies.
        double orbitR = view.px(pale.orbitRadius);
        back.ring(vx, vy, orbitR, MapScene.THIN_HALF + 0.04, MapScene.FEATHER,
                UiPalette.alpha(UiPalette.SIGNAL_EDGE, 0xE0), 0, 0);
        back.ring(vx, vy, orbitR, 0, 1.6, UiPalette.alpha(UiPalette.SIGNAL_EDGE, 0x1E), 0, 0);
        labels.markCircle(vx, vy, orbitR, 1);
        double soiR = view.px(pale.soiRadius());
        back.ring(px, py, soiR, MapScene.THIN_HALF, MapScene.FEATHER, UiPalette.alpha(UiPalette.SIGNAL_EDGE, 0xB0), 3, 3);
        labels.markCircle(px, py, soiR, 1);

        body(0, verdance, vx, vy, verdanceR, Palette.VERDANCE_LIT);
        body(1, pale, px, py, paleR, Palette.PALE_LIT);
        order(vx, vy, px, py);
        if (prediction.patchCount() > 0) {
            drawCoast(orbit, nowUt);
        }
        // The names come last of the labels placed here, after the paths and marks have claimed their spots.
        bodyLabel(verdance, vx, vy, verdanceR);
        bodyLabel(pale, px, py, paleR);
    }

    /** Draws the nearer... both bodies sit in the plane, so the order only matters if their discs overlap: Pale last. */
    private void order(double vx, double vy, double px, double py) {
        out.meshCount = 0;
        if (meshBuilt[0]) {
            out.order[out.meshCount++] = 0;
        }
        if (meshBuilt[1]) {
            out.order[out.meshCount++] = 1;
        }
    }

    private final boolean[] meshBuilt = new boolean[2];

    /**
     * A body as a lit, textured sphere seen with its poles up and the sun on its right, with its haze and limb in front.
     * Under about two units across it is one flat dot of its tone.
     */
    private void body(int index, CelestialBody b, double cx, double cy, double rPx, int tone) {
        meshBuilt[index] = false;
        if (!(rPx > 0) || !Double.isFinite(rPx)) {
            return;
        }
        if (cx + rPx < -4 || cx - rPx > out.viewWidth() + 4 || cy + rPx < -4 || cy - rPx > out.viewHeight() + 4) {
            return;
        }
        if (rPx < 2.2) {
            out.front.disc(cx, cy, Math.max(1.1, rPx), 0.4, tone);
            return;
        }
        double focal = rPx * Math.sqrt(SPHERE_CAMERA * SPHERE_CAMERA - 1);
        out.mesh[index].build(downCamera, cx, cy, focal, 0, 0, 0, 1.0, 0, 0, SPHERE_CAMERA, -Math.PI / 2,
                mapIds[index] != null ? 0xFFFFFF : tone & 0xFFFFFF);
        meshBuilt[index] = true;
        if (b.atmosphereTop > 0) {
            out.atmosphere(cx, cy, rPx, view.px(b.radius + b.atmosphereTop), 1, 0, 0.5);
        }
        out.limb(cx, cy, rPx);
        labels.markCircle(cx, cy, rPx, 4);
    }

    /** The predicted coast, its marks and the ship marker. */
    private void drawCoast(boolean orbit, double nowUt) {
        QuadBatch f = out.front;
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
                f.polyline(bufX, bufY, n, 0, 1.8, UiPalette.alpha(UiPalette.SIGNAL, 0x34));
                f.polyline(bufX, bufY, n, MapScene.ORBIT_HALF, MapScene.FEATHER, UiPalette.SIGNAL);
            } else {
                f.dashedPolyline(bufX, bufY, n, MapScene.ORBIT_HALF * 0.85, MapScene.FEATHER, DASH, GAP,
                        UiPalette.alpha(UiPalette.HYDROGEN, 0xC8));
            }
            labels.markPolyline(bufX, bufY, n, 3);
        }

        // The ship marker: a small triangle pointing along the velocity relative to the body it is around.
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
        f.disc(sx, sy, 6.5, 3.5, UiPalette.alpha(UiPalette.SIGNAL, 0x26));
        f.triangle(sx + ux * 4.6, sy + uy * 4.6, UiPalette.SIGNAL, sx - ux * 3 + nx * 3, sy - uy * 3 + ny * 3,
                UiPalette.SIGNAL, sx - ux * 3 - nx * 3, sy - uy * 3 - ny * 3, UiPalette.SIGNAL);
        labels.reserve(sx - 4, sy - 4, sx + 4, sy + 4);
        chrome.label(labels, Component.translatable("apoapsis.map3d.ship").getString(), UiPalette.SIGNAL, sx, sy, 5, true,
                Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT, Labels.Side.LEFT, Labels.Side.ABOVE,
                Labels.Side.BELOW);

        // The encounter first: it is the mark the player is after, so it gets the best place.
        for (int pass = 0; pass < 2; pass++) {
            for (MapPrediction.Mark m : prediction.marks()) {
                if (m.ut() < nowUt || (m.kind() == MapPrediction.Kind.ENCOUNTER) != (pass == 0)) {
                    continue;
                }
                // Apsides are an orbit's business: the surface shows none, as the HUD does.
                if (!orbit && (m.kind() == MapPrediction.Kind.APOAPSIS || m.kind() == MapPrediction.Kind.PERIAPSIS)) {
                    continue;
                }
                drawMark(m, cur, nowUt);
            }
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

    private void drawMark(MapPrediction.Mark m, int cur, double nowUt) {
        // The encounter's dot stays where its body is now (it lies on the Verdance line). Other marks sit on the drawn
        // line of their patch, so they use patchCentre.
        Vec3d base = m.kind() == MapPrediction.Kind.ENCOUNTER
                ? Bodies.rootState(m.anchor(), nowUt).r()
                : patchCentre(prediction, m.patch(), cur, nowUt);
        double mx = view.sx(base.x() + m.x());
        double my = view.sy(base.y() + m.y());
        if (mx < -MARGIN || my < -MARGIN || mx > out.viewWidth() + MARGIN || my > out.viewHeight() + MARGIN) {
            return;
        }
        QuadBatch f = out.front;
        switch (m.kind()) {
            case APOAPSIS, PERIAPSIS -> {
                marker(f, mx, my, UiPalette.INK);
                String key = m.kind() == MapPrediction.Kind.APOAPSIS ? "apoapsis.map.mark.ap" : "apoapsis.map.mark.pe";
                chrome.label(labels, Component.translatable(key, Fmt.distance(m.altM())).getString(),
                        UiPalette.INK_MUTED, mx, my, MARK_RADIUS, false, Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT,
                        Labels.Side.BELOW_RIGHT, Labels.Side.LEFT);
            }
            case ENCOUNTER -> {
                marker(f, mx, my, UiPalette.INK);
                double ax = mx;
                double ay = my;
                double clearance = MARK_RADIUS;
                if (!m.body().isEmpty()) {
                    // The entered body where it will be at the encounter: a faint outline and sphere of influence, bracketed.
                    CelestialBody entered = MeridianSystem.byId(m.body());
                    Vec3d ghost = Bodies.rootState(entered, m.ut()).r();
                    double gx = view.sx(ghost.x());
                    double gy = view.sy(ghost.y());
                    double gr = Math.max(3, view.px(entered.radius));
                    f.ring(gx, gy, gr, MapScene.THIN_HALF, MapScene.FEATHER, UiPalette.alpha(UiPalette.INK, 0x90), 2.2, 2.2);
                    f.ring(gx, gy, view.px(entered.soiRadius()), MapScene.THIN_HALF, MapScene.FEATHER,
                            UiPalette.alpha(UiPalette.SIGNAL_EDGE, 0xA0), 3, 3);
                    double half = gr + 4;
                    double arm = 4;
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sy = -1; sy <= 1; sy += 2) {
                            double cx = gx + sx * half;
                            double cy = gy + sy * half;
                            f.line(cx, cy, cx - sx * arm, cy, 0.2, MapScene.FEATHER, UiPalette.INK);
                            f.line(cx, cy, cx, cy - sy * arm, 0.2, MapScene.FEATHER, UiPalette.INK);
                        }
                    }
                    ax = gx;
                    ay = gy;
                    clearance = half + 2;
                    String name = Component.translatable("apoapsis.map.mark.encounter",
                            Component.translatable("apoapsis.map.body." + m.body()).getString()).getString();
                    String detail = Component.translatable("apoapsis.map.mark.countdown",
                            Fmt.clock(m.ut() - nowUt)).getString();
                    chrome.label2(labels, name, UiPalette.INK, detail, ax, ay, clearance, true, Labels.Side.RIGHT,
                            Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT, Labels.Side.LEFT, Labels.Side.ABOVE,
                            Labels.Side.BELOW);
                }
            }
            case ESCAPE -> {
                marker(f, mx, my, UiPalette.INK);
                chrome.label(labels, Component.translatable("apoapsis.map.mark.escape",
                        Component.translatable("apoapsis.map.body." + m.anchor().id).getString(),
                        Fmt.clock(m.ut() - nowUt)).getString(), UiPalette.INK_MUTED, mx, my, MARK_RADIUS, false,
                        Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT);
            }
            case ATMOSPHERE, IMPACT -> {
                marker(f, mx, my, UiPalette.COPPER);
                String key = m.kind() == MapPrediction.Kind.ATMOSPHERE
                        ? "apoapsis.map.mark.atmosphere" : "apoapsis.map.mark.impact";
                chrome.label(labels, Component.translatable(key, Fmt.clock(m.ut() - nowUt)).getString(),
                        UiPalette.COPPER, mx, my, MARK_RADIUS, false, Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT,
                        Labels.Side.BELOW_RIGHT);
            }
            default -> {
            }
        }
    }

    private static void marker(QuadBatch f, double x, double y, int colour) {
        f.disc(x, y, MARK_RADIUS + 3, 3, UiPalette.alpha(colour, 0x1C));
        f.ring(x, y, MARK_RADIUS, 0.26, MapScene.FEATHER, colour, 0, 0);
    }

    /**
     * A body's name above its disc with its surface gravity beneath, or below the disc when the spot above is taken. A
     * body so big that the spot is off screen gets no label (the breadcrumb names the focus).
     */
    private void bodyLabel(CelestialBody body, double cx, double cy, double radiusPx) {
        String name = Component.translatable("apoapsis.map.body." + body.id).getString();
        String gees = Fmt.gees(body.gravity(body.radius) / STANDARD_GRAVITY);
        chrome.label2(labels, name, UiPalette.INK, gees, cx, cy, Math.max(2, radiusPx), false, Labels.Side.ABOVE,
                Labels.Side.BELOW, Labels.Side.RIGHT, Labels.Side.LEFT);
    }

    // ---- overlays ----

    private String focusName(CelestialBody shipBody) {
        CelestialBody b = switch (view.follow) {
            case VERDANCE -> MeridianSystem.VERDANCE;
            case PALE -> MeridianSystem.PALE;
            default -> shipBody;
        };
        return Component.translatable("apoapsis.map.body." + b.id).getString();
    }

    private static String t(String key) {
        return Component.translatable("apoapsis.map." + key).getString();
    }

    /**
     * The figures of the selected thing, down the left edge: for the ship its altitude, speed and apsides; for a body
     * its gravity, size and, for Pale, its orbit.
     */
    private List<MapChrome.Stat> stats(OrbitStatePayload p, CelestialBody body, double nowUt) {
        List<MapChrome.Stat> stats = new ArrayList<>();
        CelestialBody followed = view.follow == MapView.Follow.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        if ((view.follow == MapView.Follow.VERDANCE || view.follow == MapView.Follow.PALE) && followed != body) {
            CelestialBody b = view.follow == MapView.Follow.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
            stats.add(new MapChrome.Stat(t("stat.gravity"), Fmt.gees(b.gravity(b.radius) / STANDARD_GRAVITY)));
            stats.add(new MapChrome.Stat(t("stat.radius"), Fmt.distance(b.radius)));
            stats.add(new MapChrome.Stat(t("stat.atmosphere"),
                    b.atmosphereTop > 0 ? Fmt.distance(b.atmosphereTop) : t("stat.none")));
            if (b.parent != null) {
                stats.add(new MapChrome.Stat(t("stat.orbit"), Fmt.distance(b.orbitRadius)));
                stats.add(new MapChrome.Stat(t("stat.period"), Fmt.clock(b.orbitPeriod())));
            }
            return stats;
        }
        double r = Math.sqrt(p.rx() * p.rx() + p.ry() * p.ry() + p.rz() * p.rz());
        double speed = Math.sqrt(p.vx() * p.vx() + p.vy() * p.vy() + p.vz() * p.vz());
        stats.add(new MapChrome.Stat(t("stat.alt"), Fmt.distance(r - body.radius)));
        stats.add(new MapChrome.Stat(t("stat.speed"), Fmt.speed(speed)));
        if (FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT) {
            Elements el = Elements.of(body.mu, Bodies.stateOf(p));
            if (MapPrediction.boundWithinSoi(el, body)) {
                double toAp = el.timeToApoapsis() - (nowUt - p.ut());
                stats.add(new MapChrome.Stat(t("stat.ap"), Fmt.distance(el.apoapsisRadius() - body.radius),
                        Component.translatable("apoapsis.map.stat.in", Fmt.clock(toAp)).getString(), UiPalette.INK));
                stats.add(new MapChrome.Stat(t("stat.pe"), Fmt.distance(el.periapsisRadius() - body.radius)));
            } else {
                stats.add(new MapChrome.Stat(t("stat.pe"), Fmt.distance(el.periapsisRadius() - body.radius)));
            }
        }
        return stats;
    }

    /**
     * The transfer card: a caption, then the encounter countdown once one is on the path, else the departure time of the
     * Pale window as the one big line, with the burn's delta-v for that transfer. Empty when no window or encounter
     * applies.
     */
    private List<MapChrome.Line> transferCard(OrbitStatePayload p, CelestialBody body, double nowUt) {
        List<MapChrome.Line> lines = new ArrayList<>();
        String timer;
        String burn = null;
        double encounterUt = encounterUt(nowUt);
        if (!Double.isNaN(encounterUt)) {
            timer = Component.translatable("apoapsis.map.card.encounter", Fmt.clock(encounterUt - nowUt)).getString();
        } else {
            double windowIn = paleWindowIn(p, body, nowUt);
            if (Double.isNaN(windowIn)) {
                return lines;
            }
            timer = windowIn == 0
                    ? Component.translatable("apoapsis.map.card.now").getString()
                    : Component.translatable("apoapsis.map.card.depart", Fmt.clock(windowIn)).getString();
            int cur = prediction.indexAt(nowUt);
            StateVector s = prediction.relativeState(cur, nowUt);
            double dv = TransferMath.hohmannDeltaV(body.mu, s.r().length(), s.v().length(),
                    MeridianSystem.PALE.orbitRadius);
            if (Double.isFinite(dv)) {
                burn = Component.translatable("apoapsis.map.card.burn", Fmt.speed(dv)).getString();
            }
        }
        lines.add(new MapChrome.Line(Component.translatable("apoapsis.map.card.title",
                Component.translatable("apoapsis.map.body.verdance").getString(),
                Component.translatable("apoapsis.map.body.pale").getString()).getString(), Type.LABEL,
                UiPalette.INK_MUTED));
        lines.add(new MapChrome.Line(timer, Type.HEADING, UiPalette.SIGNAL));
        if (burn != null) {
            lines.add(new MapChrome.Line(burn, Type.BODY, UiPalette.INK));
        }
        return lines;
    }

    /** The bottom row: the keys that matter and a key for the rest, and the warp while piloting. */
    private void bottomRow(@Nullable OrbitStatePayload p) {
        List<MapChrome.Hint> hints = new ArrayList<>();
        hints.add(MapChrome.Hint.of("SCROLL", t("hint.zoom")));
        hints.add(MapChrome.Hint.of("DRAG", t("hint.pan")));
        hints.add(MapChrome.Hint.of(new String[] {"1", "2", "3"}, t("hint.focus")));
        hints.add(MapChrome.Hint.of(PilotKeys.SYSTEM_MAP, t("hint.close")));
        hints.add(MapChrome.Hint.of("H", t("hint.more")));
        chrome.bottomRow(labels, hints, t("warp"), p == null ? null : Fmt.warp(p.warp()));
        double left = (helpUntilNanos - System.nanoTime()) / 1e9;
        float alpha = helpPinned ? 1f : (float) Math.max(0, Math.min(1, left / HELP_FADE_S));
        if (alpha > 0.01f) {
            List<MapChrome.Hint> help = new ArrayList<>();
            help.add(MapChrome.Hint.of(PilotKeys.WARP_UP, t("key.faster")));
            help.add(MapChrome.Hint.of(PilotKeys.WARP_DOWN, t("key.slower")));
            help.add(MapChrome.Hint.of(PilotKeys.THROTTLE_CUT, Component.translatable("apoapsis.map3d.key.cut").getString()));
            help.add(MapChrome.Hint.of("TAB", Component.translatable("apoapsis.map3d.key.map3d").getString()));
            chrome.help(labels, help, alpha);
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
}
