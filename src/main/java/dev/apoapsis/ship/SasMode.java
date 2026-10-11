package dev.apoapsis.ship;

/** What SAS does with no rotation input. G cycles OFF, HOLD, PROGRADE, RETROGRADE. The ordinal is synced; append only. */
public enum SasMode {
    /** No assistance: the ship keeps spinning. */
    OFF,
    /** Drive the angular rate to zero (hold attitude). */
    HOLD,
    /** Turn the nose along the velocity. */
    PROGRADE,
    /** Turn the nose against the velocity (for braking and landing). */
    RETROGRADE;

    public SasMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static SasMode byOrdinal(int i) {
        return values()[Math.floorMod(i, values().length)];
    }
}
