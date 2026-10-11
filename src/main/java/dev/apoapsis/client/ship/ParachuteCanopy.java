package dev.apoapsis.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * A stylised parachute canopy: a red and white dome with six lines to the pack. Drawn per parachute block while the
 * chutes are open; it is an icon, not to scale.
 */
final class ParachuteCanopy {
    private static final int GORES = 12;
    private static final int RINGS = 4;
    /** Polar angles of the dome rings from the zenith, in degrees. The last ring is the rim. */
    private static final double[] RING_DEG = {0.0, 24.0, 48.0, 70.0};
    private static final double RIM_DEG = 70.0;
    private static final float LINE_HALF_WIDTH = 0.03f;
    private static final int[] RED = {181, 56, 47};
    private static final int[] WHITE = {232, 226, 212};
    private static final int[] LINE = {32, 32, 36};

    private ParachuteCanopy() {
    }

    /**
     * Writes the canopy in body axes, relative to the centre of mass and +Y up. (ax, ay, az) is the parachute block
     * centre; (ux, uz) is the horizontal outward direction, or zero; inflation runs 0 (stowed) to 1 (open).
     */
    static void emit(PoseStack.Pose pose, VertexConsumer buffer, float ax, float ay, float az,
                     float ux, float uz, float inflation) {
        float f = inflation;
        float rise = 1.2f + (8.0f - 1.2f) * f;
        float radius = 3.0f * (0.25f + 0.75f * f);
        float cx = ax + ux * 3.0f * f;
        float cy = ay + rise;
        float cz = az + uz * 3.0f * f;
        float cos70 = (float) Math.cos(Math.toRadians(RIM_DEG));
        float rs = (float) (radius / Math.sin(Math.toRadians(RIM_DEG)));
        float[][][] pts = new float[RINGS][GORES][];
        for (int k = 0; k < RINGS; k++) {
            double t = Math.toRadians(RING_DEG[k]);
            float sinT = (float) Math.sin(t);
            float cosT = (float) Math.cos(t);
            for (int i = 0; i < GORES; i++) {
                double phi = 2.0 * Math.PI * i / GORES;
                pts[k][i] = new float[] {
                    cx + rs * sinT * (float) Math.cos(phi),
                    cy + rs * (cosT - cos70),
                    cz + rs * sinT * (float) Math.sin(phi)};
            }
        }
        for (int k = 0; k < RINGS - 1; k++) {
            float shade = 0.70f + 0.10f * k;
            for (int i = 0; i < GORES; i++) {
                int j = (i + 1) % GORES;
                int[] c = i % 2 == 0 ? RED : WHITE;
                doubleSided(pose, buffer, pts[k][i], pts[k][j], pts[k + 1][j], pts[k + 1][i],
                    (int) (c[0] * shade), (int) (c[1] * shade), (int) (c[2] * shade));
            }
        }
        float[] anchor = {ax, ay + 0.4f, az};
        float[] up = {0.0f, 1.0f, 0.0f};
        for (int i = 0; i < GORES; i += 2) {
            float[] rim = pts[RINGS - 1][i];
            float[] d = {rim[0] - anchor[0], rim[1] - anchor[1], rim[2] - anchor[2]};
            float[] w1 = unit(cross(d, up));
            if (w1 == null) {
                w1 = new float[] {1.0f, 0.0f, 0.0f};
            }
            float[] w2 = unit(cross(d, w1));
            if (w2 == null) {
                w2 = new float[] {0.0f, 0.0f, 1.0f};
            }
            ribbon(pose, buffer, anchor, rim, w1);
            ribbon(pose, buffer, anchor, rim, w2);
        }
    }

    /** A thin quad from a to b, widened by LINE_HALF_WIDTH along w. */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer buffer, float[] a, float[] b, float[] w) {
        doubleSided(pose, buffer,
            offset(a, w, LINE_HALF_WIDTH), offset(a, w, -LINE_HALF_WIDTH),
            offset(b, w, -LINE_HALF_WIDTH), offset(b, w, LINE_HALF_WIDTH),
            LINE[0], LINE[1], LINE[2]);
    }

    /** Writes a quad in both winding orders, so it shows from inside and outside. */
    private static void doubleSided(PoseStack.Pose pose, VertexConsumer buffer, float[] a, float[] b, float[] c,
                                    float[] d, int r, int g, int bl) {
        vertex(pose, buffer, a, r, g, bl);
        vertex(pose, buffer, b, r, g, bl);
        vertex(pose, buffer, c, r, g, bl);
        vertex(pose, buffer, d, r, g, bl);
        vertex(pose, buffer, d, r, g, bl);
        vertex(pose, buffer, c, r, g, bl);
        vertex(pose, buffer, b, r, g, bl);
        vertex(pose, buffer, a, r, g, bl);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float[] p, int r, int g, int bl) {
        buffer.addVertex(pose, p[0], p[1], p[2]).setColor(r, g, bl, 255);
    }

    private static float[] offset(float[] p, float[] w, float s) {
        return new float[] {p[0] + s * w[0], p[1] + s * w[1], p[2] + s * w[2]};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** The unit vector along v, or null when v is (nearly) zero. */
    private static float[] unit(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len < 1.0e-6f) {
            return null;
        }
        return new float[] {v[0] / len, v[1] / len, v[2] / len};
    }
}
