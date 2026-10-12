package dev.apoapsis.client.map3d;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.orbit.maneuver.ManeuverNode;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jspecify.annotations.Nullable;

/**
 * Mouse input for the scripted test run: it drives the open 3D map the way a player's hand would, through the screen's
 * own mouse methods, so the click, the drag and the delete are tested as they are used. Client only.
 */
public final class Map3DScript {
    private static final MouseButtonInfo LEFT = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
    private static final MouseButtonInfo RIGHT = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0);

    private Map3DScript() {}

    /** UI units (what the screen reports) to GUI pixels (what its mouse methods take). */
    private static double gui(double ui) {
        return ui * UiScale.current().scale();
    }

    private static @Nullable Map3DScreen screen() {
        return Minecraft.getInstance().gui.screen() instanceof Map3DScreen s ? s : null;
    }

    /** Clicks the ship's orbit line {@code seconds} ahead of the ship. True when a node now stands. */
    public static boolean clickOrbit(double seconds) {
        Map3DScreen s = screen();
        double[] at = s == null ? null : s.orbitPointAhead(seconds);
        if (at == null) {
            return false;
        }
        MouseButtonEvent e = new MouseButtonEvent(gui(at[0]), gui(at[1]), LEFT);
        s.mouseClicked(e, false);
        s.mouseReleased(e);
        return s.editor().nodeForTest() != null;
    }

    /**
     * Drags handle {@code handle} (an index into {@link ManeuverPlan#HANDLES}) outward by {@code pixels}, in steps of
     * a few px. False when no node is shown.
     */
    public static boolean dragHandle(int handle, double pixels) {
        Map3DScreen s = screen();
        double[] at = s == null ? null : s.editor().handleScreenForTest(handle);
        if (at == null) {
            return false;
        }
        double[] u = s.editor().handleDirForTest(handle);
        s.mouseClicked(new MouseButtonEvent(gui(at[0]), gui(at[1]), LEFT), false);
        double step = 4;
        double done = 0;
        while (done < pixels) {
            double d = Math.min(step, pixels - done);
            done += d;
            s.mouseDragged(new MouseButtonEvent(gui(at[0] + u[0] * done), gui(at[1] + u[1] * done), LEFT), gui(u[0] * d), gui(u[1] * d));
        }
        s.mouseReleased(new MouseButtonEvent(gui(at[0] + u[0] * done), gui(at[1] + u[1] * done), LEFT));
        return true;
    }

    /** Right-clicks the node. True when it is gone afterwards. */
    public static boolean rightClickNode() {
        Map3DScreen s = screen();
        double[] at = s == null ? null : s.editor().nodeScreenForTest();
        if (at == null) {
            return false;
        }
        MouseButtonEvent e = new MouseButtonEvent(gui(at[0]), gui(at[1]), RIGHT);
        s.mouseClicked(e, false);
        s.mouseReleased(e);
        return s.editor().nodeForTest() == null;
    }

    /** Clicks the middle of body 0 Verdance or 1 Pale. */
    public static boolean clickBody(int which) {
        Map3DScreen s = screen();
        double[] at = s == null ? null : s.bodyScreen(which);
        if (at == null) {
            return false;
        }
        MouseButtonEvent e = new MouseButtonEvent(gui(at[0]), gui(at[1]), LEFT);
        s.mouseClicked(e, false);
        s.mouseReleased(e);
        return true;
    }

    /** The followed focus and how far it is from the middle of the view, for the log. */
    public static String focusState() {
        Map3DScreen s = screen();
        return s == null ? "no map" : String.format(Locale.ROOT, "following %s, %.1f px off centre", s.followName(),
                s.centringErrorPx());
    }

    /** Turns the view by a left-button drag of ({@code dx}, {@code dy}) px, as a player does. */
    public static void rotate(double dx, double dy) {
        Map3DScreen s = screen();
        if (s != null) {
            s.mouseDragged(new MouseButtonEvent(400, 300, LEFT), dx, dy);
        }
    }

    /** Focuses 0 Verdance, 1 Pale or 2 the ship at {@code metres} from it. */
    public static void focus(int which, double metres) {
        Map3DScreen s = screen();
        if (s != null) {
            s.focusFor(which, metres);
        }
    }

    /** One line about the node, for the log. */
    public static String describe() {
        Map3DScreen s = screen();
        ManeuverNode n = s == null ? null : s.editor().nodeForTest();
        if (n == null) {
            return "no node";
        }
        return String.format(Locale.ROOT, "node ut %.1f prograde %.1f normal %.1f radial %.1f dv %.1f", n.ut(),
                n.prograde(), n.normal(), n.radial(), n.deltaV());
    }
}
