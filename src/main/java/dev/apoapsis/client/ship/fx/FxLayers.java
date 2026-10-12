package dev.apoapsis.client.ship.fx;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.apoapsis.Apoapsis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Render layers and sprites of the effect particles. The additive layer is drawn with the particle shader family but
 * our own fragment shader (no fog, no alpha cut-off) and a source-alpha, one blend, depth tested and not written, so the
 * glow adds to whatever is behind it and soft sprite edges fade out. It is a non-translucent layer on purpose: the
 * order-independent translucency path has no additive pipeline, and additive blending needs no ordering. Client only.
 */
public final class FxLayers {
    private static final RenderPipeline ADDITIVE_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/fx_additive"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "core/fx_particle"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    /**
     * Soft additive sprites. In the translucent group, so they are drawn after the far terrain (which writes depth in
     * the opaque stage) and over it.
     */
    private static final SingleQuadParticle.Layer ADDITIVE =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE_PIPELINE);
    /** Normally blended sprites (dust). */
    public static final SingleQuadParticle.Layer DUST = SingleQuadParticle.Layer.TRANSLUCENT;

    /** Sprite names in the particle atlas: textures/particle/fx_*.png, made by tools/art/gen_fx.py. */
    public static final Identifier GLOW = id("fx_glow");
    public static final Identifier PLUME = id("fx_plume");
    public static final Identifier SPARK = id("fx_spark");
    public static final Identifier STREAK = id("fx_streak");
    public static final Identifier PUFF = id("fx_puff");

    private FxLayers() {}

    /**
     * The layer for glowing sprites. With the improved-transparency option on, translucent layers need an OIT pipeline
     * set, which an additive blend has none of, so the ordinary translucent layer stands in (alpha blended).
     */
    public static SingleQuadParticle.Layer additive() {
        return Minecraft.getInstance().options.improvedTransparency().get() ? SingleQuadParticle.Layer.TRANSLUCENT : ADDITIVE;
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Apoapsis.MODID, name);
    }

    /** The sprite {@code name} from the particle atlas (the current one: atlases are rebuilt on a resource reload). */
    public static TextureAtlasSprite sprite(Identifier name) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(name);
    }
}
