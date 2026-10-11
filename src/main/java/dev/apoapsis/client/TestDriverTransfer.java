package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.ShipEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * The transfer script for {@link TestDriver} ({@code apoapsis.testDriver=transfer}): from the test save's 16 km orbit,
 * warp to the Pale window, point prograde, burn until the predictor shows a Pale encounter, then coast with warp across
 * Pale's sphere of influence, with map screenshots and orbit logs before, at and after the crossing.
 */
final class TestDriverTransfer {
    private static final Logger LOG = LogUtils.getLogger();

    private enum Phase { SETTLE, WAIT_WINDOW, ALIGN, BURN, COAST, AFTER_SOI, DONE }

    private Phase phase = Phase.SETTLE;
    private int phaseTicks;
    private int afterSoiTicks;
    private byte lastBody = -1;

    /** Advances the script one client tick; returns false when finished. */
    boolean tick(Minecraft mc, ShipEntity ship) {
        OrbitStatePayload p = ClientState.orbit;
        phaseTicks++;
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
        ClientPacketDistributor.sendToServer(new ShipInputPayload(ship.getId(), pitch, yaw, (byte) 0, throttle, flags));
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

    /** Pitch and yaw inputs that turn the nose toward sign * velocity, and the error in degrees. */
    private static double[] steer(ShipEntity ship, OrbitStatePayload p, int sign) {
        // Velocity in orbit-dimension axes: (X, Z, -Y).
        Vector3f d = new Vector3f((float) p.vx(), (float) p.vz(), (float) -p.vy()).normalize().mul(sign);
        Quaternionf q = ship.getOrientation();
        Vector3f dBody = q.conjugate(new Quaternionf()).transform(new Vector3f(d));
        Vector3f nose = new Vector3f(0, 1, 0);
        Vector3f forward = new Vector3f(0, 0, -1); // the Kestrel's cockpit faces north
        Vector3f right = new Vector3f(forward).cross(nose).normalize();
        Vector3f e = new Vector3f(nose).cross(dBody);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, nose.dot(dBody)))));
        double er = e.dot(right);
        double ef = e.dot(forward);
        double mag = Math.max(1e-6, Math.hypot(er, ef));
        double deadband = angle < 2 ? 2 : 0.35;
        byte pitch = (byte) (er / mag > deadband ? 1 : er / mag < -deadband ? -1 : 0);
        byte yaw = (byte) (ef / mag > deadband ? 1 : ef / mag < -deadband ? -1 : 0);
        return new double[] {pitch, yaw, angle};
    }

    private static boolean encounterPredicted(OrbitStatePayload p) {
        CelestialBody body = BodyId.byOrdinal(p.body()) == BodyId.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        // Only an encounter on this half-orbit counts (a Hohmann arrival), not a grazing one orbits later.
        List<Patch> patches = TrajectoryPredictor.predict(body, s, p.ut(), 6 * 3600, 3);
        return patches.stream().anyMatch(pt -> pt.end() == Patch.EndReason.ENCOUNTER && pt.endUt() - p.ut() < 2000);
    }

    private static void logOrbit(OrbitStatePayload p, String when) {
        CelestialBody body = BodyId.byOrdinal(p.body()) == BodyId.PALE ? MeridianSystem.PALE : MeridianSystem.VERDANCE;
        StateVector s = new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
        Elements e = Elements.of(body.mu, s);
        List<Patch> patches = TrajectoryPredictor.predict(body, s, p.ut(), 6 * 3600, 3);
        StringBuilder sb = new StringBuilder();
        for (Patch pt : patches) {
            sb.append(pt.body().id).append('/').append(pt.end()).append('@').append(fmt(pt.endUt() - p.ut())).append("s ");
        }
        LOG.info("[test driver] {}: ut {} body {} alt {} km speed {} m/s e {} Pe {} km Ap {} km warp x{}; patches: {}",
                when, fmt(p.ut()), body.id, fmt((s.r().length() - body.radius) / 1000), fmt(s.v().length()),
                String.format("%.3f", e.eccentricity()), fmt((e.periapsisRadius() - body.radius) / 1000),
                fmt((e.apoapsisRadius() - body.radius) / 1000), fmt(p.warp()), sb);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1,
                message -> LOG.info("[test driver] screenshot {}", name));
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
