package dev.apoapsis.orbit;

/**
 * Suicide-burn cue for a retro-burn landing at constant full thrust, with no drag.
 * Speeds are downward-positive m/s, accelerations are m/s^2, distances are m and times are s.
 */
public final class Landing {
    /** Padding factor applied to the stopping distance. */
    public static final double SAFETY = 1.10;
    /** Net braking, m/s^2, below which the ship cannot stop. */
    public static final double MIN_NET_DECEL = 0.5;

    private Landing() {
    }

    /** Net upward deceleration while braking: thrustAccel - gravity, m/s^2. */
    public static double netDecel(double thrustAccel, double gravity) {
        return thrustAccel - gravity;
    }

    /**
     * Distance needed to stop from vDown at constant deceleration: vDown^2 / (2 * netDecel), m.
     * Returns 0 if vDown <= 0 and +Infinity if netDecel <= 0.
     */
    public static double stoppingDistance(double vDown, double netDecel) {
        if (vDown <= 0) {
            return 0;
        }
        if (netDecel <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return vDown * vDown / (2 * netDecel);
    }

    /**
     * Seconds of free fall (no drag) until a full-thrust retro burn must start, so that the ship is at rest
     * SAFETY-padded above the ground.
     * <p>
     * With a = netDecel(thrustAccel, gravity), k = SAFETY and fallen distance d(t) = vDown*t + g*t^2/2 and speed
     * v(t) = vDown + g*t, the burn starts when radarAlt - d(t) = k * v(t)^2 / (2a). Expanding gives
     * g*B*t^2 + 2*vDown*B*t + (k*vDown^2 - 2*a*radarAlt) = 0 with B = a + k*g, whose positive root is
     * t = (-vDown*B + sqrt(vDown^2*B^2 - g*B*(k*vDown^2 - 2*a*radarAlt))) / (g*B). For g below 1e-9 the linear
     * solution t = (2*a*radarAlt - k*vDown^2) / (2*vDown*B) is used.
     *
     * @return +Infinity if vDown <= 0, NaN if the net braking is below MIN_NET_DECEL, 0 if the burn is due now or
     *     overdue, otherwise the time to the burn start in seconds
     */
    public static double burnStartIn(double radarAlt, double vDown, double gravity, double thrustAccel) {
        if (vDown <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double a = netDecel(thrustAccel, gravity);
        if (a < MIN_NET_DECEL) {
            return Double.NaN;
        }
        if (radarAlt <= SAFETY * stoppingDistance(vDown, a)) {
            return 0;
        }
        double b = a + SAFETY * gravity;
        double c = SAFETY * vDown * vDown - 2 * a * radarAlt;
        if (gravity < 1e-9) {
            return -c / (2 * vDown * b);
        }
        double disc = vDown * vDown * b * b - gravity * b * c;
        return (-vDown * b + Math.sqrt(disc)) / (gravity * b);
    }

    /**
     * Metres of room left if the burn started now: radarAlt - SAFETY * stoppingDistance(vDown, netDecel).
     * Negative means the ship will hit the ground.
     */
    public static double margin(double radarAlt, double vDown, double netDecel) {
        return radarAlt - SAFETY * stoppingDistance(vDown, netDecel);
    }
}
