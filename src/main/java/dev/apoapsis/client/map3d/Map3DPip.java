package dev.apoapsis.client.map3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.apoapsis.Apoapsis;
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
 * The 3D map's picture-in-picture path. The screen submits a {@link State} holding its finished {@link Scene3D}; the
 * renderer draws it once into the PiP's texture through a custom-geometry submit, in the same feature pass vanilla
 * uses for the sky (NeoForge's {@link RegisterPictureInPictureRenderersEvent} registers the renderer). The quads are
 * screen space, so the PiP's orthographic projection maps them one to one. Depth test is off and nothing is sorted on
 * upload, so the screen's painter order holds. Client only.
 */
public final class Map3DPip {
    private static final RenderPipeline QUADS_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/map3d_quads"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final RenderType QUADS = RenderType.create("apoapsis_map3d",
            RenderSetup.builder(QUADS_PIPELINE).createRenderSetup());

    private Map3DPip() {}

    /** Registers the renderer. Called from the mod event bus. */
    public static void onRegisterPip(RegisterPictureInPictureRenderersEvent event) {
        event.register(State.class, Renderer::new);
    }

    /**
     * One frame of the 3D map: a full-screen rectangle at GUI scale 1, and the scene to draw in it. The scene's
     * coordinates are GUI pixels from the rectangle's top left.
     */
    public record State(int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds, Scene3D scene) implements PictureInPictureRenderState {
        public State(int x0, int y0, int x1, int y1, Scene3D scene) {
            this(x0, y0, x1, y1, 1f, null, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null), scene);
        }
    }

    /** Draws a {@link State}'s scene. */
    public static final class Renderer extends PictureInPictureRenderer<State> {
        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Scene3D scene = state.scene();
            // The PiP's pose puts the origin at the texture's bottom centre (x = width / 2, y = height), so the scene's
            // GUI-pixel coordinates are shifted by half the width and by the full height.
            float halfW = (state.x1() - state.x0()) / 2f;
            float h = state.y1() - state.y0();
            collector.submitCustomGeometry(poseStack, QUADS, (pose, buffer) -> {
                for (int i = 0; i < scene.count; i++) {
                    int c = scene.argb[i];
                    buffer.addVertex(pose, scene.x[i] - halfW, scene.y[i] - h, 0f)
                            .setColor((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, c >>> 24);
                }
            });
        }

        @Override
        protected String getTextureLabel() {
            return "map3d";
        }
    }
}
