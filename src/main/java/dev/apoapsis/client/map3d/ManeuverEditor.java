package dev.apoapsis.client.map3d;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.map.Fmt;
import dev.apoapsis.client.map.Labels;
import dev.apoapsis.client.map.Palette;
import dev.apoapsis.client.map3d.ManeuverPlan.Handle;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.orbit.maneuver.ManeuverNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The 3D map's maneuver node: it places the node on the ship's orbit line (a click), slides it along the line (a drag
 * on the node), sets its delta-v by dragging one of six handles (prograde and retrograde, normal and anti-normal,
 * radial out and radial in), deletes it (right click or the Delete key), and draws the node, the handles, the marks of
 * the orbit after the burn and the readout card. The plan itself is {@link ManeuverPlan}; the line after the burn is an
 * {@link OrbitPlot} drawn by {@link Scene3D}. One node, shared by every opening of the map until it is passed, deleted,
 * or the pilot leaves the ship. Client only.
 */
public final class ManeuverEditor {
    /** Projects a root-frame point to the view: {x, y, depth}, or null when it is behind the eye. */
    interface Projector {
        double @Nullable [] screenOf(Vec3d root);
    }

    /** What the ship the player flies can do, for the burn time. */
    record ShipBurn(double massKg, double ispS, double thrustN, double deltaVAvailable) {}

    /** The card's box, GUI px. */
    record Box(int x0, int y0, int x1, int y1) {}

    private static final ManeuverPlan PLAN = new ManeuverPlan();

    private static final double LINE_PICK_PX = 7;
    private static final double SLIDE_PICK_PX = 60;
    private static final double NODE_PICK_PX = 10;
    private static final double HANDLE_PICK_PX = 9;
    private static final double CLICK_SLOP_PX = 4;
    private static final double HANDLE_FAR_PX = 46;
    private static final double HANDLE_PROBE_PX = 40;
    private static final double NODE_RING_PX = 7;
    private static final int LINE_STEP = 12;
    private static final int VIOLET = 0xFFB48CF0;
    private static final int MARK_HALF_PX = 2;

    private @Nullable Scene3D scene;
    private @Nullable OrbitPlot plot;
    private double nowUt;
    private boolean orbit;
    private double mouseX;
    private double mouseY;
    private boolean live;
    private @Nullable ShipBurn ship;

    private CelestialBody body = dev.apoapsis.orbit.MeridianSystem.VERDANCE;
    private StateVector atNode = new StateVector(Vec3d.ZERO, Vec3d.ZERO);
    private StateVector afterNode = atNode;
    private Vec3d nodeRoot = Vec3d.ZERO;
    private double[] nodeScreen = new double[3];
    private final double[][] handlePos = new double[ManeuverPlan.HANDLES.size()][2];
    private final double[][] handleDir = new double[ManeuverPlan.HANDLES.size()][2];
    private ManeuverPlan.@Nullable Apsides after;
    private ManeuverPlan.@Nullable Timing timing;

    private @Nullable OrbitPlot predicted;
    private @Nullable ManeuverNode predictedFor;
    private @Nullable OrbitPlot predictedFromPlot;

    private Projector projector = root -> null;
    private int hover = -1;
    private int dragHandle = -1;
    private boolean slideNode;
    private boolean pendingPlace;
    private double pendingUt = Double.NaN;
    private double pressX;
    private double pressY;

    /** Forgets the node: the pilot left the ship, logged out or changed level. */
    public static void forget() {
        PLAN.clear();
    }

    /** The line the burn would leave the ship on, or null when there is no node or it is not in view. */
    @Nullable OrbitPlot predicted() {
        return live ? predicted : null;
    }

    boolean live() {
        return live;
    }

