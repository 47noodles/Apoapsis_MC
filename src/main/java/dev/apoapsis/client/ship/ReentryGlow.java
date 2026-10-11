package dev.apoapsis.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/**
 * The plasma sheath of a ship heated by re-entry: a shock bubble on the windward side and a wake streaming aft. Both are
 * translucent quads in the ship's centre-of-mass frame, drawn with the lightning render type. The sheath fades out as the
 * camera nears the ship, so the view from inside the hull stays clear. Client only.
 */
final class ReentryGlow {
    /** No sheath below this heat intensity. */
    private static final float MIN_INTENSITY = 0.05f;
    /** The camera fade ramps from 0 at this many ship radii to 1 at {@link #FADE_FAR}. */
    private static final double FADE_NEAR = 0.7;
    private static final double FADE_FAR = 1.3;
    /** No sheath below this camera fade. */
    private static final float MIN_FADE = 0.02f;
    /** Bubble semi-axes in ship radii: along the wind, and across it. */
    private static final double BUBBLE_AXIAL = 1.3;
    private static final double BUBBLE_RADIAL = 0.95;
    /** The bubble ends where its normal has dot product -RIM_DOT with the wind, just behind the equator. */
    private static final double RIM_DOT = 0.15;
    private static final int BUBBLE_RINGS = 10;
    /** Wake base and tip radii in ship radii; length in ship radii is {@code 2 + 2.5 * intensity}. */
    private static final double WAKE_BASE_RADIUS = 0.7;
    private static final double WAKE_TIP_RADIUS = 0.05;
    private static final double WAKE_LENGTH_BASE = 2.0;
    private static final double WAKE_LENGTH_HEAT = 2.5;
    private static final int WAKE_RINGS = 6;
    private static final int SEGMENTS = 24;
    /** Vertex order of a quad as built, then reversed. Culling is on, so each side of a quad draws one copy. */
    private static final int[][] ORDERS = {{0, 1, 2, 3}, {0, 3, 2, 1}};

    private ReentryGlow() {}

