package dev.apoapsis.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.map3d.TexturedSphere;
import dev.apoapsis.client.sky.PlanetTextures;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * How both maps put their geometry on screen: three picture-in-picture layers, drawn in order under the text. The
 * {@link Back} layer holds what lies behind the bodies (grids, far sides of orbits), the {@link Bodies} layer the
 * textured spheres, the {@link Front} layer what lies in front (atmosphere, near sides of orbits, markers). Because a
 * layer is a rasterised image, a body hides what is behind it exactly, with no depth test and no gap at its edge. Each
 * layer is its own state class because a renderer is reused for every state of its class in a frame, and a second state
 * would overwrite the first's image. Scene coordinates are UI units from the view's top left; the state's scale turns a
 * unit into device pixels. Client only.
 */
public final class MapPip {
    private static final RenderPipeline QUADS_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/map_quads"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final RenderType QUADS = RenderType.create("apoapsis_map_quads",
            RenderSetup.builder(QUADS_PIPELINE).createRenderSetup());
    private static final RenderPipeline BODIES_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/map_bodies"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final Map<Identifier, RenderType> BODY_TYPES = new HashMap<>();
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "map_white");
    private static boolean whiteMade;

    private MapPip() {}

    /** Registers the renderers. Called from the mod event bus. */
    public static void onRegisterPip(RegisterPictureInPictureRenderersEvent event) {
        event.register(Back.class, BackRenderer::new);
        event.register(Bodies.class, BodiesRenderer::new);
        event.register(Front.class, FrontRenderer::new);
    }

    /**
     * Submits the three layers for {@code scene} over the whole screen. {@code uiScale} is the GUI pixels per UI unit
     * ({@link dev.apoapsis.client.ui.UiScale#scale()}); {@code textures} are the bodies' maps by body index, null where
     * one has not arrived (the body is then drawn as a plain lit sphere).
     */
    public static void submit(GuiGraphicsExtractor g, MapScene scene, float uiScale, @Nullable Identifier[] textures) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        ScreenRectangle bounds = PictureInPictureRenderState.getBounds(0, 0, w, h, null);
        g.submitPictureInPictureRenderState(new Back(0, 0, w, h, uiScale, null, bounds, scene));
        if (scene.meshCount > 0) {
            g.submitPictureInPictureRenderState(new Bodies(0, 0, w, h, uiScale, null, bounds, scene, textures));
        }
        g.submitPictureInPictureRenderState(new Front(0, 0, w, h, uiScale, null, bounds, scene));
    }

    private static RenderType bodyType(@Nullable Identifier texture) {
        Identifier id = texture == null ? white() : texture;
        return BODY_TYPES.computeIfAbsent(id, key -> RenderType.create("apoapsis_map_body",
                RenderSetup.builder(BODIES_PIPELINE)
                        .withTexture("Sampler0", key, PlanetTextures::sampler)
                        .createRenderSetup()));
    }

    /** A one-pixel white texture for bodies whose map has not arrived: the vertex colours then show a lit sphere. */
    private static Identifier white() {
        if (!whiteMade) {
            whiteMade = true;
            NativeImage image = new NativeImage(1, 1, false);
            image.setPixel(0, 0, 0xFFFFFFFF);
            Minecraft.getInstance().getTextureManager().register(WHITE,
                    new DynamicTexture(() -> WHITE.toString(), image));
        }
        return WHITE;
    }

    /** The layer behind the bodies. */
    public record Back(int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds, MapScene scene) implements PictureInPictureRenderState {}

    /** The bodies' layer. */
    public record Bodies(int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds, MapScene scene, @Nullable Identifier[] textures)
            implements PictureInPictureRenderState {}

    /** The layer in front of the bodies. */
    public record Front(int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds, MapScene scene) implements PictureInPictureRenderState {}

    private static void quads(QuadBatch batch, PoseStack poseStack, SubmitNodeCollector collector, float halfW,
            float h) {
        collector.submitCustomGeometry(poseStack, QUADS, (pose, buffer) -> {
            for (int i = 0; i < batch.count; i++) {
                int c = batch.argb[i];
                buffer.addVertex(pose, batch.x[i] - halfW, batch.y[i] - h, 0f)
                        .setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24);
            }
        });
    }

    /** Draws the {@link Back} layer. */
    public static final class BackRenderer extends PictureInPictureRenderer<Back> {
        @Override
        public Class<Back> getRenderStateClass() {
            return Back.class;
        }

        @Override
        protected void renderToTexture(Back state, PoseStack poseStack, SubmitNodeCollector collector) {
            float halfW = (state.x1() - state.x0()) / 2f / state.scale();
            float h = (state.y1() - state.y0()) / state.scale();
            quads(state.scene().back, poseStack, collector, halfW, h);
        }

        @Override
        protected String getTextureLabel() {
            return "map_back";
        }
    }

    /** Draws the {@link Front} layer. */
    public static final class FrontRenderer extends PictureInPictureRenderer<Front> {
        @Override
        public Class<Front> getRenderStateClass() {
            return Front.class;
        }

        @Override
        protected void renderToTexture(Front state, PoseStack poseStack, SubmitNodeCollector collector) {
            float halfW = (state.x1() - state.x0()) / 2f / state.scale();
            float h = (state.y1() - state.y0()) / state.scale();
            quads(state.scene().front, poseStack, collector, halfW, h);
        }

        @Override
        protected String getTextureLabel() {
            return "map_front";
        }
    }

    /** Draws the {@link Bodies} layer: each body's mesh with its map, the farthest first. */
    public static final class BodiesRenderer extends PictureInPictureRenderer<Bodies> {
        @Override
        public Class<Bodies> getRenderStateClass() {
            return Bodies.class;
        }

        @Override
        protected void renderToTexture(Bodies state, PoseStack poseStack, SubmitNodeCollector collector) {
            MapScene scene = state.scene();
            float halfW = (state.x1() - state.x0()) / 2f / state.scale();
            float h = (state.y1() - state.y0()) / state.scale();
            for (int n = 0; n < scene.meshCount; n++) {
                int body = scene.order[n];
                TexturedSphere mesh = scene.mesh[body];
                Identifier texture = state.textures() == null ? null : state.textures()[body];
                collector.submitCustomGeometry(poseStack, bodyType(texture), (pose, buffer) -> {
                    for (int i = 0; i < mesh.count; i++) {
                        int c = mesh.argb[i];
                        buffer.addVertex(pose, mesh.x[i] - halfW, mesh.y[i] - h, 0f)
                                .setUv(mesh.u[i], mesh.v[i])
                                .setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24);
                    }
                });
            }
        }

        @Override
        protected String getTextureLabel() {
            return "map_bodies";
        }
    }
}
