package dev.apoapsis.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.apoapsis.orbit.Vec3d;

/**
 * The visible cap of one textured body as a reusable grid of N x N sky quads, from the sub-viewer point to the limb.
 * Each grid vertex is a sight line from the viewer that hits the sphere; the vertex sits far along that sight line,
 * so the cap covers exactly the sky the flat disc did. Its texture coordinate is the hit point's lat/lon (as
 * {@link dev.apoapsis.flight.Frames}) mapped to the payload's layout. Arrays are allocated once and rewritten each
 * frame; no per-vertex allocation. Client only.
 */
final class PlanetCap {
    /** Cells per side. */
    static final int N = 48;
    private static final int V = N + 1;
    /** Brightness of the unlit side, 0..1. */
    static final double AMBIENT = 0.10;
    /** Half-width of the soft terminator, in dot-product units: the day side ramps up across it. */
    private static final double TERMINATOR = 0.15;

    private final double[] normal = new double[3 * V * V];
    private final float[] sky = new float[3 * V * V];
    private final int[] shade = new int[V * V];
    private final double[] cosPhi = new double[V], sinPhi = new double[V];
    private final double[] cosTheta = new double[V], sinTheta = new double[V];
    /** Alpha of every vertex, 0..255, set by {@link #build}: the climb fades the planet in. */
    private int alpha = 255;

    /**
     * Fills the grid for a sphere of {@code radius} seen from {@code viewer} (inertial, relative to its centre), placing
     * vertices at {@code far} along the sight lines in the dimension's axes. Cells are finest at the limb. {@code opacity}
     * 0..1 sets the cap's alpha.
     */
    void build(double radius, Vec3d viewer, SkyMath.Axes axes, double far, Vec3d sun, double opacity) {
        alpha = (int) Math.round(255 * Math.max(0, Math.min(1, opacity)));
        double d = viewer.length();
        double vx = viewer.x(), vy = viewer.y(), vz = viewer.z();
        Vec3d dir = viewer.scale(-1 / d);
        Vec3d helper = Math.abs(dir.x()) < 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d e1 = dir.cross(helper).normalize();
        Vec3d e2 = dir.cross(e1);
        double alpha = Math.asin(Math.min(1, radius / d));
        double c = d * d - radius * radius;
        for (int k = 0; k < V; k++) {
            double phi = alpha * Math.sin(Math.PI / 2 * k / N);
            cosPhi[k] = Math.cos(phi);
            sinPhi[k] = Math.sin(phi);
        }
        for (int j = 0; j < V; j++) {
            double theta = 2 * Math.PI * j / N;
            cosTheta[j] = Math.cos(theta);
            sinTheta[j] = Math.sin(theta);
        }
        double[] m = {axes.a().x(), axes.a().y(), axes.a().z(), axes.b().x(), axes.b().y(), axes.b().z(),
            axes.c().x(), axes.c().y(), axes.c().z()};
        double sx0 = sun.x(), sy0 = sun.y(), sz0 = sun.z();
        for (int k = 0; k < V; k++) {
            for (int j = 0; j < V; j++) {
                double cp = cosPhi[k], sp = sinPhi[k];
                double ct = cosTheta[j], st = sinTheta[j];
                double sx = dir.x() * cp + (e1.x() * ct + e2.x() * st) * sp;
                double sy = dir.y() * cp + (e1.y() * ct + e2.y() * st) * sp;
                double sz = dir.z() * cp + (e1.z() * ct + e2.z() * st) * sp;
                double b = vx * sx + vy * sy + vz * sz;
                double t = -b - Math.sqrt(Math.max(0, b * b - c));
                int i = k * V + j;
                double nx = (vx + t * sx) / radius, ny = (vy + t * sy) / radius, nz = (vz + t * sz) / radius;
                normal[3 * i] = nx;
                normal[3 * i + 1] = ny;
                normal[3 * i + 2] = nz;
                sky[3 * i] = (float) ((m[0] * sx + m[1] * sy + m[2] * sz) * far);
                sky[3 * i + 1] = (float) ((m[3] * sx + m[4] * sy + m[5] * sz) * far);
                sky[3 * i + 2] = (float) ((m[6] * sx + m[7] * sy + m[8] * sz) * far);
                double diffuse = clamp((nx * sx0 + ny * sy0 + nz * sz0 + TERMINATOR) / (1 + TERMINATOR));
                shade[i] = (int) Math.round(255 * (AMBIENT + (1 - AMBIENT) * diffuse));
            }
        }
    }

    /** Writes the unit surface normal of grid vertex (k, j) into {@code out[0..2]}; for tests. */
    void normalAt(int k, int j, double[] out) {
        int i = 3 * (k * V + j);
        out[0] = normal[i];
        out[1] = normal[i + 1];
        out[2] = normal[i + 2];
    }

    /** Emits the N x N quads. Each quad's U values are unwrapped against its first corner, so none smears across the seam. */
    void emit(PoseStack.Pose pose, VertexConsumer buffer) {
        for (int k = 0; k < N; k++) {
            for (int j = 0; j < N; j++) {
                int i0 = k * V + j;
                double u0 = texU(i0);
                vertex(pose, buffer, i0, u0);
                vertex(pose, buffer, i0 + 1, u0);
                vertex(pose, buffer, i0 + V + 1, u0);
                vertex(pose, buffer, i0 + V, u0);
            }
        }
    }

    private void vertex(PoseStack.Pose pose, VertexConsumer buffer, int i, double u0) {
        // Below 1 so the south pole samples the last row; the repeat sampler would wrap V = 1 to the north edge.
        double v = Math.min(texV(Math.asin(clamp1(normal[3 * i + 2]))), 1 - 1e-6);
        int l = shade[i];
        buffer.addVertex(pose, sky[3 * i], sky[3 * i + 1], sky[3 * i + 2])
                .setUv((float) unwrap(u0, texU(i)), (float) v)
                .setColor(l, l, l, alpha);
    }

    private double texU(int i) {
        return texU(Math.atan2(normal[3 * i + 1], normal[3 * i]));
    }

    /**
     * {@code u} moved by whole turns to the copy nearest {@code u0}, so a quad whose corners straddle the seam (U = 0
     * and U = 1) interpolates across one texel column and not the whole map. The sampler repeats.
     */
    static double unwrap(double u0, double u) {
        return u0 + (u - u0 - Math.rint(u - u0));
    }

    /** Column coordinate of longitude {@code lon}: U = (lon + pi) / 2pi, so texel u is at U * width. */
    static double texU(double lon) {
        return (lon + Math.PI) / (2 * Math.PI);
    }

    /** Row coordinate of latitude {@code lat}: V = (pi/2 - lat) / pi, north edge at 0. */
    static double texV(double lat) {
        return (Math.PI / 2 - lat) / Math.PI;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /** Clamp to [-1, 1] for asin. */
    private static double clamp1(double v) {
        return Math.max(-1, Math.min(1, v));
    }
}
