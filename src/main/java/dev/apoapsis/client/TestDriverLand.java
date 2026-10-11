package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.fmt;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.ship.DescentGear;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * The landing script for {@link TestDriver} ({@code apoapsis.testDriver=land}, the testLand run, started on the pad):
 * once the pilot sits in the pad Kestrel the script runs {@code /apoapsis scenario lander}, which puts the Kestrel with
 * four parachutes and four landing legs 1,200 m up. It falls until it reaches {@link #DEPLOY_SPEED_MPS}, deploys the
 * chutes, logs the speed every second, the terminal speed and the touchdown speed, and takes two screenshots: the open
 * canopy and the ship landed on its legs. Gives up after eight minutes.
 */
final class TestDriverLand implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int TIMEOUT_TICKS = 8 * 60 * 20;
    private static final int PAD_TICKS = 60;
    private static final int LANDER_WAIT_TICKS = 1200;
    /** Height above sea level, m, above which the pilot counts as being in the lander. */
    private static final double LANDER_UP_M = 800;
    private static final double DEPLOY_SPEED_MPS = 80;
    private static final int RESEND_TICKS = 10;
    /** Ticks between the canopy opening and its screenshot, and between touching down and the landed screenshot. */
    private static final int CANOPY_SHOT_TICKS = 60;
    private static final int LANDED_SHOT_TICKS = 65;
    private static final int FINISH_TICKS = 20;
    /** Seconds the speed must hold still before it counts as the terminal speed. */
    private static final int TERMINAL_SECONDS = 3;
    /** The speed still eases by about 0.1 m/s each second, as the air thickens on the way down. */
    private static final double TERMINAL_BAND_MPS = 0.15;
    private static final int RING = 6;

    private enum Phase { PAD, WAIT, FALL, LANDED, DONE }

    private Phase phase = Phase.PAD;
    private int ticks;
    private int phaseTicks;
    private int openTicks = -1;
    private int landedTicks;
    private double lastAlt;
    private double lastSpeed;
    private double previousSecondSpeed = -1;
    private int steadySeconds;
    private boolean terminalLogged;
    private final double[] ring = new double[RING];

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            LOG.warn("[test driver] land: the ship was lost at {} m, last speed {} m/s, in {}", fmt(lastAlt),
                    fmt(lastSpeed), phase);
            return false;
        }
        ticks++;
        phaseTicks++;
        if (ticks > TIMEOUT_TICKS) {
            LOG.warn("[test driver] land: no landing after eight minutes; altitude {} m, speed {} m/s", fmt(lastAlt),
                    fmt(lastSpeed));
            return false;
        }
        lastAlt = ship.getY() - FlightDimensions.seaLevel(BodyId.VERDANCE);
        Vec3 v = ship.getVelocity();
        lastSpeed = v.length();
        switch (phase) {
            case PAD -> {
                if (phaseTicks >= PAD_TICKS) {
                    mc.player.connection.sendCommand("apoapsis scenario lander");
                    LOG.info("[test driver] land: asked for the lander");
                    next(Phase.WAIT);
                }
            }
            case WAIT -> {
                if (lastAlt > LANDER_UP_M) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    LOG.info("[test driver] land: lander up at {} m, {} parachutes, {} legs", fmt(lastAlt),
                            ship.parachuteCount(), ship.landingLegCount());
                    next(Phase.FALL);
                } else if (phaseTicks > LANDER_WAIT_TICKS) {
                    LOG.warn("[test driver] land: the lander did not appear (altitude {} m)", fmt(lastAlt));
                    return false;
                }
            }
            case FALL -> fall(mc, ship, v);
            case LANDED -> {
                landedTicks++;
                if (landedTicks == 5) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                } else if (landedTicks == LANDED_SHOT_TICKS) {
                    shot(mc, "m06-landed-on-legs.png");
                    LOG.info("[test driver] land: shot m06-landed-on-legs.png at {} m", fmt(lastAlt));
                } else if (landedTicks == LANDED_SHOT_TICKS + FINISH_TICKS) {
                    next(Phase.DONE);
                    return false;
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void fall(Minecraft mc, ShipEntity ship, Vec3 v) {
        System.arraycopy(ring, 1, ring, 0, RING - 1);
        ring[RING - 1] = lastSpeed;
        if (phaseTicks % 20 == 0) {
            LOG.info("[test driver] land: t={} s alt={} m speed={} m/s vy={} m/s chutes={}", phaseTicks / 20,
                    fmt(lastAlt), fmt(lastSpeed), fmt(v.y), ship.getChuteState());
            trackTerminal(ship);
        }
        int state = ship.getChuteState();
        if (state == DescentGear.STOWED && lastSpeed >= DEPLOY_SPEED_MPS && phaseTicks % RESEND_TICKS == 0) {
            send(ship, (byte) 0, (byte) 0, ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_DEPLOY_CHUTES);
            LOG.info("[test driver] land: deploying at {} m/s, {} m", fmt(lastSpeed), fmt(lastAlt));
        }
        if (state >= DescentGear.INFLATE_TICKS && openTicks < 0) {
            openTicks = 0;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            LOG.info("[test driver] land: chutes open at {} m, {} m/s", fmt(lastAlt), fmt(lastSpeed));
        }
        if (openTicks >= 0 && ++openTicks == CANOPY_SHOT_TICKS) {
            shot(mc, "m06-canopy-third-person.png");
            LOG.info("[test driver] land: shot m06-canopy-third-person.png at {} m, {} m/s", fmt(lastAlt), fmt(lastSpeed));
        }
        if (ship.isLanded()) {
            double touchdown = 0;
            for (double s : ring) {
                touchdown = Math.max(touchdown, s);
            }
            LOG.info("[test driver] land: touchdown speed {} m/s", fmt(touchdown));
            LOG.info("[test driver] land: landed at {} m", fmt(lastAlt));
            next(Phase.LANDED);
        }
    }

    /** Logs once the speed has held within a small band for a few seconds after the chutes opened. */
    private void trackTerminal(ShipEntity ship) {
        if (terminalLogged || openTicks < 0) {
            return;
        }
        if (previousSecondSpeed >= 0 && Math.abs(lastSpeed - previousSecondSpeed) < TERMINAL_BAND_MPS) {
            steadySeconds++;
        } else {
            steadySeconds = 0;
        }
        previousSecondSpeed = lastSpeed;
        if (steadySeconds >= TERMINAL_SECONDS) {
            terminalLogged = true;
            LOG.info("[test driver] land: terminal speed {} m/s at {} m, mass {} kg", fmt(lastSpeed), fmt(lastAlt),
                    fmt(ship.massKg()));
        }
    }

    private void next(Phase to) {
        phase = to;
        phaseTicks = 0;
    }
}
