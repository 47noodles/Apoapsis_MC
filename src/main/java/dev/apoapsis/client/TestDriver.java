package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * A scripted client run for automated checks (plan item 0.23, first slice). Active only with the JVM property
 * {@code apoapsis.testDriver=true} (the gradle run {@code testDriver}). Once the player sits in a ship it takes
 * screenshots of the flight view, the map and a short engine burn into run/screenshots, logs the orbit before and
 * after the burn, then quits the game.
 */
public final class TestDriver {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int NO_SHIP_TIMEOUT = 1200;

    private static int inWorldTicks;
    private static int pilotTicks;
    private static boolean done;

    private TestDriver() {}

    public static boolean enabled() {
        return Boolean.getBoolean("apoapsis.testDriver");
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick(Minecraft.getInstance()));
    }

    private static void tick(Minecraft mc) {
        if (done || mc.player == null || mc.level == null) {
            return;
        }
        if (inWorldTicks++ == 0) {
            // The window may sit behind others on a test machine: never pause, and skip the vanilla tutorial toasts.
            mc.options.pauseOnLostFocus = false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        }
        if (!(mc.player.getVehicle() instanceof ShipEntity ship)) {
            if (inWorldTicks == NO_SHIP_TIMEOUT) {
                shot(mc, "apoapsis-00-no-ship.png");
                LOG.warn("[test driver] the player was not in a ship after {} ticks", NO_SHIP_TIMEOUT);
                finish(mc);
            }
            return;
        }
        pilotTicks++;
        switch (pilotTicks) {
            case 20 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 100 -> {
                logOrbit("before burn");
                shot(mc, "apoapsis-01-flight.png");
            }
            case 120 -> MapClient.openMap();
            case 200 -> shot(mc, "apoapsis-02-map.png");
            case 220 -> mc.gui.setScreen(null);
            case 240 -> send(ship, ShipInputPayload.THROTTLE_FULL);
            case 300 -> shot(mc, "apoapsis-03-burn.png");
            case 340 -> {
                send(ship, ShipInputPayload.THROTTLE_CUT);
                logOrbit("after a 5 s burn");
            }
            case 400 -> {
                MapClient.openMap();
            }
            case 460 -> shot(mc, "apoapsis-04-map-after-burn.png");
            case 480 -> finish(mc);
            default -> { }
        }
    }

    private static void send(ShipEntity ship, byte throttle) {
        ClientPacketDistributor.sendToServer(new ShipInputPayload(ship.getId(), (byte) 0, (byte) 0, (byte) 0, throttle, (byte) 0));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1,
                message -> LOG.info("[test driver] screenshot {}: {}", name, message.getString()));
    }

    private static void logOrbit(String when) {
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            LOG.warn("[test driver] {}: no orbit state received", when);
            return;
        }
        CelestialBody body = BodyId.byOrdinal(p.body()) == BodyId.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        Elements e = Elements.of(body.mu, s);
        LOG.info("[test driver] {}: body {} regime {} alt {} km speed {} m/s Ap {} km Pe {} km period {} s",
                when, body.id, p.regime(), round((s.r().length() - body.radius) / 1000),
                round(s.v().length()), round((e.apoapsisRadius() - body.radius) / 1000),
                round((e.periapsisRadius() - body.radius) / 1000), round(e.period()));
    }

    private static String round(double v) {
        return String.format("%.1f", v);
    }

    private static void finish(Minecraft mc) {
        done = true;
        mc.gui.setScreen(null);
        LOG.info("[test driver] finished; stopping the game");
        mc.stop();
    }
}
