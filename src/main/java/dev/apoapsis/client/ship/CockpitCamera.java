package dev.apoapsis.client.ship;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.client.OrbitNow;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.flight.Frames;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Space Engineers style cockpit camera. While the local player rides a ship, the view is the ship's orientation
 * (as the renderer draws it) times the mouse look, so the world stays fixed to the cockpit and the pilot rotates the
 * ship to change the view. Boarding converts the player's world look to cockpit-relative angles once, so the view
 * does not jump. Across an orbit and surface transition the view is held: the last ridden orientation is mapped the
 * way the server maps the ship, for up to {@link #HOLD_TICKS} client ticks, until the pilot is re-seated. After a
 * dismount the last ridden orientation is kept for up to {@link #LINGER_TICKS} in the same dimension, then the look
 * is converted back to world angles. Vanilla free look applies otherwise. Client only.
 */
public final class CockpitCamera {
    private static final double DEG = Math.PI / 180.0;
    /** Client ticks a held view lasts after the last ridden frame. */
    private static final long HOLD_TICKS = 60;
    /** Client ticks the last ridden orientation is kept in the same dimension after the pilot dismounts. */
    private static final long LINGER_TICKS = 10;

    /**
     * The ship's orientation at its last ridden frame, with the frame anchor for the dimension it was in: the inertial
     * position in orbit (null when no orbit state), or the surface position and body on a surface.
     */
    private record Ridden(Quaternionf q, ResourceKey<Level> dimension, long tick, Vec3d orbitR, BodyId body, Vec3 pos) {}

    private static Ridden last;
    /** The mapped view for the transition being held, computed once; null when not holding. */
    private static Quaternionf held;
    /** True while the cockpit view is in use: riding, holding a transition, or lingering after a dismount. */
    private static boolean active;
    /** The camera rotation applied on the last frame while active. */
    private static Quaternionf applied;

    private CockpitCamera() {}

    /** Called from {@code ShipClient.register}. Adds the camera listener on the game event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(CockpitCamera::onComputeCameraAngles);
    }

    /** Clears all held state. Called on logout, so a relogged session never reads the previous session's orientation. */
    public static void reset() {
        last = null;
        held = null;
        active = false;
        applied = null;
    }

    /**
     * Called when a client level unloads. Dimension changes unload the level too, so state is cleared only when it is
     * already too old to hold or linger: a transition in progress keeps its orientation.
     */
    public static void onLevelUnload() {
        if (last == null || ClientState.clientTicks - last.tick() > HOLD_TICKS) {
            reset();
        }
    }

    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        Player player = mc.player;
        float partialTick = event.getPartialTick();
        ResourceKey<Level> dimension = mc.level.dimension();

        if (player.getVehicle() instanceof ShipEntity ship) {
            Quaternionf q = ShipRenderCache.entry(ship.getId()).orientation(ship.getOrientation(), partialTick);
            float yaw = event.getYaw();
            float pitch = event.getPitch();
            if (!active) {
                // Boarding: the player's angles are world angles. Make them cockpit-relative once, keeping the view.
                Quaternionf rel = new Quaternionf(q).conjugate().mul(vanilla(player.getYRot(), player.getXRot()));
                float[] look = anglesOf(viewOf(rel));
                setLook(player, look[0], look[1]);
                yaw = look[0];
                pitch = look[1];
            }
            active = true;
            last = remember(q, ship, dimension, partialTick);
            held = null;
            applied = new Quaternionf(q).mul(vanilla(yaw, pitch));
            apply(event, applied);
            return;
        }
        if (!active || last == null) {
            return;
        }
        Quaternionf head = vanilla(event.getYaw(), event.getPitch());
        Quaternionf cam = null;
        if (last.dimension().equals(dimension)) {
            // Dismount: keep the last ridden orientation for a few ticks, since a transition dismounts first.
            if (ClientState.clientTicks - last.tick() <= LINGER_TICKS) {
                cam = new Quaternionf(last.q()).mul(head);
            }
        } else if (canHold(dimension)) {
            if (held == null) {
                held = across(last);
            }
            if (held != null) {
                cam = new Quaternionf(held).mul(head);
            }
        }
        if (cam == null) {
            leave(player, event);
            return;
        }
        applied = cam;
        apply(event, cam);
    }

    /**
     * Returns the view to vanilla. The player's world look becomes the view direction of the last applied camera
     * rotation, so the view does not change at this moment.
     */
    private static void leave(Player player, ViewportEvent.ComputeCameraAngles event) {
        if (applied != null) {
            float[] look = anglesOf(viewOf(applied));
            setLook(player, look[0], look[1]);
            apply(event, applied);
        }
        active = false;
        held = null;
        applied = null;
    }

    /** The ship's orientation and frame anchor for this ridden frame. */
    private static Ridden remember(Quaternionf q, ShipEntity ship, ResourceKey<Level> dimension, float partialTick) {
        long tick = ClientState.clientTicks;
        if (FlightDimensions.ORBIT.equals(dimension)) {
            OrbitStatePayload p = ClientState.orbit;
            Vec3d r = null;
            BodyId body = null;
            if (p != null && FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT) {
                r = OrbitNow.stateNow(p, partialTick).r();
                body = BodyId.byOrdinal(p.body());
            }
            return new Ridden(new Quaternionf(q), dimension, tick, r, body, null);
        }
        return new Ridden(new Quaternionf(q), dimension, tick, null, FlightDimensions.surfaceBody(dimension),
                ship.position());
    }

    /** True while the pilot is between the last ridden frame and a re-seat across orbit and surface, within the window. */
    private static boolean canHold(ResourceKey<Level> dimension) {
        if (last == null || last.dimension().equals(dimension)) {
            return false;
        }
        boolean fromOrbit = FlightDimensions.ORBIT.equals(last.dimension());
        boolean toOrbit = FlightDimensions.ORBIT.equals(dimension);
        return fromOrbit != toOrbit && ClientState.clientTicks - last.tick() <= HOLD_TICKS;
    }

    /**
     * The remembered orientation mapped the way the server maps the ship at a transition: orbit to surface uses the
     * tangent at the inertial position, surface to orbit the tangent at the surface point. Null without an orbit anchor.
     */
    private static Quaternionf across(Ridden r) {
        if (FlightDimensions.ORBIT.equals(r.dimension())) {
            if (r.orbitR() == null) {
                return null;
            }
            return Frames.orbitToSurfaceOrientation(r.q(), Frames.tangentAt(r.orbitR()));
        }
        CelestialBody sys = FlightDimensions.system(r.body());
        return Frames.surfaceToOrbitOrientation(r.q(), Frames.tangentAt(r.pos(), sys.radius));
    }

    /**
     * Vanilla's camera rotation for a yaw and pitch in degrees, without roll: the same build as Camera.setRotation.
     * The camera looks along its local -Z axis, so this rotation's view is {@link #viewOf}.
     */
    private static Quaternionf vanilla(float yaw, float pitch) {
        return new Quaternionf().rotationYXZ((float) (Math.PI - yaw * DEG), (float) (-pitch * DEG), 0f);
    }

    /** The view direction of a camera rotation. */
    private static Vector3f viewOf(Quaternionf cam) {
        return cam.transform(new Vector3f(0f, 0f, -1f));
    }

    /**
     * Vanilla yaw and pitch in degrees for a view direction, the inverse of Entity.calculateViewVector:
     * the direction is (-sin yaw cos pitch, -sin pitch, cos yaw cos pitch).
     */
    private static float[] anglesOf(Vector3f dir) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, -dir.y))));
        return new float[] {yaw, pitch};
    }

    /** Sets the player's world yaw and pitch, and their previous and head values, so no interpolation shows. */
    private static void setLook(Player player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(pitch);
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
    }

    /** Sets the camera's yaw, pitch and roll from the rotation {@code cam}, as vanilla's setRotation reads them. */
    private static void apply(ViewportEvent.ComputeCameraAngles event, Quaternionf cam) {
        Vector3f e = cam.getEulerAnglesYXZ(new Vector3f());
        event.setYaw(Mth.wrapDegrees((float) (180.0 - e.y / DEG)));
        event.setPitch((float) (-e.x / DEG));
        event.setRoll((float) (-e.z / DEG));
    }
}
