package dev.apoapsis.orbit;

/**
 * The prototype's bodies, with the figures from tools/calc/system.py (g0 = 9.81). Owned by node ORB.
 * VERDANCE: R 64 km, 1.00 g (mu = 9.81 * 64000^2), atmosphere top 12 km, 1.225 kg/m^3, scale height 2 km.
 * PALE: R 20 km, 0.16 g (mu = 0.16 * 9.81 * 20000^2), no atmosphere, circular orbit radius 320 km, phase 0 at ut 0.
 */
public final class MeridianSystem {
    private MeridianSystem() {}

    public static final CelestialBody VERDANCE = new CelestialBody("verdance", 9.81 * 64000.0 * 64000.0, 64000, 12000,
            1.225, 2000, null, 0, 0);
    public static final CelestialBody PALE = new CelestialBody("pale", 0.16 * 9.81 * 20000.0 * 20000.0, 20000, 0, 0,
            1, VERDANCE, 320000, 0);

    /** Body by id ("verdance", "pale"), or null. */
    public static CelestialBody byId(String id) {
        return switch (id) {
            case "verdance" -> VERDANCE;
            case "pale" -> PALE;
            default -> null;
        };
    }
}
