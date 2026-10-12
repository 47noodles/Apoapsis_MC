package dev.apoapsis.client.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.map3d.OrbitCamera;
import dev.apoapsis.client.map3d.TexturedSphere;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.orbit.Vec3d;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * A lit, textured planet for the menus, drawn as a picture in picture in the same way the 3D map draws its bodies (a
 * {@link TexturedSphere} mesh in screen space, a custom-geometry submit, no depth test). The mesh is built once per
 * layout; the planet turns by sliding the texture's U coordinate, so the light, which is baked into the vertex
 * colours, stays put. An atmosphere ring is drawn over the limb. Client only.
 */
public final class PlanetPip {
    private static final RenderPipeline FLAT_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/menu_planet_flat"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final RenderType FLAT = RenderType.create("apoapsis_menu_planet_flat",
            RenderSetup.builder(FLAT_PIPELINE).createRenderSetup());
    private static final RenderPipeline TEXTURED_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/menu_planet_textured"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final Map<Identifier, RenderType> TEXTURED = new HashMap<>();
    private static final double CAMERA_DISTANCE = 12.0;
    private static final int RING_SEGMENTS = 160;

    private PlanetPip() {}

    /** Registers the renderer. Called from the mod event bus. */
    public static void onRegister(RegisterPictureInPictureRenderersEvent event) {
        event.register(State.class, Renderer::new);
    }

    private static RenderType typeFor(Identifier texture) {
        return TEXTURED.computeIfAbsent(texture, id -> RenderType.create("apoapsis_menu_planet",
                RenderSetup.builder(TEXTURED_PIPELINE)
                        .withTexture("Sampler0", id, () -> RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
                        .createRenderSetup()));
    }

    /**
     * A planet laid out on screen: the sphere mesh with vertex colours for one light direction, and the atmosphere
     * ring. Build it once for a given centre, radius and light, and reuse it every frame.
     */
    public static final class Mesh {
        final TexturedSphere sphere = new TexturedSphere();
        final float cx;
        final float cy;
        final float radius;
        final int atmosphere;
        /** Ring quads: 4 vertices per segment, each (x, y, argb). */
        final float[] ringX = new float[RING_SEGMENTS * 4];
        final float[] ringY = new float[RING_SEGMENTS * 4];
        final int[] ringArgb = new int[RING_SEGMENTS * 4];

        /**
         * Builds the mesh for a planet of {@code radiusPx} GUI pixels centred on ({@code cx}, {@code cy}), lit from
         * ({@code lx}, {@code ly}, {@code lz}) in view space (x right, y up, z toward the viewer), with an atmosphere
         * ring of colour {@code atmosphere} (alpha 0 for none). {@code tilt} is a camera pitch in drag pixels from
         * the map camera's default view (30 degrees above the equator), to choose the latitude seen at the centre.
         */
        public Mesh(float cx, float cy, float radiusPx, double tilt, double lx, double ly, double lz, int atmosphere) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radiusPx;
            this.atmosphere = atmosphere;
            double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
            lx /= len;
            ly /= len;
            lz /= len;
            OrbitCamera cam = new OrbitCamera(CAMERA_DISTANCE);
            cam.drag(0, tilt);
            Vec3d eye = cam.eye();
            double focal = radiusPx * Math.sqrt(CAMERA_DISTANCE * CAMERA_DISTANCE - 1);
            sphere.build(cam, cx, cy, focal, 0, 0, 0, 1.0, eye.x(), eye.y(), eye.z());
            for (int i = 0; i < sphere.count; i++) {
                double nx = (sphere.x[i] - cx) / radiusPx;
                double ny = -(sphere.y[i] - cy) / radiusPx;
                double nz = Math.sqrt(Math.max(0, 1 - nx * nx - ny * ny));
                double d = Math.max(0, nx * lx + ny * ly + nz * lz);
                sphere.argb[i] = shade(d, nz);
            }
            buildRing(lx, ly);
        }

        /** Day to night: a cool blue shadow, a soft terminator, a slight darkening toward the limb. */
        private static int shade(double facing, double nz) {
            double day = Math.pow(facing, 0.8);
            double limb = 0.72 + 0.28 * Math.sqrt(nz);
            double r = (0.075 + 0.925 * day) * limb;
            double g = (0.10 + 0.90 * day) * limb;
            double b = (0.17 + 0.83 * day) * limb;
            return 0xFF000000 | channel(r) << 16 | channel(g) << 8 | channel(b);
        }

        private static int channel(double v) {
            return (int) Math.round(255 * Math.max(0, Math.min(1, v)));
        }

        private void buildRing(double lx, double ly) {
            double inner = radius * 0.985;
            double outer = radius * 1.075;
            double ll = Math.max(1e-6, Math.hypot(lx, ly));
            for (int s = 0; s < RING_SEGMENTS; s++) {
                double a0 = 2 * Math.PI * s / RING_SEGMENTS;
                double a1 = 2 * Math.PI * (s + 1) / RING_SEGMENTS;
                for (int k = 0; k < 4; k++) {
                    // Corner order: inner0, outer0, outer1, inner1.
                    double a = (k == 0 || k == 1) ? a0 : a1;
                    double r = (k == 0 || k == 3) ? inner : outer;
                    double dx = Math.cos(a);
                    double dy = Math.sin(a);
                    int i = s * 4 + k;
                    ringX[i] = (float) (cx + r * dx);
                    ringY[i] = (float) (cy - r * dy);
                    // Strongest on the lit side of the limb, a faint line on the dark side.
                    double lit = Math.max(0, (dx * lx + dy * ly) / ll);
                    double strength = 0.12 + 0.88 * lit * lit;
                    int base = (atmosphere >>> 24) & 0xFF;
                    int a8 = (k == 0 || k == 3) ? (int) Math.round(base * strength) : 0;
                    ringArgb[i] = UiPalette.alpha(atmosphere, a8);
                }
            }
        }
    }

    /** One planet to draw: a rectangle (full screen), the mesh, its texture and how far round it has turned. */
    public record State(int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds, Mesh mesh, Identifier texture, float turn)
            implements PictureInPictureRenderState {
        public State(int x0, int y0, int x1, int y1, Mesh mesh, Identifier texture, float turn) {
            this(x0, y0, x1, y1, 1f, null, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null), mesh, texture,
                    turn);
        }

        /** A state whose rectangle is the planet's box, clipped to a screen of {@code w} by {@code h}. */
        public static State of(Mesh mesh, Identifier texture, float turn, int w, int h) {
            float pad = mesh.radius * 1.1f;
            int x0 = Math.max(0, (int) Math.floor(mesh.cx - pad));
            int y0 = Math.max(0, (int) Math.floor(mesh.cy - pad));
            int x1 = Math.min(w, (int) Math.ceil(mesh.cx + pad));
            int y1 = Math.min(h, (int) Math.ceil(mesh.cy + pad));
            return new State(x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1), mesh, texture, turn);
        }
    }

    /** Draws a {@link State}. */
    public static final class Renderer extends PictureInPictureRenderer<State> {
        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Mesh mesh = state.mesh();
            TexturedSphere sphere = mesh.sphere;
            float ox = state.x0() + (state.x1() - state.x0()) / 2f;
            float oy = state.y1();
            float turn = state.turn();
            collector.submitCustomGeometry(poseStack, typeFor(state.texture()), (pose, buffer) -> {
                for (int i = 0; i < sphere.count; i++) {
                    int c = sphere.argb[i];
                    buffer.addVertex(pose, sphere.x[i] - ox, sphere.y[i] - oy, 0f)
                            .setUv(sphere.u[i] + turn, sphere.v[i])
                            .setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24);
                }
            });
            if ((mesh.atmosphere >>> 24) != 0) {
                collector.submitCustomGeometry(poseStack, FLAT, (pose, buffer) -> {
                    for (int i = 0; i < mesh.ringArgb.length; i++) {
                        int c = mesh.ringArgb[i];
                        buffer.addVertex(pose, mesh.ringX[i] - ox, mesh.ringY[i] - oy, 0f)
                                .setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24);
                    }
                });
            }
        }

        @Override
        protected String getTextureLabel() {
            return "apoapsis_menu_planet";
        }
    }
}
