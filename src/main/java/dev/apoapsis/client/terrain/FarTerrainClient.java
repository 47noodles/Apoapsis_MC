package dev.apoapsis.client.terrain;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.TerrainTilePayload;
import dev.apoapsis.planet.TerrainTiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The client's side of the far terrain: takes the tiles the server sends and asks for the ones it needs. Each client
 * tick it looks at where the camera is over the body (the ship's sub-point in orbit, the camera on a surface) and, from
 * the last half second of its track, how fast the point is moving. For each level of detail wanted at this altitude it
 * asks the server for a new tile when the one it has will no longer cover the camera by the time a fresh one could
 * arrive (the round trip is measured per level), and centres the new tile ahead along the track, where the camera will
 * be when it lands. Coarse levels are asked for first, since they cover the most of the screen. A level whose tile the
 * camera would outrun on the way is not asked for: the coarser level stays under it. One request per level is out at a
 * time. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class FarTerrainClient {
    /** Set false to switch the far terrain off (frame-cost comparisons, or {@code -Dapoapsis.farTerrain=off}). */
    public static volatile boolean enabled = !"off".equals(System.getProperty("apoapsis.farTerrain"));

    /** Log what is drawn once a second (test runs). */
    public static volatile boolean debug;

    /** Altitude below which each level is asked for, metres: fine tiles only matter when low. */
    private static final double[] WANTED_BELOW_M = {3_000, 10_000, 45_000};
    /** Client ticks before a request that has had no answer is repeated. */
    private static final long RETRY_TICKS = 60;
    /** Fewest client ticks between two requests of one level: the server drops a closer one without a word. */
    private static final long MIN_GAP_TICKS = 6;
    private static final double TICKS_PER_SECOND = 20.0;
    /** A tile is useful while the camera is within this share of its half width of its centre. */
    private static final double USEFUL_SHARE = 0.6;
    /** Seconds past the expected arrival that a tile must still cover the camera, or a new one is asked for. */
    private static final double SLACK_S = 0.25;
    /** A new tile is centred this many seconds of travel past where the camera will be when it lands (capped). */
    private static final double AHEAD_S = 0.5;
    /** The first guess of each level's round trip (ask, generate, send), client ticks, until one has been measured. */
    private static final double[] FIRST_LATENCY_TICKS = {12, 16, 18};
    /** Client ticks the ground velocity is measured over, and the track the sub-point left in them. */
    private static final int VELOCITY_TICKS = 10;
    /** A step of the sub-point longer than this in one tick is a jump (teleport), not travel: forget the track. */
    private static final double JUMP_M = 5_000;

    private static final double[] latencyTicks = FIRST_LATENCY_TICKS.clone();
    private static final double[] trackX = new double[VELOCITY_TICKS + 1];
    private static final double[] trackZ = new double[VELOCITY_TICKS + 1];
    private static long trackCount;
    private static String trackKey = "";
    private static double unwrappedX;
    private static double lastX;
    private static double lastZ;
    /** The ground velocity of the view in the block grid, metres per second (zero while the track is short). */
    private static double velX;
    private static double velZ;

    /** The request out for each level: what was asked and when (client ticks); null body when none. */
    private static final String[] pendingBody = new String[TerrainTilePayload.LEVELS];
    private static final int[] pendingX = new int[TerrainTilePayload.LEVELS];
    private static final int[] pendingZ = new int[TerrainTilePayload.LEVELS];
    private static final long[] pendingTick = new long[TerrainTilePayload.LEVELS];
    private static final long[] lastSendTick = new long[TerrainTilePayload.LEVELS];
    private static long ticks;

    /** Debug: per level, ticks since the last log that the level was wanted, and that its tile covered the camera. */
    private static final int[] wantedTicks = new int[TerrainTilePayload.LEVELS];
    private static final int[] coveredTicks = new int[TerrainTilePayload.LEVELS];

    /** The render-distance fog ends this far out when the terrain is fully up, blocks: beyond any far plane. */
    private static final float FAR_FOG_END_BLOCKS = 1.0e6f;
    /** Verdance's atmospheric fog ends at this many horizon distances when the terrain is fully up. */
    private static final float HORIZON_FOG_FACTOR = 2.6f;

    private FarTerrainClient() {}

    @SubscribeEvent
    static void onRegisterPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(TerrainTilePayload.TYPE, (payload, context) -> FarTerrain.accept(payload));
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FarTerrain.clear();
        FarTerrainRenderer.clear();
        OrbitPose.reset();
        trackCount = 0;
        for (int i = 0; i < pendingTick.length; i++) {
            pendingBody[i] = null;
            latencyTicks[i] = FIRST_LATENCY_TICKS[i];
        }
    }

    /**
     * With the terrain up, the voxels must not hide it: the fog that ends the world at the render distance (measured
     * as the larger of the horizontal and the vertical distance, so everything far below a high camera is fully fogged)
     * moves out past the far plane as the terrain fades in, and on Verdance the atmospheric fog stretches to the
     * horizon, so the voxels and the far mesh (which uses the same fog values) haze the same way. After every other
     * listener, so the climb's changes are the starting point.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.level == null) {
            return;
        }
        FarTerrainView v = FarTerrainView.compute(event.getCamera().position(), (float) event.getPartialTick(), false);
        if (v == null || v.mode() != FarTerrainView.Mode.SURFACE) {
            return;
        }
        FogData fog = event.getFogData();
        float fade = (float) v.fade();
        float span = Math.max(4f, fog.renderDistanceEnd - fog.renderDistanceStart);
        float renderEnd = fog.renderDistanceEnd + (FAR_FOG_END_BLOCKS - fog.renderDistanceEnd) * fade;
        fog.renderDistanceEnd = renderEnd;
        fog.renderDistanceStart = renderEnd - span;
        if (v.water() && event.getType() == FogType.ATMOSPHERIC) {
            double h = Math.max(v.altitude(), 0);
            float horizon = (float) Math.sqrt(2 * v.radius() * h + h * h);
            float end = Math.max(fog.environmentalEnd, HORIZON_FOG_FACTOR * horizon);
            fog.environmentalEnd += (end - fog.environmentalEnd) * fade;
        }
    }

    /** The view of the last client tick that wanted tiles, for the test driver. */
    private static volatile @Nullable FarTerrainView latest;

    /** One line about where the camera is over a body, for test logs: mode, body, ground point x z, altitude. */
    public static String describeView() {
        FarTerrainView v = latest;
        return v == null ? "no view" : String.format("%s %s ground point x=%.0f z=%.0f altitude %.0f m",
                v.mode(), v.body().id, v.subX(), v.subZ(), v.altitude());
    }

    /** The block-grid point {x, z} under the camera in the last view, or null when there was none. */
    public static double @Nullable [] groundPoint() {
        FarTerrainView v = latest;
        return v == null ? null : new double[] {v.subX(), v.subZ()};
    }

    /** Average CPU milliseconds per frame the far terrain took since the last call (-1 if it drew nothing). */
    public static double cpuMsPerFrame() {
        return FarTerrainRenderer.takeStats();
    }

    /** A tile arrived: the request for its level is answered, and its round trip updates the level's estimate. */
    static void onTileArrived(TerrainTilePayload tile) {
        int level = tile.level();
        boolean answered = tile.body().equals(pendingBody[level]);
        long trip = ticks - pendingTick[level];
        if (debug) {
            FarTerrainView v = latest;
            Apoapsis.LOG.info("[far-terrain] L{} tile {},{} arrived {} ticks after its request; view {} speed {} m/s",
                    level, tile.centerX(), tile.centerZ(), answered ? trip : -1, v == null ? "none"
                            : Math.round(v.subX() - tile.centerX()) + "," + Math.round(v.subZ() - tile.centerZ())
                            + " from its centre", Math.round(Math.hypot(velX, velZ)));
        }
        if (answered) {
            latencyTicks[level] = Math.max(4, Math.min(80, 0.6 * latencyTicks[level] + 0.4 * trip));
            pendingBody[level] = null;
        }
    }

    /**
     * Follows the sub-point over the last {@link #VELOCITY_TICKS} ticks and takes its ground velocity from the track (a
     * single tick's step is noisy by a few percent at orbital speed). Longitude is unwrapped across the antimeridian. A
     * change of body or mode, or a jump, starts the track over.
     */
    private static void track(FarTerrainView view) {
        String key = view.body().id + view.mode();
        double x = view.subX();
        double z = view.subZ();
        boolean orbit = view.mode() == FarTerrainView.Mode.ORBIT;
        double circumference = 2 * Math.PI * view.radius();
        double dx = x - lastX;
        if (orbit && trackCount > 0) {
            dx -= circumference * Math.rint(dx / circumference);
        }
        if (!key.equals(trackKey) || Math.hypot(dx, z - lastZ) > JUMP_M) {
            trackKey = key;
            trackCount = 0;
            unwrappedX = x;
        } else {
            unwrappedX += dx;
        }
        lastX = x;
        lastZ = z;
        int slot = (int) (trackCount % trackX.length);
        trackX[slot] = unwrappedX;
        trackZ[slot] = z;
        trackCount++;
        int span = (int) Math.min(trackCount - 1, VELOCITY_TICKS);
        if (span >= 3) {
            int old = (int) ((trackCount - 1 - span) % trackX.length);
            velX = (unwrappedX - trackX[old]) * TICKS_PER_SECOND / span;
            velZ = (z - trackZ[old]) * TICKS_PER_SECOND / span;
        } else {
            velX = 0;
            velZ = 0;
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || mc.level == null) {
            return;
        }
        FarTerrainView view = FarTerrainView.compute(mc.player.getEyePosition(), 1.0f, true);
        latest = view;
        if (view == null) {
            trackCount = 0;
            return;
        }
        track(view);
        if (debug) {
            logCoverage(view);
        }
        if (!TerrainTiles.mayRequest(mc.player)) {
            return; // the server answers only pilots and the orbit regime; flying without a ship asks for nothing
        }
        for (int level = TerrainTilePayload.LEVELS - 1; level >= 0; level--) {
            request(view, level);
        }
    }

    /**
     * Asks for the tile of {@code level} when it is needed. The camera will be {@code latency + SLACK_S} seconds on
     * along its track when a tile asked for now could be in place: if the current tile still covers that spot there is
     * nothing to do. Otherwise the new tile is centred past that spot, so it lasts longest, unless the camera moves too
     * fast for a tile of this level to outlast its own trip, in which case the coarser level is left to cover it.
     */
    private static void request(FarTerrainView view, int level) {
        if (view.altitude() >= WANTED_BELOW_M[level]) {
            return;
        }
        String body = view.body().id;
        double half = TerrainTilePayload.widthOf(level) / 2.0;
        double useful = USEFUL_SHARE * half;
        double latency = latencyTicks[level] / TICKS_PER_SECOND;
        double speed = Math.hypot(velX, velZ);
        if (body.equals(pendingBody[level]) && ticks - pendingTick[level] < RETRY_TICKS) {
            // A request is out: wait for it, unless it would land where the camera will no longer be.
            double remaining = Math.max(0, latency - (ticks - pendingTick[level]) / TICKS_PER_SECOND);
            if (ticks - lastSendTick[level] < MIN_GAP_TICKS
                    || Math.abs(view.subX() + velX * remaining - pendingX[level]) <= 0.85 * half
                    && Math.abs(view.subZ() + velZ * remaining - pendingZ[level]) <= 0.85 * half) {
                return;
            }
        }
        TerrainTilePayload tile = FarTerrain.tile(body, level);
        double horizon = latency + SLACK_S;
        if (tile != null && Math.abs(view.subX() + velX * horizon - tile.centerX()) <= useful
                && Math.abs(view.subZ() + velZ * horizon - tile.centerZ()) <= useful) {
            return;
        }
        if (ticks - lastSendTick[level] < MIN_GAP_TICKS) {
            return;
        }
        double ahead = Math.min(speed * AHEAD_S, 0.5 * useful);
        if (level < TerrainTilePayload.LEVELS - 1 && speed * (latency + AHEAD_S) > useful + ahead) {
            return; // the camera would outrun the tile: keep the coarse level
        }
        double dirX = speed > 1e-3 ? velX / speed : 0;
        double dirZ = speed > 1e-3 ? velZ / speed : 0;
        double cx = view.subX() + velX * latency + dirX * ahead;
        double cz = view.subZ() + velZ * latency + dirZ * ahead;
        if (view.mode() == FarTerrainView.Mode.ORBIT) {
            double circumference = 2 * Math.PI * view.radius();
            cx -= circumference * Math.rint(cx / circumference);
            cz = Math.max(-circumference / 4, Math.min(circumference / 4, cz));
        }
        int x = (int) Math.round(cx);
        int z = (int) Math.round(cz);
        pendingBody[level] = body;
        pendingX[level] = x;
        pendingZ[level] = z;
        pendingTick[level] = ticks;
        lastSendTick[level] = ticks;
        if (debug) {
            Apoapsis.LOG.info("[far-terrain] ask L{} at {},{} (camera {},{}) alt {} m speed {} m/s round trip {} ticks",
                    level, x, z, Math.round(view.subX()), Math.round(view.subZ()), Math.round(view.altitude()),
                    Math.round(speed), Math.round(latencyTicks[level]));
        }
        ClientPacketDistributor.sendToServer(new TerrainTilePayload.Request(body, level, x, z));
    }

    /** Debug: counts the ticks the camera was over each wanted level's tile; logs every 5 s. */
    private static void logCoverage(FarTerrainView view) {
        for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
            if (view.altitude() >= WANTED_BELOW_M[level]) {
                continue;
            }
            wantedTicks[level]++;
            TerrainTilePayload tile = FarTerrain.tile(view.body().id, level);
            double half = TerrainTilePayload.widthOf(level) / 2.0;
            if (tile != null && Math.abs(view.subX() - tile.centerX()) <= half
                    && Math.abs(view.subZ() - tile.centerZ()) <= half) {
                coveredTicks[level]++;
            }
        }
        if (ticks % 100 == 0) {
            StringBuilder sb = new StringBuilder("[far-terrain] camera over tile, ticks:");
            for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
                sb.append(" L").append(level).append(' ').append(coveredTicks[level]).append('/')
                        .append(wantedTicks[level]);
                coveredTicks[level] = 0;
                wantedTicks[level] = 0;
            }
            Apoapsis.LOG.info(sb.toString());
        }
    }
}
