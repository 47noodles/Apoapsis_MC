package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TransfersTest {
    private static final double TOL = 1e-9;

    @Test
    void hohmannLeadMatchesHandComputedValue() {
        // mu = 1, r1 = 1, r2 = 4, so a = 2.5 and the target period is 2 pi sqrt(64) = 16 pi.
        // transfer / period = sqrt((a / r2)^3) / 2 = sqrt(0.244140625) / 2, so lead = pi (1 - sqrt(0.244140625)).
        double period = 2 * Math.PI * Math.sqrt(4.0 * 4.0 * 4.0);
        double expected = Math.PI * (1 - Math.sqrt(0.244140625));
        assertEquals(expected, Transfers.hohmannLead(1.0, 1.0, 4.0, period), TOL);
        assertEquals(1.5893, expected, 1e-4);
    }

    @Test
    void windowSecondsWaitsAlmostATurnWhenRateIsPositiveAndErrorIsAhead() {
        // Error +0.5 rad, rate +0.25 rad/s: the error grows, so it next reaches a whole turn after (2 pi - 0.5) / 0.25 s.
        assertEquals((2 * Math.PI - 0.5) / 0.25, Transfers.windowSeconds(0.5, 0.25), TOL);
    }

    @Test
    void windowSecondsIsShortWhenRateIsPositiveAndErrorIsBehind() {
        // Error -0.5 rad, rate +0.25 rad/s: the error reaches zero in 2 s.
        assertEquals(2.0, Transfers.windowSeconds(-0.5, 0.25), TOL);
    }

    @Test
    void windowSecondsCountsDownWhenRateIsNegative() {
        // Error +0.5 rad falling at 0.25 rad/s reaches zero in 2 s.
        assertEquals(2.0, Transfers.windowSeconds(0.5, -0.25), TOL);
    }

    @Test
    void windowSecondsIsNaNForZeroOrNonFiniteRate() {
        assertTrue(Double.isNaN(Transfers.windowSeconds(0.5, 0.0)));
        assertTrue(Double.isNaN(Transfers.windowSeconds(0.5, Double.NaN)));
    }
}
