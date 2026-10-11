package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntegratorTest {
    private static final CelestialBody VERDANCE = MeridianSystem.VERDANCE;

    private static double specificEnergy(StateVector s) {
        return 0.5 * s.v().lengthSquared() - VERDANCE.mu / s.r().length();
    }

    @Test
    void rk4KeepsACircularOrbitCircularOverOnePeriod() {
        double r = 79000;
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(VERDANCE.mu / r), 0));

        for (int i = 0; i < 696; i++) {
            s = Integrator.rk4(VERDANCE, s, 1, null);
        }

        assertEquals(r, s.r().length(), 1);
    }

    @Test
    void constantPrograde1MetrePerSecondSquaredRaisesSpecificEnergy() {
        double r = 79000;
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(VERDANCE.mu / r), 0));
        double before = specificEnergy(s);

        for (int i = 0; i < 100; i++) {
            s = Integrator.rk4(VERDANCE, s, 1, state -> state.v().normalize());
        }

        double gain = specificEnergy(s) - before;
        // Thrust does about |v| * t of work: roughly 71,000 J/kg for 100 s at ~713 m/s, a little more as v grows.
        assertTrue(gain > 6.0e4 && gain < 9.0e4, "energy gain " + gain + " J/kg");
    }
}
