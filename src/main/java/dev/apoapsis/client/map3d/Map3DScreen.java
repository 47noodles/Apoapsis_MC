package dev.apoapsis.client.map3d;

import static dev.apoapsis.client.map.Palette.BRASS;
import static dev.apoapsis.client.map.Palette.COPPER;
import static dev.apoapsis.client.map.Palette.INK;
import static dev.apoapsis.client.map.Palette.INK_MUTED;
import static dev.apoapsis.client.map.Palette.SIGNAL;
import static dev.apoapsis.client.map.Palette.SIGNAL_EDGE;
import static dev.apoapsis.client.map.Palette.STEEL_500;
import static dev.apoapsis.client.map.Palette.STEEL_700;
import static dev.apoapsis.client.map.Palette.STEEL_950;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.MapClient;
import dev.apoapsis.client.OverlayMessages;
import dev.apoapsis.client.map.Fmt;
import dev.apoapsis.client.map.Labels;
import dev.apoapsis.client.map.Palette;
import dev.apoapsis.client.map.TransferMath;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.sky.PlanetTextures;
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
import java.util.Locale;
import java.util.Random;
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
 * with the wheel. The scene is drawn by {@link Scene3D} through a picture-in-picture ({@link Map3DPip}); labels and the
 * 2D overlays (readout, keys, warp, overlay messages, transfer card) are drawn after it as crisp text. The map key opens
 * it by default (see {@link dev.apoapsis.client.MapClient}; Tab flips to the 2D map). It also holds the maneuver node
 * ({@link ManeuverEditor}) and the site markers ({@link SiteMarkers}), and a click on a body or the ship focuses it.
 * Does not pause the game. Client only.
 */
