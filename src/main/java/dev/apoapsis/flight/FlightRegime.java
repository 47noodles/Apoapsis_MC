package dev.apoapsis.flight;

/** SURFACE: flat-frame physics in a body's surface dimension. ORBIT: patched-conic flight in apoapsis:orbit. Append only. */
public enum FlightRegime {
    SURFACE,
    ORBIT;

    public static FlightRegime byOrdinal(int i) {
        return values()[Math.floorMod(i, values().length)];
    }
}
