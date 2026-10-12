package dev.apoapsis.client.map;

import net.minecraft.client.Minecraft;

/** Hooks for the scripted test runs to drive the open 2D map the way the focus keys and the wheel do. Client only. */
public final class MapScript {
    private MapScript() {}

    /** Follows 0 Verdance, 1 Pale or 2 the ship in the open 2D map, fitting {@code halfExtentM} metres. False when it is not open. */
    public static boolean focus(int which, double halfExtentM) {
        if (Minecraft.getInstance().gui.screen() instanceof MapScreen s) {
            s.focusFor(which, halfExtentM);
            return true;
        }
        return false;
    }
}