public final class Map3DScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    private static final int STAR_COUNT = 160;
    private static final long STAR_SEED = 0x41504F41L;
    /** The view's distance on opening, and on Verdance and Pale: enough to show Pale's orbit. */
    private static final double SYSTEM_DISTANCE_M = 4 * MeridianSystem.PALE.orbitRadius;
    private static final double MAX_DISTANCE_M = 1e8;
    private static final double MIN_DISTANCE_M = 2000;
    /** Time constants, s, of the jump a focus change or a new prediction leaves in the view, and of the zoom. */
    private static final double FOCUS_TAU_S = 0.12;
    private static final double DISTANCE_TAU_S = 0.1;
    /** A press that moves less than this is a click, px. */
    private static final double CLICK_SLOP_PX = 4;
    /** How close a click must be to the ship's mark to focus it, px, and the smallest disc a click on a body hits. */
    private static final double SHIP_PICK_PX = 9;
    private static final double MIN_BODY_PICK_PX = 6;
    private static final double MARGIN_PX = 24;
    private static final int LABEL_H = 9;
    private static final int LINE_STEP = 12;
    /** The controls are a column of keycaps, bottom left: its width, rows and row step, px. */
    private static final int HINT_W = 150;
    private static final int HINT_ROWS = 8;
    private static final int HINT_STEP = 17;
    /** Half the side of a mark's square, px. */
    private static final int MARK_HALF_PX = 2;

    private static final int INK_RGB = Palette.INK & 0xFFFFFF;

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

    /** What the view orbits. */
    enum Follow { VERDANCE, PALE, SHIP }

    private final OrbitCamera camera = new OrbitCamera(SYSTEM_DISTANCE_M);
    private final Scene3D scene = new Scene3D();
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
    private int viewW = 1;
    private int viewH = 1;

    /** Opened by {@link dev.apoapsis.client.MapClient#openMap()} when the 3D map is on. */
    public Map3DScreen() {
        super(Component.translatable("screen.apoapsis.map"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Full-bleed steel with the fixed star field, as the 2D map. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        graphics.fill(0, 0, w, h, STEEL_950);
        for (int i = 0; i < STAR_COUNT; i++) {
            int x = (int) (STAR_X[i] * (w - 1));
            int y = (int) (STAR_Y[i] * (h - 1));
            graphics.fill(x, y, x + 1, y + 1, (STAR_ALPHA[i] << 24) | INK_RGB);
        }
        Minecraft.getInstance().gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        frame++;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        viewW = w;
        viewH = h;
        Labels labels = new Labels();
        int bottomY = h - 22;
        // The bottom chrome claims its boxes first so no label lands on it, and is drawn last.
        labels.reserve(8, bottomY - (HINT_ROWS - 1) * HINT_STEP - 6, 8 + HINT_W, bottomY + LABEL_H + 6);
        labels.reserve(w / 2 - 120, bottomY - 6, w / 2 + 120, bottomY + LABEL_H + 6);
        Component overlay = OverlayMessages.current(Minecraft.getInstance());
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            Component msg = Component.translatable("apoapsis.map.board");
            graphics.text(font(), msg, (w - font().width(msg)) / 2, h / 2 - 4, INK_MUTED, false);
            drawBottom(graphics, overlay, bottomY, w, null);
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

        editor.begin(scene, plot, nowUt, orbit, this::screenOf, OrbitCamera.focal(h), shipBurn(), mouseX, mouseY);
        for (int k = 0; k < 2; k++) {
            mapIds[k] = PlanetTextures.texture(k == 0 ? MeridianSystem.VERDANCE.id : MeridianSystem.PALE.id);
            textured[k] = mapIds[k] != null;
        }
        scene.build(camera, plot, editor.predicted(), nowUt, w, h, focus, textured);
        // The textured bodies are a layer of their own, under the flat scene (haze, orbits, ship).
        if (scene.texCount > 0) {
            graphics.submitPictureInPictureRenderState(new Map3DPip.BodiesState(0, 0, w, h, scene, mapIds));
        }
        graphics.submitPictureInPictureRenderState(new Map3DPip.State(0, 0, w, h, scene));

        drawTopLeft(graphics, labels, p, body, orbit);
        drawTime(graphics, labels, w);
        // The cards claim their boxes first, so no label lands under one. The node's sits lowest; the transfer's above.
        ManeuverEditor.Box nodeBox = editor.cardBox(font(), w, h, 0);
        int lift = nodeBox == null ? 0 : nodeBox.y1() - nodeBox.y0() + 6;
        Card card = transferCard(p, body, orbit, lift);
        if (nodeBox != null) {
            labels.reserve(nodeBox.x0() - 2, nodeBox.y0() - 2, nodeBox.x1() + 2, nodeBox.y1() + 2);
        }
        if (card != null) {
            labels.reserve(card.x0() - 2, card.y0() - 2, card.x1() + 2, card.y1() + 2);
        }
        editor.draw(graphics, font(), labels, w, h);
        drawBodyLabels(graphics, labels);
        drawMarks(graphics, labels);
        SiteMarkers.draw(graphics, font(), labels, w, h, nowUt, camera, focus, this::screenOf);
        drawBottom(graphics, overlay, bottomY, w, p);
        if (nodeBox != null) {
            editor.drawCard(graphics, font(), nodeBox);
        }
        if (card != null) {
            drawCard(graphics, card);
        }
    }

    /** What the piloted ship can do, for the burn time; null when the player rides no ship. */
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

    /** Projects a root-frame point to the view: {x, y, depth}, or null when it is behind the eye. */
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

    /** A label beside a point, the body's name centred above its disc. */
    private void drawBodyLabels(GuiGraphicsExtractor g, Labels labels) {
        drawBodyLabel(g, labels, MeridianSystem.VERDANCE, Component.translatable("apoapsis.map.body.verdance"));
        drawBodyLabel(g, labels, MeridianSystem.PALE, Component.translatable("apoapsis.map.body.pale"));
        double[] s = screenOf(plot.shipRootAt(nowUt));
        if (s != null) {
            label(g, labels, Component.translatable("apoapsis.map3d.ship"), (int) Math.round(s[0]) + 7,
                    (int) Math.round(s[1]) - 4, SIGNAL);
        }
    }

    private void drawBodyLabel(GuiGraphicsExtractor g, Labels labels, CelestialBody body, Component name) {
        Vec3d centre = body == MeridianSystem.VERDANCE ? Vec3d.ZERO : OrbitPlot.rootPosition(body, nowUt);
        double[] s = screenOf(centre);
        if (s == null) {
            return;
        }
        double radiusPx = OrbitCamera.focal(viewH) * body.radius / s[2];
        int x = (int) Math.round(s[0] - font().width(name) / 2.0);
        int y = (int) Math.round(s[1] - Math.max(2, radiusPx) - LABEL_H - 6);
        label(g, labels, name, x, y, INK);
    }

    /** The predicted marks and the nodes, each with a small marker and its label, after the time passed. */
    private void drawMarks(GuiGraphicsExtractor g, Labels labels) {
        CelestialBody coreBody = plot.patches.get(plot.current).body();
        for (OrbitPlot.Mark m : plot.marks) {
            if (m.ut() < nowUt) {
                continue;
            }
            double[] s = screenOf(m.at());
            if (s == null || s[0] < -MARGIN_PX || s[1] < -MARGIN_PX || s[0] > viewW + MARGIN_PX
                    || s[1] > viewH + MARGIN_PX) {
                continue;
            }
            int x = (int) Math.round(s[0]);
            int y = (int) Math.round(s[1]);
            switch (m.kind()) {
                case APOAPSIS, PERIAPSIS -> {
                    double alt = m.at().sub(OrbitPlot.rootPosition(coreBody, m.ut())).length() - coreBody.radius;
                    String key = m.kind() == OrbitPlot.Kind.APOAPSIS ? "apoapsis.map.mark.ap" : "apoapsis.map.mark.pe";
                    drawMarker(g, x, y, INK);
                    label(g, labels, Component.translatable(key, distance(alt)), x + 7, y - 4, INK_MUTED);
                }
                case ENCOUNTER -> {
                    double[] ghost = screenOf(OrbitPlot.rootPosition(m.body(), m.ut()));
                    if (ghost != null) {
                        brackets(g, ghost[0], ghost[1], Math.max(3, OrbitCamera.focal(viewH) * m.body().radius / ghost[2]) + 4);
                    }
                    drawMarker(g, x, y, INK);
                    Component title = Component.translatable("apoapsis.map.mark.encounter",
                            spaced(Component.translatable("apoapsis.map.body." + m.body().id).getString()));
                    Component countdown = Component.translatable("apoapsis.map.mark.countdown",
                            clock(m.ut() - nowUt));
                    label(g, labels, title, x + 7, y - 4, INK);
                    label(g, labels, countdown, x + 7, y + LABEL_H + 2, INK_MUTED);
                }
                case ESCAPE -> {
                    drawMarker(g, x, y, INK);
                    label(g, labels, Component.translatable("apoapsis.map.mark.escape",
                            Component.translatable("apoapsis.map.body." + coreBody.id), clock(m.ut() - nowUt)),
                            x + 7, y - 4, INK_MUTED);
                }
                case ATMOSPHERE, IMPACT -> {
                    drawMarker(g, x, y, COPPER);
                    String key = m.kind() == OrbitPlot.Kind.ATMOSPHERE
                            ? "apoapsis.map.mark.atmosphere" : "apoapsis.map.mark.impact";
                    label(g, labels, Component.translatable(key, clock(m.ut() - nowUt)), x + 7, y - 4, COPPER);
                }
                case ASCENDING_NODE, DESCENDING_NODE -> {
                    drawMarker(g, x, y, BRASS);
                    String key = m.kind() == OrbitPlot.Kind.ASCENDING_NODE ? "apoapsis.map3d.node.an"
                            : "apoapsis.map3d.node.dn";
                    label(g, labels, Component.translatable(key), x + 7, y - 4, BRASS);
                }
                default -> {
                }
            }
        }
    }

    private void drawMarker(GuiGraphicsExtractor g, double x, double y, int argb) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);
        g.fill(cx - MARK_HALF_PX, cy - MARK_HALF_PX, cx + MARK_HALF_PX + 1, cy + MARK_HALF_PX + 1, argb);
    }

    /** Four corner brackets round a ghost of a body, {@code half} px from its centre. */
    private void brackets(GuiGraphicsExtractor g, double x, double y, double half) {
        int cx = (int) Math.round(x), cy = (int) Math.round(y), hf = (int) Math.round(half);
        int len = 4;
        for (int sx : new int[] {-1, 1}) {
            for (int sy : new int[] {-1, 1}) {
                int ex = cx + sx * hf, ey = cy + sy * hf;
                g.fill(Math.min(ex, ex - sx * len), ey, Math.max(ex, ex - sx * len) + 1, ey + 1, INK);
                g.fill(ex, Math.min(ey, ey - sy * len), ex + 1, Math.max(ey, ey - sy * len) + 1, INK);
            }
        }
    }

    /** A label at (x, y) when it is on screen and clear of every label already placed. */
    private boolean label(GuiGraphicsExtractor g, Labels labels, Component text, int x, int y, int argb) {
        return label(g, font(), labels, viewW, viewH, text, x, y, argb);
    }

    /** As {@link #label(GuiGraphicsExtractor, Labels, Component, int, int, int)}, for the overlays outside this screen. */
    static boolean label(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Labels labels, int viewW, int viewH,
            Component text, int x, int y, int argb) {
        int w = font.width(text);
        if (x + w < 0 || y + LABEL_H < 0 || x > viewW || y > viewH) {
            return false;
        }
        if (!labels.claim(x - 1, y - 1, x + w + 1, y + LABEL_H + 1)) {
            return false;
        }
        g.text(font, text, x, y, argb, false);
        return true;
    }

    /**
     * Top left, no box round the figures: the breadcrumb in a frame, then altitude and speed, the apsides of an
     * orbit that stays in its sphere of influence (else periapsis), an escape countdown, and the relative inclination
     * when the ship orbits Verdance.
     */
    private void drawTopLeft(GuiGraphicsExtractor g, Labels labels, OrbitStatePayload p, CelestialBody body,
            boolean orbit) {
        int x = 16;
        int y = 14;
        Component crumb = Component.translatable("apoapsis.map.crumb",
                spaced(Component.translatable("apoapsis.map.system").getString()),
                spaced(Component.translatable("apoapsis.map.body." + body.id).getString()));
        int crumbW = font().width(crumb);
        frame(g, x, y, x + crumbW + 14, y + LABEL_H + 8, STEEL_500);
        g.text(font(), crumb, x + 7, y + 4, INK, false);
        int blockRight = x + crumbW + 14;

        StateVector s = plot.shipStateAt(nowUt);
        double r = s.r().length();
        double speed = s.v().length();
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("apoapsis.map.top.alt", distance(r - body.radius)));
        lines.add(Component.translatable("apoapsis.map.top.speed", speedText(speed)));
        if (orbit) {
            Elements el = Elements.of(body.mu, s);
            if (el.eccentricity() < 1 && el.apoapsisRadius() < body.soiRadius()) {
                lines.add(Component.translatable("apoapsis.map.top.apsides",
                        distance(el.apoapsisRadius() - body.radius), distance(el.periapsisRadius() - body.radius)));
                lines.add(Component.translatable("apoapsis.map.top.ap_in", clock(el.timeToApoapsis())));
            } else {
                lines.add(Component.translatable("apoapsis.map.top.periapsis",
                        distance(Elements.of(body.mu, s).periapsisRadius() - body.radius)));
            }
            if (plot.shipBody() == MeridianSystem.VERDANCE) {
                double deg = OrbitPlot.relativeInclinationDeg(s);
                lines.add(Component.translatable("apoapsis.map3d.rel_inc", String.format(Locale.ROOT, "%.1f°", deg)));
            }
        }
        Patch cur = plot.patches.get(plot.current);
        if (cur.end() == Patch.EndReason.ESCAPE) {
            lines.add(Component.translatable("apoapsis.map.top.escape", clock(cur.endUt() - nowUt)));
        }
        int lineY = y + LABEL_H + 8 + 12;
        for (Component line : lines) {
            g.text(font(), line, x, lineY, INK, false);
            blockRight = Math.max(blockRight, x + font().width(line));
            lineY += LINE_STEP;
        }
        labels.reserve(x - 2, y - 2, blockRight + 2, lineY);
    }

    /** Top right: the universal time in a frame. */
    private void drawTime(GuiGraphicsExtractor g, Labels labels, int w) {
        Component time = Component.translatable("apoapsis.map.time", clock(nowUt));
        int tw = font().width(time);
        int x1 = w - 16;
        int x0 = x1 - tw - 14;
        int y = 14;
        frame(g, x0, y, x1, y + LABEL_H + 8, STEEL_500);
        g.text(font(), time, x0 + 7, y + 4, INK, false);
        labels.reserve(x0 - 2, y - 2, x1 + 2, y + LABEL_H + 10);
    }

    /** The transfer card's lines and box, bottom right. */
    private record Card(Component title, Component timer, Component burn, int x0, int y0, int x1, int y1) {}

    /**
     * The transfer card, on a predicted encounter: "TRANSFER · VERDANCE TO PALE", the countdown, and the burn's delta-v
     * for a Hohmann transfer to Pale's orbit. Null when there is no encounter or the ship is not orbiting Verdance.
     */
    private @Nullable Card transferCard(OrbitStatePayload p, CelestialBody body, boolean orbit, int lift) {
        if (body != MeridianSystem.VERDANCE || !orbit) {
            return null;
        }
        Double encounter = null;
        for (OrbitPlot.Mark m : plot.marks) {
            if (m.kind() == OrbitPlot.Kind.ENCOUNTER && m.ut() >= nowUt) {
                encounter = m.ut();
                break;
            }
        }
        if (encounter == null) {
            return null;
        }
        Component timer = Component.translatable("apoapsis.map.card.encounter", clock(encounter - nowUt));
        StateVector s = plot.shipStateAt(nowUt);
        double dv = TransferMath.hohmannDeltaV(body.mu, s.r().length(), s.v().length(), MeridianSystem.PALE.orbitRadius);
        Component burn = Double.isFinite(dv) ? Component.translatable("apoapsis.map.card.burn", speedText(dv)) : null;
        Component title = Component.translatable("apoapsis.map.card.title",
                Component.translatable("apoapsis.map.body.verdance"), Component.translatable("apoapsis.map.body.pale"));
        int w = Math.max(font().width(title), font().width(timer));
        if (burn != null) {
            w = Math.max(w, font().width(burn));
        }
        int x1 = viewW - 16;
        int y1 = viewH - 16 - lift;
        int h = 3 * LINE_STEP + 10;
        return new Card(title, timer, burn, x1 - w - 20, y1 - h, x1, y1);
    }

    private void drawCard(GuiGraphicsExtractor g, Card card) {
        g.fill(card.x0(), card.y0(), card.x1(), card.y1(), (0xC0 << 24) | (STEEL_950 & 0xFFFFFF));
        frame(g, card.x0(), card.y0(), card.x1() - 1, card.y1() - 1, STEEL_500);
        int tx = card.x0() + 10;
        int ty = card.y0() + 6;
        g.text(font(), card.title(), tx, ty, INK_MUTED, false);
        g.text(font(), card.timer(), tx, ty + LINE_STEP, SIGNAL, false);
        if (card.burn() != null) {
            g.text(font(), card.burn(), tx, ty + 2 * LINE_STEP, INK, false);
        }
    }

    /**
     * Bottom: the controls as a column of keycaps, bottom left, the last row on {@code y}; the warp strip bottom centre
     * when piloting; an overlay message in the bottom keycap row's place.
     */
    private void drawBottom(GuiGraphicsExtractor g, @Nullable Component overlay, int y, int w,
            @Nullable OrbitStatePayload p) {
        if (overlay != null) {
            g.text(font(), overlay, 16, y, INK, false);
        } else {
            int row = y - (HINT_ROWS - 1) * HINT_STEP;
            hint(g, 16, row, Component.translatable("apoapsis.map.key.zoom"), Component.literal("SCROLL"));
            hint(g, 16, row += HINT_STEP, Component.translatable("apoapsis.map3d.key.rotate"), Component.literal("DRAG"));
            hint(g, 16, row += HINT_STEP, Component.translatable("apoapsis.map.key.focus"),
                    Component.literal("1"), Component.literal("2"), Component.literal("3"));
            hint(g, 16, row += HINT_STEP, Component.translatable("apoapsis.map3d.key.node"), Component.literal("CLICK"));
            hint(g, 16, row += HINT_STEP, Component.translatable("apoapsis.map3d.key.warp"),
                    PilotKeys.WARP_UP.getTranslatedKeyMessage(), PilotKeys.WARP_DOWN.getTranslatedKeyMessage());
            hint(g, 16, row += HINT_STEP, Component.translatable("apoapsis.map3d.key.cut"),
                    PilotKeys.THROTTLE_CUT.getTranslatedKeyMessage());
            hint(g, 16, row += HINT_STEP, Component.translatable("apoapsis.map3d.key.map2d"), Component.literal("TAB"));
            hint(g, 16, y, Component.translatable("apoapsis.map.key.close"),
                    PilotKeys.SYSTEM_MAP.getTranslatedKeyMessage());
        }
        if (p != null) {
            drawWarp(g, warpText(p.warp()), y, w);
        }
    }

    /** Keycaps for one control, then what they do. Returns the x after it. */
    private int hint(GuiGraphicsExtractor g, int x, int y, Component what, Component... keys) {
        int at = x;
        for (Component key : keys) {
            at += keycap(g, at, y, key) + 3;
        }
        g.text(font(), what, at + 3, y + 2, INK_MUTED, false);
        return at + 3 + font().width(what) + 18;
    }

    /** One keycap: a small raised box with its key name. Returns its width. */
    private int keycap(GuiGraphicsExtractor g, int x, int y, Component key) {
        int kw = font().width(key) + 8;
        int h = LABEL_H + 5;
        g.fill(x, y - 2, x + kw, y - 2 + h, STEEL_700);
        frame(g, x, y - 2, x + kw, y - 2 + h, STEEL_500);
        g.text(font(), key, x + 4, y, INK, false);
        return kw;
    }

    /** The warp strip: "WARP" and the current step in a bracketed cell, centred on the bottom edge. */
    private void drawWarp(GuiGraphicsExtractor g, String step, int y, int w) {
        Component label = Component.translatable("apoapsis.map.warp");
        int labelW = font().width(label);
        int cellW = font().width(Component.literal(step)) + 12;
        int total = labelW + 10 + cellW;
        int x0 = w / 2 - total / 2;
        frame(g, x0 - 8, y - 6, x0 + total + 8, y + LABEL_H + 5, STEEL_500);
        g.text(font(), label, x0, y, INK_MUTED, false);
        int cx = x0 + labelW + 10;
        g.fill(cx, y - 3, cx + cellW, y + LABEL_H + 3, (0xA0 << 24) | (SIGNAL_EDGE & 0xFFFFFF));
        g.text(font(), Component.literal(step), cx + 6, y, SIGNAL, false);
    }

    /** A rectangle outline, one pixel wide, from (x0, y0) to (x1, y1). */
    static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1 + 1, y0 + 1, argb);
        g.fill(x0, y1, x1 + 1, y1 + 1, argb);
        g.fill(x0, y0, x0 + 1, y1 + 1, argb);
        g.fill(x1, y0, x1 + 1, y1 + 1, argb);
    }

    private net.minecraft.client.gui.Font font() {
        return Minecraft.getInstance().font;
    }

    private static Component spaced(String s) {
        return Component.literal(Fmt.spaced(s));
    }

    private static Component distance(double m) {
        return Component.literal(Fmt.distance(m));
    }

    private static Component clock(double seconds) {
        return Component.literal(Fmt3d.clock(seconds));
    }

    private static String speedText(double ms) {
        return String.format(Locale.ROOT, "%.1f m/s", ms);
    }

    private static String warpText(float w) {
        return Fmt.warp(w);
    }

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

    /**
     * A press. The maneuver editor goes first (a handle, the node, the orbit line); else a left press on the ship or a
     * body is remembered, and focuses it on release if the pointer did not move.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        pressX = event.x();
        pressY = event.y();
        pressMoved = false;
        pressTarget = null;
        if (editor.mousePressed(event.x(), event.y(), event.button())) {
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && plot != null) {
            pressTarget = targetAt(event.x(), event.y());
        }
        return true;
    }

    /** A drag: the editor's (a handle or the node), else with the left button the view turns, free of any pole. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (editor.mouseDragged(dx, dy, event.hasShiftDown())) {
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (Math.hypot(event.x() - pressX, event.y() - pressY) > CLICK_SLOP_PX) {
            pressMoved = true;
        }
        camera.drag(dx, dy);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean took = editor.mouseReleased(event.x(), event.y(), event.button());
        if (!took && event.button() == InputConstants.MOUSE_BUTTON_LEFT && !pressMoved && pressTarget != null) {
            setFollow(pressTarget);
        }
        pressTarget = null;
        return true;
    }

    /** The ship's mark or the nearest body's disc under the pointer, or null. The ship wins; of two bodies the nearer. */
    private @Nullable Follow targetAt(double mx, double my) {
        double[] ship = screenOf(plot.shipRootAt(nowUt));
        if (ship != null && Math.hypot(ship[0] - mx, ship[1] - my) <= SHIP_PICK_PX) {
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
            double radiusPx = Math.max(MIN_BODY_PICK_PX, OrbitCamera.focal(viewH) * b.radius / s[2]);
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

    /** The screen position of the ship's orbit {@code seconds} from now, or null: for {@link Map3DScript}. */
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

    /** How far the followed point is from the middle of the view, px: zero once a focus change has settled. */
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
