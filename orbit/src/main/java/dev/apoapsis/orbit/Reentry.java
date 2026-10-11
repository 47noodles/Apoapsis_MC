package dev.apoapsis.orbit;

/**
 * Heating of a ship moving fast through air. Heat flux scales with sqrt(airDensity) * speed^3, so the heat index
 * is normalised to 1 at the reference density and reference speed.
 */
public final class Reentry {
    /** Reference air density, kg/m^3. */
    public static final double RHO_REF = 1.225;
    /** Reference speed, m/s. */
    public static final double V_REF = 800.0;
    /** Heat index at which heating starts (intensity 0 at or below). */
    public static final double ON = 0.01;
    /** Heat index at which heating is at full intensity (intensity 1 at or above). */
    public static final double FULL = 0.30;

    private Reentry() {
    }

    /**
     * Heat index sqrt(airDensity / RHO_REF) * (speed / V_REF)^3, dimensionless.
     * Returns 0 if speed or airDensity is <= 0.
     */
    public static double heatIndex(double speed, double airDensity) {
        if (speed <= 0 || airDensity <= 0) {
            return 0;
        }
        double ratio = speed / V_REF;
        return Math.sqrt(airDensity / RHO_REF) * ratio * ratio * ratio;
    }

    /**
     * Heating intensity in [0, 1]: smoothstep t * t * (3 - 2t) of the heat index, where t runs linearly from
     * 0 at ON to 1 at FULL.
     */
    public static double intensity(double speed, double airDensity) {
        double index = heatIndex(speed, airDensity);
        if (index <= ON) {
            return 0;
        }
        if (index >= FULL) {
            return 1;
        }
        double t = (index - ON) / (FULL - ON);
        return t * t * (3 - 2 * t);
    }
}
