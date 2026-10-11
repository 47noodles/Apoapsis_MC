package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CelestialBodyTest {
    private static final CelestialBody VERDANCE = MeridianSystem.VERDANCE;
    private static final CelestialBody PALE = MeridianSystem.PALE;

    @Test
    void paleSphereOfInfluenceIsAbout60Point6Km() {
        assertEquals(60_600, PALE.soiRadius(), 500);
    }

    @Test
    void paleOrbitPeriodIsAbout5674Seconds() {
        assertEquals(5674, PALE.orbitPeriod(), 20);
    }

    @Test
    void rootBodyHasInfiniteSoiAndNoOrbit() {
        assertEquals(Double.POSITIVE_INFINITY, VERDANCE.soiRadius());
        assertEquals(Double.NaN, VERDANCE.orbitPeriod());
        StateVector s = VERDANCE.stateAt(1234);
        assertEquals(0, s.r().length(), 0);
        assertEquals(0, s.v().length(), 0);
    }

    @Test
    void paleStateIsCircularAndPhasedFromUtZero() {
        StateVector atZero = PALE.stateAt(0);
        assertEquals(PALE.orbitRadius, atZero.r().length(), 1e-6);
        assertEquals(Math.sqrt(VERDANCE.mu / PALE.orbitRadius), atZero.v().length(), 1e-9);
        assertEquals(PALE.orbitRadius, atZero.r().x(), 1e-6);
        assertEquals(0, atZero.r().y(), 1e-6);

        // A quarter period on, Pale has moved a quarter turn about the Z axis (prograde).
        StateVector quarter = PALE.stateAt(PALE.orbitPeriod() / 4);
        assertEquals(0, quarter.r().x(), 1e-3);
        assertEquals(PALE.orbitRadius, quarter.r().y(), 1e-3);
        assertEquals(0, quarter.r().z(), 0);
    }

    @Test
    void densityFollowsTheExponentialAndStopsAtTheTopOfTheAtmosphere() {
        assertEquals(1.225, VERDANCE.density(-5), 0);
        assertEquals(1.225, VERDANCE.density(0), 0);
        assertEquals(1.225 * Math.exp(-1), VERDANCE.density(2000), 1e-12);
        assertEquals(0, VERDANCE.density(12001), 0);
        assertEquals(0, PALE.density(0), 0);
    }

    @Test
    void systemListsBodiesAndChildren() {
        assertEquals(List.of(VERDANCE, PALE), MeridianSystem.all());
        assertEquals(List.of(PALE), MeridianSystem.children(VERDANCE));
        assertTrue(MeridianSystem.children(PALE).isEmpty());
        assertTrue(MeridianSystem.children(null).isEmpty());
        assertSame(PALE, MeridianSystem.byId("pale"));
    }
}
