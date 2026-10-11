package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeplerTest {
    private static final double MU = MeridianSystem.VERDANCE.mu;

    /** Periapsis state at radius rp on a conic of eccentricity e (perpendicular velocity, vis-viva). */
    private static StateVector periapsisState(double rp, double e) {
        double vp = Math.sqrt(MU * (1 + e) / rp);
        return new StateVector(new Vec3d(rp, 0, 0), new Vec3d(0, vp, 0));
    }

    private static double specificEnergy(StateVector s) {
        return 0.5 * s.v().lengthSquared() - MU / s.r().length();
    }

    @Test
    void circularOrbitReturnsToStartAfterOnePeriod() {
        double r = 79000;
        StateVector start = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(MU / r), 0));
        double period = 2 * Math.PI * Math.sqrt(r * r * r / MU);

        StateVector after = Kepler.propagate(MU, start, period);

        assertEquals(0, after.r().sub(start.r()).length(), 1e-6 * r);
        assertEquals(0, after.v().sub(start.v()).length(), 1e-6 * Math.sqrt(MU / r));
    }

    @Test
    void zeroTimeReturnsTheSameState() {
        StateVector start = periapsisState(70000, 0.5);
        assertSame(start, Kepler.propagate(MU, start, 0));
    }

    @Test
    void ellipticEnergyAndAngularMomentumAreConserved() {
        StateVector start = periapsisState(70000, 0.5);
        double energy = specificEnergy(start);
        double hMag = start.r().cross(start.v()).length();
        for (double dt : new double[] {1000, 10000}) {
            StateVector s = Kepler.propagate(MU, start, dt);
            assertEquals(energy, specificEnergy(s), 1e-9 * Math.abs(energy), "energy after " + dt + " s");
            assertEquals(hMag, s.r().cross(s.v()).length(), 1e-9 * hMag, "|h| after " + dt + " s");
        }
    }

    @Test
    void hyperbolicEnergyAndAngularMomentumAreConserved() {
        StateVector start = periapsisState(70000, 1.5);
        double energy = specificEnergy(start);
        double hMag = start.r().cross(start.v()).length();
        for (double dt : new double[] {1000, 10000}) {
            StateVector s = Kepler.propagate(MU, start, dt);
            assertEquals(energy, specificEnergy(s), 1e-9 * Math.abs(energy), "energy after " + dt + " s");
            assertEquals(hMag, s.r().cross(s.v()).length(), 1e-9 * hMag, "|h| after " + dt + " s");
        }
    }

    @Test
    void propagatingForwardThenBackReturnsToStart() {
        StateVector[] starts = {periapsisState(70000, 0.5), periapsisState(70000, 1.5)};
        for (StateVector start : starts) {
            for (double dt : new double[] {1000, 10000}) {
                StateVector there = Kepler.propagate(MU, start, dt);
                StateVector back = Kepler.propagate(MU, there, -dt);
                assertEquals(0, back.r().sub(start.r()).length(), 1e-6, "round trip over " + dt + " s");
                assertEquals(0, back.v().sub(start.v()).length(), 1e-6, "velocity round trip over " + dt + " s");
            }
        }
    }

    @Test
    void negativeTimeMatchesReverseOfForward() {
        StateVector start = periapsisState(70000, 0.5);
        StateVector before = Kepler.propagate(MU, start, -500);
        StateVector forward = Kepler.propagate(MU, before, 500);
        assertEquals(0, forward.r().sub(start.r()).length(), 1e-6);
    }

    @Test
    void radialOutwardHyperbolaPropagatesOutward() {
        // Radial, outward, hyperbolic: angular momentum is zero, so only energy is a check.
        double mu = MeridianSystem.PALE.mu;
        StateVector start = new StateVector(new Vec3d(30000, 0, 0), new Vec3d(900, 0, 0));
        double energy = 0.5 * 900 * 900 - mu / 30000;
        StateVector after = Kepler.propagate(mu, start, 40);
        assertTrue(after.r().length() > 60000, "radius after 40 s is " + after.r().length());
        assertEquals(energy, 0.5 * after.v().lengthSquared() - mu / after.r().length(), 1e-9 * Math.abs(energy));
    }
}
