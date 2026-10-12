package dev.apoapsis.client.map3d;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.MapClient;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.OverlayMessages;
import dev.apoapsis.client.map.Fmt;
import dev.apoapsis.client.map.Labels;
import dev.apoapsis.client.map.MapChrome;
import dev.apoapsis.client.map.MapKeys;
import dev.apoapsis.client.map.MapPip;
import dev.apoapsis.client.map.MapScene;
import dev.apoapsis.client.map.QuadBatch;
import dev.apoapsis.client.map.TransferMath;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.client.ui.UiBackdrop;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText.Type;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * The 3D system map: a view that orbits a focus (1 Verdance, 2 Pale, 3 the piloted ship) with free rotation, and zooms
 * with the wheel. It is full-bleed: the scene is drawn by {@link Scene3D} into a {@link MapScene} and put on screen by
 * {@link MapPip}, and the only things over it are light overlays from {@link MapChrome} (the breadcrumb, the clock, the
 * figures of the selected thing down the left edge, a card for a transfer or a node, one quiet row of key hints and
 * the warp), plus labels placed by the shared {@link Labels} solver. The map key opens it by default (see
 * {@link MapClient}; Tab flips to the 2D map). It also holds the maneuver node ({@link ManeuverEditor}) and the site
 * markers ({@link SiteMarkers}), and a click on a body or the ship focuses it. Does not pause the game. Every
 * coordinate here is in UI units, so the layout is the same at any GUI scale. Client only.
 */
