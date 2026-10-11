package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.ship.ChaseCamera;
import dev.apoapsis.client.ship.CockpitCamera;
import dev.apoapsis.ship.ShipEntity;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaternionf;
import org.slf4j.Logger;

/**
 * Records every rendered frame while a capture is active (driven by {@link TestDriverAscent}): one CSV row per frame goes
 * to run/screenshots/takeoff-frames.csv with the frame index, System.nanoTime, the partial tick, the main camera
 * position, the ship's and the player's interpolated positions, the ship's interpolated orientation, and the ship's raw
 * and old positions. The interpolations are the ones the renderer uses: Entity.getPosition(partialTick) and
 * ShipEntity.getRenderOrientation. Each frame is also grabbed at a quarter size into run/screenshots/rec-{mode}-NNNNN.png.
 *
 * <p>With {@code -Dapoapsis.record.frames=false} (or the environment variable {@code APOAPSIS_RECORD_FRAMES=false}) no
 * image is read back and only the CSV is written. The readback stalls the render thread to a few frames a second, which
 * changes how client ticks and server packets interleave, so the CSV of a frames-off run shows the jitter a player sees
 * at a normal frame rate. Measure it with tools/dev/render_position_residual.py.
 *
 * <p>Separately, in every scripted run, the {@link #WATCH_FRAMES} frames after each change of dimension are written to
 * the game log and to run/screenshots/transition-frames.csv: the camera, the cockpit eye that the ship's drawn pose
 * gives, and the distance between them. A seated pilot's camera must sit on the eye from the first frame in the new
 * level. Frames that CockpitCamera.holdFrame held back are marked, and left out of the worst distance. Unless
 * {@code -Dapoapsis.record.transitionShots=false}, the first frame after the switch that was really drawn is also saved
 * as jitter-transition-N-frameK.png.
 */
final class TestDriverRecord {
    private static final Logger LOG = LogUtils.getLogger();
    /** Screenshot downscale factor: a quarter size, about 400 x 225 at the test window size. */
    private static final int DOWNSCALE = 4;
    private static final String CSV = "takeoff-frames.csv";
    private static final String HEADER = "mode,frame,nanotime,partial_tick,cam_x,cam_y,cam_z,ship_x,ship_y,ship_z,"
            + "player_x,player_y,player_z,qx,qy,qz,qw,client_tick,ship_raw_x,ship_raw_y,ship_raw_z,"
            + "ship_old_x,ship_old_y,ship_old_z,srv_tick,srv_x,srv_y,srv_z,lag_ticks,newest_stamp,pre_nanotime\n";
    /** False when frames are not to be grabbed: only the CSV is written (property or environment variable). */
    private static final boolean GRAB_FRAMES = !"false".equalsIgnoreCase(
            System.getProperty("apoapsis.record.frames", System.getenv("APOAPSIS_RECORD_FRAMES")));

    private static final String TRANSITION_CSV = "transition-frames.csv";
    private static final String TRANSITION_HEADER = "transition,frame,nanotime,client_tick,dimension,riding,ships,"
            + "cam_x,cam_y,cam_z,eye_x,eye_y,eye_z,distance,held\n";
    /** Frames logged after each change of dimension. */
    private static final int WATCH_FRAMES = 40;
    private static final boolean TRANSITION_SHOTS = !"false".equalsIgnoreCase(
            System.getProperty("apoapsis.record.transitionShots", "true"));

    private static String mode;
    private static int frame;
    private static long firstNanos;
    private static long lastNanos;
    private static BufferedWriter csv;
    /** System.nanoTime at the start of the frame being drawn (RenderFrameEvent.Pre). */
    private static long preNanos;

    private static ResourceKey<Level> lastDimension;
    private static BufferedWriter transitionCsv;
    private static int transitions;
    /** Frames of the current transition logged so far; WATCH_FRAMES or more when none is being watched. */
    private static int watched = WATCH_FRAMES;
    private static boolean watching;
    private static double worstDistance;
    private static int worstFrame;
    private static int firstSeatedFrame;
    private static int heldFrames;
    private static boolean shotTaken;

    private TestDriverRecord() {}

