package dev.apoapsis.client.sky;

import dev.apoapsis.orbit.Vec3d;

/**
 * Atmosphere geometry for a body in the sky, as quads on sight lines at {@code far} from the camera: an outer glow from
 * the limb out to the rim angle, and a thin halo just inside the limb. Colours are ARGB per vertex. Client only.
 */
final class AtmosphereRim {
    private AtmosphereRim() {}

    /** Segments around the limb, for both builders. */
    static final int SEGMENTS = 72;
    /** Bands in the outer glow; it has one more vertex ring than bands. */
    private static final int OUTER_RINGS = 9;
    /** Bands in the inner halo; it has one more vertex ring than bands. */
    private static final int INNER_RINGS = 4;
    /** Share of the limb angle that the inner halo reaches inward. */
    private static final double INNER_SPAN = 0.10;
    /** Alpha at the limb of the outer glow, before fade and lighting. */
    private static final double OUTER_ALPHA = 0.55;
    /** Alpha at the limb of the inner halo, on the lit side, before fade. */
    private static final double INNER_ALPHA = 0.30;
    /** Glow colour at the limb: blue-white. */
    private static final int CORE_RGB = 0xBFE2FF;
    /** Glow colour at the outer edge: deep blue. */
    private static final int DEEP_RGB = 0x3B7FE0;
    /** Colour near the day/night terminator: warm orange. */
    private static final int WARM_RGB = 0xFF9A5A;
    /** Night-side floor of the outer glow, as a share of full brightness. */
    private static final double NIGHT_FLOOR = 0.08;

    /** Vertices (x, y, z per vertex), ARGB per vertex, and quad vertex indices (4 per cell). */
    record Geometry(float[] xyz, int[] argb, int[] order) {
    }

    /**
     * The outer glow: rings from the limb {@code angle} out to {@code rimAngle}. Alpha falls to zero at the rim, and the
     * colour runs from blue-white at the limb to deep blue, with a warm band near the terminator and a dark night side.
     *
     * @param dir      unit direction to the body centre, in local axes
     * @param e1       unit axis perpendicular to {@code dir}
     * @param e2       unit axis perpendicular to {@code dir} and {@code e1}
     * @param angle    limb angle, radians
     * @param rimAngle outer angle of the glow, radians, above {@code angle}
     * @param alpha    overall fade, 0..1
     * @param sunLocal sun direction, in the same axes as {@code dir}
     * @param far      distance of every vertex from the camera
     * @param segments segments around the limb
     */
    static Geometry outer(Vec3d dir, Vec3d e1, Vec3d e2, double angle, double rimAngle, double alpha, Vec3d sunLocal,
            double far, int segments) {
        int ring = segments + 1;
        double span = Math.max(rimAngle - angle, 1e-6);
        float[] xyz = new float[(OUTER_RINGS + 1) * ring * 3];
        int[] argb = new int[(OUTER_RINGS + 1) * ring];
        for (int i = 0; i <= segments; i++) {
            Vec3d limb = limb(e1, e2, i, segments);
            double d = limb.dot(sunLocal);
            double day = smoothstep(-0.25, 0.35, d);
            double warm = 0.7 * Math.exp(-sq((d - 0.05) / 0.22));
            for (int k = 0; k <= OUTER_RINGS; k++) {
                double u = (double) k / OUTER_RINGS;
                double phi = angle + span * u;
                int v = k * ring + i;
                put(xyz, v, sightLine(dir, limb, phi).scale(far));
                int rgb = mix(mix(CORE_RGB, DEEP_RGB, Math.pow(u, 0.7)), WARM_RGB, warm);
                double light = NIGHT_FLOOR + (1 - NIGHT_FLOOR) * day;
                argb[v] = pack(OUTER_ALPHA * alpha * Math.pow(1 - u, 1.6) * light * (1 + 0.25 * warm), rgb);
            }
        }
        return new Geometry(xyz, argb, quads(OUTER_RINGS, segments));
    }

    /**
     * The inner halo: rings from the limb inward to {@code angle * (1 - INNER_SPAN)}, bright on the lit side and fading
     * to zero inward. Lies over the planet cap, so it is drawn after it.
     *
     * @param dir      unit direction to the body centre, in local axes
     * @param e1       unit axis perpendicular to {@code dir}
     * @param e2       unit axis perpendicular to {@code dir} and {@code e1}
     * @param angle    limb angle, radians
     * @param alpha    overall fade, 0..1
     * @param sunLocal sun direction, in the same axes as {@code dir}
     * @param far      distance of every vertex from the camera
     * @param segments segments around the limb
     */
    static Geometry inner(Vec3d dir, Vec3d e1, Vec3d e2, double angle, double alpha, Vec3d sunLocal, double far,
            int segments) {
        int ring = segments + 1;
        float[] xyz = new float[(INNER_RINGS + 1) * ring * 3];
        int[] argb = new int[(INNER_RINGS + 1) * ring];
        for (int i = 0; i <= segments; i++) {
            Vec3d limb = limb(e1, e2, i, segments);
            double day = smoothstep(-0.25, 0.35, limb.dot(sunLocal));
            for (int k = 0; k <= INNER_RINGS; k++) {
                double u = (double) k / INNER_RINGS;
                double phi = angle - angle * INNER_SPAN * u;
                int v = k * ring + i;
                put(xyz, v, sightLine(dir, limb, phi).scale(far));
                argb[v] = pack(INNER_ALPHA * alpha * day * sq(1 - u), CORE_RGB);
            }
        }
        return new Geometry(xyz, argb, quads(INNER_RINGS, segments));
    }

    /** Unit limb direction at segment {@code i} of {@code segments}. */
    private static Vec3d limb(Vec3d e1, Vec3d e2, int i, int segments) {
        double t = 2 * Math.PI * i / segments;
        return e1.scale(Math.cos(t)).add(e2.scale(Math.sin(t)));
    }

    /** Sight line at angle {@code phi} from the centre direction, toward limb direction {@code limb}. Unit length. */
    private static Vec3d sightLine(Vec3d dir, Vec3d limb, double phi) {
        return dir.scale(Math.cos(phi)).add(limb.scale(Math.sin(phi)));
    }

    /** Quads between consecutive rings: {@code bands} bands of {@code segments} cells, 4 indices each. */
    private static int[] quads(int bands, int segments) {
        int ring = segments + 1;
        int[] order = new int[bands * segments * 4];
        for (int k = 0; k < bands; k++) {
            for (int i = 0; i < segments; i++) {
                int o = 4 * (k * segments + i);
                int a = k * ring + i;
                order[o] = a;
                order[o + 1] = a + 1;
                order[o + 2] = a + ring + 1;
                order[o + 3] = a + ring;
            }
        }
        return order;
    }

    private static void put(float[] xyz, int vertex, Vec3d p) {
        xyz[3 * vertex] = (float) p.x();
        xyz[3 * vertex + 1] = (float) p.y();
        xyz[3 * vertex + 2] = (float) p.z();
    }

    /** ARGB from an alpha in 0..1 (clamped) and an RGB colour. */
    private static int pack(double alpha, int rgb) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Channel-wise blend of two RGB colours, {@code t} 0..1 from {@code from} to {@code to}. */
    private static int mix(int from, int to, double t) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int c0 = (from >> shift) & 0xFF;
            int c1 = (to >> shift) & 0xFF;
            out |= ((int) Math.round(c0 + (c1 - c0) * t)) << shift;
        }
        return out;
    }

    /** Hermite ramp from 0 at {@code e0} to 1 at {@code e1}, clamped outside. */
    private static double smoothstep(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    private static double sq(double x) {
        return x * x;
    }
}
