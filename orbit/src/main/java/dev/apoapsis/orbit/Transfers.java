package dev.apoapsis.orbit;

/**
 * Pure transfer maths shared by the map and the test driver: where a Hohmann transfer must start, and how long a
 * launch window stays open. No game state.
 */
public final class Transfers {
    private static final double TURN = 2 * Math.PI;

    private Transfers() {}

    /**
     * Lead of a target over the departing ship at the moment a Hohmann transfer from radius {@code r1} to the target's
     * circular orbit at radius {@code r2} starts, rad. The lead is the angle the target moves ahead of the ship's burn
     * point, measured from the ship's position to the target's, so it is pi minus the angle the target sweeps during the
     * half-period transfer. {@code mu} is the parent's gravitational parameter (m^3/s^2) and {@code targetPeriodS} the
     * target's orbit period (s).
     */
    public static double hohmannLead(double mu, double r1, double r2, double targetPeriodS) {
        double a = (r1 + r2) / 2;
        double transferS = Math.PI * Math.sqrt(a * a * a / mu);
        return Math.PI - TURN * transferS / targetPeriodS;
    }

    /**
     * Seconds until a lead error reaches a whole turn again: the time t >= 0 at which
     * {@code errorRad + rateRadPerS * t} is a multiple of 2 pi. {@code errorRad} is the lead minus the wanted lead, rad;
     * {@code rateRadPerS} is how fast the lead changes, rad/s. NaN when the rate is zero or not finite.
     */
    public static double windowSeconds(double errorRad, double rateRadPerS) {
        if (rateRadPerS == 0 || !Double.isFinite(rateRadPerS)) {
            return Double.NaN;
        }
        double ahead = rateRadPerS > 0 ? -errorRad : errorRad;
        double remaining = ((ahead % TURN) + TURN) % TURN;
        return remaining / Math.abs(rateRadPerS);
    }
}
