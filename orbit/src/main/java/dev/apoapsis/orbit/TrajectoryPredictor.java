package dev.apoapsis.orbit;

import java.util.List;

/** Patched-conic prediction. STUB from S0; node ORB implements it. */
public final class TrajectoryPredictor {
    private TrajectoryPredictor() {}

    /**
     * Predicts the coasting trajectory from state {@code s} around {@code body} at {@code ut}, switching bodies at SOI
     * boundaries, for up to {@code horizonSeconds} and {@code maxPatches} patches. Bodies considered: {@code body},
     * its parent, and its children among the MeridianSystem bodies.
     */
    public static List<Patch> predict(CelestialBody body, StateVector s, double ut, double horizonSeconds,
                                      int maxPatches) {
        return List.of(new Patch(body, ut, ut + horizonSeconds, s, Patch.EndReason.NONE));
    }
}