    /**
     * Prepares one frame. {@code orbit} is whether the ship is in the orbit regime (a node needs one); {@code mouseX} and
     * {@code mouseY} are the pointer, for the hover.
     */
    void begin(Scene3D scene, OrbitPlot plot, double nowUt, boolean orbit, Projector projector, double focalPx,
            @Nullable ShipBurn ship, double mouseX, double mouseY) {
        this.scene = scene;
        this.plot = plot;
        this.nowUt = nowUt;
        this.orbit = orbit;
        this.projector = projector;
        this.ship = ship;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        live = false;
        hover = -1;
        ManeuverNode node = PLAN.node();
        if (node != null && node.ut() <= nowUt) {
            PLAN.clear();
            node = null;
        }
        if (node == null || !orbit) {
            predicted = null;
            predictedFor = null;
            return;
        }
        Patch cur = plot.patches.get(plot.current);
        if (Double.isFinite(cur.endUt()) && node.ut() > cur.endUt()) {
            return;
        }
        body = cur.body();
        atNode = cur.stateAt(node.ut());
        afterNode = ManeuverPlan.afterBurn(atNode, node);
        nodeRoot = OrbitPlot.rootPosition(body, nowUt).add(atNode.r());
        if (predictedFor == null || !predictedFor.equals(node) || predictedFromPlot != plot) {
            predicted = OrbitPlot.compute(body, afterNode, node.ut(), nowUt);
            predictedFor = node;
            predictedFromPlot = plot;
        }
        after = ManeuverPlan.apsides(body, afterNode);
        timing = ship == null ? null
                : ManeuverPlan.timing(node, nowUt, ship.massKg(), ship.ispS(), ship.thrustN());
        double[] s = projector.screenOf(nodeRoot);
        if (s == null) {
            return;
        }
        nodeScreen = s;
        live = true;
        double worldPerPx = s[2] / focalPx;
        for (int i = 0; i < ManeuverPlan.HANDLES.size(); i++) {
            Vec3d dir = ManeuverPlan.direction(atNode, ManeuverPlan.HANDLES.get(i));
            double[] p = projector.screenOf(nodeRoot.add(dir.scale(HANDLE_PROBE_PX * worldPerPx)));
            double ux = ManeuverPlan.HANDLES.get(i).sign();
            double uy = 0;
            double len = 0;
            if (p != null) {
                double vx = p[0] - s[0];
                double vy = p[1] - s[1];
                len = Math.hypot(vx, vy);
                if (len > 1.5) {
                    ux = vx / len;
                    uy = vy / len;
                }
            }
            // An axis seen end on is drawn short, so the handle shows which way the view looks along it.
            double far = HANDLE_FAR_PX * (0.55 + 0.45 * Math.min(1, len / HANDLE_PROBE_PX));
            handleDir[i][0] = ux;
            handleDir[i][1] = uy;
            handlePos[i][0] = s[0] + ux * far;
            handlePos[i][1] = s[1] + uy * far;
        }
        hover = dragHandle >= 0 ? dragHandle : handleAt(mouseX, mouseY);
    }

