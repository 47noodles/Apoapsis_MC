package dev.apoapsis.client.map3d;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.client.OverlayMessages;
import dev.apoapsis.client.ship.PilotKeys;
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
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The 3D system map: a view that orbits a focus (1 Verdance, 2 Pale, 3 the piloted ship) with free rotation, and zooms
 * with the wheel. The scene is drawn by {@link Scene3D} through a picture-in-picture ({@link Map3DPip}); labels and the
 * 2D overlays (readout, keys, warp, overlay messages, transfer card) are drawn after it as crisp text. Opened by the
 * {@code apoapsis.map3d} system property (see {@link dev.apoapsis.client.MapClient}). Does not pause the game. Client only.
 */
public final class Map3DScreen extends Screen {
    private static final int RECOMPUTE_FRAMES = 10;
    private static final int STAR_COUNT = 160;
    private static final long STAR_SEED = 0x41504F41L;
    /** The view's distance on opening, and on Verdance and Pale: enough to show Pale's orbit. */
    private static final double SYSTEM_DISTANCE_M = 4 * MeridianSystem.PALE.orbitRadius;
    private static final double MAX_DISTANCE_M = 1e8;
    private static final double MIN_DISTANCE_M = 2000;
    /** Share of the gap closed each frame by the focus and by the distance: smooth changes. */
    private static final double FOCUS_EASE = 0.2;
    private static final double DISTANCE_EASE = 0.15;
    private static final double MARGIN_PX = 24;
    private static final int LABEL_H = 9;
    private static final int LINE_STEP = 12;
    private static final int KEYCAP_ROW_W = 560;
    /** Half the side of a mark's square, px. */
    private static final int MARK_HALF_PX = 2;

    private static final int STEEL_950 = 0xFF0D1013;
    private static final int STEEL_700 = 0xFF2A323B;
    private static final int STEEL_500 = 0xFF56616D;
    private static final int INK = 0xFFE8E2D4;
    private static final int INK_MUTED = 0xFFA39C8E;
    private static final int BRASS = 0xFFC9A25A;
    private static final int COPPER = 0xFFC46F3E;
    private static final int SIGNAL = 0xFF3AD1C1;
    private static final int SIGNAL_EDGE = 0xFF1F4F4A;
    private static final int INK_RGB = 0xE8E2D4;

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
    /** The focus in the root frame, eased toward its target. */
    private Vec3d focus = Vec3d.ZERO;
    private boolean snapFocus = true;
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
        labels.reserve(8, bottomY - 6, 8 + KEYCAP_ROW_W, bottomY + LABEL_H + 6);
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
            plot = OrbitPlot.compute(body, stateOf(p), p.ut(), nowUt);
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

        scene.build(camera, plot, nowUt, w, h, focus);
        graphics.submitPictureInPictureRenderState(new Map3DPip.State(0, 0, w, h, scene));

