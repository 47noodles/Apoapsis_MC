package dev.apoapsis.client.map;

/** Pure orbit maths for the map's transfer card. No game state. Client only. */
public final class TransferMath {
    private TransferMath() {}

    /**
     * Total delta-v of a Hohmann transfer from a ship at radius {@code r1} (m) moving at {@code v1} (m/s) to a circular
     * orbit at radius {@code r2} around a body of gravitational parameter {@code mu} (m^3/s^2): the burn at r1 plus the
     * burn that circularises at r2, both by vis-viva. Returns NaN for inputs that are not a bound orbit.
     */
    public static double hohmannDeltaV(double mu, double r1, double v1, double r2) {
        if (!(mu > 0 && r1 > 0 && r2 > 0 && v1 >= 0) || !Double.isFinite(v1)) {
            return Double.NaN;
        }
        double a = (r1 + r2) / 2;
        double transferAtR1 = Math.sqrt(mu * (2 / r1 - 1 / a));
        double transferAtR2 = Math.sqrt(mu * (2 / r2 - 1 / a));
        double circularAtR2 = Math.sqrt(mu / r2);
        return Math.abs(transferAtR1 - v1) + Math.abs(circularAtR2 - transferAtR2);
    }
}
