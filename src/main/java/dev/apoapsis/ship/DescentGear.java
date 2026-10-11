package dev.apoapsis.ship;

/**
 * The numbers and maths of the descent gear: parachutes (drag) and landing legs (shock absorption). Pure functions,
 * so the physics in {@link ShipEntity} and the GameTests read the same rules.
 */
public final class DescentGear {
    /** Chute state: not deployed. */
    public static final int STOWED = 0;
    /** Chute state: shredded by a deployment that was too fast; the chutes are gone from the ship. */
    public static final int TORN = -1;
    /** Ticks a deployed chute takes to fill, so the drag grows over two seconds instead of snapping on. */
    public static final int INFLATE_TICKS = 40;
    /** Fastest ship speed, m/s, at which the chutes survive being deployed. */
    public static final double SHRED_SPEED_MPS = 250.0;
    /**
     * Drag area Cd * A of one fully open chute, m^2. Sized for the Kestrel: with four chutes and four legs it weighs
     * about 9.5 t dry, and a terminal speed under 8 m/s at sea level needs about 2,500 m^2 of Cd * A in all. The drawn
     * canopy is a stylised icon, not to scale.
     */
    public static final double CHUTE_CD_A_M2 = 650.0;

    /**
     * How much larger, in metres, the ship's bounding cube is while a canopy stands over it, so the chase camera backs
     * off and the whole canopy stays in view.
     */
    public static final double CANOPY_REACH_M = 4.0;

    /** Vertical impact speed, m/s, a ship survives without enough legs. */
    public static final double BASE_CRASH_VERTICAL_MPS = 12.0;
    /** Extra survivable vertical impact speed per leg at the lowest layer, m/s. */
    public static final double LEG_BONUS_MPS = 4.0;
    /** The most vertical impact speed any leg set absorbs, m/s. */
    public static final double MAX_CRASH_VERTICAL_MPS = 24.0;
    /** Legs at the lowest layer needed before they absorb anything. */
    public static final int MIN_LEGS = 3;
    /** How far above the ship's lowest block centre, in blocks, a leg still counts as being on the lowest layer. */
    public static final double LOWEST_LAYER_TOLERANCE = 0.5;

    private DescentGear() {}

    /** How open the chutes are, 0 (stowed or torn) to 1 (full), for a chute state of {@code ticks} since deployment. */
    public static double inflation(int ticks) {
        if (ticks <= STOWED) {
            return 0.0;
        }
        return Math.min(ticks, INFLATE_TICKS) / (double) INFLATE_TICKS;
    }

    /** Drag area Cd * A of {@code chutes} chutes in chute state {@code ticks}, m^2. */
    public static double chuteDragAreaM2(int chutes, int ticks) {
        return Math.max(chutes, 0) * CHUTE_CD_A_M2 * inflation(ticks);
    }

    /**
     * Terminal speed, m/s, of a ship falling in air of density {@code airDensity} kg/m^3 under gravity {@code gravity}
     * m/s^2, where the hull has drag area {@code hullDragAreaM2} and {@code chutes} chutes are fully open. Drag is
     * F = 1/2 rho v^2 CdA, balanced against the weight. Infinite in a vacuum.
     */
    public static double terminalSpeedMps(double massKg, double gravity, double airDensity, double hullDragAreaM2, int chutes) {
        double area = hullDragAreaM2 + chuteDragAreaM2(chutes, INFLATE_TICKS);
        if (airDensity <= 0 || area <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(2.0 * massKg * gravity / (airDensity * area));
    }

    /** Result of checking whether the chutes may open: {@link #OK}, {@link #NO_AIR} or {@link #TOO_FAST}. */
    public enum DeployCheck {
        /** The chutes may open. */
        OK,
        /** There is no air to catch the ship: the air density is zero or below. */
        NO_AIR,
        /** The ship is faster than {@link DescentGear#SHRED_SPEED_MPS}, so the chutes would shred. */
        TOO_FAST
    }

    /**
     * Whether the chutes may open now. {@link DeployCheck#NO_AIR} when {@code airDensity} kg/m^3 is not positive,
     * otherwise {@link DeployCheck#TOO_FAST} when {@code airspeedMps} m/s is above {@link #SHRED_SPEED_MPS}, otherwise
     * {@link DeployCheck#OK}.
     */
    public static DeployCheck deployCheck(double airDensity, double airspeedMps) {
        if (airDensity <= 0) {
            return DeployCheck.NO_AIR;
        }
        if (airspeedMps > SHRED_SPEED_MPS) {
            return DeployCheck.TOO_FAST;
        }
        return DeployCheck.OK;
    }

    /**
     * The vertical impact speed, m/s, above which the ship is destroyed, given the number of landing legs on its
     * lowest layer: 12 without legs or with fewer than 3, otherwise 12 + 4 per leg, never more than 24.
     */
    public static double verticalCrashLimitMps(int legsAtLowestLayer) {
        if (legsAtLowestLayer < MIN_LEGS) {
            return BASE_CRASH_VERTICAL_MPS;
        }
        return Math.min(MAX_CRASH_VERTICAL_MPS, BASE_CRASH_VERTICAL_MPS + LEG_BONUS_MPS * legsAtLowestLayer);
    }
}
