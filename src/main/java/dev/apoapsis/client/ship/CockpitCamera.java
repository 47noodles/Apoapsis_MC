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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Space Engineers style cockpit camera. While the local player rides a ship, the view is the ship's orientation
 * (as the renderer draws it) times the mouse look, so the world stays fixed to the cockpit and the pilot rotates the
 * ship to change the view. Across an orbit and surface transition the view is held: the last ridden orientation is
 * mapped the way the server maps the ship, for up to {@link #HOLD_TICKS} client ticks, until the pilot is re-seated.
 * Vanilla free look applies when neither holds. Client only.
 */
public final class CockpitCamera {
    private static final double DEG = Math.PI / 180.0;
    /** Client ticks a held view lasts after the last ridden frame. */
    private static final long HOLD_TICKS = 60;

    /**
     * The ship's orientation at its last ridden frame, with the frame anchor for the dimension it was in: the inertial
     * position in orbit (null when no orbit state), or the surface position and body on a surface.
     */
    private record Ridden(Quaternionf q, ResourceKey<Level> dimension, long tick, Vec3d orbitR, BodyId body, Vec3 pos) {}

    private static Ridden last;
    /** The mapped view for the transition being held, computed once; null when not holding. */
    private static Quaternionf held;

    private CockpitCamera() {}

    /** Called from {@code ShipClient.register}. Adds the camera listener on the game event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(CockpitCamera::onComputeCameraAngles);
    }

    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        // The mouse look, read as relative to the cockpit. At identity ship orientation this is vanilla's rotation.
        Quaternionf head = new Quaternionf().rotationYXZ((float) (Math.PI - event.getYaw() * DEG),
                (float) (-event.getPitch() * DEG), 0f);
        ResourceKey<Level> dimension = mc.level.dimension();

        if (mc.player.getVehicle() instanceof ShipEntity ship) {
            Quaternionf q = ShipRenderCache.entry(ship.getId()).orientation(ship.getOrientation(), partialTick);
            last = remember(q, ship, dimension, partialTick);
            held = null;
            apply(event, new Quaternionf(q).mul(head));
            return;
        }
        if (!canHold(dimension)) {
            return;
        }
        if (held == null) {
            held = across(last);
        }
        if (held != null) {
            apply(event, new Quaternionf(held).mul(head));
        }
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

    /** Sets the camera's yaw, pitch and roll from the rotation {@code cam}, as vanilla's setRotation reads them. */
    private static void apply(ViewportEvent.ComputeCameraAngles event, Quaternionf cam) {
        Vector3f e = cam.getEulerAnglesYXZ(new Vector3f());
        event.setYaw(Mth.wrapDegrees((float) (180.0 - e.y / DEG)));
        event.setPitch((float) (-e.x / DEG));
        event.setRoll((float) (-e.z / DEG));
    }
}