        drawTopLeft(graphics, labels, p, body, orbit);
        drawTime(graphics, labels, w);
        drawBodyLabels(graphics, labels);
        drawMarks(graphics, labels);
        Card card = transferCard(p, body, orbit);
        if (card != null) {
            labels.reserve(card.x0() - 2, card.y0() - 2, card.x1() + 2, card.y1() + 2);
        }
        drawBottom(graphics, overlay, bottomY, w, p);
        if (card != null) {
            drawCard(graphics, card);
        }
    }

    /** Eases the focus toward the followed point, and the distance toward its target in log space. */
    private void easeView() {
        Vec3d goal = focusTarget(follow);
        if (snapFocus) {
            focus = goal;
            snapFocus = false;
        } else {
            focus = focus.add(goal.sub(focus).scale(FOCUS_EASE));
        }
        double d = camera.distance();
        double logD = Math.log(d) + (Math.log(targetDistance) - Math.log(d)) * DISTANCE_EASE;
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
        int w = font().width(text);
        if (x + w < 0 || y + LABEL_H < 0 || x > viewW || y > viewH) {
            return false;
        }
        if (!labels.claim(x - 1, y - 1, x + w + 1, y + LABEL_H + 1)) {
            return false;
        }
        g.text(font(), text, x, y, argb, false);
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
    private @Nullable Card transferCard(OrbitStatePayload p, CelestialBody body, boolean orbit) {
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
        double dv = hohmannDeltaV(body.mu, s.r().length(), s.v().length(), MeridianSystem.PALE.orbitRadius);
        Component burn = Double.isFinite(dv) ? Component.translatable("apoapsis.map.card.burn", speedText(dv)) : null;
        Component title = Component.translatable("apoapsis.map.card.title",
                Component.translatable("apoapsis.map.body.verdance"), Component.translatable("apoapsis.map.body.pale"));
        int w = Math.max(font().width(title), font().width(timer));
        if (burn != null) {
            w = Math.max(w, font().width(burn));
        }
        int x1 = viewW - 16;
        int y1 = viewH - 16;
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

    /** The Hohmann delta-v from radius r1 at speed v1 to a circular orbit of radius r2; NaN for an unbound orbit. */
    static double hohmannDeltaV(double mu, double r1, double v1, double r2) {
        if (!(mu > 0 && r1 > 0 && r2 > 0 && v1 >= 0) || !Double.isFinite(v1)) {
            return Double.NaN;
        }
        double a = (r1 + r2) / 2;
        double transferAtR1 = Math.sqrt(mu * (2 / r1 - 1 / a));
        double transferAtR2 = Math.sqrt(mu * (2 / r2 - 1 / a));
        double circularAtR2 = Math.sqrt(mu / r2);
        return Math.abs(transferAtR1 - v1) + Math.abs(circularAtR2 - transferAtR2);
    }

    /**
     * Bottom: the controls as keycaps, bottom left; the warp strip bottom centre when piloting; an overlay message in
     * the keycaps' place.
     */
    private void drawBottom(GuiGraphicsExtractor g, @Nullable Component overlay, int y, int w,
            @Nullable OrbitStatePayload p) {
        if (overlay != null) {
            g.text(font(), overlay, 16, y, INK, false);
        } else {
            int x = 16;
            x = hint(g, x, y, Component.translatable("apoapsis.map.key.zoom"), Component.literal("SCROLL"));
            x = hint(g, x, y, Component.translatable("apoapsis.map3d.key.rotate"), Component.literal("DRAG"));
            x = hint(g, x, y, Component.translatable("apoapsis.map.key.focus"),
                    Component.literal("1"), Component.literal("2"), Component.literal("3"));
            x = hint(g, x, y, Component.translatable("apoapsis.map.key.warp"),
                    PilotKeys.WARP_UP.getTranslatedKeyMessage(), PilotKeys.WARP_DOWN.getTranslatedKeyMessage());
            x = hint(g, x, y, Component.translatable("apoapsis.map3d.key.cut"),
                    PilotKeys.THROTTLE_CUT.getTranslatedKeyMessage());
            hint(g, x, y, Component.translatable("apoapsis.map.key.close"),
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
    private static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int argb) {
        g.fill(x0, y0, x1 + 1, y0 + 1, argb);
        g.fill(x0, y1, x1 + 1, y1 + 1, argb);
        g.fill(x0, y0, x0 + 1, y1 + 1, argb);
        g.fill(x1, y0, x1 + 1, y1 + 1, argb);
    }

    private net.minecraft.client.gui.Font font() {
        return Minecraft.getInstance().font;
    }

    private static Component spaced(String s) {
        return Component.literal(Fmt3d.spaced(s));
    }

    private static Component distance(double m) {
        return Component.literal(Fmt3d.distance(m));
    }

    private static Component clock(double seconds) {
        return Component.literal(Fmt3d.clock(seconds));
    }

    private static String speedText(double ms) {
        return String.format(Locale.ROOT, "%.1f m/s", ms);
    }

    private static String warpText(float w) {
        if (w == Math.rint(w)) {
            return "x" + (long) w;
        }
        return String.format(Locale.ROOT, "x%.1f", w);
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

    /** A drag with the left button turns the view about the screen's axes: free rotation, no pole. */
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        camera.drag(dx, dy);
        return true;
    }

    /** The wheel zooms, exponentially: the target distance moves by a fixed factor per tick. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        targetDistance *= Math.exp(-scrollY * OrbitCamera.ZOOM_PER_TICK);
        clampTarget();
        return true;
    }

    /** A label box claim: each frame starts empty, a label is drawn only when its box meets none already claimed. */
    static final class Labels {
        private final java.util.List<int[]> boxes = new java.util.ArrayList<>();

        boolean claim(int x0, int y0, int x1, int y1) {
            for (int[] b : boxes) {
                if (x0 < b[2] && b[0] < x1 && y0 < b[3] && b[1] < y1) {
                    return false;
                }
            }
            boxes.add(new int[] {x0, y0, x1, y1});
            return true;
        }

        void reserve(int x0, int y0, int x1, int y1) {
            boxes.add(new int[] {x0, y0, x1, y1});
        }
    }
}