    private int handleAt(double mx, double my) {
        int best = -1;
        double bestD = HANDLE_PICK_PX;
        for (int i = 0; i < handlePos.length; i++) {
            double d = Math.hypot(handlePos[i][0] - mx, handlePos[i][1] - my);
            if (d <= bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private boolean nearNode(double mx, double my, double px) {
        return Math.hypot(nodeScreen[0] - mx, nodeScreen[1] - my) <= px;
    }

    /** The universal time for a hit on the current patch's line, or NaN when no future point matches. */
    private double utOf(double[] hit) {
        OrbitPlot p = plot;
        if (p == null) {
            return Double.NaN;
        }
        int c = p.current;
        double ut = p.t0[c] + (p.t1[c] - p.t0[c]) * hit[0] / OrbitPlot.SAMPLES;
        Patch cur = p.patches.get(c);
        double period = Elements.of(cur.body().mu, cur.startState()).period();
        double last = Double.isFinite(cur.endUt()) ? cur.endUt() : Double.POSITIVE_INFINITY;
        return ManeuverPlan.nextUt(ut, nowUt, period, last);
    }

    /**
     * A press. Left: on a handle starts its drag; on the node starts sliding it; on the orbit line arms a placement
     * that happens on release if the pointer did not move (so a drag from the line still turns the view). Right: on
     * the node or a handle deletes the node. Returns whether the editor took the press.
     */
    boolean mousePressed(double mx, double my, int button) {
        pressX = mx;
        pressY = my;
        mouseX = mx;
        mouseY = my;
        if (plot == null || !orbit || scene == null) {
            return false;
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (live && (nearNode(mx, my, NODE_PICK_PX * 1.6) || handleAt(mx, my) >= 0)) {
                PLAN.clear();
                reset();
                return true;
            }
            return false;
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (live) {
            int h = handleAt(mx, my);
            if (h >= 0) {
                dragHandle = h;
                return true;
            }
            if (nearNode(mx, my, NODE_PICK_PX)) {
                slideNode = true;
                return true;
            }
        }
        double[] hit = scene.pickCurrent(mx, my, LINE_PICK_PX);
        if (hit != null) {
            double ut = utOf(hit);
            if (Double.isFinite(ut)) {
                pendingPlace = true;
                pendingUt = ut;
                return true;
            }
        }
        return false;
    }

    /** A drag by (dx, dy) px. {@code fine} slows a handle drag tenfold. True when the editor took it. */
    boolean mouseDragged(double dx, double dy, boolean fine) {
        mouseX += dx;
        mouseY += dy;
        if (dragHandle >= 0 && PLAN.has()) {
            double[] u = handleDir[dragHandle];
            double outward = dx * u[0] + dy * u[1];
            double speed = atNode.v().length();
            PLAN.addDv(ManeuverPlan.HANDLES.get(dragHandle), outward * ManeuverPlan.dvPerPixel(speed, fine));
            return true;
        }
        if (slideNode && scene != null) {
            double[] hit = scene.pickCurrent(mouseX, mouseY, SLIDE_PICK_PX);
            if (hit != null) {
                double ut = utOf(hit);
                if (Double.isFinite(ut)) {
                    PLAN.moveTo(ut);
                }
            }
            return true;
        }
        if (pendingPlace) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) > CLICK_SLOP_PX) {
                pendingPlace = false;
            }
            return false;
        }
        return false;
    }

    /** A release. Completes an armed placement. True when the editor took the press this release ends. */
    boolean mouseReleased(double mx, double my, int button) {
        boolean took = dragHandle >= 0 || slideNode || pendingPlace;
        if (pendingPlace && Double.isFinite(pendingUt)) {
            if (PLAN.has()) {
                PLAN.moveTo(pendingUt);
            } else {
                PLAN.place(pendingUt);
            }
        }
        reset();
        return took;
    }

    /** The Delete key. True when there was a node to delete. */
    boolean deleteNode() {
        if (!PLAN.has()) {
            return false;
        }
        PLAN.clear();
        reset();
        return true;
    }

    private void reset() {
        dragHandle = -1;
        slideNode = false;
        pendingPlace = false;
        pendingUt = Double.NaN;
    }

    /** The node's screen position, or null when no node is shown; for the scripted test run. */
    double @Nullable [] nodeScreenForTest() {
        return live ? nodeScreen.clone() : null;
    }

    /** The screen position of handle {@code i}, or null when no node is shown; for the scripted test run. */
    double @Nullable [] handleScreenForTest(int i) {
        return live ? handlePos[i].clone() : null;
    }

    /** The screen direction (outward unit vector) of handle {@code i}; for the scripted test run. */
    double[] handleDirForTest(int i) {
        return handleDir[i].clone();
    }

    /** The node as set now, or null; for the scripted test run. */
    @Nullable ManeuverNode nodeForTest() {
        return PLAN.node();
    }

    // ---- drawing ----

    /** The node, its handles and the marks of the orbit after the burn. */
    void draw(GuiGraphicsExtractor g, Font font, Labels labels, int viewW, int viewH) {
        if (!live) {
            return;
        }
        drawPredictedMarks(g, font, labels, viewW, viewH);
        double nx = nodeScreen[0];
        double ny = nodeScreen[1];
        for (int i = 0; i < handlePos.length; i++) {
            int colour = axisColour(ManeuverPlan.HANDLES.get(i));
            boolean on = i == hover;
            double[] u = handleDir[i];
            line(g, nx + u[0] * (NODE_RING_PX + 1), ny + u[1] * (NODE_RING_PX + 1), handlePos[i][0], handlePos[i][1],
                    Palette.alpha(colour, on ? 0xFF : 0x80));
            drawHandle(g, handlePos[i][0], handlePos[i][1], colour, ManeuverPlan.HANDLES.get(i).sign() > 0, on);
        }
        ring(g, nx, ny, NODE_RING_PX, Palette.BRASS);
        g.fill((int) Math.round(nx) - 1, (int) Math.round(ny) - 1, (int) Math.round(nx) + 2, (int) Math.round(ny) + 2,
                Palette.BRASS);
        labels.reserve((int) nx - 10, (int) ny - 10, (int) nx + 10, (int) ny + 10);
        for (double[] h : handlePos) {
            labels.reserve((int) h[0] - 6, (int) h[1] - 6, (int) h[0] + 7, (int) h[1] + 7);
        }
        if (hover >= 0 && PLAN.node() != null) {
            ManeuverNode n = PLAN.node();
            Handle h = ManeuverPlan.HANDLES.get(hover);
            double value = switch (h.axis()) {
                case PROGRADE -> n.prograde();
                case NORMAL -> n.normal();
                case RADIAL -> n.radial();
            };
            Component text = Component.translatable("apoapsis.map3d.node.handle",
                    Component.translatable("apoapsis.map3d.axis." + h.axis().name().toLowerCase(Locale.ROOT)),
                    String.format(Locale.ROOT, "%+.1f m/s", value));
            int w = font.width(text);
            double[] u = handleDir[hover];
            int x = (int) Math.round(handlePos[hover][0] + (u[0] >= 0 ? 9 : -9 - w));
            int y = (int) Math.round(handlePos[hover][1] - 4);
            Map3DScreen.label(g, font, labels, viewW, viewH, text, x, y, Palette.INK);
        }
    }

    private void drawPredictedMarks(GuiGraphicsExtractor g, Font font, Labels labels, int viewW, int viewH) {
        OrbitPlot p = predicted;
        if (p == null || PLAN.node() == null || PLAN.node().deltaV() < 0.05) {
            return;
        }
        CelestialBody core = p.patches.get(p.current).body();
        for (OrbitPlot.Mark m : p.marks) {
            if (m.kind() != OrbitPlot.Kind.APOAPSIS && m.kind() != OrbitPlot.Kind.PERIAPSIS
                    && m.kind() != OrbitPlot.Kind.ENCOUNTER) {
                continue;
            }
            double[] s = projector.screenOf(m.at());
            if (s == null || s[0] < 0 || s[1] < 0 || s[0] > viewW || s[1] > viewH) {
                continue;
            }
            // An apsis the burn leaves in place sits on the node: the card has its altitude, and a label would sit on
            // the handles.
            if (Math.hypot(s[0] - nodeScreen[0], s[1] - nodeScreen[1]) < 2 * HANDLE_FAR_PX / 3) {
                continue;
            }
            int x = (int) Math.round(s[0]);
            int y = (int) Math.round(s[1]);
            g.fill(x - MARK_HALF_PX, y - MARK_HALF_PX, x + MARK_HALF_PX + 1, y + MARK_HALF_PX + 1, Palette.BRASS);
            Component text;
            if (m.kind() == OrbitPlot.Kind.ENCOUNTER) {
                text = Component.translatable("apoapsis.map.mark.encounter",
                        Fmt.spaced(Component.translatable("apoapsis.map.body." + m.body().id).getString()));
            } else {
                double alt = m.at().sub(OrbitPlot.rootPosition(core, m.ut())).length() - core.radius;
                String key = m.kind() == OrbitPlot.Kind.APOAPSIS ? "apoapsis.map.mark.ap" : "apoapsis.map.mark.pe";
                text = Component.translatable(key, Fmt.distance(alt));
            }
            Map3DScreen.label(g, font, labels, viewW, viewH, text, x + 7, y - 4, Palette.BRASS);
        }
    }

    private static int axisColour(Handle h) {
        return switch (h.axis()) {
            case PROGRADE -> Palette.GO;
            case NORMAL -> VIOLET;
            case RADIAL -> Palette.HYDROGEN;
        };
    }

    private static void drawHandle(GuiGraphicsExtractor g, double cx, double cy, int colour, boolean solid, boolean on) {
        int x = (int) Math.round(cx);
        int y = (int) Math.round(cy);
        int h = on ? 5 : 4;
        if (solid) {
            g.fill(x - h, y - h, x + h + 1, y + h + 1, colour);
        } else {
            Map3DScreen.frame(g, x - h, y - h, x + h, y + h, colour);
            g.fill(x - h + 1, y - h + 1, x + h, y + h, Palette.alpha(Palette.STEEL_950, 0xC0));
        }
        if (on) {
            Map3DScreen.frame(g, x - h - 2, y - h - 2, x + h + 2, y + h + 2, Palette.INK);
        }
    }

    private static void ring(GuiGraphicsExtractor g, double cx, double cy, double r, int argb) {
        int n = 40;
        for (int k = 0; k < n; k++) {
            double a = 2 * Math.PI * k / n;
            int x = (int) Math.round(cx + r * Math.cos(a));
            int y = (int) Math.round(cy + r * Math.sin(a));
            g.fill(x, y, x + 1, y + 1, argb);
        }
    }

    private static void line(GuiGraphicsExtractor g, double x0, double y0, double x1, double y1, int argb) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        int steps = (int) Math.max(1, Math.round(Math.max(Math.abs(dx), Math.abs(dy))));
        for (int i = 0; i <= steps; i++) {
            int x = (int) Math.round(x0 + dx * i / steps);
            int y = (int) Math.round(y0 + dy * i / steps);
            g.fill(x, y, x + 1, y + 1, argb);
        }
    }

