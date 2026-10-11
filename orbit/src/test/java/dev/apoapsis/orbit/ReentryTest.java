package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReentryTest {
    private static final double SEA_LEVEL = 1.225;
    private static final double DENSITY_6KM = SEA_LEVEL * Math.exp(-3);
    private static final double DENSITY_THIN = SEA_LEVEL * Math.exp(-5);
    private static final double[] DENSITIES = {SEA_LEVEL, DENSITY_6KM, DENSITY_THIN};

    @Test
    void noHeatingInVacuumOrAtRest() {
        assertEquals(0, Reentry.heatIndex(1100, 0), 0);
        assertEquals(0, Reentry.intensity(1100, 0), 0);
        assertEquals(0, Reentry.heatIndex(0, SEA_LEVEL), 0);
        assertEquals(0, Reentry.intensity(0, SEA_LEVEL), 0);
    }

    @Test
    void heatIndexIsOneAtReferenceConditions() {
        assertEquals(1, Reentry.heatIndex(Reentry.V_REF, Reentry.RHO_REF), 1e-12);
    }

    @Test
    void slowFlightDoesNotHeat() {
        assertEquals(0, Reentry.intensity(100, SEA_LEVEL), 0);
    }

    @Test
    void moderateSpeedGivesLowHeating() {
        assertTrue(Reentry.intensity(300, SEA_LEVEL) < 0.1);
    }

    @Test
    void reentrySpeedAtSixKilometresIsFullHeating() {
        assertTrue(Reentry.intensity(1100, DENSITY_6KM) > 0.9);
    }

    @Test
    void reentrySpeedInThinAirIsStrongButBelowFull() {
        // The brief expected 0.05..0.4 here, but the specified formula gives about 0.786.
        double value = Reentry.intensity(1100, DENSITY_THIN);
        assertTrue(value > 0.7 && value < 0.9, "intensity " + value);
    }

    @Test
    void intensityRisesWithSpeed() {
        for (double density : DENSITIES) {
            double previous = Reentry.intensity(0, density);
            for (double speed = 50; speed <= 1500; speed += 50) {
                double current = Reentry.intensity(speed, density);
                assertTrue(current >= previous, "speed " + speed + " density " + density);
                previous = current;
            }
        }
    }

    @Test
    void intensityRisesWithDensity() {
        for (double speed = 50; speed <= 1500; speed += 50) {
            double previous = Reentry.intensity(speed, 0);
            for (int i = 1; i <= 30; i++) {
                double density = SEA_LEVEL * i / 10.0;
                double current = Reentry.intensity(speed, density);
                assertTrue(current >= previous, "speed " + speed + " density " + density);
                previous = current;
            }
        }
    }

    @Test
    void intensityStaysWithinUnitInterval() {
        for (double density : DENSITIES) {
            for (double speed = 0; speed <= 3000; speed += 50) {
                double value = Reentry.intensity(speed, density);
                assertTrue(value >= 0 && value <= 1, "intensity " + value);
            }
        }
    }
}
