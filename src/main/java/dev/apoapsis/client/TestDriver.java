package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.logOrbit;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;

import com.mojang.logging.LogUtils;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * A scripted client run for automated checks (plan item 0.23, first slice). Active only with the JVM property
 * {@code apoapsis.testDriver}. {@code true} runs the short flight-and-map script: once the player sits in a ship it
 * takes screenshots of the flight view, the map and a short engine burn into run/screenshots, logs the orbit before and
 * after the burn, then quits the game. The other values choose a named script: {@code transfer} (the Pale transfer),
 * {@code ascent} (a launch from the pad), {@code pale} (the transfer, then a landing on Pale) and {@code land} (a drop
 * from 1,200 m under parachutes).
 */
public final class TestDriver {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int NO_SHIP_TIMEOUT = 1200;
    /** Ticks without a ship before a named script is told the pilot has left (two seconds, past any level change). */
    private static final int LEFT_SHIP_TICKS = 40;

    private static int inWorldTicks;
    private static int pilotTicks;
    private static int leftShipTicks;
    private static boolean done;

    private TestDriver() {}

    public static boolean enabled() {
        String mode = System.getProperty("apoapsis.testDriver");
        return mode != null && !mode.equals("false");
    }

    /** The named script for the property value, or null for the short flight-and-map script. */
    private static final TestDriverSupport.Script SCRIPT = script(System.getProperty("apoapsis.testDriver"));

    private static TestDriverSupport.Script script(String mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case "transfer" -> new TestDriverTransfer();
            case "ascent" -> new TestDriverAscent();
            case "pale" -> new TestDriverPale();
            case "land" -> new TestDriverLand();
            case "map" -> new TestDriverMap();
            default -> null;
        };
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick(Minecraft.getInstance()));
        TestDriverRecord.register();
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
            if (SCRIPT != null && pilotTicks > 0) {
                // A crash or ejection ends a named script: it is told once the pilot has been gone for a while.
                if (++leftShipTicks == LEFT_SHIP_TICKS) {
                    SCRIPT.tick(mc, null);
                    finish(mc);
                }
                return;
            }
            if (inWorldTicks == NO_SHIP_TIMEOUT) {
                shot(mc, "apoapsis-00-no-ship.png");
                LOG.warn("[test driver] the player was not in a ship after {} ticks", NO_SHIP_TIMEOUT);
                finish(mc);
            }
            return;
        }
        leftShipTicks = 0;
        pilotTicks++;
        if (SCRIPT != null) {
            if (!SCRIPT.tick(mc, ship)) {
                finish(mc);
            }
            return;
        }
        // Throttle commands are sent on every tick of their window, so one lost packet cannot skip the burn.
        if (pilotTicks >= 240 && pilotTicks < 340) {
            send(ship, (byte) 0, (byte) 0, ShipInputPayload.THROTTLE_FULL, (byte) 0);
        } else if (pilotTicks >= 340 && pilotTicks < 350) {
            send(ship, (byte) 0, (byte) 0, ShipInputPayload.THROTTLE_CUT, (byte) 0);
        }
        switch (pilotTicks) {
            case 20 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 100 -> {
                logOrbitNow("before burn");
                shot(mc, "apoapsis-01-flight.png");
            }
            case 120 -> MapClient.openMap();
            case 200 -> shot(mc, "apoapsis-02-map.png");
            case 220 -> mc.gui.setScreen(null);
            case 300 -> shot(mc, "apoapsis-03-burn.png");
            case 340 -> logOrbitNow("after a 5 s burn");
            case 400 -> {
                MapClient.openMap();
            }
            case 460 -> shot(mc, "apoapsis-04-map-after-burn.png");
            case 480 -> finish(mc);
            default -> { }
        }
    }

    private static void logOrbitNow(String when) {
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            LOG.warn("[test driver] {}: no orbit state received", when);
            return;
        }
        logOrbit(p, when);
    }

    private static void finish(Minecraft mc) {
        done = true;
        TestDriverRecord.end();
        mc.gui.setScreen(null);
        LOG.info("[test driver] finished; stopping the game");
        mc.stop();
    }
}
