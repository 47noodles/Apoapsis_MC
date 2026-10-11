package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrajectoryPredictorTest {
    private static final CelestialBody VERDANCE = MeridianSystem.VERDANCE;
    private static final CelestialBody PALE = MeridianSystem.PALE;

    /** Ship at 79 km on Verdance, on the transfer ellipse that reaches Pale's orbit (r = 320 km). */
    private static StateVector hohmannShip() {
        double th = Math.toRadians(-91.4);
        return new StateVector(new Vec3d(79000 * Math.cos(th), 79000 * Math.sin(th), 0),
                new Vec3d(903.2 * -Math.sin(th), 903.2 * Math.cos(th), 0));
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
}
