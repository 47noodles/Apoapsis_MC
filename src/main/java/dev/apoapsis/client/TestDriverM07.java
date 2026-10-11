package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.fmt;
import static dev.apoapsis.client.TestDriverSupport.sasTo;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Landing;
import dev.apoapsis.ship.DescentGear;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * The milestone 7 screenshot run for {@link TestDriver} ({@code apoapsis.testDriver=m07}, the testM07 run, started on the
 * pad). In order: a lander from 1,700 m flies its retro burn at the radar cue; a re-entry from 9,000 m; a chute descent
 * from 8,000 m; Verdance's rim from orbit at day, terminator and night; and a view from inside 3 km. Each waiting phase
 * gives up after thirty seconds with a warning, and the whole run gives up after twelve minutes.
 */
final class TestDriverM07 implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int TIMEOUT_TICKS = 12 * 60 * 20;
    private static final int WAIT_CAP_TICKS = 30 * 20;
    private static final int PAD_TICKS = 60;
    /** Ticks a dive or orbit command needs to take effect before the phase looks at the ship's state. */
    private static final int SETTLE_TICKS = 40;
    private static final int FINISH_TICKS = 40;
    /** Height above sea level, m, above which the ship counts as lifted off the ground. */
    private static final double UP_M = 800;
    private static final double INSIDE_3KM_M = 3500;
    private static final double DEPLOY_SPEED_MPS = 80;
    private static final int RESEND_TICKS = 10;
    /** Seconds before the burn at which the radar cue shot is taken. */
    private static final double CUE_S = 10;
    /** The radar readout shows below this radar altitude, m; the cue shot waits for it. */
    private static final double RADAR_SHOT_M = 1400;
    private static final int BURN_SHOT_TICKS = 5;
    /** The radar altitude, m, at which the near-ground shot is taken during the burn. */
    private static final double GROUND_SHOT_M = 60;
    /** The burn aims to arrive at this descent speed at the ground, m/s, using this share of the available braking. */
    private static final double TOUCHDOWN_MPS = 2.0;
    private static final double BRAKE_SHARE = 0.6;
    private static final int LANDED_SHOT_TICKS = 65;
    private static final int REENTRY_TICKS = 240;
    private static final int CHUTE_SHOT_AFTER_TICKS = 60;
    private static final String[] RIM_LABELS = {"day", "terminator", "night"};
    private static final int[] RIM_TIMES = {60, 100, 140};

    private enum Phase {
        PAD, WAIT_UP, SKY, FALL, BURN, LANDED, REENTRY_WAIT, REENTRY, CHUTE_WAIT, CHUTE, RIM_WAIT, RIM_SHOTS, SKY3KM_WAIT,
        SKY3KM, DONE
    }

    private Phase phase = Phase.PAD;
    private int ticks;
    private int phaseTicks;
    private int rimIndex;
    private int chuteShotTick = -1;
    private boolean sasSet;
    private boolean cueShot;
    private boolean groundShot;
    private boolean nanWarned;
    private double lastAlt;
    private double lastSpeed;

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            LOG.warn("[test driver] m07: the ship was lost at {} m, in {}", fmt(lastAlt), phase);
            return false;
        }
        ticks++;
        phaseTicks++;
        if (ticks == 1) {
            // The Custodian guide and the scenario messages would cover the lower left of every shot.
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        }
        mc.gui.toastManager().clear();
        mc.gui.hud.setOverlayMessage(Component.empty(), false);
        if (ticks > TIMEOUT_TICKS) {
            LOG.warn("[test driver] m07: twelve minutes are up, in {} at {} m", phase, fmt(lastAlt));
            return false;
        }
        OrbitStatePayload p = ClientState.orbit;
        boolean orbit = p != null && TestDriverSupport.inOrbit(p);
        lastAlt = p == null ? ship.getY() - FlightDimensions.seaLevel(BodyId.VERDANCE) : TestDriverSupport.altitudeM(ship, p);
        lastSpeed = p == null ? ship.getVelocity().length() : TestDriverSupport.speedMps(ship, p);
        switch (phase) {
            case PAD -> {
                if (phaseTicks >= PAD_TICKS) {
                    cmd(mc, "apoapsis scenario lander burn 1700");
                    next(Phase.WAIT_UP);
                }
            }
            case WAIT_UP -> {
                if (lastAlt > UP_M || capped()) {
                    next(Phase.SKY);
                }
            }
            case SKY -> {
                if (phaseTicks == 1) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                } else if (phaseTicks == 20) {
                    snap(mc, "m07-sky-climb-1700m.png");
                } else if (phaseTicks == 40) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                } else if (phaseTicks == 60) {
                    snap(mc, "m07-sky-climb-1700m-third.png");
                    next(Phase.FALL);
                }
            }
            case FALL -> fall(mc, ship);
            case BURN -> burn(mc, ship);
            case LANDED -> landed(mc, ship);
            case REENTRY_WAIT -> {
                if (phaseTicks == 1) {
                    cmd(mc, "apoapsis seamless dive 9000 1100 15 plain");
                }
                if ((lastAlt > UP_M && phaseTicks >= SETTLE_TICKS) || capped()) {
                    next(Phase.REENTRY);
                }
            }
            case REENTRY -> reentry(mc);
            case CHUTE_WAIT -> {
                if (phaseTicks == 1) {
                    cmd(mc, "apoapsis seamless dive 8000 160 65 chutes");
                }
                if ((lastAlt > UP_M && phaseTicks >= SETTLE_TICKS) || capped()) {
                    next(Phase.CHUTE);
                }
            }
            case CHUTE -> chute(mc, ship);
            case RIM_WAIT -> {
                if (phaseTicks == 1) {
                    cmd(mc, "apoapsis seamless orbit verdance 40000 " + RIM_TIMES[rimIndex] + " 0 0");
                }
                if ((orbit && phaseTicks >= SETTLE_TICKS) || capped()) {
                    next(Phase.RIM_SHOTS);
                }
            }
            case RIM_SHOTS -> rimShots(mc);
            case SKY3KM_WAIT -> {
                if (phaseTicks == 1) {
                    cmd(mc, "apoapsis seamless orbit verdance 3000 60 0 0");
                }
                if ((lastAlt < INSIDE_3KM_M && phaseTicks >= SETTLE_TICKS) || capped()) {
                    next(Phase.SKY3KM);
                }
            }
            case SKY3KM -> {
                if (phaseTicks == 1) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                } else if (phaseTicks == 20) {
                    snap(mc, "m07-sky-inside-3km.png");
                } else if (phaseTicks == 25) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                } else if (phaseTicks == 45) {
                    snap(mc, "m07-sky-inside-3km-third.png");
                } else if (phaseTicks == 50) {
                    next(Phase.DONE);
                }
            }
            case DONE -> {
                if (phaseTicks >= FINISH_TICKS) {
                    LOG.info("[test driver] m07: all phases done; stopping");
                    return false;
                }
            }
        }
        return true;
    }

    /** Phase FALL: the lander falls until the radar cue, then the burn starts at zero cue time or after the cap. */
    private void fall(Minecraft mc, ShipEntity ship) {
        if (phaseTicks == 1) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (ship.isLanded()) {
            LOG.warn("[test driver] m07: landed before the burn cue at {} m", fmt(lastAlt));
            next(Phase.LANDED);
            return;
        }
        if (!sasSet) {
            sasSet = sasTo(ship, SasMode.RETROGRADE, phaseTicks);
        }
        double radar = radarM(mc, ship);
        double t = burnIn(ship, radar);
        if (phaseTicks % 40 == 0) {
            LOG.info("[test driver] m07: radar {} m, speed {} m/s, burn in {} s", fmt(radar), fmt(lastSpeed), fmt(t));
        }
        if (Double.isNaN(t)) {
            if (!nanWarned) {
                LOG.warn("[test driver] m07: no burn timing from {} m radar", fmt(radar));
                nanWarned = true;
            }
        } else if (t == 0) {
            next(Phase.BURN);
            return;
        } else if (!cueShot && t > 0 && t <= CUE_S && radar <= RADAR_SHOT_M) {
            cueShot = true;
            snap(mc, "m07-radar-cue-first-person.png");
        }
        if (capped()) {
            next(Phase.BURN);
        }
    }

    /**
     * Phase BURN: retrograde SAS, full throttle while the descent is faster than a speed that falls with the radar altitude,
     * cut above it, so the ship follows the curve down to a soft touchdown instead of stopping high and climbing away.
     */
    private void burn(Minecraft mc, ShipEntity ship) {
        if (phaseTicks == 1) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            LOG.info("[test driver] m07: burn at {} m, {} m/s, {} kg", fmt(lastAlt), fmt(lastSpeed), fmt(ship.massKg()));
        }
        double radar = radarM(mc, ship);
        double vDown = -ship.getVelocity().y;
        double target = TOUCHDOWN_MPS + Math.sqrt(2 * BRAKE_SHARE * Math.max(0, netDecel(ship, radar)) * Math.max(0, radar));
        byte throttle = vDown > target ? ShipInputPayload.THROTTLE_FULL : ShipInputPayload.THROTTLE_CUT;
        send(ship, (byte) 0, (byte) 0, throttle, (byte) 0);
        if (phaseTicks % 10 == 0) {
            LOG.info("[test driver] m07: burning, throttle {}, v {} m/s down, target {} m/s, radar {} m", ship.getThrottle(),
                    fmt(vDown), fmt(target), fmt(radar));
        }
        if (phaseTicks == BURN_SHOT_TICKS) {
            snap(mc, "m07-burn-now-third-person.png");
        } else if (!groundShot && radar <= GROUND_SHOT_M) {
            groundShot = true;
            snap(mc, "m07-radar-near-ground.png");
        }
        if (ship.isLanded() || capped()) {
            next(Phase.LANDED);
        }
    }

    /** Net upward acceleration at full thrust at the ground, m/s^2, as the radar readout works it out. */
    private double netDecel(ShipEntity ship, double radar) {
        CelestialBody body = FlightDimensions.system(BodyId.VERDANCE);
        double altAboveSeaM = ship.getY() - FlightDimensions.seaLevel(BodyId.VERDANCE);
        double pf = pressureFraction(body, altAboveSeaM - radar);
        double g = body.gravity(body.radius + altAboveSeaM - radar);
        return ship.maxThrustN(pf) / ship.massKg() - g;
    }

    /** Phase LANDED: cut the throttle, wait for the ground to settle, shoot the ship on its legs, then re-enter. */
    private void landed(Minecraft mc, ShipEntity ship) {
        if (phaseTicks == 1) {
            send(ship, (byte) 0, (byte) 0, ShipInputPayload.THROTTLE_CUT, (byte) 0);
            LOG.info("[test driver] m07: throttle cut at {} m", fmt(lastAlt));
        }
        if (phaseTicks == LANDED_SHOT_TICKS) {
            snap(mc, "m07-landed-after-burn.png");
            next(Phase.REENTRY_WAIT);
        }
    }

    /** Phase REENTRY: fixed timeline of shots while the dive heats up. */
    private void reentry(Minecraft mc) {
        if (phaseTicks == 1) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (phaseTicks == 20) {
            snap(mc, "m07-reentry-first-person.png");
        } else if (phaseTicks == 40) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (phaseTicks == 60) {
            snap(mc, "m07-reentry-third-person.png");
        } else if (phaseTicks == 200) {
            snap(mc, "m07-reentry-third-person-2.png");
        } else if (phaseTicks == REENTRY_TICKS) {
            next(Phase.CHUTE_WAIT);
        }
    }

    /** Phase CHUTE: deploy the chutes at speed, shoot them open, then move on to the rim. */
    private void chute(Minecraft mc, ShipEntity ship) {
        if (phaseTicks == 1) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        int state = ship.getChuteState();
        if (state == DescentGear.STOWED && lastSpeed >= DEPLOY_SPEED_MPS && phaseTicks % RESEND_TICKS == 0) {
            send(ship, (byte) 0, (byte) 0, ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_DEPLOY_CHUTES);
            LOG.info("[test driver] m07: deploying at {} m/s, {} m", fmt(lastSpeed), fmt(lastAlt));
        }
        if (chuteShotTick < 0 && state >= DescentGear.INFLATE_TICKS) {
            chuteShotTick = phaseTicks;
            snap(mc, "m07-chute-above-2km.png");
        }
        boolean shotDone = chuteShotTick >= 0 && phaseTicks >= chuteShotTick + CHUTE_SHOT_AFTER_TICKS;
        if (shotDone || ship.isLanded() || capped()) {
            next(Phase.RIM_WAIT);
        }
    }

    /** Phase RIM_SHOTS: a first-person and a third-person shot of the rim at the current time of day. */
    private void rimShots(Minecraft mc) {
        String label = RIM_LABELS[rimIndex];
        if (phaseTicks == 1) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (phaseTicks == 10) {
            snap(mc, "m07-rim-orbit-" + label + ".png");
        } else if (phaseTicks == 20) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (phaseTicks == 30) {
            snap(mc, "m07-rim-orbit-" + label + "-third.png");
        } else if (phaseTicks == 40) {
            if (rimIndex + 1 < RIM_LABELS.length) {
                rimIndex++;
                next(Phase.RIM_WAIT);
            } else {
                next(Phase.SKY3KM_WAIT);
            }
        }
    }

    /** Seconds until the full-thrust retro burn must start: 0 when due now, +Infinity when not descending, NaN when no burn stops the lander. */
    private double burnIn(ShipEntity ship, double radar) {
        CelestialBody body = FlightDimensions.system(BodyId.VERDANCE);
        double altAboveSeaM = ship.getY() - FlightDimensions.seaLevel(BodyId.VERDANCE);
        double pf = pressureFraction(body, altAboveSeaM - radar);
        double thrustAccel = ship.maxThrustN(pf) / ship.massKg();
        double g = body.gravity(body.radius + altAboveSeaM - radar);
        return Landing.burnStartIn(radar, -ship.getVelocity().y, g, thrustAccel);
    }

    /** Air pressure as a fraction of sea level at height h above sea level (m). */
    private static double pressureFraction(CelestialBody body, double h) {
        return body.seaLevelDensity > 0 ? body.density(Math.max(0, h)) / body.seaLevelDensity : 0;
    }

    /** Metres from the ship's lowest point to the terrain under its centre, as the HUD measures it. NaN if not loaded. */
    private static double radarM(Minecraft mc, ShipEntity ship) {
        int bx = Mth.floor(ship.getX());
        int bz = Mth.floor(ship.getZ());
        if (!mc.level.hasChunkAt(bx, bz)) {
            return Double.NaN;
        }
        double lowestY = ship.getY() - ship.lowestPointBelowCentreM();
        double groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        return Math.max(0, lowestY - groundY);
    }

    /** True once the phase has waited past its cap, with a warning. */
    private boolean capped() {
        if (phaseTicks <= WAIT_CAP_TICKS) {
            return false;
        }
        LOG.warn("[test driver] m07: {} waited 30 s at {} m; moving on", phase, fmt(lastAlt));
        return true;
    }

    private void snap(Minecraft mc, String name) {
        shot(mc, name);
        LOG.info("[test driver] m07: shot {} at {} m, {} m/s", name, fmt(lastAlt), fmt(lastSpeed));
    }

    private void cmd(Minecraft mc, String text) {
        mc.player.connection.sendCommand(text);
        LOG.info("[test driver] m07: sent the command \"{}\"", text);
    }

    private void next(Phase to) {
        phase = to;
        phaseTicks = 0;
        LOG.info("[test driver] m07: {} at {} m, {} m/s", to, fmt(lastAlt), fmt(lastSpeed));
    }
}
