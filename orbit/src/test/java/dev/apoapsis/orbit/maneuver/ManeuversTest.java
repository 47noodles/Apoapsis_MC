package dev.apoapsis.orbit.maneuver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Patch;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManeuversTest {

    private static StateVector hohmannShip() {
        double th = Math.toRadians(-91.4);
        return new StateVector(new Vec3d(79000 * Math.cos(th), 79000 * Math.sin(th), 0),
                new Vec3d(903.2 * -Math.sin(th), 903.2 * Math.cos(th), 0));
    }

    /** Apoapsis radius from the post-burn state, via energy and angular momentum. */
    private static double apoapsis(double mu, StateVector s) {
        double r = s.r().length();
        double v2 = s.v().lengthSquared();
        double energy = v2 / 2 - mu / r;
        double h = s.r().cross(s.v()).length();
        double a = -mu / (2 * energy);
        double e = Math.sqrt(1 + 2 * energy * h * h / (mu * mu));
        return a * (1 + e);
    }

    @Test
    void radialBurnPointsAwayFromTheBody() {
        double r = 79000;
        double v = Math.sqrt(MeridianSystem.VERDANCE.mu / r);
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, v, 0));
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            // radial-out = prograde x normal = y x z = +x, so a +5 m/s radial burn is (+5, 0, 0).
            Vec3d dv = Maneuvers.worldDeltaV(s, new ManeuverNode(0, 0, 0, 5));
            assertTrue(dv.dot(s.r()) > 0, "radial burn should point away from the body");
            assertEquals(5, dv.x(), 1e-9);
            assertEquals(0, dv.y(), 1e-9);
            assertEquals(0, dv.z(), 1e-9);
        });
    }

    @Test
    void hohmannBurnReachesTargetApoapsisWithinHalfPercent() {
        double mu = MeridianSystem.VERDANCE.mu;
        double r1 = 79000;
        double r2 = 120000;
        double v1 = Math.sqrt(mu / r1);
        double dv1 = v1 * (Math.sqrt(2 * r2 / (r1 + r2)) - 1);
        StateVector s = new StateVector(new Vec3d(r1, 0, 0), new Vec3d(0, v1, 0));
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            StateVector after = Maneuvers.applyAt(s, new ManeuverNode(0, dv1, 0, 0));
            double ra = apoapsis(mu, after);
            double relErr = Math.abs(ra - r2) / r2;
            assertTrue(relErr < 0.005, "apoapsis " + ra + " vs target " + r2 + ", rel err " + relErr);
        });
    }

    @Test
    void tenDegreeNormalBurnTiltsVelocityByTenDegrees() {
        double r = 79000;
        double v = Math.sqrt(MeridianSystem.VERDANCE.mu / r);
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, v, 0));
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            ManeuverNode node = new ManeuverNode(0, 0, v * Math.tan(Math.toRadians(10)), 0);
            StateVector after = Maneuvers.applyAt(s, node);
            double dot = s.v().dot(after.v());
            double angleDeg = Math.toDegrees(Math.acos(dot / (s.v().length() * after.v().length())));
            assertEquals(10.0, angleDeg, 0.1);
        });
    }

    @Test
    void burnSecondsMatchesHandComputedValue() {
        // ve = 300 * 9.80665 = 2941.995; t = 1000 * ve / 10000 * (1 - exp(-100 / ve)) = 9.831957 s
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            double t = Maneuvers.burnSeconds(100, 1000, 300, 10000);
            assertEquals(9.831957, t, 1e-4);
        });
    }

    @Test
    void predictAfterNodeReachesPale() {
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            List<Patch> patches = Maneuvers.predictAfter(MeridianSystem.VERDANCE, hohmannShip(), 0,
                    new ManeuverNode(100, 0, 0, 0), 3000, 8);
            assertTrue(patches.size() >= 2, "expected at least 2 patches, got " + patches.size());
            assertEquals(MeridianSystem.PALE, patches.get(1).body());
        });
    }
}
