package dev.apoapsis.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElementsTest {
    private static final double MU = MeridianSystem.VERDANCE.mu;

    private static StateVector periapsisState(double rp, double e) {
        double vp = Math.sqrt(MU * (1 + e) / rp);
        return new StateVector(new Vec3d(rp, 0, 0), new Vec3d(0, vp, 0));
    }

    @Test
    void circularOrbitAt79kmHasCircularElements() {
        double r = 79000;
        StateVector s = new StateVector(new Vec3d(r, 0, 0), new Vec3d(0, Math.sqrt(MU / r), 0));

        Elements el = Elements.of(MU, s);

        assertTrue(el.eccentricity() < 1e-9, "e = " + el.eccentricity());
        assertEquals(r, el.periapsisRadius(), 1);
        assertEquals(r, el.apoapsisRadius(), 1);
        assertEquals(696, el.period(), 1);
        assertEquals(0, el.timeToPeriapsis(), 0);
        assertEquals(el.period() / 2, el.timeToApoapsis(), 1e-9);
        assertEquals(0, el.inclination(), 1e-12);
    }

    @Test
    void inclinationMatchesTheAngleOfTheAngularMomentum() {
        double r = 79000;
        double vc = Math.sqrt(MU / r);
        double incl = Math.toRadians(30);
        StateVector s = new StateVector(new Vec3d(r, 0, 0),
                new Vec3d(0, vc * Math.cos(incl), vc * Math.sin(incl)));

        assertEquals(incl, Elements.of(MU, s).inclination(), 1e-9);
    }

    @Test
    void transferOrbitHasPeriapsisAndApoapsisOfTheHohmannArc() {
        double th = Math.toRadians(-91.4);
        StateVector s = new StateVector(new Vec3d(79000 * Math.cos(th), 79000 * Math.sin(th), 0),
                new Vec3d(903.2 * -Math.sin(th), 903.2 * Math.cos(th), 0));

        Elements el = Elements.of(MU, s);

        assertEquals(0.6040, el.eccentricity(), 1e-3);
        assertEquals(79000, el.periapsisRadius(), 1);
        assertEquals(320000, el.apoapsisRadius(), 1000);
        assertEquals(0, el.timeToPeriapsis(), 1e-3);
        assertEquals(el.period() / 2, el.timeToApoapsis(), 1e-3);
    }

    @Test
    void ellipticTimesCountDownToTheNextPassage() {
        StateVector start = periapsisState(70000, 0.5);
        Elements at0 = Elements.of(MU, start);
        double period = at0.period();

        Elements later = Elements.of(MU, Kepler.propagate(MU, start, 300));

        assertEquals(period - 300, later.timeToPeriapsis(), 1e-3);
        assertEquals(period / 2 - 300, later.timeToApoapsis(), 1e-3);
        assertEquals(period, later.period(), 1e-6);
    }

    @Test
    void hyperbolicTimeToPeriapsisIsNegativeOnceThePeriapsisHasPassed() {
        StateVector start = periapsisState(70000, 1.5);
        Elements at0 = Elements.of(MU, start);

        assertEquals(1.5, at0.eccentricity(), 1e-9);
        assertTrue(at0.semiMajorAxis() < 0, "a = " + at0.semiMajorAxis());
        assertEquals(Double.NaN, at0.period());
        assertEquals(Double.NaN, at0.timeToApoapsis());
        assertEquals(Double.POSITIVE_INFINITY, at0.apoapsisRadius());
        assertEquals(0, at0.timeToPeriapsis(), 1e-3);

        Elements after = Elements.of(MU, Kepler.propagate(MU, start, 1000));
        assertEquals(-1000, after.timeToPeriapsis(), 1e-3);
    }

    @Test
    void parabolicOrbitUsesBarkersEquation() {
        double rp = 79000;
        StateVector start = new StateVector(new Vec3d(rp, 0, 0), new Vec3d(0, Math.sqrt(2 * MU / rp), 0));

        Elements el = Elements.of(MU, start);
        assertEquals(1, el.eccentricity(), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, el.semiMajorAxis());
        assertEquals(Double.NaN, el.period());

        Elements after = Elements.of(MU, Kepler.propagate(MU, start, 100));
        assertEquals(-100, after.timeToPeriapsis(), 1e-3);
    }
}
