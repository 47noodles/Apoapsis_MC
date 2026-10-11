package dev.apoapsis.client.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Third-person chase camera for a ship. While the local player rides a ship in third person, the detached camera
 * distance is {@code clamp(2.5 * halfExtent + 4, 4, 48)} blocks times a zoom factor. The mouse wheel changes the zoom
 * (steps of 1.15, range 0.5 to 4) only in that case, and the factor is kept for the session. The distance eases
 * toward its target over a few frames. First person and on foot stay vanilla. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class ChaseCamera {
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 4.0;
    private static final double ZOOM_STEP = 1.15;
    /** Blocks of camera distance per block of ship half-extent. */
    private static final double DISTANCE_PER_HALF_EXTENT = 2.5;
    private static final double DISTANCE_BASE = 4.0;
    private static final double DISTANCE_MIN = 4.0;
    private static final double DISTANCE_MAX = 48.0;
    /** Time constant of the distance easing, seconds. About 95 percent of a change lands within 0.25 s. */
    private static final double EASE_SECONDS = 0.08;
    /** Gaps longer than this, seconds, snap the distance instead of easing (after a level change or a pause). */
    private static final double SNAP_SECONDS = 0.5;

    /** Zoom factor set by the wheel; 1 means the base distance. Kept for the session, not saved. */
    private static double zoom = 1.0;
    /** The eased distance in blocks; negative when no chase camera has run yet. */
    private static double shown = -1.0;
    private static long lastNanos;

    private ChaseCamera() {}

    /** The ship the local player's camera is following in third person, or null. */
    private static ShipEntity chasedShip(Minecraft mc) {
        if (mc.player == null || mc.getCameraEntity() != mc.player || mc.options.getCameraType().isFirstPerson()) {
            return null;
        }
        return mc.player.getVehicle() instanceof ShipEntity ship ? ship : null;
    }

    /**
     * Sets the detached camera distance for a ship. Vanilla passes the detached branch only in third person, so this
     * event never changes first person. The ship's bounding cube is {@code 2 * halfExtent} wide.
     */
    @SubscribeEvent
    public static void onDetachedDistance(CalculateDetachedCameraDistanceEvent event) {
        ShipEntity ship = chasedShip(Minecraft.getInstance());
        if (ship == null) {
            shown = -1.0;
            return;
        }
        double halfExtent = ship.getBoundingBox().getXsize() / 2.0;
        double base = Mth.clamp(DISTANCE_PER_HALF_EXTENT * halfExtent + DISTANCE_BASE, DISTANCE_MIN, DISTANCE_MAX);
        double target = base * zoom;

        long now = System.nanoTime();
        double gap = (now - lastNanos) / 1e9;
        if (shown < 0.0 || gap > SNAP_SECONDS) {
            shown = target;
        } else {
            shown += (target - shown) * (1.0 - Math.exp(-gap / EASE_SECONDS));
        }
        lastNanos = now;
        event.setDistance((float) shown);
    }

    /**
     * Zooms the chase camera with the wheel while piloting in third person. Wheel up moves closer, wheel down pulls
     * back. The event is cancelled only then, so the hotbar keeps the wheel everywhere else.
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        double dy = event.getScrollDeltaY();
        if (dy == 0.0 || mc.gui.screen() != null || chasedShip(mc) == null) {
            return;
        }
        double factor = dy < 0.0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
        zoom = Mth.clamp(zoom * factor, ZOOM_MIN, ZOOM_MAX);
        event.setCanceled(true);
    }
}
