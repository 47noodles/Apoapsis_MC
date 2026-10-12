package dev.apoapsis.client;

import com.mojang.logging.LogUtils;
import dev.apoapsis.orbit.CelestialBody;
import dev.apoapsis.planet.TerrainSampler;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Where on Verdance the return-trip script may land: stretches of the equator (z 0, the way the ground track runs) that
 * are dry and fairly flat, read from the world generator's own terrain heights through the integrated server. The
 * first run of the script landed on the sea floor, 16 m under the water, and called it a landing on legs; the escape
 * search is told to prefer entries that end on one of these stretches.
 */
final class ReturnLandingSites {
    private static final Logger LOG = LogUtils.getLogger();
    /** Metres between sampled columns. */
    private static final int STEP_M = 100;
    /**
     * A site's half-width, m: every column within it must be dry and the heights within {@link #MAX_RELIEF_M}. An entry's
     * ground track is only known to a few kilometres (two runs landed 0.4 and 2.9 km from the plan), so a site is wide.
     */
    private static final int HALF_WIDTH_M = 1500;
    /** Dry means at least this far above sea level, m. */
    private static final int MIN_ABOVE_SEA_M = 3;
    private static final int MAX_RELIEF_M = 60;

    /** Centres (x, m) of the acceptable sites, ascending, within one circumference of the body. */
    private final double[] centres;
    private final double radiusM;
    private final int seaLevel;

    private ReturnLandingSites(double[] centres, double radiusM, int seaLevel) {
        this.centres = centres;
        this.radiusM = radiusM;
        this.seaLevel = seaLevel;
    }

    /** Samples the terrain of {@code server}'s overworld (Verdance) along z 0; null when the server is not there. */
    static ReturnLandingSites scan(MinecraftServer server, CelestialBody verdance) {
        if (server == null || server.overworld() == null) {
            LOG.warn("[test driver] return landing sites: no integrated server (server {})", server);
            return null;
        }
        TerrainSampler sampler = TerrainSampler.of(server.overworld());
        TerrainSampler.Worker worker = sampler.worker();
        double circumference = 2 * Math.PI * verdance.radius;
        int n = (int) (circumference / STEP_M);
        int half = HALF_WIDTH_M / STEP_M;
        float[] ground = new float[n];
        for (int i = 0; i < n; i++) {
            ground[i] = worker.ground((int) xOf(i, n), 0);
        }
        double[] found = new double[n];
        int count = 0;
        int dry = 0;
        float highest = -Float.MAX_VALUE;
        for (float g : ground) {
            highest = Math.max(highest, g);
            if (g >= sampler.seaLevel() + MIN_ABOVE_SEA_M) {
                dry++;
            }
        }
        LOG.info("[test driver] return landing sites: {} of {} equator columns are above sea level {} (highest {} m)", dry, n,
                sampler.seaLevel(), highest);
        for (int i = 0; i < n; i++) {
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            for (int k = -half; k <= half; k++) {
                float g = ground[Math.floorMod(i + k, n)];
                lo = Math.min(lo, g);
                hi = Math.max(hi, g);
            }
            if (lo >= sampler.seaLevel() + MIN_ABOVE_SEA_M && hi - lo <= MAX_RELIEF_M) {
                found[count++] = xOf(i, n);
            }
        }
        double[] centres = new double[count];
        System.arraycopy(found, 0, centres, 0, count);
        return new ReturnLandingSites(centres, verdance.radius, sampler.seaLevel());
    }

    private static double xOf(int i, int n) {
        return (i - n / 2) * (double) STEP_M;
    }

    /** True when no stretch of the equator qualifies. */
    boolean isEmpty() {
        return centres.length == 0;
    }

    /** Number of qualifying sample columns (each is the centre of an acceptable site). */
    int count() {
        return centres.length;
    }

    /** Sea level of the terrain the sites were read from, m. */
    int seaLevel() {
        return seaLevel;
    }

    /**
     * The penalty for the escape search: the angle (rad) from the entry longitude's landing point to the nearest acceptable
     * site, 0 on one. {@code rangeM} is the distance flown from the atmosphere top to the ground, eastward.
     */
    DoubleUnaryOperator penalty(double rangeM) {
        double circ = 2 * Math.PI * radiusM;
        return lon -> {
            double x = lon * radiusM + rangeM;
            x = Math.IEEEremainder(x, circ);
            double best = Double.MAX_VALUE;
            for (double c : centres) {
                double d = Math.abs(Math.IEEEremainder(x - c, circ));
                if (d < best) {
                    best = d;
                }
            }
            return best / radiusM;
        };
    }

    /** The nearest acceptable site's x to a landing x, m (for the log). */
    double nearest(double x) {
        double circ = 2 * Math.PI * radiusM;
        double best = Double.NaN;
        double bd = Double.MAX_VALUE;
        for (double c : centres) {
            double d = Math.abs(Math.IEEEremainder(x - c, circ));
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        return best;
    }
}
