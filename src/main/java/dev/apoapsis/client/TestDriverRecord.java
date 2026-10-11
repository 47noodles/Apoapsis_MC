package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.ship.ShipRenderCache;
import dev.apoapsis.ship.ShipEntity;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.slf4j.Logger;

/**
 * Records every rendered frame while a capture is active (driven by {@link TestDriverAscent}): each frame is grabbed at
 * a quarter size into run/screenshots/rec-{mode}-NNNNN.png, and one CSV row per frame goes to
 * run/screenshots/takeoff-frames.csv with the frame index, System.nanoTime, the partial tick, the main camera position,
 * the ship's and the player's interpolated positions, and the ship's interpolated orientation. The interpolations are the
 * ones the renderer uses: Entity.getPosition(partialTick) and ShipRenderCache's orientation.
 */
final class TestDriverRecord {
    private static final Logger LOG = LogUtils.getLogger();
    /** Screenshot downscale factor: a quarter size, about 400 x 225 at the test window size. */
    private static final int DOWNSCALE = 4;
    private static final String CSV = "takeoff-frames.csv";
    private static final String HEADER = "mode,frame,nanotime,partial_tick,cam_x,cam_y,cam_z,ship_x,ship_y,ship_z,"
            + "player_x,player_y,player_z,qx,qy,qz,qw\n";

    private static String mode;
    private static int frame;
    private static long firstNanos;
    private static long lastNanos;
    private static BufferedWriter csv;

    private TestDriverRecord() {}

    /** Hooks the per-frame grab into the render loop. Called once from {@link TestDriver#register()}. */
    static void register() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post.class, TestDriverRecord::onFrame);
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
        Screenshot.grab(mc.gameDirectory, String.format("rec-%s-%05d.png", mode, frame),
                mc.gameRenderer.mainRenderTarget(), DOWNSCALE, message -> { });

        Vec3 cam = mc.gameRenderer.mainCamera().position();
        Vec3 player = mc.player.getPosition(partial);
        double[] ship = {Double.NaN, Double.NaN, Double.NaN};
        double[] q = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        if (mc.player.getVehicle() instanceof ShipEntity s) {
            Vec3 sp = s.getPosition(partial);
            ship = new double[] {sp.x, sp.y, sp.z};
            Quaternionf o = ShipRenderCache.entry(s.getId()).orientation(s.getOrientation(), partial);
            q = new double[] {o.x, o.y, o.z, o.w};
        }
        try {
            csv.write(String.format("%s,%d,%d,%.4f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.6f,%.6f,%.6f,%.6f%n",
                    mode, frame, now, partial, cam.x, cam.y, cam.z, ship[0], ship[1], ship[2],
                    player.x, player.y, player.z, q[0], q[1], q[2], q[3]));
        } catch (IOException e) {
            LOG.warn("[test driver] frame CSV write failed", e);
        }
    }
}
