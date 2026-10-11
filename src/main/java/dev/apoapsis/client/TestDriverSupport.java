package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.flight.FlightDimensions;
import dev.apoapsis.flight.FlightRegime;
import dev.apoapsis.net.OrbitStatePayload;
import dev.apoapsis.net.ShipInputPayload;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Elements;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.TrajectoryPredictor;
import dev.apoapsis.orbit.Vec3d;
import dev.apoapsis.ship.SasMode;
import dev.apoapsis.ship.ShipEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Helpers shared by the scripts of {@link TestDriver}: screenshots, state logs, input packets, steering and SAS. A script
 * is ticked once per client tick; its ship is null once the pilot has left it (a crash ends the run).
 */
final class TestDriverSupport {
    private static final Logger LOG = LogUtils.getLogger();

    /** One scripted run. Returns false when the run is finished. */
    interface Script {
        boolean tick(Minecraft mc, ShipEntity ship);
    }

    private TestDriverSupport() {}

    /** Saves a screenshot of the main view into the game's screenshots folder. */
    static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1,
                message -> LOG.info("[test driver] screenshot {}: {}", name, message.getString()));
    }

    /** Sends one input packet for {@code ship}. Throttle commands and one-shot flags are per packet. */
    static void send(ShipEntity ship, byte pitch, byte yaw, byte throttle, byte flags) {
        ClientPacketDistributor.sendToServer(new ShipInputPayload(ship.getId(), pitch, yaw, (byte) 0, throttle, flags));
    }

    /** The payload's state as a vector in the inertial frame of its body. */
    static StateVector stateOf(OrbitStatePayload p) {
        return new StateVector(new Vec3d(p.rx(), p.ry(), p.rz()), new Vec3d(p.vx(), p.vy(), p.vz()));
    }

    /** True in the orbit regime (patched conics), false on a surface. */
    static boolean inOrbit(OrbitStatePayload p) {
        return FlightRegime.byOrdinal(p.regime()) == FlightRegime.ORBIT;
    }

    /** Altitude in m: above the body's radius in orbit, above sea level on a surface (the regime's own measure). */
    static double altitudeM(ShipEntity ship, OrbitStatePayload p) {
        if (inOrbit(p)) {
            return stateOf(p).r().length() - FlightDimensions.system(BodyId.byOrdinal(p.body())).radius;
        }
        return ship.getY() - FlightDimensions.seaLevel(BodyId.byOrdinal(p.body()));
    }

    /** Speed in m/s: the orbit speed, or the ship's own speed on a surface. */
    static double speedMps(ShipEntity ship, OrbitStatePayload p) {
        return inOrbit(p) ? stateOf(p).v().length() : ship.getVelocity().length();
    }

    /** One line with the regime, body, altitude and speed. */
    static void logState(ShipEntity ship, OrbitStatePayload p, String when) {
        LOG.info("[test driver] {}: regime {} body {} alt {} m speed {} m/s", when,
                FlightRegime.byOrdinal(p.regime()), BodyId.byOrdinal(p.body()).id, fmt(altitudeM(ship, p)),
                fmt(speedMps(ship, p)));
    }

    /** An orbit log: the elements, and the predicted patches over six hours. */
    static void logOrbit(OrbitStatePayload p, String when) {
        CelestialBody body = FlightDimensions.system(BodyId.byOrdinal(p.body()));
        StateVector s = stateOf(p);
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

    /**
     * Pitch and yaw inputs that turn the nose toward {@code sign} * velocity, and the error in degrees: the returned
     * array is {pitch, yaw, angle}. Used by the scripts that steer by hand (sign +1 prograde, -1 retrograde).
     */
    static double[] steer(ShipEntity ship, OrbitStatePayload p, int sign) {
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

    /**
     * Presses SAS toward {@code target}: one press every 8 ticks, since the mode syncs back to the client late. Returns
     * true once the ship reads {@code target}.
     */
    static boolean sasTo(ShipEntity ship, SasMode target, int tick) {
        if (ship.getSasMode() == target) {
            return true;
        }
        if (tick % 8 == 0) {
            send(ship, (byte) 0, (byte) 0, ShipInputPayload.THROTTLE_HOLD, ShipInputPayload.FLAG_TOGGLE_SAS);
        }
        return false;
    }

    static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