    // ---- the readout card ----

    private record Line(Component text, int argb) {}

    private List<Line> cardLines() {
        ManeuverNode n = PLAN.node();
        List<Line> lines = new ArrayList<>();
        if (n == null || !live) {
            return lines;
        }
        lines.add(new Line(Component.translatable("apoapsis.map3d.node.title", Fmt3d.clock(n.ut() - nowUt)),
                Palette.INK_MUTED));
        boolean over = ship != null && n.deltaV() > ship.deltaVAvailable() + 0.05;
        Component dv = ship == null
                ? Component.translatable("apoapsis.map3d.node.dv", speed(n.deltaV()))
                : Component.translatable("apoapsis.map3d.node.dv_of", speed(n.deltaV()), speed(ship.deltaVAvailable()));
        lines.add(new Line(dv, over ? Palette.ALERT : Palette.BRASS));
        lines.add(new Line(Component.translatable("apoapsis.map3d.node.parts", signed(n.prograde()),
                signed(n.normal()), signed(n.radial())), Palette.INK_MUTED));
        ManeuverPlan.Timing t = timing;
        if (t == null) {
            lines.add(new Line(Component.translatable("apoapsis.map3d.node.nothrust"), Palette.ALERT));
        } else if (t.secondsToStart() > 0) {
            lines.add(new Line(Component.translatable("apoapsis.map3d.node.burn", duration(t.burnSeconds()),
                    Fmt3d.clock(t.secondsToStart())), Palette.INK));
        } else {
            lines.add(new Line(Component.translatable("apoapsis.map3d.node.burn_now", duration(t.burnSeconds())),
                    Palette.GO));
        }
        ManeuverPlan.Apsides a = after;
        if (a != null) {
            lines.add(a.bound()
                    ? new Line(Component.translatable("apoapsis.map3d.node.after", Fmt.distance(a.apoapsisAltitude()),
                            Fmt.distance(a.periapsisAltitude())), Palette.INK)
                    : new Line(Component.translatable("apoapsis.map3d.node.after_escape",
                            Fmt.distance(a.periapsisAltitude())), Palette.INK));
        }
        lines.add(new Line(Component.translatable("apoapsis.map3d.node.help"), Palette.INK_MUTED));
        return lines;
    }

