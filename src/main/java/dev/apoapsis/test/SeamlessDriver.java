package dev.apoapsis.test;

import com.mojang.logging.LogUtils;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.terrain.FarTerrainClient;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

/**
 * Runs a script of client steps for the far-terrain screenshots and frame-cost runs. Inert unless the file
 * {@code apoapsis-seamless.txt} exists in the game directory. One step per line:
 * <pre>
 * wait N            wait N client ticks
 * cmd TEXT          send a chat command (no slash)
 * shot NAME         screenshot to screenshots/NAME, logging the ground point and altitude
 * tpsub BODY ALT YAW PITCH   go to BODY's surface dimension at ALT metres above sea level over the last ground point
 * camera first|third
 * gui on|off        show or hide the HUD
 * farterrain on|off switch the far terrain
 * perf N            average frame time over N ticks, with the far terrain's own CPU time, logged
 * log TEXT          write a line to the log
 * quit              stop the game
 * </pre>
 * Used with the testFlight run, whose world has the Pale and Verdance dimensions and operator commands.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class SeamlessDriver {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String SCRIPT = "apoapsis-seamless.txt";
    /** Ticks in the world before the script starts, so the test-flight set-up has finished. */
    private static final int START_DELAY = 160;

    private static List<String> steps;
    private static int next;
    private static int waitTicks;
    private static int inWorld;
    private static boolean loaded;
    /** Frame timing for a perf step: ticks left, frames and start time (ns). */
    private static int perfTicks;
    private static String perfLabel;
    private static long perfStart;
    private static int perfFrames;

    private SeamlessDriver() {}

    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Pre event) {
        if (perfTicks > 0) {
            perfFrames++;
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!loaded) {
            loaded = true;
            Path file = mc.gameDirectory.toPath().resolve(SCRIPT);
            if (!Files.isRegularFile(file)) {
                return;
            }
            try {
                steps = new ArrayList<>();
                for (String line : Files.readAllLines(file)) {
                    if (!line.isBlank() && !line.startsWith("#")) {
                        steps.add(line.strip());
                    }
                }
            } catch (IOException e) {
                LOG.warn("[seamless] cannot read {}", file, e);
                steps = null;
                return;
            }
            mc.options.pauseOnLostFocus = false;
            // Without input for a minute the game drops to 30 fps, which would hide the frame-cost differences.
            mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            LOG.info("[seamless] script with {} steps", steps.size());
        }
        if (steps == null || ++inWorld < START_DELAY) {
            return;
        }
        if (perfTicks > 0) {
            if (--perfTicks == 0) {
                double ms = (System.nanoTime() - perfStart) / 1e6 / Math.max(1, perfFrames);
                LOG.info("[seamless] perf {}: {} ms per frame ({} fps) over {} frames; far terrain CPU {} ms per frame",
                        perfLabel, String.format("%.2f", ms), String.format("%.0f", 1000 / ms), perfFrames,
                        String.format("%.3f", FarTerrainClient.cpuMsPerFrame()));
            }
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (next < steps.size() && waitTicks == 0 && perfTicks == 0) {
            run(mc, steps.get(next++));
        }
    }

    /** Teleports the player to the surface of a body at the ground point the camera was last over (orbit side shots). */
    private static void tpSub(Minecraft mc, String arg) {
        String[] p = arg.split("\\s+");
        double[] ground = FarTerrainClient.groundPoint();
        if (ground == null || p.length < 4) {
            LOG.warn("[seamless] tpsub '{}': no ground point ({})", arg, FarTerrainClient.describeView());
            return;
        }
        BodyId body = "pale".equals(p[0]) ? BodyId.PALE : BodyId.VERDANCE;
        String dimension = body == BodyId.PALE ? "apoapsis:pale" : "minecraft:overworld";
        double y = FlightDimensions.seaLevel(body) + Double.parseDouble(p[1]);
        LOG.info("[seamless] tpsub to {} x={} y={} z={}", body.id, Math.round(ground[0]), Math.round(y),
                Math.round(ground[1]));
        mc.player.connection.sendCommand(String.format(Locale.ROOT, "execute in %s run tp @s %.1f %.1f %.1f %s %s",
                dimension, ground[0], y, ground[1], p[2], p[3]));
    }

    private static void run(Minecraft mc, String line) {
        String[] parts = line.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1] : "";
        switch (parts[0]) {
            case "wait" -> waitTicks = Integer.parseInt(arg);
            case "cmd" -> mc.player.connection.sendCommand(arg);
            case "shot" -> {
                LOG.info("[seamless] shot {} over {}", arg, FarTerrainClient.describeView());
                Screenshot.grab(mc.gameDirectory, arg, mc.gameRenderer.mainRenderTarget(), 1,
                        message -> LOG.info("[seamless] screenshot {}: {}", arg, message.getString()));
            }
            case "tpsub" -> tpSub(mc, arg);
            case "look" -> {
                String[] a = arg.split("\\s+");
                dev.apoapsis.client.ship.CockpitCamera.turn(Double.parseDouble(a[0]), Double.parseDouble(a[1]));
            }
            case "camera" -> mc.options.setCameraType(
                    "third".equals(arg) ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
            case "gui" -> {
                if (mc.gui.hud.isHidden() != "off".equals(arg)) {
                    mc.gui.hud.toggle();
                }
            }
            case "farterrain" -> FarTerrainClient.enabled = "on".equals(arg);
            case "vsync" -> {
                mc.options.enableVsync().set("on".equals(arg));
                mc.options.framerateLimit().set("on".equals(arg) ? 60 : 260);
            }
            case "perf" -> {
                String[] p = arg.split("\\s+", 2);
                perfTicks = Integer.parseInt(p[0]);
                perfLabel = p.length > 1 ? p[1] : "";
                perfFrames = 0;
                perfStart = System.nanoTime();
                FarTerrainClient.cpuMsPerFrame();
            }
            case "debug" -> FarTerrainClient.debug = "on".equals(arg);
            case "log" -> LOG.info("[seamless] {}", arg);
            case "quit" -> {
                LOG.info("[seamless] done");
                mc.stop();
            }
            default -> LOG.warn("[seamless] unknown step '{}'", line);
        }
    }
}