    /**
     * Draws the sheath. The pose origin is the ship's centre of mass in world axes. {@code wind} is the unit direction of
     * motion, {@code radius} the ship's half size in metres, {@code toCamera} the camera position minus the centre of mass.
     */
    static void submit(PoseStack poseStack, SubmitNodeCollector collector, float intensity, Vec3 wind, double radius,
            Vec3 toCamera) {
        if (intensity < MIN_INTENSITY || wind.lengthSqr() < 1e-12) {
            return;
        }
        float fade = (float) smoothstep(FADE_NEAR * radius, FADE_FAR * radius, toCamera.length());
        if (fade < MIN_FADE) {
            return;
        }
        Vec3 w = wind.normalize();
        Vec3 helper = Math.abs(w.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 a = w.cross(helper).normalize();
        Vec3 b = w.cross(a);
        float heat = (float) Math.pow(intensity, 1.5);
        Look look = new Look(255, mix(90, 235, heat), mix(30, 180, heat), 0.45f * intensity * fade, toCamera);
        Grid bubble = bubble(w, a, b, radius);
        Grid wake = wake(w, a, b, intensity, radius);
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(),
            (pose, buffer) -> emit(pose, buffer, bubble, look));
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(),
            (pose, buffer) -> emit(pose, buffer, wake, look));
    }

    /** The windward half of an ellipsoid: rings of latitude from the nose to a rim just behind the equator. */
    private static Grid bubble(Vec3 w, Vec3 a, Vec3 b, double radius) {
        double rim = Math.atan2(BUBBLE_AXIAL * RIM_DOT, BUBBLE_RADIAL * Math.sqrt(1 - RIM_DOT * RIM_DOT));
        double thetaMax = Math.PI / 2 + rim;
        Vec3[][] pos = new Vec3[BUBBLE_RINGS + 1][SEGMENTS];
        Vec3[][] nrm = new Vec3[BUBBLE_RINGS + 1][SEGMENTS];
        for (int i = 0; i <= BUBBLE_RINGS; i++) {
            double theta = thetaMax * i / BUBBLE_RINGS;
            double cs = Math.cos(theta);
            double sn = Math.sin(theta);
            for (int j = 0; j < SEGMENTS; j++) {
                Vec3 c = radial(a, b, j);
                pos[i][j] = w.scale(radius * BUBBLE_AXIAL * cs).add(c.scale(radius * BUBBLE_RADIAL * sn));
                nrm[i][j] = w.scale(cs / BUBBLE_AXIAL).add(c.scale(sn / BUBBLE_RADIAL)).normalize();
            }
        }
        // Full strength over the nose, fading to nothing at the rim, so the bubble has no hard edge around the hull.
        double[] scale = new double[BUBBLE_RINGS + 1];
        for (int i = 0; i <= BUBBLE_RINGS; i++) {
            scale[i] = 1.0 - smoothstep(0.55, 1.0, (double) i / BUBBLE_RINGS);
        }
        return new Grid(pos, nrm, scale);
    }

    /** A cone from the centre of mass aft along -wind, narrowing to its tip. Alpha fades to nothing at the tip. */
    private static Grid wake(Vec3 w, Vec3 a, Vec3 b, float intensity, double radius) {
        Vec3 aft = w.scale(-1);
        double length = (WAKE_LENGTH_BASE + WAKE_LENGTH_HEAT * intensity) * radius;
        double r0 = WAKE_BASE_RADIUS * radius;
        double r1 = WAKE_TIP_RADIUS * radius;
        double slope = (r0 - r1) / length;
        Vec3[][] pos = new Vec3[WAKE_RINGS + 1][SEGMENTS];
        Vec3[][] nrm = new Vec3[WAKE_RINGS + 1][SEGMENTS];
        double[] scale = new double[WAKE_RINGS + 1];
        for (int i = 0; i <= WAKE_RINGS; i++) {
            double t = (double) i / WAKE_RINGS;
            double rr = r0 + (r1 - r0) * t;
            scale[i] = Math.pow(1 - t, 2.2);
            for (int j = 0; j < SEGMENTS; j++) {
                Vec3 c = radial(a, b, j);
                pos[i][j] = aft.scale(length * t).add(c.scale(rr));
                // Side normal of the cone: the radial direction tilted toward the tip.
                nrm[i][j] = c.add(aft.scale(slope)).normalize();
            }
        }
        return new Grid(pos, nrm, scale);
    }

    /** The unit vector at segment {@code j} of a circle in the plane of {@code a} and {@code b}. */
    private static Vec3 radial(Vec3 a, Vec3 b, int j) {
        double phi = 2 * Math.PI * j / SEGMENTS;
        return a.scale(Math.cos(phi)).add(b.scale(Math.sin(phi)));
    }

    /** Emits every cell of a grid as two quads, one per winding, so each face is drawn from whichever side it faces. */
    private static void emit(PoseStack.Pose pose, VertexConsumer buffer, Grid grid, Look look) {
        int[] rings = new int[4];
        int[] segs = new int[4];
        for (int i = 0; i + 1 < grid.pos().length; i++) {
            for (int j = 0; j < SEGMENTS; j++) {
                int j1 = (j + 1) % SEGMENTS;
                rings[0] = i;
                rings[1] = i + 1;
                rings[2] = i + 1;
                rings[3] = i;
                segs[0] = j;
                segs[1] = j;
                segs[2] = j1;
                segs[3] = j1;
                for (int[] order : ORDERS) {
                    for (int k : order) {
                        int r = rings[k];
                        int s = segs[k];
                        vertex(pose, buffer, grid.pos()[r][s], grid.nrm()[r][s], grid.ringScale()[r], look);
                    }
                }
            }
        }
    }

    /** One vertex: heat colour, and alpha from the strength, the Fresnel term and the ring scale. */
    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 p, Vec3 n, double ringScale, Look look) {
        Vec3 v = look.toCamera().subtract(p);
        double len = v.length();
        double facing = len < 1e-9 ? 0.0 : Math.abs(n.dot(v)) / len;
        double fres = Math.pow(1 - facing, 2.0) * 0.8 + 0.2;
        int alpha = (int) Math.round(255 * Math.min(1.0, look.strength() * fres * ringScale));
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(look.r(), look.g(), look.b(), alpha);
    }

    /** Hermite ramp from 0 at {@code edge0} to 1 at {@code edge1}. */
    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.min(1.0, Math.max(0.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private static int mix(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    /** A mesh as rings by segments of points and unit normals, with an alpha scale per ring. */
    private record Grid(Vec3[][] pos, Vec3[][] nrm, double[] ringScale) {}

    /** The colour of a mesh, its alpha before the Fresnel and ring terms, and the camera offset for the Fresnel term. */
    private record Look(int r, int g, int b, float strength, Vec3 toCamera) {}
}
