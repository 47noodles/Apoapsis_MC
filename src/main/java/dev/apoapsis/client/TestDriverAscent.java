package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.altitudeM;
import static dev.apoapsis.client.TestDriverSupport.fmt;
import static dev.apoapsis.client.TestDriverSupport.inOrbit;
import static dev.apoapsis.client.TestDriverSupport.logState;
import static dev.apoapsis.client.TestDriverSupport.sasTo;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;

import com.mojang.logging.LogUtils;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * The ascent script for {@link TestDriver} ({@code apoapsis.testDriver=ascent}, the testAscent run, started on the pad):
 * settle, first person, SAS HOLD, then full throttle straight up. Shots at 20, 200 (third person), 300, 1,000, 1,500 and
 * 1,900 m, then one just after the orbit transition, where the throttle is cut and the game quits. Gives up after six
 * minutes.
 */
final class TestDriverAscent implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int TIMEOUT_TICKS = 6 * 60 * 20;
    private static final int SETTLE_TICKS = 60;
    private static final int SAS_TIMEOUT_TICKS = 200;
    /** Altitude (m) at which each climb shot is taken, its file, and its camera. */
    private static final double[] SHOT_M;
    private static final String[] SHOT_FILE;
    private static final CameraType[] SHOT_CAMERA;

    static {
        // -Dapoapsis.climbShots (or APOAPSIS_CLIMB_SHOTS)=15t,60t,500f,... takes one shot per altitude, first (f) or third (t) person (effects checks).
        String custom = System.getProperty("apoapsis.climbShots", System.getenv().getOrDefault("APOAPSIS_CLIMB_SHOTS", ""));
        if (custom.isBlank()) {
            SHOT_M = new double[] {20, 200, 300, 1000, 1500, 1900};
            SHOT_FILE = new String[] {
                    "ascent-01-20m-first-person.png", "ascent-02-200m-third-person.png", "ascent-03-300m-first-person.png",
                    "ascent-04-1000m-first-person.png", "ascent-05-1500m-first-person.png",
                    "ascent-06-1900m-first-person.png"};
            SHOT_CAMERA = new CameraType[] {
                    CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK, CameraType.FIRST_PERSON, CameraType.FIRST_PERSON,
                    CameraType.FIRST_PERSON, CameraType.FIRST_PERSON};
        } else {
            // Each entry is an altitude with f (first person) or t (third person): 15f,60t,500f,...
            String[] parts = custom.split(",");
            SHOT_M = new double[parts.length];
            SHOT_FILE = new String[parts.length];
            SHOT_CAMERA = new CameraType[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                boolean third = part.endsWith("t");
                SHOT_M[i] = Double.parseDouble(part.substring(0, part.length() - 1));
                SHOT_FILE[i] = "climb-" + part.substring(0, part.length() - 1) + "m-" + (third ? "third" : "first") + ".png";
                SHOT_CAMERA[i] = third ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON;
            }
        }
    }

    /** Ticks between switching the camera and the shot, so the new view is drawn before it is grabbed. */
    private static final int CAMERA_LEAD_TICKS = 5;
    private static final int AFTER_ORBIT_SHOT_TICK = 20;
    private static final int AFTER_ORBIT_END_TICK = 30;

    /** With {@code apoapsis.testRecord=true}: 10 s first person, then 10 s third person, every render frame captured. */
    private static final boolean RECORD = Boolean.getBoolean("apoapsis.testRecord");
    private static final int RECORD_TICKS = 10 * 20;

    private enum Phase { SETTLE, SAS, CLIMB, AFTER_ORBIT, RECORD_FIRST, RECORD_THIRD, DONE }

    private Phase phase = Phase.SETTLE;
    private int phaseTicks;
    private int totalTicks;
    private int nextShot;
    private int shotAt = -1;
    private double lastAlt;

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            LOG.warn("[test driver] ascent: the pilot left the ship at {} m, in {}", fmt(lastAlt), phase);
            return false;
        }
        totalTicks++;
        phaseTicks++;
        OrbitStatePayload p = ClientState.orbit;
        if (p == null) {
            return true;
        }
        if (totalTicks > TIMEOUT_TICKS) {
            LOG.warn("[test driver] ascent: no orbit transition after 6 minutes; last altitude {} m", fmt(lastAlt));
            return false;
        }
        if (!inOrbit(p)) {
            lastAlt = altitudeM(ship, p);
        }
        byte throttle = ShipInputPayload.THROTTLE_HOLD;
        switch (phase) {
            case SETTLE -> {
                if (phaseTicks >= SETTLE_TICKS) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    next("settled, first person");
                }
            }
            case SAS -> {
                if (sasTo(ship, SasMode.HOLD, phaseTicks)) {
                    next("SAS HOLD");
                } else if (phaseTicks > SAS_TIMEOUT_TICKS) {
                    next("SAS still " + ship.getSasMode() + ", climbing anyway");
                }
            }
            case CLIMB -> {
                throttle = ShipInputPayload.THROTTLE_FULL;
                if (inOrbit(p)) {
                    LOG.info("[test driver] ascent: orbit transition, last surface altitude {} m", fmt(lastAlt));
                    next("orbit transition");
                    throttle = ShipInputPayload.THROTTLE_CUT;
                } else {
                    climbShots(mc, ship, p);
                }
            }
            case AFTER_ORBIT -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                if (phaseTicks == AFTER_ORBIT_SHOT_TICK) {
                    shot(mc, "ascent-07-after-transition.png");
                    logState(ship, p, "shot ascent-07-after-transition.png");
                }
                if (phaseTicks >= AFTER_ORBIT_END_TICK) {
                    logState(ship, p, "throttle cut, finishing");
                    next("done");
                }
            }
            case RECORD_FIRST -> {
                throttle = inOrbit(p) ? ShipInputPayload.THROTTLE_CUT : ShipInputPayload.THROTTLE_FULL;
                if (phaseTicks >= RECORD_TICKS) {
                    next("first person recorded");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
            }
            case RECORD_THIRD -> {
                // Full throttle until the orbit transition; past it the throttle is cut, or the ship would fall back.
                throttle = inOrbit(p) ? ShipInputPayload.THROTTLE_CUT : ShipInputPayload.THROTTLE_FULL;
                if (phaseTicks >= RECORD_TICKS) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    next("third person recorded, finishing");
                }
            }
            case DONE -> {
                return false;
            }
        }
        send(ship, (byte) 0, (byte) 0, throttle, (byte) 0);
        return true;
    }

    /** Switches the camera, then takes each climb shot a few ticks later, in altitude order. */
    private void climbShots(Minecraft mc, ShipEntity ship, OrbitStatePayload p) {
        if (shotAt < 0 && nextShot < SHOT_M.length && lastAlt >= SHOT_M[nextShot]) {
            mc.options.setCameraType(SHOT_CAMERA[nextShot]);
            shotAt = phaseTicks + CAMERA_LEAD_TICKS;
        }
        if (shotAt >= 0 && phaseTicks >= shotAt) {
            shot(mc, SHOT_FILE[nextShot]);
            logState(ship, p, "shot " + SHOT_FILE[nextShot]);
            nextShot++;
            shotAt = -1;
        }
    }

    private void next(String why) {
        LOG.info("[test driver] ascent {} -> {}: {}", phase, nextPhase(), why);
        phase = nextPhase();
        phaseTicks = 0;
        if (phase == Phase.RECORD_FIRST) {
            TestDriverRecord.begin("first");
        } else if (phase == Phase.RECORD_THIRD) {
            TestDriverRecord.begin("third");
        }
    }

    private Phase nextPhase() {
        return switch (phase) {
            case SETTLE -> Phase.SAS;
            case SAS -> RECORD ? Phase.RECORD_FIRST : Phase.CLIMB;
            case CLIMB -> Phase.AFTER_ORBIT;
            case RECORD_FIRST -> Phase.RECORD_THIRD;
            case AFTER_ORBIT, RECORD_THIRD, DONE -> Phase.DONE;
        };
    }
}
