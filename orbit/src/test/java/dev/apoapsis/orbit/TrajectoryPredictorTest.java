package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrajectoryPredictorTest {
    private static final CelestialBody VERDANCE = MeridianSystem.VERDANCE;
    private static final CelestialBody PALE = MeridianSystem.PALE;
    /** Encounter time of the Hohmann ship before the per-sample step change (916.815050 s). */
    private static final double HOHMANN_ENCOUNTER_UT = 916.815;

    /** Ship at 79 km on Verdance, on the transfer ellipse that reaches Pale's orbit (r = 320 km). */
    private static StateVector hohmannShip() {
        double th = Math.toRadians(-91.4);
        return new StateVector(new Vec3d(79000 * Math.cos(th), 79000 * Math.sin(th), 0),
                new Vec3d(903.2 * -Math.sin(th), 903.2 * Math.cos(th), 0));
    }

    /** A state 16 km above Verdance's surface (r = 80 km), moving tangentially at {@code k} times circular speed. */
    private static StateVector sixteenKmOrbit(double k) {
        double r = VERDANCE.radius + 16000;
        return new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, k * Math.sqrt(VERDANCE.mu / r), 0));
    }

    @Test
    void hohmannShipEncountersPaleAndThenHitsIt() {
        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, hohmannShip(), 0, 3000, 8);

        Patch first = patches.get(0);
        assertSame(VERDANCE, first.body());
        assertEquals(Patch.EndReason.ENCOUNTER, first.end());
        // Pale's SOI is entered at about 917 s, before apoapsis (1,396 s), which is already inside it. The SOI
        // (60.6 km) is the definition used here; see the report on the 1,150-1,450 s window.
        assertTrue(first.endUt() > 900 && first.endUt() < 930, "encounter at " + first.endUt() + " s");
        assertEquals(HOHMANN_ENCOUNTER_UT, first.endUt(), 1.0, "encounter time unchanged by the step change");
        double soi = PALE.soiRadius();
        Vec3d relative = first.stateAt(first.endUt()).r().sub(PALE.stateAt(first.endUt()).r());
        assertEquals(soi, relative.length(), 1.0, "distance to Pale at the encounter");
        assertFalse(first.stateAt(first.endUt() - 1).r().sub(PALE.stateAt(first.endUt() - 1).r()).length() < soi);

        Patch second = patches.get(1);
        assertSame(PALE, second.body());
        assertEquals(first.endUt(), second.startUt(), 0);
    }

    @Test
    void suborbitalShipEndsWithAtmosphere() {
        StateVector s = new StateVector(new Vec3d(84000, 0, 0), new Vec3d(0, 100, 0));

        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, s, 0, 3000, 8);

        assertEquals(Patch.EndReason.ATMOSPHERE, patches.get(0).end());
        assertEquals(1, patches.size());
        assertTrue(patches.get(0).endUt() < 120, "atmosphere entry at " + patches.get(0).endUt() + " s");
    }

    @Test
    void bisectionFinishesAtHugeUniverseTime() {
        // At ut = 1e13 s adjacent doubles are about 2 ms apart, so the 1 ms bisection tolerance is unreachable.
        StateVector s = new StateVector(new Vec3d(84000, 0, 0), new Vec3d(0, 100, 0));
        List<Patch> patches = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> TrajectoryPredictor.predict(VERDANCE, s, 1e13, 3000, 8));

        assertEquals(Patch.EndReason.ATMOSPHERE, patches.get(0).end());
        assertTrue(patches.get(0).endUt() >= 1e13, "endUt " + patches.get(0).endUt());
    }

    @Test
    void radialEscapeFromPaleReturnsToVerdance() {
        StateVector s = new StateVector(new Vec3d(30000, 0, 0), new Vec3d(900, 0, 0));

        List<Patch> patches = TrajectoryPredictor.predict(PALE, s, 0, 3000, 8);

        assertSame(PALE, patches.get(0).body());
        assertEquals(Patch.EndReason.ESCAPE, patches.get(0).end());
        assertSame(VERDANCE, patches.get(1).body());
        assertEquals(Patch.EndReason.NONE, patches.get(1).end());
        assertEquals(3000, patches.get(1).endUt(), 0);
    }

    @Test
    void quietOrbitRunsToTheHorizon() {
        // Circular 30 km above Pale's centre: outside its 20 km radius and inside its 60.6 km SOI.
        double r = 30000;
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(PALE.mu / r), 0));

        List<Patch> patches = TrajectoryPredictor.predict(PALE, s, 50, 600, 8);

        assertEquals(1, patches.size());
        assertEquals(Patch.EndReason.NONE, patches.get(0).end());
        assertEquals(650, patches.get(0).endUt(), 0);
    }

    @Test
    void patchCapEndsTheLastPatchWithNoneAtTheEvent() {
        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, hohmannShip(), 0, 3000, 1);

        assertEquals(1, patches.size());
        assertEquals(Patch.EndReason.NONE, patches.get(0).end());
        assertTrue(patches.get(0).endUt() > 900 && patches.get(0).endUt() < 930, "ends at " + patches.get(0).endUt());
    }

    @Test
    void predictionNeverReturnsAnEmptyList() {
        assertEquals(1, TrajectoryPredictor.predict(VERDANCE, hohmannShip(), 0, 3000, 0).size());
        assertEquals(1, TrajectoryPredictor.predict(VERDANCE, hohmannShip(), 0, 0, 8).size());
    }

    @Test
    void hohmannPredictionIsFast() {
        TrajectoryPredictor.predict(VERDANCE, hohmannShip(), 0, 3000, 8);

        long start = System.nanoTime();
        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, hohmannShip(), 0, 3000, 8);
        long elapsed = System.nanoTime() - start;

        System.out.printf("TrajectoryPredictor.predict (Hohmann, 3000 s, warm): %.3f ms%n", elapsed / 1e6);
        assertFalse(patches.isEmpty());
        assertTrue(elapsed < 200_000_000L, "predict took " + elapsed / 1e6 + " ms");
    }

    @Test
    void barelyBoundOrbitAt14140PredictsQuickly() {
        assertBarelyBoundOrbitPredictsQuickly(1.4140);
    }

    @Test
    void barelyBoundOrbitAt14142PredictsQuickly() {
        assertBarelyBoundOrbitPredictsQuickly(1.4142);
    }

    /**
     * The map asks for 1.2 periods of a closed orbit; for these orbits that is far beyond 24 h, so the horizon is
     * clamped and the march stays short.
     */
    private static void assertBarelyBoundOrbitPredictsQuickly(double k) {
        StateVector s = sixteenKmOrbit(k);
        double horizon = 1.2 * Elements.of(VERDANCE.mu, s).period();
        TrajectoryPredictor.predict(VERDANCE, s, 0, horizon, 3);

        long start = System.nanoTime();
        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, s, 0, horizon, 3);
        long elapsed = System.nanoTime() - start;

        System.out.printf("TrajectoryPredictor.predict (16 km, %.4fx circular, 1.2 periods, warm): %.3f ms, %d patches%n",
                k, elapsed / 1e6, patches.size());
        assertFalse(patches.isEmpty());
        assertTrue(patches.get(patches.size() - 1).endUt() <= TrajectoryPredictor.MAX_HORIZON_SECONDS);
        assertTrue(elapsed < 500_000_000L, "predict took " + elapsed / 1e6 + " ms");
    }

    @Test
    void stateInsidePaleSoiLabelledAsVerdancePredictsAroundPale() {
        // Circular 0.99 of Pale's SOI radius out from Pale, but labelled as a state around Verdance.
        double d = 0.99 * PALE.soiRadius();
        StateVector relative = new StateVector(new Vec3d(d, 0, 0), new Vec3d(0, Math.sqrt(PALE.mu / d), 0));
        StateVector paleNow = PALE.stateAt(0);
        StateVector s = new StateVector(paleNow.r().add(relative.r()), paleNow.v().add(relative.v()));

        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, s, 0, 3000, 8);

        Patch first = patches.get(0);
        assertSame(PALE, first.body());
        assertEquals(0, first.startUt(), 0);
        assertEquals(0, first.startState().r().sub(relative.r()).length(), 1e-6, "start state in Pale's frame");
        assertEquals(Patch.EndReason.NONE, first.end());
        assertEquals(3000, first.endUt(), 0);
    }

    @Test
    void stepBudgetEndsAPatchShortOfALongHorizon() {
        // Verdance orbit at 79 km (period about 700 s): the step is 1 s, so 20,000 samples reach only 20,000 s.
        double r = VERDANCE.radius + 15000;
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(VERDANCE.mu / r), 0));

        List<Patch> patches = TrajectoryPredictor.predict(VERDANCE, s, 0, 1e6, 8);

        Patch last = patches.get(patches.size() - 1);
        assertEquals(Patch.EndReason.NONE, last.end());
        assertEquals(TrajectoryPredictor.MAX_STEPS_PER_PATCH, last.endUt(), 1e-6);
    }
}
