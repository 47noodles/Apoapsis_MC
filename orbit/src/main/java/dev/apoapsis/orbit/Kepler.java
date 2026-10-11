package dev.apoapsis.orbit;

/**
 * Two-body propagation by the universal-variable formulation (Curtis, algorithm 3.4). Each call is analytic from the
 * given state, so repeated calls never accumulate error.
 */
public final class Kepler {
    private static final int MAX_ITERATIONS = 100;
    private static final int MAX_BRACKET_STEPS = 100;
    private static final double RELATIVE_TOLERANCE = 1e-12;
    /** Below this |z| the Stumpff functions use their Taylor series, which avoids cancellation. */
    private static final double SERIES_LIMIT = 1e-2;
    private static final double[] C_SERIES = {
            1.0 / 2, -1.0 / 24, 1.0 / 720, -1.0 / 40320, 1.0 / 3628800, -1.0 / 479001600,
            1.0 / 87178291200.0, -1.0 / 20922789888000.0};
    private static final double[] S_SERIES = {
            1.0 / 6, -1.0 / 120, 1.0 / 5040, -1.0 / 362880, 1.0 / 39916800, -1.0 / 6227020800.0,
            1.0 / 1307674368000.0, -1.0 / 355687428096000.0};

    private Kepler() {}

    /**
     * Propagates a two-body state by {@code dt} seconds (dt may be negative) for any conic (elliptic, parabolic,
     * hyperbolic), using the universal-variable formulation.
     */
    public static StateVector propagate(double mu, StateVector s, double dt) {
        if (dt == 0) {
            return s;
        }
        Vec3d r0 = s.r();
        Vec3d v0 = s.v();
        double r0Mag = r0.length();
        double rdotv = r0.dot(v0);
        double sqrtMu = Math.sqrt(mu);
        double alpha = 2 / r0Mag - v0.lengthSquared() / mu;

        double chi = solveChi(sqrtMu, alpha, r0Mag, rdotv, dt, initialGuess(mu, alpha, r0Mag, rdotv, dt));

        double chi2 = chi * chi;
        double z = alpha * chi2;
        double c = stumpffC(z);
        double sv = stumpffS(z);
        double f = 1 - chi2 / r0Mag * c;
        double g = dt - chi2 * chi / sqrtMu * sv;
        Vec3d r = r0.scale(f).add(v0.scale(g));
        double rMag = r.length();
        double fDot = sqrtMu / (rMag * r0Mag) * chi * (z * sv - 1);
        double gDot = 1 - chi2 / rMag * c;
        Vec3d v = r0.scale(fDot).add(v0.scale(gDot));
        return new StateVector(r, v);
    }

    /**
     * Solves F(chi) = 0 with F(chi) = (r0.v0/sqrt(mu)) chi^2 C(z) + (1 - alpha r0) chi^3 S(z) + r0 chi - sqrt(mu) dt.
     * dF/dchi equals the radius, so F is strictly increasing: the root is bracketed, and Newton steps that leave the
     * bracket are replaced by bisection. The guess is only a starting point, so a poor guess still converges.
     */
    private static double solveChi(double sqrtMu, double alpha, double r0, double rdotv, double dt, double guess) {
        double x = guess;
        double[] atX = residual(sqrtMu, alpha, r0, rdotv, dt, x);
        double lo = x;
        double hi = x;
        double step = Math.max(1, Math.abs(x));
        if (atX[0] < 0) {
            hi = x + step;
            for (int i = 0; i < MAX_BRACKET_STEPS && residual(sqrtMu, alpha, r0, rdotv, dt, hi)[0] <= 0; i++) {
                lo = hi;
                step *= 2;
                hi = lo + step;
            }
        } else {
            lo = x - step;
            for (int i = 0; i < MAX_BRACKET_STEPS && residual(sqrtMu, alpha, r0, rdotv, dt, lo)[0] >= 0; i++) {
                hi = lo;
                step *= 2;
                lo = hi - step;
            }
        }
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double[] at = residual(sqrtMu, alpha, r0, rdotv, dt, x);
            if (at[0] == 0) {
                return x;
            }
            if (at[0] < 0) {
                lo = x;
            } else {
                hi = x;
            }
            double next = at[1] > 0 ? x - at[0] / at[1] : Double.NaN;
            if (!(next > lo && next < hi)) {
                next = 0.5 * (lo + hi);
            }
            double dChi = next - x;
            x = next;
            if (Math.abs(dChi) <= RELATIVE_TOLERANCE * Math.abs(x)) {
                break;
            }
        }
        return x;
    }

    /** Returns {F(chi), dF/dchi} for the universal Kepler equation. */
    private static double[] residual(double sqrtMu, double alpha, double r0, double rdotv, double dt, double chi) {
        double chi2 = chi * chi;
        double z = alpha * chi2;
        double c = stumpffC(z);
        double sv = stumpffS(z);
        double f = rdotv / sqrtMu * chi2 * c + (1 - alpha * r0) * chi2 * chi * sv + r0 * chi - sqrtMu * dt;
        double fPrime = rdotv / sqrtMu * chi * (1 - z * sv) + (1 - alpha * r0) * chi2 * c + r0;
        return new double[] {f, fPrime};
    }

    /** Elliptic guess sqrt(mu)|alpha|dt; Vallado's guess for hyperbolic orbits; sqrt(mu)dt/r0 for parabolic. */
    private static double initialGuess(double mu, double alpha, double r0, double rdotv, double dt) {
        double sqrtMu = Math.sqrt(mu);
        if (alpha > 0) {
            return sqrtMu * alpha * dt;
        }
        if (alpha < 0) {
            double a = 1 / alpha;
            double sign = Math.signum(dt);
            double guess = sign * Math.sqrt(-a)
                    * Math.log(-2 * mu * alpha * dt / (rdotv + sign * Math.sqrt(-mu * a) * (1 - r0 * alpha)));
            if (Double.isFinite(guess)) {
                return guess;
            }
        }
        return sqrtMu * dt / r0;
    }

    /** Stumpff function C(z) = (1 - cos sqrt z)/z for z > 0, (cosh sqrt(-z) - 1)/(-z) for z < 0. */
    static double stumpffC(double z) {
        if (Math.abs(z) < SERIES_LIMIT) {
            return horner(C_SERIES, z);
        }
        if (z > 0) {
            return (1 - Math.cos(Math.sqrt(z))) / z;
        }
        return (Math.cosh(Math.sqrt(-z)) - 1) / -z;
    }

    /** Stumpff function S(z) = (sqrt z - sin sqrt z)/(sqrt z)^3 for z > 0, (sinh sqrt(-z) - sqrt(-z))/(sqrt(-z))^3 for z < 0. */
    static double stumpffS(double z) {
        if (Math.abs(z) < SERIES_LIMIT) {
            return horner(S_SERIES, z);
        }
        if (z > 0) {
            double s = Math.sqrt(z);
            return (s - Math.sin(s)) / (s * z);
        }
        double s = Math.sqrt(-z);
        return (Math.sinh(s) - s) / (s * -z);
    }

    /** Evaluates sum of c[k] z^k. */
    private static double horner(double[] c, double z) {
        double acc = 0;
        for (int k = c.length - 1; k >= 0; k--) {
            acc = acc * z + c[k];
        }
        return acc;
    }
}
