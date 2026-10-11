package dev.apoapsis.client.terrain;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.TerrainTilePayload;
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
 * tick it looks at where the camera is over the body (the ship's sub-point in orbit, the camera on a surface) and,
 * for each level of detail wanted at this altitude, asks the server for a new tile when it has none or the point has
 * moved more than a quarter of the tile's width from its centre. One request per level is out at a time. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class FarTerrainClient {
    /** Set false to switch the far terrain off (frame-cost comparisons, or {@code -Dapoapsis.farTerrain=off}). */
    public static volatile boolean enabled = !"off".equals(System.getProperty("apoapsis.farTerrain"));

    /** Log what is drawn once a second (test runs). */
    public static volatile boolean debug;

    /**
     * Altitude below which each level is asked for, metres: fine tiles only matter when low, and a ship that moves
     * faster than a tile can be made (about a second and a half for the big ones) would only ever get stale tiles.
     */
    private static final double[] WANTED_BELOW_M = {3_000, 10_000, 45_000};
    /** Client ticks before a request that has had no answer is repeated. */
    private static final long RETRY_TICKS = 100;
    /**
     * Ground speed above which each level is not asked for, metres per second: a tile is stale by the time it arrives
     * when the camera crosses a quarter of its width during the generation and the trip (about 0.5 s for the fine
     * level, 1.5 s for the others). The server works on the finest request first, so a stream of useless fine tiles
     * at orbital speed would also starve the coarse ones.
     */
    private static final double[] MAX_SPEED_M_S = {500, 1_300, 4_500};
    /** The ground speed of the view, metres per second, smoothed over a few ticks. */
    private static double groundSpeed;
    private static String speedBody = "";
    private static double lastSubX;
    private static double lastSubZ;

    /** The request out for each level: what was asked and when (client ticks); zero tick when none. */
    private static final String[] pendingBody = new String[TerrainTilePayload.LEVELS];
    private static final int[] pendingX = new int[TerrainTilePayload.LEVELS];
    private static final int[] pendingZ = new int[TerrainTilePayload.LEVELS];
    private static final long[] pendingTick = new long[TerrainTilePayload.LEVELS];
    private static long ticks;

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
        for (int i = 0; i < pendingTick.length; i++) {
            pendingBody[i] = null;
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

    /** A tile arrived: the request for its level is answered. */
    static void onTileArrived(TerrainTilePayload tile) {
        if (tile.body().equals(pendingBody[tile.level()])) {
            pendingBody[tile.level()] = null;
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
            return;
        }
        double instant = view.body().id.equals(speedBody)
                ? Math.min(20_000, Math.hypot(view.subX() - lastSubX, view.subZ() - lastSubZ) * 20) : 0;
        groundSpeed = 0.7 * groundSpeed + 0.3 * instant;
        speedBody = view.body().id;
        lastSubX = view.subX();
        lastSubZ = view.subZ();
        for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
            if (view.altitude() >= WANTED_BELOW_M[level] || groundSpeed > MAX_SPEED_M_S[level]) {
                continue;
            }
            TerrainTilePayload tile = FarTerrain.tile(view.body().id, level);
            int quarter = TerrainTilePayload.widthOf(level) / 4;
            if (tile != null && Math.abs(view.subX() - tile.centerX()) <= quarter
                    && Math.abs(view.subZ() - tile.centerZ()) <= quarter) {
                continue;
            }
            if (view.body().id.equals(pendingBody[level]) && ticks - pendingTick[level] < RETRY_TICKS
                    && Math.abs(view.subX() - pendingX[level]) <= quarter / 2
                    && Math.abs(view.subZ() - pendingZ[level]) <= quarter / 2) {
                continue;
            }
            int x = (int) Math.round(view.subX());
            int z = (int) Math.round(view.subZ());
            pendingBody[level] = view.body().id;
            pendingX[level] = x;
            pendingZ[level] = z;
            pendingTick[level] = ticks;
            ClientPacketDistributor.sendToServer(new TerrainTilePayload.Request(view.body().id, level, x, z));
        }
    }
}
