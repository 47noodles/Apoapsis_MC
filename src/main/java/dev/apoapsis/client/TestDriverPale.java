package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.altitudeM;
import static dev.apoapsis.client.TestDriverSupport.aim;
import static dev.apoapsis.client.TestDriverSupport.fmt;
import static dev.apoapsis.client.TestDriverSupport.inOrbit;
import static dev.apoapsis.client.TestDriverSupport.logOrbit;
import static dev.apoapsis.client.TestDriverSupport.logState;
import static dev.apoapsis.client.TestDriverSupport.sasTo;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;
import static dev.apoapsis.client.TestDriverSupport.speedMps;
import static dev.apoapsis.client.TestDriverSupport.steer;
import static dev.apoapsis.client.TestDriverSupport.stateOf;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * The Pale landing script for {@link TestDriver} ({@code apoapsis.testDriver=pale}, the testPale run). It runs the Pale
 * transfer ({@link TestDriverTransfer}), then inside Pale's sphere burns retrograde with SAS RETROGRADE until the
 * periapsis is below the surface, and coasts with warp. Once the altitude is below 1.3 stopping distances plus 600 m
 * it brakes retrograde at full throttle, in either regime, to under 25 m/s, then falls with the engines off until
 * Pale's surface regime begins.
 *
 * <p>The descent is a closed-loop controller, run every client tick from {@link #CONTROL_START_M} above the ground.
 * The vertical speed target is v_t(h) = -clamp(sqrt(2 * 0.6 * a_net * h), 2, 250) m/s, held at -2 m/s below 30 m and
 * -1 m/s below 3 m, with a_net = T/m - g. The throttle wanted is clamp((g + k (v_t - v_z)) / (a_max cos tilt), 0, 1)
 * with k = 3/s. It is reached with THROTTLE_UP and THROTTLE_DOWN steps, or in one jump: THROTTLE_FULL for a rise of
 * more than 0.3, THROTTLE_CUT for a fall of more than 0.3 to zero. Above {@link #TILT_ABOVE_M} the nose tilts by at
 * most {@link #MAX_TILT_DEG} toward the retrograde horizontal to kill the horizontal speed; below it the nose is held
 * upright. Thrust is applied only while the nose is within {@link #ALIGN_DEG} of the wanted attitude. Logs the outcome:
 * landed (with the touchdown speeds) or lost (with the last speed seen). Gives up ten minutes after the transfer.
 */
final class TestDriverPale implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int TIMEOUT_TICKS = 10 * 60 * 20;
    private static final int BURN_TIMEOUT_TICKS = 20 * 60;
    /** Warp is raised above this altitude (m) and lowered below it, so the descent is not overshot. */
    private static final double WARP_DOWN_M = 10_000;
    private static final int WARP_MAX = 50;
    /** The descent controller takes the throttle this far above the ground (m). */
    private static final double CONTROL_START_M = 3_000;
    /** The horizontal speed is killed with a tilt above this height (m); below it the nose stays upright. */
    private static final double TILT_ABOVE_M = 200;
    /** Largest tilt used to kill the horizontal speed. A 10 degree cap cannot do it: the surface descent is ~8 s. */
    private static final double MAX_TILT_DEG = 45;
    /** Tilt tangent per unit of v_h / |v_z|. */
    private static final double TILT_GAIN = 1.3;
    /** Throttle gain of the vertical speed error, 1/s. */
    private static final double K_VZ = 3.0;
    /** A throttle change larger than this is made in one FULL or CUT command, not 1/40 steps (a full ramp takes 2 s). */
    private static final double THROTTLE_JUMP = 0.3;
    /**
     * The retrograde braking burn starts once the altitude is below BRAKE_MARGIN times the stopping distance plus
     * BRAKE_PAD_M, in either regime, so most of the speed is gone before the surface regime (1,500 m) begins.
     */
    private static final double BRAKE_MARGIN = 1.3;
    private static final double BRAKE_PAD_M = 600;
    /** Braking ends below this speed (m/s); the ship then falls to the surface regime, where the controller lands it. */
    private static final double BRAKE_DONE_MPS = 25;
    /** Warp is lowered (and SAS set retrograde) once the altitude is below this many stopping distances plus 2 km. */
    private static final double WARP_DOWN_STOPS = 3;
    /** Braking curve factor in v_t = sqrt(2 * BRAKE * a_net * h). */
    private static final double BRAKE = 0.6;
    private static final double V_MIN_MPS = 2.0;
    private static final double V_MAX_MPS = 250.0;
    /** Vertical speed targets (m/s, downward) below {@link #HOLD_BELOW_M} and {@link #FINAL_BELOW_M}. */
    private static final double HOLD_BELOW_M = 30;
    private static final double FINAL_BELOW_M = 3;
    private static final double V_HOLD_MPS = 2.0;
    private static final double V_FINAL_MPS = 1.0;
    /** One throttle step per tick (ShipControls THROTTLE_STEP). */
    private static final double THROTTLE_STEP = 1.0 / 40.0;
    /** The suicide-burn gate of round 2, kept: thrust only while the nose is within this many degrees of the wanted attitude. */
    private static final double ALIGN_DEG = 10;
    /** The crash limits (ShipEntity): vertical and contact speed, m/s. */
    private static final double CRASH_VERTICAL_MPS = 12.0;
    private static final double CRASH_CONTACT_MPS = 30.0;
    private static final int LOG_TICKS = 20;
    private static final int LANDED_THIRD_SHOT_TICKS = 20;
    private static final int LANDED_FIRST_SHOT_TICKS = 40;
    private static final int LANDED_END_TICKS = 60;

    private enum Phase { TRANSFER, SPOOL_DOWN, PERI_BURN, COAST, BRAKE, FALL, DESCENT, LANDED, DONE }

    private final TestDriverTransfer transfer = new TestDriverTransfer();
    private Phase phase = Phase.TRANSFER;
    private int phaseTicks;
    private int landTicks;
    private boolean shot200;
    private String pendingShot;
    private int pendingAt;
    private double lastAlt;
    private double lastSpeed;
    private double lastVy;
    private Vec3 lastVel = Vec3.ZERO;
    private Vec3 prevVel = Vec3.ZERO;
    private byte lastRegime = -1;
    private int flips;
    /** Vertical speed at the last descent log line, for the measured vertical acceleration. */
    private double logVz;

    /** True once the ship has touched down on Pale (the run is in its landed phase or past it). */
    boolean touchedDown() {
        return phase == Phase.LANDED || phase == Phase.DONE;
    }

    /** The current phase's name, for telemetry; the transfer's own phase names while it runs. */
    String phaseName() {
        return phase == Phase.TRANSFER ? "transfer-" + transfer.phaseName() : phase.name();
    }

    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            if (phase == Phase.LANDED) {
                return false;
            }
            LOG.warn("[test driver] pale OUTCOME: LOST in {} at alt {} m; last speed {} m/s, vertical {} m/s; regime flips {}",
                    phase, fmt(lastAlt), fmt(lastSpeed), fmt(lastVy), flips);
            return false;
        }
        OrbitStatePayload p = ClientState.orbit;
        if (phase == Phase.TRANSFER) {
            if (!transfer.isDone()) {
                transfer.tick(mc, ship);
                return true;
            }
            if (p == null || BodyId.byOrdinal(p.body()) != BodyId.PALE) {
                LOG.warn("[test driver] pale: the transfer ended without a Pale encounter; stopping");
                return false;
            }
            mc.gui.setScreen(null);
            next(Phase.SPOOL_DOWN, "in Pale's sphere, warping down for the burn");
            return true;
        }
        if (p == null) {
            return true;
        }
        phaseTicks++;
        landTicks++;
        if (landTicks > TIMEOUT_TICKS) {
            LOG.warn("[test driver] pale OUTCOME: timed out after 10 minutes in {}; altitude {} m", phase, fmt(altitudeM(ship, p)));
            return false;
        }
        prevVel = lastVel;
        lastVel = ship.getVelocity();
        lastAlt = altitudeM(ship, p);
        lastSpeed = speedMps(ship, p);
        lastVy = lastVel.y;
        if (lastRegime != -1 && p.regime() != lastRegime) {
            flips++;
            LOG.info("[test driver] pale regime flip {} at landing tick {}: alt {} m, vertical speed {} m/s, phase {}",
                    FlightRegime.byOrdinal(lastRegime), landTicks, fmt(lastAlt), fmt(lastVy), phase);
        }
        lastRegime = p.regime();
        if (pendingShot != null && phaseTicks >= pendingAt) {
            shot(mc, pendingShot);
            logState(ship, p, "shot " + pendingShot);
            pendingShot = null;
        }

        byte pitch = 0, yaw = 0, throttle = ShipInputPayload.THROTTLE_HOLD, flags = 0;
        switch (phase) {
            case SPOOL_DOWN -> {
                if (p.warp() > 1) {
                    if (phaseTicks % 10 == 0) {
                        flags = ShipInputPayload.FLAG_WARP_DOWN;
                    }
                } else {
                    next(Phase.PERI_BURN, "warp x1, retrograde burn");
                }
            }
            case PERI_BURN -> {
                // SAS RETROGRADE turns the nose; burn once it points within 5 degrees of the retrograde direction.
                boolean retro = sasTo(ship, SasMode.RETROGRADE, phaseTicks);
                if (retro && steer(ship, p, -1)[2] < 5) {
                    throttle = ShipInputPayload.THROTTLE_FULL;
                }
                if (phaseTicks % 60 == 0) {
                    logOrbit(p, "retrograde burn, Pe " + fmt(periapsisM(p)) + " m");
                }
                if (periapsisM(p) < 0) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    logOrbit(p, "periapsis below the surface, engines off");
                    next(Phase.COAST, "periapsis below the surface");
                } else if (phaseTicks > BURN_TIMEOUT_TICKS) {
                    logOrbit(p, "burn timed out");
                    next(Phase.COAST, "burn timed out");
                }
            }
            case COAST -> {
                double stop = stoppingM(ship, lastSpeed, lastAlt);
                double slowBelow = Math.max(WARP_DOWN_M, WARP_DOWN_STOPS * stop + 2_000);
                if (lastAlt <= BRAKE_MARGIN * stop + BRAKE_PAD_M && p.warp() <= 1) {
                    logOrbit(p, "braking burn at alt " + fmt(lastAlt) + " m, speed " + fmt(lastSpeed)
                            + " m/s, stopping distance " + fmt(stop) + " m");
                    next(Phase.BRAKE, "stopping distance reached");
                } else if (!inOrbit(p)) {
                    // Not expected: the brake should have started higher. Brake now if fast, else land.
                    next(lastSpeed > 2 * BRAKE_DONE_MPS ? Phase.BRAKE : Phase.DESCENT,
                            "in Pale's surface regime unbraked at alt " + fmt(lastAlt) + " m, speed " + fmt(lastSpeed));
                } else {
                    if (phaseTicks % 10 == 0) {
                        if (lastAlt > slowBelow && p.warp() < WARP_MAX) {
                            flags = ShipInputPayload.FLAG_WARP_UP;
                        } else if (lastAlt <= slowBelow && p.warp() > 1) {
                            flags = ShipInputPayload.FLAG_WARP_DOWN;
                        }
                    }
                    if (lastAlt <= slowBelow) {
                        // SAS needs time to turn the nose retrograde, so it is set before the brake begins.
                        sasTo(ship, SasMode.RETROGRADE, phaseTicks);
                    }
                    if (phaseTicks % 200 == 0) {
                        logOrbit(p, "coasting to the surface, stopping distance " + fmt(stop) + " m");
                    }
                }
            }
            case BRAKE -> {
                // Full retrograde burn, in either regime, until the speed is nearly gone.
                boolean retro = sasTo(ship, SasMode.RETROGRADE, phaseTicks);
                Vec3 v = ship.getVelocity();
                double err = inOrbit(p) ? steer(ship, p, -1)[2]
                        : aim(ship, new Vector3f((float) -v.x, (float) -v.y, (float) -v.z))[2];
                throttle = retro && err < 8 ? ShipInputPayload.THROTTLE_FULL : ShipInputPayload.THROTTLE_HOLD;
                if (phaseTicks % LOG_TICKS == 0) {
                    LOG.info("[test driver] pale brake: alt {} m, speed {} m/s, stopping {} m, nose {} deg off retrograde, "
                            + "throttle {}, regime {}", fmt(lastAlt), fmt(lastSpeed),
                            fmt(stoppingM(ship, lastSpeed, lastAlt)), fmt(err), fmt(ship.getThrottle()),
                            FlightRegime.byOrdinal(p.regime()));
                }
                boolean low = !inOrbit(p) && ship.getY() - groundY(ship) < 150;
                if (lastSpeed < BRAKE_DONE_MPS || low) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    if (inOrbit(p)) {
                        next(Phase.FALL, "braked to " + fmt(lastSpeed) + " m/s at alt " + fmt(lastAlt) + " m");
                    } else {
                        startDescent(mc, "braked to " + fmt(lastSpeed) + " m/s in the surface regime at alt " + fmt(lastAlt) + " m");
                    }
                }
            }
            case FALL -> {
                // Engines off, nose retrograde (up, as the ship falls), until the surface regime begins.
                throttle = ShipInputPayload.THROTTLE_CUT;
                sasTo(ship, SasMode.RETROGRADE, phaseTicks);
                if (!inOrbit(p)) {
                    startDescent(mc, "in Pale's surface regime at alt " + fmt(lastAlt) + " m, speed " + fmt(lastSpeed) + " m/s");
                } else if (lastAlt <= BRAKE_MARGIN * stoppingM(ship, lastSpeed, lastAlt) + BRAKE_PAD_M) {
                    next(Phase.BRAKE, "falling too fast again at alt " + fmt(lastAlt) + " m");
                }
            }
            case DESCENT -> {
                double h = ship.getY() - FlightDimensions.seaLevel(BodyId.PALE);
                double dist = ship.getY() - groundY(ship);
                Vec3 v = ship.getVelocity();
                double vh = Math.hypot(v.x, v.z);
                double a = ship.maxThrustN(pressureFraction(h)) / ship.massKg();
                double g = gravityMps2(h);
                double vt = targetVz(dist, a - g);
                // Horizontal kill: the nose tilts toward the retrograde horizontal by atan(TILT_GAIN * v_h / |v_z|), capped,
                // so the tilt is largest while the horizontal speed is large and falls away with it. Above TILT_ABOVE_M only.
                double sin = 0;
                if (dist > TILT_ABOVE_M && vh > 0.05) {
                    sin = Math.sin(Math.min(Math.atan(TILT_GAIN * vh / Math.max(-v.y, 1)), Math.toRadians(MAX_TILT_DEG)));
                }
                double cos = Math.sqrt(1 - sin * sin);
                double wanted = clamp((g + K_VZ * (vt - v.y)) / (a * cos), 0, 1);
                Vector3f want = new Vector3f(0, 1, 0);
                if (sin > 0) {
                    want.set((float) (-v.x / vh * sin), (float) cos, (float) (-v.z / vh * sin));
                }
                double[] aimed = aim(ship, want);
                double actual = ship.getThrottle();
                boolean control = dist <= CONTROL_START_M;
                if (control) {
                    boolean aligned = aimed[2] < ALIGN_DEG;
                    double diff = wanted - actual;
                    if (!aligned || Math.abs(diff) <= THROTTLE_STEP / 2) {
                        throttle = ShipInputPayload.THROTTLE_HOLD;
                    } else if (diff > THROTTLE_JUMP) {
                        throttle = ShipInputPayload.THROTTLE_FULL;
                    } else if (diff < -THROTTLE_JUMP && wanted < THROTTLE_STEP) {
                        throttle = ShipInputPayload.THROTTLE_CUT;
                    } else {
                        throttle = diff > 0 ? ShipInputPayload.THROTTLE_UP : ShipInputPayload.THROTTLE_DOWN;
                    }
                    pitch = (byte) aimed[0];
                    yaw = (byte) aimed[1];
                    flags |= sasPress(ship, SasMode.OFF, phaseTicks);
                } else {
                    throttle = actual > 0 ? ShipInputPayload.THROTTLE_DOWN : ShipInputPayload.THROTTLE_HOLD;
                    flags |= sasPress(ship, SasMode.RETROGRADE, phaseTicks);
                }
                if (p.warp() > 1 && dist <= WARP_DOWN_M && phaseTicks % 10 == 0) {
                    flags |= ShipInputPayload.FLAG_WARP_DOWN;
                }
                if (!shot200 && dist < 200) {
                    shot200 = true;
                    schedule("pale-02-200m-first-person.png", 0);
                }
                if (phaseTicks % LOG_TICKS == 0) {
                    LOG.info("[test driver] pale descent: h {} m, v_z {} m/s, v_horizontal {} m/s, target v_z {} m/s, "
                            + "throttle wanted {} actual {}, nose {} deg off wanted, tilt {} deg, a_z measured {} m/s^2, "
                            + "a_max {} m/s^2, control {}",
                            fmt(dist), fmt(v.y), fmt(vh), fmt(vt), control ? fmt(wanted) : "-", fmt(actual),
                            fmt(aimed[2]), fmt(Math.toDegrees(Math.asin(sin))), fmt((v.y - logVz) / (LOG_TICKS / 20.0)),
                            fmt(a), control);
                    logVz = v.y;
                }
                if (ship.isLanded()) {
                    throttle = ShipInputPayload.THROTTLE_CUT;
                    next(Phase.LANDED, "landed");
                    double vert = -prevVel.y;
                    double horiz = Math.hypot(prevVel.x, prevVel.z);
                    double contact = prevVel.length();
                    LOG.info("[test driver] pale touchdown: vertical {} m/s, horizontal {} m/s, contact {} m/s, ground {} m",
                            fmt(vert), fmt(horiz), fmt(contact), fmt(dist));
                    LOG.info("[test driver] pale OUTCOME: LANDED intact; touchdown vertical {} m/s, horizontal {} m/s, contact {} m/s "
                            + "(limits {} vertical / {} contact m/s), regime flips {}: {}",
                            fmt(vert), fmt(horiz), fmt(contact), fmt(CRASH_VERTICAL_MPS), fmt(CRASH_CONTACT_MPS), flips,
                            contact <= CRASH_CONTACT_MPS && vert <= CRASH_VERTICAL_MPS ? "within the limits" : "over a limit");
                }
            }
            case LANDED -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                if (phaseTicks == 1) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (phaseTicks == LANDED_THIRD_SHOT_TICKS) {
                    shot(mc, "pale-03-touchdown-third-person.png");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
                if (phaseTicks == LANDED_FIRST_SHOT_TICKS) {
                    shot(mc, "pale-04-landed-first-person.png");
                }
                if (phaseTicks >= LANDED_END_TICKS) {
                    return false;
                }
            }
            case TRANSFER, DONE -> {
                return false;
            }
        }
        send(ship, pitch, yaw, throttle, flags);
        return true;
    }

    private void next(Phase to, String why) {
        LOG.info("[test driver] pale {} -> {}: {}", phase, to, why);
        phase = to;
        phaseTicks = 0;
    }

    private void startDescent(Minecraft mc, String why) {
        next(Phase.DESCENT, why);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        schedule("pale-01-descent-1500m-first-person.png", 5);
    }

    /**
     * Distance (m) needed to stop from {@code speed} at full thrust, against Pale's gravity at {@code alt}. Effectively
     * infinite when the engines cannot beat gravity.
     */
    private static double stoppingM(ShipEntity ship, double speed, double alt) {
        double net = ship.maxThrustN(0) / ship.massKg() - gravityMps2(alt);
        return net > 0.5 ? speed * speed / (2 * net) : 1e9;
    }

    private void schedule(String name, int leadTicks) {
        pendingShot = name;
        pendingAt = phaseTicks + leadTicks;
    }

    /** Presses SAS toward {@code target} once per 8 ticks while it reads another mode; the flag is merged into the packet. */
    private static byte sasPress(ShipEntity ship, SasMode target, int tick) {
        return ship.getSasMode() != target && tick % 8 == 0 ? ShipInputPayload.FLAG_TOGGLE_SAS : 0;
    }

    /**
     * Vertical speed target, m/s and negative (downward), at height h above the ground: the braking curve for the net
     * deceleration, clamped to [2, 250] m/s, then held at -2 m/s below 30 m and -1 m/s below 3 m.
     */
    static double targetVz(double h, double aNet) {
        if (h < FINAL_BELOW_M) {
            return -V_FINAL_MPS;
        }
        if (h < HOLD_BELOW_M) {
            return -V_HOLD_MPS;
        }
        return -clamp(Math.sqrt(2 * BRAKE * Math.max(aNet, 0) * h), V_MIN_MPS, V_MAX_MPS);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    /** Periapsis height above Pale's radius, m (negative once the orbit meets the surface). */
    private static double periapsisM(OrbitStatePayload p) {
        CelestialBody body = MeridianSystem.PALE;
        return Elements.of(body.mu, stateOf(p)).periapsisRadius() - body.radius;
    }

    /** Ground height under the ship: the top of the surface blocks, in block y (m). */
    private static double groundY(ShipEntity ship) {
        return ship.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(ship.getX()), (int) Math.floor(ship.getZ()));
    }

    /** Gravity at height h above sea level, m/s^2. */
    private static double gravityMps2(double h) {
        CelestialBody sys = FlightDimensions.system(BodyId.PALE);
        return sys.gravity(sys.radius + Math.max(h, 0));
    }

    /** Pressure over sea-level pressure at height h above sea level (Pale has no air, so this is 0). */
    private static double pressureFraction(double h) {
        CelestialBody sys = FlightDimensions.system(BodyId.PALE);
        double rho = sys.density(Math.max(h, 0));
        return sys.seaLevelDensity > 0 ? rho / sys.seaLevelDensity : 0;
    }
}
