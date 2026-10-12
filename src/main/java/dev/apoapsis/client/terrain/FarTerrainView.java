package dev.apoapsis.client.terrain;

import dev.apoapsis.client.ClientState;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.TerrainTilePayload;
import dev.apoapsis.orbit.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where the camera is relative to a body, for the far terrain: which body, whether it is in that body's surface
 * dimension or looking down from the orbit dimension, the sub-camera point in the body's block grid, the altitude and
 * how far the terrain has faded in. Computed once per frame from the camera and the piloted ship's state.
 *
 * @param mode      {@link Mode#SURFACE} in a body's surface dimension, {@link Mode#ORBIT} in the orbit dimension
 * @param body      the body below the camera
 * @param radius    the body's radius, metres
 * @param seaLevel  block y of height 0 on the body
 * @param water     true when ground below sea level is covered by water (Verdance)
 * @param camX      camera position, blocks (surface mode only)
 * @param camY      camera position, blocks (surface mode only)
 * @param camZ      camera position, blocks (surface mode only)
 * @param viewer    camera position relative to the body centre, inertial, metres (orbit mode only)
 * @param subX      the point on the ground under the camera, block grid x
 * @param subZ      the point on the ground under the camera, block grid z
 * @param altitude  height above the ground (surface) or above the surface (orbit), metres
 * @param fade      0..1: the share of the terrain drawn
 */
record FarTerrainView(Mode mode, BodyId body, double radius, int seaLevel, boolean water, double camX, double camY,
        double camZ, Vec3d viewer, double subX, double subZ, double altitude, double fade) {
    enum Mode { SURFACE, ORBIT }

    /** Surface terrain starts to show this far above the ground, metres, and all there {@link #SURFACE_FADE_M} higher. */
    static final double SURFACE_MIN_ALT_M = 100.0;
    static final double SURFACE_FADE_M = 100.0;
    /** Orbit terrain is fully there below this altitude, metres, and gone {@link #ORBIT_FADE_M} higher. */
    static final double ORBIT_FULL_ALT_M = 30_000.0;
    static final double ORBIT_FADE_M = 10_000.0;

    /** Tiles are asked for from this height above the ground on a surface, metres, so they are in before the fade. */
    static final double SURFACE_ASK_ALT_M = 60.0;
    /** Tiles are asked for below this altitude in orbit, metres. */
    static final double ORBIT_ASK_ALT_M = 60_000.0;

    /**
     * The view for the local player's camera at {@code cam}, or null when there is no far terrain to draw. With
     * {@code forRequests} the fade does not count: a view comes back wherever tiles should be asked for.
     */
    static @Nullable FarTerrainView compute(Vec3 cam, float partialTick, boolean forRequests) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        if (FlightDimensions.ORBIT.equals(level.dimension())) {
            OrbitStatePayload orbit = ClientState.orbit;
            if (orbit == null) {
                return null;
            }
            BodyId body = BodyId.byOrdinal(orbit.body());
            double radius = FlightDimensions.system(body).radius;
            Vec3d r = OrbitPose.position(orbit, partialTick);
            double length = r.length();
            double altitude = length - radius;
            double fade = 1 - smooth((altitude - ORBIT_FULL_ALT_M) / ORBIT_FADE_M);
            if (forRequests ? altitude > ORBIT_ASK_ALT_M : fade <= 0) {
                return null;
            }
            double lon = Math.atan2(r.y(), r.x());
            double lat = Math.asin(Math.max(-1, Math.min(1, r.z() / length)));
            return new FarTerrainView(Mode.ORBIT, body, radius, (int) FlightDimensions.seaLevel(body),
                    body == BodyId.VERDANCE, 0, 0, 0, r, lon * radius, -lat * radius, altitude, fade);
        }
        if (!Level.OVERWORLD.equals(level.dimension()) && !FlightDimensions.PALE.equals(level.dimension())) {
            return null;
        }
        BodyId body = FlightDimensions.surfaceBody(level.dimension());
        int seaLevel = (int) FlightDimensions.seaLevel(body);
        double ground = groundBelow(body, cam.x, cam.z, seaLevel);
        double altitude = cam.y - ground;
        double fade = smooth((altitude - SURFACE_MIN_ALT_M) / SURFACE_FADE_M);
        if (forRequests ? altitude < SURFACE_ASK_ALT_M : fade <= 0) {
            return null;
        }
        return new FarTerrainView(Mode.SURFACE, body, FlightDimensions.system(body).radius, seaLevel,
                body == BodyId.VERDANCE, cam.x, cam.y, cam.z, null, cam.x, cam.z, altitude, fade);
    }

    /** The ground height under a point from the finest tile that covers it, or sea level when no tile does. */
    static double groundBelow(BodyId body, double x, double z, int seaLevel) {
        for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
            TerrainTilePayload t = FarTerrain.tile(body.id, level);
            if (t == null) {
                continue;
            }
            int half = (t.size() - 1) / 2;
            int i = (int) Math.round((x - t.centerX()) / t.spacing()) + half;
            int j = (int) Math.round((z - t.centerZ()) / t.spacing()) + half;
            if (i >= 0 && i < t.size() && j >= 0 && j < t.size()) {
                return t.heights()[j * t.size() + i];
            }
        }
        return seaLevel;
    }

    /** 0 below 0, 1 above 1, smoothstep between. */
    static double smooth(double t) {
        double c = Math.max(0, Math.min(1, t));
        return c * c * (3 - 2 * c);
    }
}
