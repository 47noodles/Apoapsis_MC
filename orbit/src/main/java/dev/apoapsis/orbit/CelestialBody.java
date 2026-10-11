package dev.apoapsis.orbit;

/**
 * A body in the system. STUB from S0; node ORB implements the methods. Constructor and fields are fixed.
 * A body with a parent moves on a circular, prograde orbit in the parent's XY plane:
 * angle(ut) = orbitPhaseAtEpoch + 2*pi*ut/orbitPeriod(), position = orbitRadius*(cos, sin, 0).
 */
public final class CelestialBody {
    public final String id;
    /** Gravitational parameter GM, m^3/s^2. */
    public final double mu;
    /** Mean radius, m. */
    public final double radius;
    /** Altitude of the top of the atmosphere, m (0 if none). */
    public final double atmosphereTop;
    /** Air density at sea level, kg/m^3 (0 if none). */
    public final double seaLevelDensity;
    /** Exponential scale height of the atmosphere, m. */
    public final double scaleHeight;
    /** Parent body, or null for the root of this system. */
    public final CelestialBody parent;
    /** Orbit radius around the parent, m (0 for the root). */
    public final double orbitRadius;
    /** Orbit angle at ut = 0, radians. */
    public final double orbitPhaseAtEpoch;

    public CelestialBody(String id, double mu, double radius, double atmosphereTop, double seaLevelDensity,
                         double scaleHeight, CelestialBody parent, double orbitRadius, double orbitPhaseAtEpoch) {
        this.id = id;
        this.mu = mu;
        this.radius = radius;
        this.atmosphereTop = atmosphereTop;
        this.seaLevelDensity = seaLevelDensity;
        this.scaleHeight = scaleHeight;
        this.parent = parent;
        this.orbitRadius = orbitRadius;
        this.orbitPhaseAtEpoch = orbitPhaseAtEpoch;
    }

    /** Sphere of influence radius (Laplace): orbitRadius * (mu / parent.mu)^0.4; +Infinity for the root. */
    public double soiRadius() {
        if (parent == null) {
            return Double.POSITIVE_INFINITY;
        }
        return orbitRadius * Math.pow(mu / parent.mu, 0.4);
    }

    /** Orbit period around the parent, s; NaN for the root. */
    public double orbitPeriod() {
        if (parent == null) {
            return Double.NaN;
        }
        return 2 * Math.PI * Math.sqrt(orbitRadius * orbitRadius * orbitRadius / parent.mu);
    }

    /** Position and velocity relative to the parent at universal time {@code ut}; the zero state for the root. */
    public StateVector stateAt(double ut) {
        if (parent == null) {
            return new StateVector(Vec3d.ZERO, Vec3d.ZERO);
        }
        double angle = orbitPhaseAtEpoch + 2 * Math.PI * ut / orbitPeriod();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double speed = Math.sqrt(parent.mu / orbitRadius);
        return new StateVector(new Vec3d(orbitRadius * cos, orbitRadius * sin, 0),
                new Vec3d(-speed * sin, speed * cos, 0));
    }

    /** Air density at altitude {@code h} metres: seaLevelDensity * exp(-h / scaleHeight), 0 above atmosphereTop. */
    public double density(double h) {
        if (h < 0) {
            return seaLevelDensity;
        }
        if (h <= atmosphereTop) {
            return seaLevelDensity * Math.exp(-h / scaleHeight);
        }
        return 0;
    }

    /** Gravitational acceleration magnitude at distance {@code r} from the centre, m/s^2. */
    public double gravity(double r) {
        return mu / (r * r);
    }
}
