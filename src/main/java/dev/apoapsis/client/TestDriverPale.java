package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.altitudeM;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * The Pale landing script for {@link TestDriver} ({@code apoapsis.testDriver=pale}, the testPale run). It runs the Pale
 * transfer ({@link TestDriverTransfer}), then inside Pale's sphere burns retrograde with SAS RETROGRADE until the
 * periapsis is below the surface, coasts with warp until the descent reaches Pale's surface regime, and makes a suicide
 * burn: full throttle once the ground distance falls below 1.2 times the stopping distance v^2 / (2a), with a = T/m - g,
 * and cut below 2 m/s. Logs the outcome: landed (with the contact speed) or lost (with the last speed seen). Gives up
 * ten minutes after the transfer.
 */
final class TestDriverPale implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int TIMEOUT_TICKS = 10 * 60 * 20;
    private static final int BURN_TIMEOUT_TICKS = 20 * 60;
    /** Warp is raised above this altitude (m) and lowered below it, so the descent is not overshot. */
    private static final double WARP_DOWN_M = 10_000;
    private static final int WARP_MAX = 50;
    private static final double SAFETY = 1.2;
    /** The suicide burn starts only when the nose is within this many degrees of retrograde. */
    private static final double ALIGN_DEG = 10;
    private static final double LOW_SPEED_MPS = 2.0;
    /** The crash limits (ShipEntity): vertical and contact speed, m/s. */
    private static final double CRASH_VERTICAL_MPS = 12.0;
    private static final double CRASH_CONTACT_MPS = 30.0;
    private static final int LANDED_FIRST_TICKS = 20;
    private static final int LANDED_THIRD_TICKS = 40;
    private static final int LANDED_END_TICKS = 60;

    private enum Phase { TRANSFER, SPOOL_DOWN, PERI_BURN, COAST, DESCENT, LANDED, DONE }

    private final TestDriverTransfer transfer = new TestDriverTransfer();
    private Phase phase = Phase.TRANSFER;
    private int phaseTicks;
    private int landTicks;
    private boolean burning;
    private boolean shot200;
    private String pendingShot;
    private int pendingAt;
    private double lastAlt;
    private double lastSpeed;
    private double lastVy;
    private double prevSpeed;
    private double prevVy;
    private byte lastRegime = -1;
    private int flips;
    private boolean cutAtLowSpeed;

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
        prevSpeed = lastSpeed;
        prevVy = lastVy;
        lastAlt = altitudeM(ship, p);
        lastSpeed = speedMps(ship, p);
        lastVy = ship.getVelocity().y;
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

        byte pitch = 0, yaw = 0, throttle = ShipInputPayload.THROTTLE_CUT, flags = 0;
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
                if (!inOrbit(p)) {
                    next(Phase.DESCENT, "in Pale's surface regime at alt " + fmt(lastAlt) + " m");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    schedule("pale-01-descent-1500m-first-person.png", 5);
                } else {
                    if (phaseTicks % 10 == 0) {
                        if (lastAlt > WARP_DOWN_M && p.warp() < WARP_MAX) {
                            flags = ShipInputPayload.FLAG_WARP_UP;
                        } else if (lastAlt <= WARP_DOWN_M && p.warp() > 1) {
                            flags = ShipInputPayload.FLAG_WARP_DOWN;
                        }
                    }
                    if (lastAlt <= WARP_DOWN_M) {
                        // SAS needs time to turn the nose retrograde, so it is set before the descent begins.
                        sasTo(ship, SasMode.RETROGRADE, phaseTicks);
                    }
                    if (phaseTicks % 200 == 0) {
                        logOrbit(p, "coasting to the surface");
                    }
                }
            }
            case DESCENT -> {
                double h = ship.getY() - FlightDimensions.seaLevel(BodyId.PALE);
                double dist = ship.getY() - groundY(ship);
                double vy = lastVy;
                double decel = netDecelMps2(ship, h);
                double stop = decel > 0 ? lastSpeed * lastSpeed / (2 * decel) : Double.POSITIVE_INFINITY;
                boolean retro = sasTo(ship, SasMode.RETROGRADE, phaseTicks);
                double misalign = retrogradeMisalignDeg(ship);
                if (!burning && retro && vy < 0 && misalign < ALIGN_DEG && dist <= SAFETY * stop) {
                    burning = true;
                    LOG.info("[test driver] pale suicide burn: ground {} m, stop {} m (x1.2 = {} m), speed {} m/s, a {} m/s^2, mass {} kg, thrust {} N, nose {} deg off retrograde",
                            fmt(dist), fmt(stop), fmt(SAFETY * stop), fmt(lastSpeed), fmt(decel),
                            fmt(ship.massKg()), fmt(ship.maxThrustN(pressureFraction(h))), fmt(misalign));
                }
                // Latched: once below the low speed the engines stay off, instead of toggling every tick around it.
                if (burning && !cutAtLowSpeed && lastSpeed <= LOW_SPEED_MPS) {
                    cutAtLowSpeed = true;
                    LOG.info("[test driver] pale: engines cut at {} m/s, ground {} m", fmt(lastSpeed), fmt(dist));
                }
                if (burning && !cutAtLowSpeed && !ship.isLanded()) {
                    throttle = ShipInputPayload.THROTTLE_FULL;
                }
                if (!shot200 && dist < 200) {
                    shot200 = true;
                    schedule("pale-02-200m-first-person.png", 0);
                }
                if (phaseTicks % 10 == 0) {
                    LOG.info("[test driver] pale descent: h {} m, ground {} m, speed {} m/s, vy {} m/s, stop {} m, burning {}",
                            fmt(h), fmt(dist), fmt(lastSpeed), fmt(vy), fmt(stop), burning);
                }
                if (ship.isLanded()) {
                    next(Phase.LANDED, "landed");
                    LOG.info("[test driver] pale OUTCOME: LANDED intact; contact speed {} m/s, vertical {} m/s (limits {} / {} m/s), regime flips {}: {}",
                            fmt(prevSpeed), fmt(-prevVy), fmt(CRASH_CONTACT_MPS), fmt(CRASH_VERTICAL_MPS), flips,
                            prevSpeed <= CRASH_CONTACT_MPS && -prevVy <= CRASH_VERTICAL_MPS ? "within the limits" : "over a limit");
                    schedule("pale-03-ground-first-person.png", 0);
                }
            }
            case LANDED -> {
                throttle = ShipInputPayload.THROTTLE_CUT;
                if (phaseTicks == LANDED_FIRST_TICKS) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (phaseTicks == LANDED_THIRD_TICKS) {
                    schedule("pale-04-ground-third-person.png", 0);
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

    private void schedule(String name, int leadTicks) {
        pendingShot = name;
        pendingAt = phaseTicks + leadTicks;
    }

    /** Periapsis height above Pale's radius, m (negative once the orbit meets the surface). */
    private static double periapsisM(OrbitStatePayload p) {
        CelestialBody body = MeridianSystem.PALE;
        return Elements.of(body.mu, stateOf(p)).periapsisRadius() - body.radius;
    }

    /**
     * Degrees between the ship's nose and the retrograde direction, in the surface frame (the ship's own velocity and
     * orientation, as the surface SAS uses them).
     */
    private static double retrogradeMisalignDeg(ShipEntity ship) {
        Vec3 v = ship.getVelocity();
        if (v.lengthSqr() < 1e-6) {
            return 180;
        }
        Vector3f retro = new Vector3f((float) -v.x, (float) -v.y, (float) -v.z).normalize();
        Vector3f body = ship.getOrientation().conjugate(new Quaternionf()).transform(retro);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, body.y))));
    }

    /** Ground height under the ship: the top of the surface blocks, in block y (m). */
    private static double groundY(ShipEntity ship) {
        return ship.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(ship.getX()), (int) Math.floor(ship.getZ()));
    }

    /** Net deceleration a = T / m - g, with full-throttle thrust at the ship's height h above sea level, m/s^2. */
    private static double netDecelMps2(ShipEntity ship, double h) {
        CelestialBody sys = FlightDimensions.system(BodyId.PALE);
        double g = sys.gravity(sys.radius + Math.max(h, 0));
        return ship.maxThrustN(pressureFraction(h)) / ship.massKg() - g;
    }

    /** Pressure over sea-level pressure at height h above sea level (Pale has no air, so this is 0). */
    private static double pressureFraction(double h) {
        CelestialBody sys = FlightDimensions.system(BodyId.PALE);
        double rho = sys.density(Math.max(h, 0));
        return sys.seaLevelDensity > 0 ? rho / sys.seaLevelDensity : 0;
    }
}
