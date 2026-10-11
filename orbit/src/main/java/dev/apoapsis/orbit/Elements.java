package dev.apoapsis.orbit;

/**
 * Orbit summary derived from a state vector.
 *
 * @param semiMajorAxis   metres (negative for hyperbolic, infinite for parabolic)
 * @param eccentricity    0 circular, below 1 elliptic, 1 parabolic, above 1 hyperbolic
 * @param inclination     radians from the equatorial (XY) plane
 * @param periapsisRadius metres from the body centre
 * @param apoapsisRadius  metres from the body centre; +Infinity when eccentricity is 1 or more
 * @param period          seconds; NaN when eccentricity is 1 or more
 * @param timeToPeriapsis seconds until the next periapsis (negative if it has passed on an escape trajectory)
 * @param timeToApoapsis  seconds until the next apoapsis; NaN when eccentricity is 1 or more
 */
public record Elements(double semiMajorAxis, double eccentricity, double inclination, double periapsisRadius,
                       double apoapsisRadius, double period, double timeToPeriapsis, double timeToApoapsis) {

    /** Below this eccentricity the orbit is treated as circular. */
    private static final double CIRCULAR_TOLERANCE = 1e-9;
    /** Within this distance of e = 1 the orbit is treated as parabolic (Barker's equation). */
    private static final double PARABOLIC_TOLERANCE = 1e-9;
    private static final double TWO_PI = 2 * Math.PI;

    /** Orbit elements of state {@code s} around a body with gravitational parameter {@code mu}. */
    public static Elements of(double mu, StateVector s) {
        Vec3d r = s.r();
        Vec3d v = s.v();
        double rMag = r.length();
        if (rMag == 0) {
            // The centre of the body: no orbit to describe.
            return new Elements(Double.POSITIVE_INFINITY, 1, 0, 0, Double.POSITIVE_INFINITY, Double.NaN, 0,
                    Double.NaN);
        }
        double vSq = v.lengthSquared();
        Vec3d h = r.cross(v);
        double hMag = h.length();
        Vec3d rHat = r.scale(1 / rMag);
        double e = v.cross(h).scale(1 / mu).sub(rHat).length();

        double energy = 0.5 * vSq - mu / rMag;
        double sma = Math.abs(energy) <= 1e-12 * (0.5 * vSq + mu / rMag)
                ? Double.POSITIVE_INFINITY
                : -mu / (2 * energy);
        double inclination = hMag > 0 ? Math.acos(clamp(h.z() / hMag, -1, 1)) : 0;
        double periapsis = hMag * hMag / mu / (1 + e);

        boolean circular = e < CIRCULAR_TOLERANCE;
        boolean elliptic = e < 1 - PARABOLIC_TOLERANCE;
        boolean parabolic = !elliptic && e < 1 + PARABOLIC_TOLERANCE;
        double apoapsis = elliptic ? sma * (1 + e) : Double.POSITIVE_INFINITY;
        double period = elliptic ? TWO_PI * Math.sqrt(sma * sma * sma / mu) : Double.NaN;

        double rdotv = r.dot(v);
        double timeToPeriapsis;
        double timeToApoapsis;
        if (circular) {
            timeToPeriapsis = 0;
            timeToApoapsis = period / 2;
        } else if (elliptic) {
            // Eccentric anomaly E, then mean anomaly M = E - e sin E; M is the phase measured from periapsis.
            double n = Math.sqrt(mu / (sma * sma * sma));
            double cosE = (1 - rMag / sma) / e;
            double sinE = rdotv / (e * Math.sqrt(mu * sma));
            double eccentricAnomaly = Math.atan2(sinE, cosE);
            double m = mod2Pi(eccentricAnomaly - e * sinE);
            timeToPeriapsis = angleUntil(-m) / n;
            timeToApoapsis = angleUntil(Math.PI - m) / n;
        } else if (parabolic) {
            // Barker's equation: t - tp = (1/2) sqrt(p^3/mu) (D + D^3/3), with D = (r.v)/h = tan(nu/2).
            double p = hMag * hMag / mu;
            double d = hMag > 0 ? rdotv / hMag : 0;
            double sincePeriapsis = 0.5 * Math.sqrt(p * p * p / mu) * (d + d * d * d / 3);
            timeToPeriapsis = -sincePeriapsis;
            timeToApoapsis = Double.NaN;
        } else {
            // Hyperbolic anomaly H with sinh H = (r.v)/(e sqrt(mu |a|)); M = e sinh H - H, signed.
            double absA = -sma;
            double n = Math.sqrt(mu / (absA * absA * absA));
            double sinhH = rdotv / (e * Math.sqrt(mu * absA));
            double hyperbolicAnomaly = asinh(sinhH);
            double meanAnomaly = e * sinhH - hyperbolicAnomaly;
            timeToPeriapsis = -meanAnomaly / n;
            timeToApoapsis = Double.NaN;
        }
        return new Elements(sma, e, inclination, periapsis, apoapsis, period, timeToPeriapsis, timeToApoapsis);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    /** Maps an angle into [0, 2 pi). */
    private static double mod2Pi(double x) {
        double m = x % TWO_PI;
        return m < 0 ? m + TWO_PI : m;
    }

    /** Angle still to travel to an event, in [0, 2 pi); an event within rounding noise of now counts as now. */
    private static double angleUntil(double x) {
        double angle = mod2Pi(x);
        return angle > TWO_PI - 1e-12 ? 0 : angle;
    }

    /** Inverse hyperbolic sine, accurate for small arguments. */
    private static double asinh(double x) {
        double magnitude = Math.log1p(Math.abs(x) + x * x / (1 + Math.sqrt(1 + x * x)));
        return Math.copySign(magnitude, x);
    }
}
