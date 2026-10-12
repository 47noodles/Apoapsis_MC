package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.send;

import com.mojang.logging.LogUtils;
import dev.apoapsis.client.map.MapScript;
import dev.apoapsis.client.map3d.Map3DScript;
import dev.apoapsis.client.sky.PlanetTextures;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;

/**
 * The map screenshot script for {@link TestDriver} ({@code apoapsis.testDriver=mapshots}): from the test save's 16 km
 * orbit it takes the 3D and 2D maps zoomed out over the system, on Verdance with the ship's orbit and on Pale, at
 * 1920x1080 and 1280x720; then it burns to a Pale transfer and takes the encounter views, the 3D one with a maneuver node.
 * Files are {@code imm-map-<tag>-<map>-<view>-<w>x<h>.png}, the tag from the environment variable
 * {@code APOAPSIS_MAP_TAG} (default {@code after}). Frame times with each map open are logged. Test tooling only.
 */
final class TestDriverMapShots implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int[][] SIZES = {{1920, 1080}, {1280, 720}};
    private static final double SYSTEM_3D_M = 2.3 * MeridianSystem.PALE.orbitRadius;
    private static final double SYSTEM_2D_M = 1.25 * MeridianSystem.PALE.orbitRadius;
    private static final String TAG = System.getenv("APOAPSIS_MAP_TAG") == null ? "after" : System.getenv("APOAPSIS_MAP_TAG");

    private enum Phase { SETTLE, SHOTS_A, WAIT_WINDOW, ALIGN, BURN, SHOTS_B, DONE }

    private Phase phase = Phase.SETTLE;
    private int phaseTicks;
    private int totalTicks;
    private boolean sized;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepWait;

    private long frameStart;
    private final List<Double> frames = new ArrayList<>();
    private boolean measuring;
    private static boolean listening;

    private record Step(int delay, Runnable action) {}

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            LOG.warn("[test driver] mapshots: the pilot left the ship during {}", phase);
            return false;
        }
        phaseTicks++;
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, e -> frameStart = System.nanoTime());
            NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post.class, e -> {
                if (measuring) {
                    frames.add((System.nanoTime() - frameStart) / 1e6);
                }
            });
        }
        if (++totalTicks == 20) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            return true;
        }
        byte pitch = 0, yaw = 0, throttle = ShipInputPayload.THROTTLE_HOLD, flags = 0;
        switch (phase) {
            case SETTLE -> {
                boolean maps = PlanetTextures.texture("verdance") != null && PlanetTextures.texture("pale") != null;
                if ((maps && phaseTicks >= 300) || phaseTicks >= 1800) {
                    LOG.info("[test driver] mapshots: planet maps {}", maps ? "present" : "MISSING");
                    buildShotsA(mc);
                    next(Phase.SHOTS_A);
                }
            }
            case SHOTS_A -> {
                if (runSteps(mc)) {
                    next(Phase.WAIT_WINDOW);
                }
            }
            case WAIT_WINDOW -> {
                double err = windowError(p);
                if (err > 8 && p.warp() < 10 && phaseTicks % 10 == 0) {
                    flags = ShipInputPayload.FLAG_WARP_UP;
                } else if (err <= 8 && p.warp() > 1 && phaseTicks % 10 == 0) {
                    flags = ShipInputPayload.FLAG_WARP_DOWN;
                } else if (err <= 8 && p.warp() <= 1) {
                    next(Phase.ALIGN);
                }
                if (phaseTicks == 1) {
                    mc.gui.setScreen(null);
                }
            }
            case ALIGN, BURN -> {
                double[] steer = TestDriverSupport.steer(ship, p, +1);
                pitch = (byte) steer[0];
                yaw = (byte) steer[1];
                if (phase == Phase.ALIGN) {
                    if ((steer[2] < 3 && windowError(p) < 1.5) || phaseTicks > 1200) {
                        next(Phase.BURN);
                        throttle = ShipInputPayload.THROTTLE_FULL;
                    }
                } else {
                    throttle = ShipInputPayload.THROTTLE_FULL;
                    if ((phaseTicks % 5 == 0 && encounterPredicted(p)) || phaseTicks > 400) {
                        throttle = ShipInputPayload.THROTTLE_CUT;
                        buildShotsB(mc);
                        next(Phase.SHOTS_B);
                    }
                }
            }
            case SHOTS_B -> {
                if (phaseTicks <= 10) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                }
                if (runSteps(mc)) {
                    next(Phase.DONE);
                }
            }
            case DONE -> {
                return false;
            }
        }
        send(ship, pitch, yaw, throttle, flags);
        return true;
    }

    private void next(Phase to) {
        LOG.info("[test driver] mapshots {} -> {}", phase, to);
        phase = to;
        phaseTicks = 0;
    }

    private void add(int delay, Runnable r) {
        steps.add(new Step(delay, r));
    }

    /** Runs the queued steps, each after its delay in ticks. True when the queue is empty. */
    private boolean runSteps(Minecraft mc) {
        if (stepIndex >= steps.size()) {
            return true;
        }
        if (stepWait++ >= steps.get(stepIndex).delay()) {
            steps.get(stepIndex).action().run();
            stepIndex++;
            stepWait = 0;
        }
        return stepIndex >= steps.size();
    }

    private void size(Minecraft mc, int w, int h) {
        add(10, () -> SDLVideo.SDL_SetWindowSize(mc.getWindow().handle(), w, h));
        add(30, () -> { });
    }

    private void shot(Minecraft mc, String map, String view) {
        add(5, () -> { });
        add(60, () -> {
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            String name = String.format(Locale.ROOT, "imm-map-%s-%s-%s-%dx%d.png", TAG, map, view, w, h);
            Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, m -> { });
            LOG.info("[test driver] mapshots: {}", name);
        });
    }

    private void measure(String label) {
        add(1, () -> {
            frames.clear();
            measuring = true;
        });
        add(120, () -> {
            measuring = false;
            if (frames.size() > 10) {
                List<Double> sorted = new ArrayList<>(frames);
                sorted.sort(Double::compare);
                double mean = frames.stream().mapToDouble(d -> d).average().orElse(0);
                LOG.info("[test driver] mapshots frame time {}: mean {} ms, p95 {} ms ({} frames)", label,
                        String.format(Locale.ROOT, "%.2f", mean),
                        String.format(Locale.ROOT, "%.2f", sorted.get((int) (sorted.size() * 0.95))), frames.size());
            }
        });
    }

    private static void ensure(Minecraft mc, boolean want3d) {
        if (mc.gui.screen() == null) {
            MapClient.openMap();
        }
        boolean is3d = mc.gui.screen() instanceof dev.apoapsis.client.map3d.Map3DScreen;
        if (is3d != want3d) {
            MapClient.switchMap();
        }
    }

    private void open3d(Minecraft mc) {
        add(1, () -> ensure(mc, true));
        add(10, () -> { });
    }

    private void open2d(Minecraft mc) {
        add(1, () -> ensure(mc, false));
        add(10, () -> { });
    }

    private void buildShotsA(Minecraft mc) {
        for (int[] s : SIZES) {
            size(mc, s[0], s[1]);
            open3d(mc);
            add(1, () -> Map3DScript.focus(0, SYSTEM_3D_M));
            add(40, () -> { });
            shot(mc, "3d", "system");
            measure("3d system " + s[0]);
            add(1, () -> Map3DScript.focus(2, 3 * 16000 + 64000 * 2.5));
            add(60, () -> { });
            shot(mc, "3d", "verdance-ship");
            measure("3d verdance " + s[0]);
            add(1, () -> Map3DScript.focus(1, 3 * MeridianSystem.PALE.soiRadius()));
            add(60, () -> { });
            shot(mc, "3d", "pale");
            open2d(mc);
            add(1, () -> MapScript.focus(0, SYSTEM_2D_M));
            shot(mc, "2d", "system");
            measure("2d system " + s[0]);
            add(1, () -> MapScript.focus(0, 104000));
            shot(mc, "2d", "verdance-ship");
            measure("2d verdance " + s[0]);
            add(1, () -> MapScript.focus(1, MeridianSystem.PALE.soiRadius() * 1.1));
            shot(mc, "2d", "pale");
        }
    }

    private void buildShotsB(Minecraft mc) {
        for (int[] s : SIZES) {
            size(mc, s[0], s[1]);
            open3d(mc);
            add(1, () -> Map3DScript.focus(0, SYSTEM_3D_M));
            add(40, () -> {
                for (double ahead : new double[] {500, 300, 800, 200, 1200, 120, 1800, 60}) {
                    if (Map3DScript.clickOrbit(ahead)) {
                        LOG.info("[test driver] mapshots: node placed {} s ahead", ahead);
                        break;
                    }
                }
            });
            add(40, () -> { });
            shot(mc, "3d", "encounter-node");
            measure("3d encounter " + s[0]);
            add(1, () -> Map3DScript.rightClickNode());
            add(20, () -> { });
            open2d(mc);
            add(1, () -> MapScript.focus(0, SYSTEM_2D_M));
            shot(mc, "2d", "encounter");
            measure("2d encounter " + s[0]);
        }
    }

    /** Degrees between Pale's current lead over the ship and the Hohmann lead (around Verdance only). */
    private static double windowError(OrbitStatePayload p) {
        if (BodyId.byOrdinal(p.body()) != BodyId.VERDANCE) {
            return 0;
        }
        double mu = MeridianSystem.VERDANCE.mu;
        double r1 = Math.hypot(p.rx(), p.ry());
        double r2 = MeridianSystem.PALE.orbitRadius;
        double a = (r1 + r2) / 2;
        double transfer = Math.PI * Math.sqrt(a * a * a / mu);
        double leadAtBurn = Math.PI - 2 * Math.PI * transfer / MeridianSystem.PALE.orbitPeriod();
        StateVector pale = MeridianSystem.PALE.stateAt(p.ut());
        double lead = Math.atan2(pale.r().y(), pale.r().x()) - Math.atan2(p.ry(), p.rx());
        double diff = Math.IEEEremainder(lead - leadAtBurn, 2 * Math.PI);
        return Math.toDegrees(Math.abs(diff)) * (diff < 0 ? 10 : 1);
    }

    private static boolean encounterPredicted(OrbitStatePayload p) {
        CelestialBody body = BodyId.byOrdinal(p.body()) == BodyId.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        List<Patch> patches = TrajectoryPredictor.predict(body, s, p.ut(), 6 * 3600, 3);
        return patches.stream().anyMatch(pt -> pt.end() == Patch.EndReason.ENCOUNTER && pt.endUt() - p.ut() < 2000);
    }
}