public final class Map3DScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    /** The view's distance on opening, and on Verdance: Pale's orbit fills about half the width. */
    static final double SYSTEM_DISTANCE_M = 2.3 * MeridianSystem.PALE.orbitRadius;
    private static final double MAX_DISTANCE_M = 1e8;
    private static final double MIN_DISTANCE_M = 2000;
    /** Time constants, s, of the jump a focus change or a new prediction leaves in the view, and of the zoom. */
    private static final double FOCUS_TAU_S = 0.12;
    private static final double DISTANCE_TAU_S = 0.1;
    /** A press that moves less than this is a click, UI units. */
    private static final double CLICK_SLOP = 3;
    /** How close a click must be to the ship's mark to focus it, UI units, and the smallest disc a click on a body hits. */
    private static final double SHIP_PICK = 7;
    private static final double MIN_BODY_PICK = 5;
    private static final double MARGIN = 12;
    private static final double MARK_RADIUS = 2.4;
    /** How long the key list shows by itself the first time the map opens in a session, s, and its fade. */
    private static final double HELP_FIRST_S = 7;
    private static final double HELP_FADE_S = 1.5;
    private static boolean helpSeen;

    /** What the view orbits. */
    enum Follow { VERDANCE, PALE, SHIP }

    private final OrbitCamera camera = new OrbitCamera(SYSTEM_DISTANCE_M);
    private final Scene3D scene = new Scene3D();
    private final MapScene out = new MapScene();
    private final Labels labels = new Labels();
    private final MapChrome chrome = new MapChrome();
    private Follow follow = Follow.VERDANCE;
    private boolean followChosen;
    /** The distance the camera eases toward; the wheel and the focus keys set it. */
    private double targetDistance = SYSTEM_DISTANCE_M;
    /**
     * The view's centre in the root frame: the followed point exactly, plus {@link #focusOffset}. The offset is what is
     * left of a jump (a focus change, or the prediction being recomputed) and decays, so the focus is centred at rest
     * and glides on a change, whatever the time warp.
     */
    private Vec3d focus = Vec3d.ZERO;
    private Vec3d focusOffset = Vec3d.ZERO;
    private boolean snapFocus = true;
    private long lastFrameNanos;
    private final ManeuverEditor editor = new ManeuverEditor();
    private final boolean[] textured = new boolean[2];
    private final Identifier[] mapIds = new Identifier[2];
    private double pressX;
    private double pressY;
    private boolean pressMoved;
    private @Nullable Follow pressTarget;
    private OrbitPlot plot;
    private OrbitStatePayload plotSource;
    private long frame;
    private long lastComputeFrame = -RECOMPUTE_FRAMES;
    private double nowUt;
    private double viewW = 640;
    private double viewH = 360;
    private float uiScale = 1;
    private boolean helpPinned;
    private long helpUntilNanos;

    /** Opened by {@link dev.apoapsis.client.MapClient#openMap()} when the 3D map is on. */
    public Map3DScreen() {
        super(Component.translatable("screen.apoapsis.map"));
        camera.drag(-110, 0);
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
        UiScale ui = UiScale.current();
        uiScale = ui.scale();
        viewW = graphics.guiWidth() / (double) uiScale;
        viewH = graphics.guiHeight() / (double) uiScale;
        chrome.begin(graphics);
        labels.begin(viewW, viewH);
        out.begin(viewW, viewH);
        double mx = mouseX / (double) uiScale;
        double my = mouseY / (double) uiScale;
        Component overlay = OverlayMessages.current(Minecraft.getInstance());
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            bottomRow(null, false);
            chrome.textCentered(Type.BODY, Component.translatable("apoapsis.map.board").getString(),
                    (float) (viewW / 2), (float) (viewH / 2 - 4), UiPalette.INK_MUTED);
            chrome.flush();
            return;
        }
        CelestialBody body = MeridianSystem.byId(BodyId.byOrdinal(p.body()).id);
        nowUt = OrbitNow.utNow(p, partialTick);
        if (plot == null || (plotSource != p && frame - lastComputeFrame >= RECOMPUTE_FRAMES)) {
            // A new prediction moves the followed point a little: take that into the offset, so the view does not jump.
            Vec3d before = plot == null ? null : focusTarget(follow);
            plot = OrbitPlot.compute(body, stateOf(p), p.ut(), nowUt);
            if (before != null) {
                focusOffset = focusOffset.add(before.sub(focusTarget(follow)));
            }
            plotSource = p;
            lastComputeFrame = frame;
        }
        boolean orbit = FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
        if (!followChosen) {
            followChosen = true;
            follow = orbit ? Follow.SHIP : Follow.VERDANCE;
            targetDistance = defaultDistance(follow);
        }
        clampTarget();
        easeView();

        // The overlays lay out first and claim their boxes, so no label lands under one.
        chrome.crumb(labels, Component.translatable("apoapsis.map.system").getString(), focusName(body));
        chrome.time(labels, Component.translatable("apoapsis.map.time").getString(), Fmt.universalTime(nowUt));
        chrome.stats(labels, stats(p, body, orbit));
        float bottom = chrome.cardBottom();
        List<MapChrome.Line> nodeLines = editor.cardLines();
        bottom = chrome.card(labels, nodeLines, bottom) - 4;
        List<MapChrome.Line> transfer = transferCard(p, body, orbit);
        chrome.card(labels, transfer, bottom);
        bottomRow(p, orbit);
        if (overlay != null) {
            chrome.notice(labels, overlay.getString());
        }

        editor.begin(scene, plot, nowUt, orbit, this::screenOf, OrbitCamera.focal(viewH), shipBurn(), mx, my);
        for (int k = 0; k < 2; k++) {
            mapIds[k] = PlanetTextures.texture(k == 0 ? MeridianSystem.VERDANCE.id : MeridianSystem.PALE.id);
            textured[k] = mapIds[k] != null;
        }
        scene.build(out, labels, camera, plot, editor.predicted(), nowUt, viewW, viewH, focus, textured);
        editor.draw(out, chrome, labels, scene);
        drawBodyLabels();
        drawMarks();
        SiteMarkers.draw(out, chrome, labels, viewH, nowUt, camera, focus, this::screenOf);

        chrome.scrims(out.front);
        MapPip.submit(graphics, out, uiScale, mapIds);
        chrome.flush();
    }

    /** What the player flies, for the burn time; null when the player rides no ship. */
    private static ManeuverEditor.@Nullable ShipBurn shipBurn() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof ShipEntity ship) {
            return new ManeuverEditor.ShipBurn(ship.massKg(), ship.ispSeconds(0), ship.maxThrustN(0), ship.deltaV(0));
        }
        return null;
    }

    /**
     * Moves the offset toward zero and the distance toward its target in log space, by the time since the last frame,
     * and puts the focus on the followed point plus what is left of the offset.
     */
    private void easeView() {
        long now = System.nanoTime();
        double dt = lastFrameNanos == 0 ? 0 : Math.min(0.1, (now - lastFrameNanos) / 1e9);
        lastFrameNanos = now;
        if (snapFocus) {
            focusOffset = Vec3d.ZERO;
            snapFocus = false;
        } else {
            focusOffset = focusOffset.scale(Math.exp(-dt / FOCUS_TAU_S));
        }
        focus = focusTarget(follow).add(focusOffset);
        double d = camera.distance();
        double logD = Math.log(d) + (Math.log(targetDistance) - Math.log(d)) * (1 - Math.exp(-dt / DISTANCE_TAU_S));
        camera.setDistance(Math.exp(logD));
    }

    /** The point the view is centred on for a focus, in the root frame, now. */
    private Vec3d focusTarget(Follow f) {
        return switch (f) {
            case VERDANCE -> Vec3d.ZERO;
            case PALE -> OrbitPlot.rootPosition(MeridianSystem.PALE, nowUt);
            case SHIP -> plot.shipRootAt(nowUt);
        };
    }

    /** A distance that frames a focus: the system on Verdance, Pale's sphere of influence, the ship's orbit. */
    private double defaultDistance(Follow f) {
        return switch (f) {
            case VERDANCE -> SYSTEM_DISTANCE_M;
            case PALE -> 3 * MeridianSystem.PALE.soiRadius();
            case SHIP -> {
                CelestialBody b = plot.shipBody();
                yield Math.max(6 * b.radius, 2.5 * plot.shipRootAt(nowUt).length());
            }
        };
    }

    private double minDistance() {
        return switch (follow) {
            case PALE -> Math.max(MIN_DISTANCE_M, 1.2 * MeridianSystem.PALE.radius);
            case VERDANCE -> Math.max(MIN_DISTANCE_M, 1.2 * MeridianSystem.VERDANCE.radius);
            case SHIP -> Math.max(MIN_DISTANCE_M, 1.2 * plot.shipBody().radius);
        };
    }

    private void clampTarget() {
        targetDistance = Math.max(minDistance(), Math.min(MAX_DISTANCE_M, targetDistance));
    }

    private void setFollow(Follow f) {
        if (f == follow || plot == null) {
            return;
        }
        // The view stays where it is and glides to the new focus: the offset is the way back.
        focusOffset = focus.sub(focusTarget(f));
        follow = f;
        targetDistance = defaultDistance(f);
        clampTarget();
    }

    private static StateVector stateOf(OrbitStatePayload p) {
        return new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
    }

    /** Projects a root-frame point to the view (UI units): {x, y, depth}, or null when it is behind the eye. */
    private double @Nullable [] screenOf(Vec3d root) {
        double[] out = new double[3];
        double depth = camera.project(root.x() - focus.x(), root.y() - focus.y(), root.z() - focus.z(), viewW / 2.0,
                viewH / 2.0, OrbitCamera.focal(viewH), out);
        if (Double.isNaN(depth)) {
            return null;
        }
        out[2] = depth;
        return out;
    }

    // ---- overlays ----

    /** The name of what the view is on: the ship's body, or the body followed. */
    private String focusName(CelestialBody shipBody) {
        CelestialBody b = switch (follow) {
            case VERDANCE -> MeridianSystem.VERDANCE;
            case PALE -> MeridianSystem.PALE;
            case SHIP -> shipBody;
        };
        return Component.translatable("apoapsis.map.body." + b.id).getString();
    }

    /**
     * The figures of the selected thing, down the left edge: for the ship its altitude, speed, apsides, inclination and
     * an escape countdown; for a body its gravity, size and, for Pale, its orbit.
     */
    private List<MapChrome.Stat> stats(OrbitStatePayload p, CelestialBody shipBody, boolean orbit) {
        List<MapChrome.Stat> stats = new ArrayList<>();
        boolean shipSelected = follow == Follow.SHIP || (follow == Follow.VERDANCE && shipBody == MeridianSystem.VERDANCE)
                || (follow == Follow.PALE && shipBody == MeridianSystem.PALE);
        if (shipSelected) {
            StateVector s = plot.shipStateAt(nowUt);
            double r = s.r().length();
            stats.add(new MapChrome.Stat(t("stat.alt"), Fmt.distance(r - shipBody.radius)));
            stats.add(new MapChrome.Stat(t("stat.speed"), Fmt.speed(s.v().length())));
            if (orbit) {
                Elements el = Elements.of(shipBody.mu, s);
                if (el.eccentricity() < 1 && el.apoapsisRadius() < shipBody.soiRadius()) {
                    stats.add(new MapChrome.Stat(t("stat.ap"), Fmt.distance(el.apoapsisRadius() - shipBody.radius),
                            Component.translatable("apoapsis.map.stat.in", Fmt.clock(el.timeToApoapsis())).getString(),
                            UiPalette.INK));
                    stats.add(new MapChrome.Stat(t("stat.pe"), Fmt.distance(el.periapsisRadius() - shipBody.radius)));
                } else {
                    stats.add(new MapChrome.Stat(t("stat.pe"), Fmt.distance(el.periapsisRadius() - shipBody.radius)));
                }
                if (plot.shipBody() == MeridianSystem.VERDANCE) {
                    stats.add(new MapChrome.Stat(t("stat.inc"),
                            String.format(Locale.ROOT, "%.1f°", OrbitPlot.relativeInclinationDeg(s))));
                }
            }
            Patch cur = plot.patches.get(plot.current);
            if (cur.end() == Patch.EndReason.ESCAPE) {
                stats.add(new MapChrome.Stat(t("stat.escape"), Fmt.clock(cur.endUt() - nowUt)));
            }
            return stats;
        }
        CelestialBody b = follow == Follow.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        stats.add(new MapChrome.Stat(t("stat.gravity"), Fmt.gees(b.gravity(b.radius) / 9.80665)));
        stats.add(new MapChrome.Stat(t("stat.radius"), Fmt.distance(b.radius)));
        stats.add(new MapChrome.Stat(t("stat.atmosphere"),
                b.atmosphereTop > 0 ? Fmt.distance(b.atmosphereTop) : t("stat.none")));
        if (b.parent != null) {
            stats.add(new MapChrome.Stat(t("stat.orbit"), Fmt.distance(b.orbitRadius)));
            stats.add(new MapChrome.Stat(t("stat.period"), Fmt.clock(b.orbitPeriod())));
        }
        return stats;
    }

    private static String t(String key) {
        return Component.translatable("apoapsis.map." + key).getString();
    }

    /**
     * The transfer card on a predicted encounter: a caption, the countdown as the one big line, and the burn's delta-v
     * for a Hohmann transfer to Pale's orbit. Empty when there is no encounter or the ship is not orbiting Verdance.
     */
    private List<MapChrome.Line> transferCard(OrbitStatePayload p, CelestialBody body, boolean orbit) {
        List<MapChrome.Line> lines = new ArrayList<>();
        if (body != MeridianSystem.VERDANCE || !orbit) {
            return lines;
        }
        Double encounter = null;
        for (OrbitPlot.Mark m : plot.marks) {
            if (m.kind() == OrbitPlot.Kind.ENCOUNTER && m.ut() >= nowUt) {
                encounter = m.ut();
                break;
            }
        }
        if (encounter == null) {
            return lines;
        }
        StateVector s = plot.shipStateAt(nowUt);
        double dv = TransferMath.hohmannDeltaV(body.mu, s.r().length(), s.v().length(), MeridianSystem.PALE.orbitRadius);
        lines.add(new MapChrome.Line(Component.translatable("apoapsis.map.card.title",
                Component.translatable("apoapsis.map.body.verdance").getString(),
                Component.translatable("apoapsis.map.body.pale").getString()).getString(), Type.LABEL,
                UiPalette.INK_MUTED));
        lines.add(new MapChrome.Line(Component.translatable("apoapsis.map.card.encounter",
                Fmt3d.clock(encounter - nowUt)).getString(), Type.HEADING, UiPalette.SIGNAL));
        if (Double.isFinite(dv)) {
            lines.add(new MapChrome.Line(Component.translatable("apoapsis.map.card.burn", Fmt.speed(dv)).getString(),
                    Type.BODY, UiPalette.INK));
        }
        return lines;
    }

    /** The bottom row: the four keys that matter and a key for the rest, and the warp while piloting. */
    private void bottomRow(@Nullable OrbitStatePayload p, boolean orbit) {
        List<MapChrome.Hint> hints = new ArrayList<>();
        hints.add(MapChrome.Hint.of("SCROLL", t("hint.zoom")));
        hints.add(MapChrome.Hint.of("DRAG", t("hint.rotate")));
        hints.add(MapChrome.Hint.of(new String[] {"1", "2", "3"}, t("hint.focus")));
        hints.add(MapChrome.Hint.of(PilotKeys.SYSTEM_MAP, t("hint.close")));
        hints.add(MapChrome.Hint.of("H", t("hint.more")));
        chrome.bottomRow(labels, hints, t("warp"), p == null ? null : Fmt.warp(p.warp()));
        float alpha = helpPinned ? 1f : helpAlpha();
        if (alpha > 0.01f) {
            List<MapChrome.Hint> help = new ArrayList<>();
            if (orbit) {
                help.add(MapChrome.Hint.of("CLICK", Component.translatable("apoapsis.map3d.key.node").getString()));
            }
            help.add(MapChrome.Hint.of(PilotKeys.WARP_UP, Component.translatable("apoapsis.map.key.faster").getString()));
            help.add(MapChrome.Hint.of(PilotKeys.WARP_DOWN, Component.translatable("apoapsis.map.key.slower").getString()));
            help.add(MapChrome.Hint.of(PilotKeys.THROTTLE_CUT, Component.translatable("apoapsis.map3d.key.cut").getString()));
            help.add(MapChrome.Hint.of("TAB", Component.translatable("apoapsis.map3d.key.map2d").getString()));
            chrome.help(labels, help, alpha);
        }
    }

    private float helpAlpha() {
        double left = (helpUntilNanos - System.nanoTime()) / 1e9;
        return (float) Math.max(0, Math.min(1, left / HELP_FADE_S));
    }

    // ---- labels and marks ----

    /** The ship's name beside its marker, then each body's name above its disc. */
    private void drawBodyLabels() {
        if (!Double.isNaN(scene.ship[0])) {
            chrome.label(labels, Component.translatable("apoapsis.map3d.ship").getString(), UiPalette.SIGNAL,
                    scene.ship[0], scene.ship[1], 5, true, Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT,
                    Labels.Side.BELOW_RIGHT, Labels.Side.LEFT, Labels.Side.ABOVE, Labels.Side.BELOW);
        }
        drawBodyLabel(MeridianSystem.VERDANCE);
        drawBodyLabel(MeridianSystem.PALE);
    }

    private void drawBodyLabel(CelestialBody body) {
        Vec3d centre = body == MeridianSystem.VERDANCE ? Vec3d.ZERO : OrbitPlot.rootPosition(body, nowUt);
        double[] s = screenOf(centre);
        if (s == null) {
            return;
        }
        double radiusPx = scene.radiusPx(centre, body.radius);
        chrome.label(labels, Component.translatable("apoapsis.map.body." + body.id).getString(), UiPalette.INK, s[0], s[1],
                Math.max(2, radiusPx), false, Labels.Side.ABOVE, Labels.Side.BELOW, Labels.Side.RIGHT, Labels.Side.LEFT);
    }

    /** The predicted marks, each a small ring and a label, after the time passed; a mark behind a body is not shown. */
    private void drawMarks() {
        CelestialBody coreBody = plot.patches.get(plot.current).body();
        QuadBatch f = out.front;
        // The encounter first: it is the mark the player is after, so it gets the best place.
        for (int pass = 0; pass < 2; pass++) {
            for (OrbitPlot.Mark m : plot.marks) {
                if (m.ut() < nowUt || (m.kind() == OrbitPlot.Kind.ENCOUNTER) != (pass == 0)) {
                    continue;
                }
                double[] s = screenOf(m.at());
                if (s == null || !scene.visible(m.at()) || s[0] < -MARGIN || s[1] < -MARGIN || s[0] > viewW + MARGIN
                        || s[1] > viewH + MARGIN) {
                    continue;
                }
                switch (m.kind()) {
                    case APOAPSIS, PERIAPSIS -> {
                        double alt = m.at().sub(OrbitPlot.rootPosition(coreBody, m.ut())).length() - coreBody.radius;
                        String key = m.kind() == OrbitPlot.Kind.APOAPSIS ? "apoapsis.map.mark.ap" : "apoapsis.map.mark.pe";
                        marker(f, s, UiPalette.INK);
                        chrome.label(labels, Component.translatable(key, Fmt.distance(alt)).getString(),
                                UiPalette.INK_MUTED, s[0], s[1], MARK_RADIUS, false, Labels.Side.RIGHT,
                                Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT, Labels.Side.LEFT);
                    }
                    case ENCOUNTER -> encounter(f, m, s);
                    case ESCAPE -> {
                        marker(f, s, UiPalette.INK);
                        chrome.label(labels, Component.translatable("apoapsis.map.mark.escape",
                                Component.translatable("apoapsis.map.body." + coreBody.id).getString(),
                                Fmt3d.clock(m.ut() - nowUt)).getString(), UiPalette.INK_MUTED, s[0], s[1], MARK_RADIUS,
                                false, Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT);
                    }
                    case ATMOSPHERE, IMPACT -> {
                        marker(f, s, UiPalette.COPPER);
                        String key = m.kind() == OrbitPlot.Kind.ATMOSPHERE
                                ? "apoapsis.map.mark.atmosphere" : "apoapsis.map.mark.impact";
                        chrome.label(labels, Component.translatable(key, Fmt3d.clock(m.ut() - nowUt)).getString(),
                                UiPalette.COPPER, s[0], s[1], MARK_RADIUS, false, Labels.Side.RIGHT,
                                Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT);
                    }
                    case ASCENDING_NODE, DESCENDING_NODE -> {
                        marker(f, s, UiPalette.BRASS);
                        String key = m.kind() == OrbitPlot.Kind.ASCENDING_NODE ? "apoapsis.map3d.node.an"
                                : "apoapsis.map3d.node.dn";
                        chrome.label(labels, Component.translatable(key).getString(), UiPalette.BRASS, s[0], s[1],
                                MARK_RADIUS, false, Labels.Side.RIGHT, Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT);
                    }
                    default -> {
                    }
                }
            }
        }
    }

    /** An encounter: a ghost of the body where it will be, bracketed, the entry point, and a two-line label. */
    private void encounter(QuadBatch f, OrbitPlot.Mark m, double[] entry) {
        double[] ghost = screenOf(OrbitPlot.rootPosition(m.body(), m.ut()));
        double clearance = MARK_RADIUS;
        double ax = entry[0];
        double ay = entry[1];
        if (ghost != null) {
            double gr = Math.max(3, scene.radiusPx(OrbitPlot.rootPosition(m.body(), m.ut()), m.body().radius));
            f.ring(ghost[0], ghost[1], gr, MapScene.THIN_HALF, MapScene.FEATHER,
                    UiPalette.alpha(UiPalette.INK, 0x90), 2.2, 2.2);
            double half = gr + 4;
            double arm = 4;
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    double cx = ghost[0] + sx * half;
                    double cy = ghost[1] + sy * half;
                    f.line(cx, cy, cx - sx * arm, cy, 0.2, MapScene.FEATHER, UiPalette.INK);
                    f.line(cx, cy, cx, cy - sy * arm, 0.2, MapScene.FEATHER, UiPalette.INK);
                }
            }
            ax = ghost[0];
            ay = ghost[1];
            clearance = half + 2;
        }
        marker(f, entry, UiPalette.INK);
        String name = Component.translatable("apoapsis.map.mark.encounter",
                Component.translatable("apoapsis.map.body." + m.body().id).getString()).getString();
        String detail = Component.translatable("apoapsis.map.mark.countdown", Fmt3d.clock(m.ut() - nowUt)).getString();
        chrome.label2(labels, name, UiPalette.INK, detail, ax, ay, clearance, true, Labels.Side.RIGHT,
                Labels.Side.ABOVE_RIGHT, Labels.Side.BELOW_RIGHT, Labels.Side.LEFT, Labels.Side.ABOVE, Labels.Side.BELOW);
    }

    private static void marker(QuadBatch f, double[] s, int colour) {
        f.disc(s[0], s[1], MARK_RADIUS + 3, 3, UiPalette.alpha(colour, 0x1C));
        f.ring(s[0], s[1], MARK_RADIUS, 0.26, MapScene.FEATHER, colour, 0, 0);
    }

    // ---- input ----

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (PilotKeys.SYSTEM_MAP.matches(event)) {
            onClose();
            return true;
        }
        // With a screen open vanilla does not click the warp and throttle keys, so the map sends them itself.
        if (PilotKeys.WARP_UP.matches(event) && sendShip(ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_WARP_UP)) {
            return true;
        }
        if (PilotKeys.WARP_DOWN.matches(event)
                && sendShip(ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_WARP_DOWN)) {
            return true;
        }
        if (PilotKeys.THROTTLE_CUT.matches(event) && sendShip(ShipInputPayload.THROTTLE_CUT, (byte) 0)) {
            return true;
        }
        int key = event.input();
        if (key == InputConstants.KEY_H) {
            helpPinned = !helpPinned;
            return true;
        }
        if (key == InputConstants.KEY_1) {
            setFollow(Follow.VERDANCE);
            return true;
        }
        if (key == InputConstants.KEY_2) {
            setFollow(Follow.PALE);
            return true;
        }
        if (key == InputConstants.KEY_3) {
            setFollow(Follow.SHIP);
            return true;
        }
        if (key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE) {
            return editor.deleteNode();
        }
        if (key == InputConstants.KEY_TAB) {
            MapClient.switchMap();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Sends one ship input to the ship the player rides. False when the player rides no ship. */
    private static boolean sendShip(byte throttle, byte flag) {
        Minecraft mc = Minecraft.getInstance();
        Entity vehicle = mc.player == null ? null : mc.player.getVehicle();
        if (!(vehicle instanceof ShipEntity ship) || mc.getConnection() == null) {
            return false;
        }
        ClientPacketDistributor.sendToServer(new ShipInputPayload(ship.getId(), (byte) 0, (byte) 0, (byte) 0,
                throttle, flag));
        return true;
    }

    /** GUI pixels (what the mouse reports) to UI units. */
    private double ui(double gui) {
        return gui / UiScale.current().scale();
    }

    /**
     * A press. The maneuver editor goes first (a handle, the node, the orbit line); else a left press on the ship or a
     * body is remembered, and focuses it on release if the pointer did not move.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = ui(event.x());
        double my = ui(event.y());
        pressX = mx;
        pressY = my;
        pressMoved = false;
        pressTarget = null;
        if (editor.mousePressed(mx, my, event.button())) {
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && plot != null) {
            pressTarget = targetAt(mx, my);
        }
        return true;
    }

    /** A drag: the editor's (a handle or the node), else with the left button the view turns, free of any pole. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double scale = UiScale.current().scale();
        if (editor.mouseDragged(dx / scale, dy / scale, event.hasShiftDown())) {
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (Math.hypot(ui(event.x()) - pressX, ui(event.y()) - pressY) > CLICK_SLOP) {
            pressMoved = true;
        }
        // The turn per pixel was tuned in GUI pixels at a GUI scale of 3: the same turn per device pixel at any scale.
        double k = Minecraft.getInstance().getWindow().getGuiScale() / 3.0;
        camera.drag(dx * k, dy * k);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean took = editor.mouseReleased(ui(event.x()), ui(event.y()), event.button());
        if (!took && event.button() == InputConstants.MOUSE_BUTTON_LEFT && !pressMoved && pressTarget != null) {
            setFollow(pressTarget);
        }
        pressTarget = null;
        return true;
    }

    /** The ship's mark or the nearest body's disc under the pointer, or null. The ship wins; of two bodies the nearer. */
    private @Nullable Follow targetAt(double mx, double my) {
        double[] ship = screenOf(plot.shipRootAt(nowUt));
        if (ship != null && Math.hypot(ship[0] - mx, ship[1] - my) <= SHIP_PICK) {
            return Follow.SHIP;
        }
        Follow best = null;
        double bestDepth = Double.MAX_VALUE;
        for (Follow f : new Follow[] {Follow.VERDANCE, Follow.PALE}) {
            CelestialBody b = f == Follow.VERDANCE ? MeridianSystem.VERDANCE : MeridianSystem.PALE;
            double[] s = screenOf(OrbitPlot.rootPosition(b, nowUt));
            if (s == null) {
                continue;
            }
            double radiusPx = Math.max(MIN_BODY_PICK, OrbitCamera.focal(viewH) * b.radius / s[2]);
            if (Math.hypot(s[0] - mx, s[1] - my) <= radiusPx && s[2] < bestDepth) {
                best = f;
                bestDepth = s[2];
            }
        }
        return best;
    }

    /** The editor, for {@link Map3DScript}. */
    ManeuverEditor editor() {
        return editor;
    }

    /** The screen position (UI units) of the ship's orbit {@code seconds} from now, or null: for {@link Map3DScript}. */
    double @Nullable [] orbitPointAhead(double seconds) {
        if (plot == null) {
            return null;
        }
        StateVector st = plot.patches.get(plot.current).stateAt(nowUt + seconds);
        return screenOf(OrbitPlot.rootPosition(plot.shipBody(), nowUt).add(st.r()));
    }

    /** The screen position of body 0 Verdance or 1 Pale, or null: for {@link Map3DScript}. */
    double @Nullable [] bodyScreen(int which) {
        if (plot == null) {
            return null;
        }
        return screenOf(OrbitPlot.rootPosition(which == 0 ? MeridianSystem.VERDANCE : MeridianSystem.PALE, nowUt));
    }

    /** How far the followed point is from the middle of the view, UI units: zero once a focus change has settled. */
    double centringErrorPx() {
        double[] s = plot == null ? null : screenOf(focusTarget(follow));
        return s == null ? Double.NaN : Math.hypot(s[0] - viewW / 2.0, s[1] - viewH / 2.0);
    }

    /** The followed focus's name: for {@link Map3DScript}. */
    String followName() {
        return follow.name();
    }

    /** Focuses 0 Verdance, 1 Pale or 2 the ship, at distance {@code metres} from it: for {@link Map3DScript}. */
    void focusFor(int which, double metres) {
        setFollow(Follow.values()[which]);
        targetDistance = metres;
        clampTarget();
    }

    /** The wheel zooms, exponentially: the target distance moves by a fixed factor per tick. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        targetDistance *= Math.exp(-scrollY * OrbitCamera.ZOOM_PER_TICK);
        clampTarget();
        return true;
    }
}
