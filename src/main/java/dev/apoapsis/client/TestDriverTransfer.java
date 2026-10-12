package dev.apoapsis.client;

import static dev.apoapsis.client.TestDriverSupport.fmt;
import static dev.apoapsis.client.TestDriverSupport.logOrbit;
import static dev.apoapsis.client.TestDriverSupport.send;
import static dev.apoapsis.client.TestDriverSupport.shot;
import static dev.apoapsis.client.TestDriverSupport.steer;

import com.mojang.logging.LogUtils;
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
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * The transfer script for {@link TestDriver} ({@code apoapsis.testDriver=transfer}): from the test save's 16 km orbit,
 * warp to the Pale window, point prograde, burn until the predictor shows a Pale encounter, then coast with warp across
 * Pale's sphere of influence, with map screenshots and orbit logs before, at and after the crossing. The Pale script
 * runs these phases first, then {@link #isDone()} tells it the ship is in Pale's sphere.
 */
final class TestDriverTransfer implements TestDriverSupport.Script {
    private static final Logger LOG = LogUtils.getLogger();

    private enum Phase { SETTLE, WAIT_WINDOW, ALIGN, BURN, COAST, AFTER_SOI, DONE }

    private Phase phase = Phase.SETTLE;
    private int phaseTicks;
    private int totalTicks;
    private int afterSoiTicks;
    private byte lastBody = -1;

    /** True once the script has finished: the ship is past the Pale SOI change (or the script gave up). */
    boolean isDone() {
        return phase == Phase.DONE;
    }

    /** The current phase's name, for telemetry. */
    String phaseName() {
        return phase.name();
    }

    /** Advances the script one client tick; returns false when finished. */
    @Override
    public boolean tick(Minecraft mc, ShipEntity ship) {
        if (ship == null) {
            LOG.warn("[test driver] transfer: the pilot left the ship during {}", phase);
            return false;
        }
        phaseTicks++;
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
                if (phaseTicks > 60) {
                    next(Phase.WAIT_WINDOW, p, "settled");
                }
            }
            case WAIT_WINDOW -> {
                double err = windowError(p);
                if (phaseTicks % 20 == 0) {
                    LOG.info("[test driver] window error {} deg, warp x{}", fmt(err), fmt(p.warp()));
                }
                if (err > 8 && p.warp() < 10 && phaseTicks % 10 == 0) {
                    flags = ShipInputPayload.FLAG_WARP_UP;
                } else if (err <= 8 && p.warp() > 1 && phaseTicks % 10 == 0) {
                    flags = ShipInputPayload.FLAG_WARP_DOWN;
                } else if (err <= 8 && p.warp() <= 1) {
                    next(Phase.ALIGN, p, "window close");
                }
            }
            case ALIGN, BURN -> {
                double[] steer = steer(ship, p, +1);
                pitch = (byte) steer[0];
                yaw = (byte) steer[1];
                if (phase == Phase.ALIGN) {
                    if (phaseTicks % 20 == 0) {
                        LOG.info("[test driver] aligning prograde: off by {} deg", fmt(steer[2]));
                    }
                    if ((steer[2] < 3 && windowError(p) < 1.5) || phaseTicks > 1200) {
                        next(Phase.BURN, p, "aligned, burning");
                        throttle = ShipInputPayload.THROTTLE_FULL;
                    }
                } else {
                    throttle = ShipInputPayload.THROTTLE_FULL; // every burn tick, so one lost packet cannot stall the burn
                    if (phaseTicks % 5 == 0 && encounterPredicted(p)) {
                        throttle = ShipInputPayload.THROTTLE_CUT;
                        logOrbit(p, "encounter predicted, engines off");
                        shot(mc, "transfer-01-after-burn.png");
                        next(Phase.COAST, p, "coasting");
                    } else if (phaseTicks > 400) {
                        throttle = ShipInputPayload.THROTTLE_CUT;
                        logOrbit(p, "burn timed out without an encounter");
                        next(Phase.COAST, p, "coasting anyway");
                    }
                }
            }
            case COAST -> {
                if (phaseTicks <= 10) {
                    throttle = ShipInputPayload.THROTTLE_CUT; // every coast tick at first, so one lost packet is harmless
                }
                if (p.body() != lastBody && lastBody != -1) {
                    logOrbit(p, "SOI changed (payload body now " + BodyId.byOrdinal(p.body()).id + ")");
                    shot(mc, "transfer-02-at-soi.png");
                    next(Phase.AFTER_SOI, p, "inside the new SOI");
                } else if (phaseTicks % 10 == 0 && p.warp() < 50) {
                    flags = ShipInputPayload.FLAG_WARP_UP;
                }
                if (phaseTicks % 100 == 0) {
                    logOrbit(p, "coasting");
                }
                if (phaseTicks == 40) {
                    MapClient.openMap();
                }
                if (phaseTicks == 80) {
                    shot(mc, "transfer-01b-map-encounter.png");
                }
                if (phaseTicks > 6000) {
                    logOrbit(p, "no SOI change after 300 s of coasting");
                    next(Phase.DONE, p, "giving up");
                }
            }
            case AFTER_SOI -> {
                afterSoiTicks++;
                if (afterSoiTicks == 60) {
                    logOrbit(p, "3 s after the SOI change");
                    shot(mc, "transfer-03-after-soi.png");
                }
                if (afterSoiTicks == 200) {
                    logOrbit(p, "10 s after the SOI change");
                    shot(mc, "transfer-04-after-soi-later.png");
                    next(Phase.DONE, p, "done");
                }
            }
            case DONE -> {
                return false;
            }
        }
        lastBody = p.body();
        send(ship, pitch, yaw, throttle, flags);
        return true;
    }

    private void next(Phase to, OrbitStatePayload p, String why) {
        LOG.info("[test driver] {} -> {}: {}", phase, to, why);
        phase = to;
        phaseTicks = 0;
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
        return Math.toDegrees(Math.abs(diff)) * (diff < 0 ? 10 : 1); // passed the window: report a large error
    }

    private static boolean encounterPredicted(OrbitStatePayload p) {
        CelestialBody body = BodyId.byOrdinal(p.body()) == BodyId.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        // Only an encounter on this half-orbit counts (a Hohmann arrival), not a grazing one orbits later.
        List<Patch> patches = TrajectoryPredictor.predict(body, s, p.ut(), 6 * 3600, 3);
        return patches.stream().anyMatch(pt -> pt.end() == Patch.EndReason.ENCOUNTER && pt.endUt() - p.ut() < 2000);
    }
}