    /** Hooks the per-frame grab into the render loop. Called once from {@link TestDriver#register()}. */
    static void register() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, e -> preNanos = System.nanoTime());
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post.class, TestDriverRecord::onFrame);
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post.class, TestDriverRecord::watchTransition);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, TestDriverRecord::onServerTick);
    }

    /** Starts a capture for {@code name} ("first" or "third"), ending any capture in progress. */
    static void begin(String name) {
        end();
        Minecraft mc = Minecraft.getInstance();
        try {
            Path path = mc.gameDirectory.toPath().resolve("screenshots").resolve(CSV);
            Files.createDirectories(path.getParent());
            boolean fresh = !Files.exists(path);
            csv = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (fresh) {
                csv.write(HEADER);
            }
        } catch (IOException e) {
            LOG.warn("[test driver] could not open the frame CSV", e);
            csv = null;
        }
        mode = name;
        frame = 0;
        firstNanos = 0;
        lastNanos = 0;
        LOG.info("[test driver] recording {} person", name);
    }

    /** Ends the capture in progress, if any, and reports its frame rate. */
    static void end() {
        if (mode != null && frame > 1 && lastNanos > firstNanos) {
            double seconds = (lastNanos - firstNanos) / 1e9;
            LOG.info("[test driver] recorded {} {} frames in {} s: {} fps during capture", frame, mode,
                    String.format("%.2f", seconds), String.format("%.1f", (frame - 1) / seconds));
        }
        mode = null;
        if (csv != null) {
            try {
                csv.close();
            } catch (IOException e) {
                LOG.warn("[test driver] could not close the frame CSV", e);
            }
            csv = null;
        }
        closeTransitionCsv();
    }

    private static void onFrame(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mode == null || mc.player == null || mc.level == null || csv == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = System.nanoTime();
        frame++;
        if (firstNanos == 0) {
            firstNanos = now;
        }
        lastNanos = now;
        if (GRAB_FRAMES) {
            Screenshot.grab(mc.gameDirectory, String.format("rec-%s-%05d.png", mode, frame),
                    mc.gameRenderer.mainRenderTarget(), DOWNSCALE, message -> { });
        }

        Vec3 cam = mc.gameRenderer.mainCamera().position();
        Vec3 player = mc.player.getPosition(partial);
        double[] ship = {Double.NaN, Double.NaN, Double.NaN};
        double[] q = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        double[] raw = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        double[] truth = serverTruth(mc);
        double lag = Double.NaN;
        int newest = -1;
        if (mc.player.getVehicle() instanceof ShipEntity s) {
            lag = s.smoothingLagTicks();
            newest = s.smoothingNewestTick();
            Vec3 sp = s.getPosition(partial);
            ship = new double[] {sp.x, sp.y, sp.z};
            raw = new double[] {s.getX(), s.getY(), s.getZ(), s.xo, s.yo, s.zo};
            Quaternionf o = s.getRenderOrientation(partial);
            q = new double[] {o.x, o.y, o.z, o.w};
        }
        try {
            csv.write(String.format("%s,%d,%d,%.4f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.6f,%.6f,%.6f,%.6f,"
                            + "%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.0f,%.5f,%.5f,%.5f,%.4f,%d,%d%n",
                    mode, frame, now, partial, cam.x, cam.y, cam.z, ship[0], ship[1], ship[2],
                    player.x, player.y, player.z, q[0], q[1], q[2], q[3], ClientState.clientTicks,
                    raw[0], raw[1], raw[2], raw[3], raw[4], raw[5], truth[0], truth[1], truth[2], truth[3], lag, newest, preNanos));
        } catch (IOException e) {
            LOG.warn("[test driver] frame CSV write failed", e);
        }
    }

    /** The server's own ship as of the end of its latest tick: {game time, x, y, z}. Written on the server thread. */
    private static volatile double[] truth = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};

    /** Server thread, end of each tick: the pilot's ship, with the tick it was last moved in. */
    private static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.getVehicle() instanceof ShipEntity s) {
                truth = new double[] {s.level().getGameTime(), s.getX(), s.getY(), s.getZ()};
                return;
            }
        }
    }

    /**
     * The integrated server's own ship: {game time, x, y, z}, NaN when the client does not ride one. This is the flight
     * the client is trying to draw, a staircase in wall-clock time but exact at each server tick, so the CSV can be
     * compared with it.
     */
    private static double[] serverTruth(Minecraft mc) {
        if (mc.player.getVehicle() instanceof ShipEntity) {
            return truth;
        }
        return new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    }

    // ---- the frames after a change of dimension ------------------------------------------------------------

    /**
     * Notices a change of the client level's dimension and logs the next {@link #WATCH_FRAMES} frames: where the camera
     * is, where the cockpit eye of the nearest ship is (the ship the player rides, or else the nearest one in the level),
     * and the distance. Both come from the pose the frame was drawn with (Entity.getPosition and getRenderOrientation).
     */
    private static void watchTransition(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ResourceKey<Level> dimension = mc.level.dimension();
        if (lastDimension != null && !dimension.equals(lastDimension)) {
            finishWatch();
            transitions++;
            watched = 0;
            watching = true;
            worstDistance = 0;
            worstFrame = 0;
            firstSeatedFrame = 0;
            heldFrames = 0;
            shotTaken = false;
            LOG.info("[test driver] transition {}: {} -> {}", transitions, lastDimension.identifier(), dimension.identifier());
        }
        lastDimension = dimension;
        if (watched >= WATCH_FRAMES) {
            return;
        }
        watched++;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = System.nanoTime();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        boolean riding = mc.player.getVehicle() instanceof ShipEntity;
        int ships = 0;
        ShipEntity nearest = riding ? (ShipEntity) mc.player.getVehicle() : null;
        double nearestSq = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ShipEntity s) {
                ships++;
                double d = s.getPosition(partial).distanceToSqr(cam);
                if (!riding && d < nearestSq) {
                    nearestSq = d;
                    nearest = s;
                }
            }
        }
        double distance = Double.NaN;
        Vec3 eye = null;
        if (nearest != null) {
            eye = nearest.cockpitEye(nearest.getPosition(partial), nearest.getRenderOrientation(partial),
                    mc.player.getEyeHeight());
            distance = eye.distanceTo(cam);
            if (!mc.options.getCameraType().isFirstPerson() && ChaseCamera.shownDistance() >= 0) {
                // The chase camera sits that far behind the eye on purpose: what must not move is the gap.
                distance = Math.abs(distance - ChaseCamera.shownDistance());
            }
        }
        if (riding && firstSeatedFrame == 0) {
            firstSeatedFrame = watched;
        }
        // A held frame drew nothing new: the screen kept the last good frame, so its camera is never seen.
        boolean held = CockpitCamera.wasFrameHeld();
        if (held) {
            heldFrames++;
        } else if (!Double.isNaN(distance) && distance > worstDistance) {
            worstDistance = distance;
            worstFrame = watched;
        }
        LOG.info("[test driver] transition {} frame {}: held {} riding {} ships {} camera {} eye {} distance {}",
                transitions, watched, held, riding, ships, fmt(cam), eye == null ? "none" : fmt(eye),
                Double.isNaN(distance) ? "n/a" : String.format("%.3f", distance));
        writeTransitionRow(mc, now, riding, ships, cam, eye, distance, held);
        if (TRANSITION_SHOTS && !held && !shotTaken) {
            shotTaken = true;
            Screenshot.grab(mc.gameDirectory, "jitter-transition-" + transitions + "-frame" + watched + ".png",
                    mc.gameRenderer.mainRenderTarget(), 1, message -> { });
        }
        if (watched == WATCH_FRAMES) {
            finishWatch();
        }
    }

    private static void finishWatch() {
        if (!watching) {
            return;
        }
        watching = false;
        LOG.info("[test driver] transition {}: {} frames ({} held), worst camera-to-eye distance over the drawn ones {} "
                        + "at frame {}, seated from frame {}",
                transitions, watched, heldFrames, String.format("%.3f", worstDistance), worstFrame,
                firstSeatedFrame == 0 ? "never" : String.valueOf(firstSeatedFrame));
        watched = WATCH_FRAMES;
    }

    private static void writeTransitionRow(Minecraft mc, long now, boolean riding, int ships, Vec3 cam, Vec3 eye,
                                           double distance, boolean held) {
        try {
            if (transitionCsv == null) {
                Path path = mc.gameDirectory.toPath().resolve("screenshots").resolve(TRANSITION_CSV);
                Files.createDirectories(path.getParent());
                boolean fresh = !Files.exists(path);
                transitionCsv = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                if (fresh) {
                    transitionCsv.write(TRANSITION_HEADER);
                }
            }
            Vec3 e = eye == null ? new Vec3(Double.NaN, Double.NaN, Double.NaN) : eye;
            transitionCsv.write(String.format("%d,%d,%d,%d,%s,%b,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%b%n", transitions,
                    watched, now, ClientState.clientTicks, mc.level.dimension().identifier(), riding, ships,
                    cam.x, cam.y, cam.z, e.x, e.y, e.z, distance, held));
            transitionCsv.flush();
        } catch (IOException ex) {
            LOG.warn("[test driver] transition CSV write failed", ex);
        }
    }

    private static void closeTransitionCsv() {
        if (transitionCsv != null) {
            try {
                transitionCsv.close();
            } catch (IOException e) {
                LOG.warn("[test driver] could not close the transition CSV", e);
            }
            transitionCsv = null;
        }
    }

    private static String fmt(Vec3 v) {
        return String.format("(%.2f, %.2f, %.2f)", v.x, v.y, v.z);
    }
}
