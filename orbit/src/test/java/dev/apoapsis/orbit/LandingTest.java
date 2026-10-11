package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LandingTest {
    private static final double EARTH_G = 9.81;
    private static final double MOON_G = 1.62;
    private static final double DT = 0.0005;

    @Test
    void stoppingDistanceIsVSquaredOverTwiceDecel() {
        assertEquals(25, Landing.stoppingDistance(10, 2), 1e-12);
        assertEquals(0, Landing.stoppingDistance(0, 2), 0);
        assertEquals(0, Landing.stoppingDistance(-3, 2), 0);
        assertEquals(Double.POSITIVE_INFINITY, Landing.stoppingDistance(10, 0));
        assertEquals(Double.POSITIVE_INFINITY, Landing.stoppingDistance(10, -1));
    }

    @Test
    void netDecelIsThrustMinusGravity() {
        assertEquals(10.19, Landing.netDecel(20, EARTH_G), 1e-9);
    }

    @Test
    void marginIsRoomLeftAfterPadding() {
        assertEquals(72.5, Landing.margin(100, 10, 2), 1e-9);
        assertTrue(Landing.margin(10, 10, 2) < 0);
        assertEquals(Double.NEGATIVE_INFINITY, Landing.margin(100, 10, 0));
    }

    @Test
    void burnStartIsInfiniteWhenNotDescending() {
        assertEquals(Double.POSITIVE_INFINITY, Landing.burnStartIn(1000, 0, EARTH_G, 2 * EARTH_G));
        assertEquals(Double.POSITIVE_INFINITY, Landing.burnStartIn(1000, -5, EARTH_G, 2 * EARTH_G));
    }

    @Test
    void burnStartIsNanWhenThrustCannotBeatGravity() {
        assertTrue(Double.isNaN(Landing.burnStartIn(1000, 50, EARTH_G, EARTH_G)));
        assertTrue(Double.isNaN(Landing.burnStartIn(1000, 50, EARTH_G, EARTH_G + 0.4)));
        assertFalse(Double.isNaN(Landing.burnStartIn(1000, 50, EARTH_G, EARTH_G + 0.6)));
    }

    @Test
    void burnStartIsZeroWhenAlreadyTooLow() {
        assertEquals(0, Landing.burnStartIn(10, 50, EARTH_G, 2 * EARTH_G), 0);
    }

    @Test
    void burnStartMatchesHandCalculation() {
        // Hand-solved quadratic for h=1000 m, v=50 m/s, g=9.81, thrust=2g: t is about 5.365 s.
        assertEquals(5.365, Landing.burnStartIn(1000, 50, EARTH_G, 2 * EARTH_G), 0.01);
    }

    @Test
    void burnStartIsFiniteForZeroGravity() {
        // With no gravity the burn is at t = (2*a*h - k*v^2) / (2*v*(a + 0)), here (2*10*1000 - 1.1*2500)/(2*50*10).
        double expected = (2 * 10 * 1000 - Landing.SAFETY * 2500) / (2 * 50 * 10.0);
        assertEquals(expected, Landing.burnStartIn(1000, 50, 0, 10), 1e-9);
    }

    @Test
    void simulatedBurnLandsSafelyWithinPadding() {
        assertSimulatedBurnLandsSafely(1000, 50, EARTH_G, 2 * EARTH_G);
        assertSimulatedBurnLandsSafely(300, 30, EARTH_G, 1.5 * EARTH_G);
        assertSimulatedBurnLandsSafely(5000, 150, MOON_G, 3 * MOON_G);
    }

    /**
     * Free-falls for burnStartIn seconds, burns at full thrust until at rest (semi-implicit Euler, dt = 0.0005 s)
     * and checks the ground clearance. The ideal clearance is (SAFETY - 1) * stoppingDistance at burn start.
     */
    private static void assertSimulatedBurnLandsSafely(double h0, double v0, double g, double thrust) {
        double a = Landing.netDecel(thrust, g);
        double tBurn = Landing.burnStartIn(h0, v0, g, thrust);
        assertTrue(Double.isFinite(tBurn) && tBurn > 0, "burn time " + tBurn);

        double hBurn = h0 - (v0 * tBurn + 0.5 * g * tBurn * tBurn);
        double vBurn = v0 + g * tBurn;
        assertEquals(0, Landing.margin(hBurn, vBurn, a), 1e-6);
        double tEarly = 0.5 * tBurn;
        double hEarly = h0 - (v0 * tEarly + 0.5 * g * tEarly * tEarly);
        double vEarly = v0 + g * tEarly;
        assertTrue(Landing.margin(hEarly, vEarly, a) > 0, "margin before burn");

        double h = h0;
        double v = v0;
        long freeSteps = Math.round(tBurn / DT);
        for (long i = 0; i < freeSteps; i++) {
            v += g * DT;
            h -= v * DT;
        }
        double stopDistance = Landing.stoppingDistance(v, a);
        while (v > 0) {
            v -= a * DT;
            h -= Math.max(v, 0) * DT;
        }
        double clearance = h;
        assertTrue(clearance >= 0, "clearance " + clearance + " below ground");
        double bound = (Landing.SAFETY - 1) * stopDistance + 2;
        assertTrue(clearance <= bound, "clearance " + clearance + " exceeds bound " + bound);
    }
}
