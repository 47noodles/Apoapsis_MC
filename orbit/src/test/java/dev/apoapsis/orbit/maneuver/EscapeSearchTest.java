package dev.apoapsis.orbit.maneuver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.orbit.Kepler;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.StateVector;
import dev.apoapsis.orbit.Vec3d;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EscapeSearchTest {
    private static final CelestialBody PALE = MeridianSystem.PALE;
    private static final double UT = 100_000;
    private static final double ALT_M = 12_000;
    private static final double TARGET_PE_M = 6_000;

    /** A circular, prograde, equatorial orbit of Pale at {@link #ALT_M}, with the ship on +X at {@link #UT}. */
    private static StateVector parking() {
        double r = PALE.radius + ALT_M;
        return new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(PALE.mu / r), 0));
    }

    private static double periAfter(double burnUt, double dv) {
        StateVector atBurn = Kepler.propagate(PALE.mu, parking(), burnUt - UT);
        StateVector after = Maneuvers.applyAt(atBurn, new ManeuverNode(burnUt, dv, 0, 0));
        return EscapeSearch.parentPeriapsisAltM(PALE, after, burnUt);
    }

    @Test
    void aSmallProgradeBurnAtTheOpenPhaseHitsTheTargetPeriapsis() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            EscapeSearch.Plan plan = EscapeSearch.planAt(PALE, parking(), UT, UT, TARGET_PE_M);
            assertNotNull(plan, "the window is open at the start phase");
            // Leaving Pale's sphere of influence from 12 km up takes about 30 m/s, far under the circular speed (140 m/s).
            assertTrue(plan.deltaV() > 15 && plan.deltaV() < 60, "burn was " + plan.deltaV());
            assertEquals(TARGET_PE_M, plan.periapsisAltM(), 200, "periapsis after the burn");
            // A burn 0.2 m/s smaller leaves the periapsis above the target: this one is the smallest that works.
            assertTrue(periAfter(UT, plan.deltaV() - 0.2) > TARGET_PE_M, "a smaller burn should miss the target");
            assertTrue(plan.entryUt() > UT + 600 && plan.entryUt() < UT + 3600, "entry at " + (plan.entryUt() - UT));
        });
    }

    @Test
    void noBurnWorksWhenTheShipIsOnTheClosedSideOfThePhase() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () ->
                // About 800 s later the ship is on the far side of Pale, where even 200 m/s does not send it home.
                assertNull(EscapeSearch.planAt(PALE, parking(), UT, UT + 800, TARGET_PE_M)));
    }

    @Test
    void findWithoutALongitudeTargetPicksTheSmallestBurnInTheWindow() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            EscapeSearch.Plan best = EscapeSearch.find(PALE, parking(), UT, UT, UT + 1500, 30, TARGET_PE_M, Double.NaN, 200);
            assertNotNull(best);
            for (double t = UT; t <= UT + 1500; t += 100) {
                EscapeSearch.Plan other = EscapeSearch.planAt(PALE, parking(), UT, t, TARGET_PE_M);
                if (other != null) {
                    assertTrue(best.deltaV() <= other.deltaV() + 0.3,
                            "plan at +" + (t - UT) + " s needs " + other.deltaV() + ", best was " + best.deltaV());
                }
            }
        });
    }

    @Test
    void findWithALongitudeTargetLandsWithinOneDegreeOfAReachableOne() {
        assertTimeoutPreemptively(Duration.ofSeconds(240), () -> {
            // Each lap moves Pale about 90 degrees and the open window covers about 40, so only some longitudes can be
            // reached in a dozen laps: aim at the entry longitudes of three burns that are known to work.
            double period = 2 * Math.PI * Math.sqrt(Math.pow(PALE.radius + ALT_M, 3) / PALE.mu);
            double synodic = 1 / (1 / period - 1 / PALE.orbitPeriod());
            for (int lap : new int[] {2, 5, 8}) {
                EscapeSearch.Plan reference = EscapeSearch.planAt(PALE, parking(), UT, UT + lap * synodic + 30, TARGET_PE_M);
                assertNotNull(reference, "lap " + lap);
                EscapeSearch.Plan plan = EscapeSearch.find(PALE, parking(), UT, UT, UT + 12 * period, 45, TARGET_PE_M,
                        reference.entryLonRad(), 200);
                assertNotNull(plan, "lap " + lap);
                double err = Math.abs(Math.IEEEremainder(plan.entryLonRad() - reference.entryLonRad(), 2 * Math.PI));
                assertTrue(Math.toDegrees(err) < 1.0, "lap " + lap + " missed by " + Math.toDegrees(err) + " deg");
                assertEquals(TARGET_PE_M, plan.periapsisAltM(), 300);
            }
        });
    }

    @Test
    void findWithAPenaltyFunctionPrefersTheEntryItScoresBest() {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            double period = 2 * Math.PI * Math.sqrt(Math.pow(PALE.radius + ALT_M, 3) / PALE.mu);
            double synodic = 1 / (1 / period - 1 / PALE.orbitPeriod());
            EscapeSearch.Plan early = EscapeSearch.planAt(PALE, parking(), UT, UT + 30, TARGET_PE_M);
            EscapeSearch.Plan later = EscapeSearch.planAt(PALE, parking(), UT, UT + 5 * synodic + 30, TARGET_PE_M);
            assertNotNull(early);
            assertNotNull(later);
            // Only longitudes near the later plan's entry are acceptable (a dry landing site there).
            double site = later.entryLonRad();
            EscapeSearch.Plan plan = EscapeSearch.find(PALE, parking(), UT, UT, UT + 8 * synodic, 45, TARGET_PE_M,
                    lon -> Math.max(0, Math.abs(Math.IEEEremainder(lon - site, 2 * Math.PI)) - Math.toRadians(0.5)), 200);
            assertNotNull(plan);
            double err = Math.abs(Math.IEEEremainder(plan.entryLonRad() - site, 2 * Math.PI));
            assertTrue(Math.toDegrees(err) < 1.0, "missed the acceptable band by " + Math.toDegrees(err) + " deg");
        });
    }
}