    private static String speed(double ms) {
        return String.format(Locale.ROOT, "%.1f m/s", ms);
    }

    private static String signed(double ms) {
        return String.format(Locale.ROOT, "%+.1f", ms);
    }

    private static String duration(double seconds) {
        return seconds < 100 ? String.format(Locale.ROOT, "%.1f s", seconds) : Fmt.clock(seconds);
    }

    /** The card's box in a {@code viewW} by {@code viewH} view, {@code lift} px above the bottom margin; null when no node. */
    @Nullable Box cardBox(Font font, int viewW, int viewH, int lift) {
        List<Line> lines = cardLines();
        if (lines.isEmpty()) {
            return null;
        }
        int w = 0;
        for (Line l : lines) {
            w = Math.max(w, font.width(l.text()));
        }
        int x1 = viewW - 16;
        int y1 = viewH - 16 - lift;
        int h = lines.size() * LINE_STEP + 10;
        return new Box(x1 - w - 20, y1 - h, x1, y1);
    }

    /** Draws the card in {@code box}, as from {@link #cardBox}. */
    void drawCard(GuiGraphicsExtractor g, Font font, Box box) {
        g.fill(box.x0(), box.y0(), box.x1(), box.y1(), Palette.alpha(Palette.STEEL_950, 0xC0));
        Map3DScreen.frame(g, box.x0(), box.y0(), box.x1() - 1, box.y1() - 1, Palette.STEEL_500);
        int y = box.y0() + 6;
        for (Line l : cardLines()) {
            g.text(font, l.text(), box.x0() + 10, y, l.argb(), false);
            y += LINE_STEP;
        }
    }
}
