package dev.apoapsis.mixin.client;

import dev.apoapsis.client.sky.SkyBodies;
import dev.apoapsis.client.sky.SkyClimb;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The sky colour along the climb: Verdance's sky is pulled teal low down, then every surface sky goes to black and the
 * stars come out by the transition altitude. Biomes set the sky colour, so no NeoForge event reaches it. Client only.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void apoapsis$climbSky(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state,
            CallbackInfo ci) {
        if (state.skybox != DimensionType.Skybox.OVERWORLD || state.skyColor == null) {
            return;
        }
        double share = SkyBodies.climbShare(level.dimension(), camera.position().y());
        if (share <= 0) {
            return;
        }
        state.skyColor = SkyClimb.sky(state.skyColor, Level.OVERWORLD.equals(level.dimension()), share);
        state.starBrightness = (float) Math.max(state.starBrightness, share);
        if (state.sunriseAndSunsetColor != null) {
            Vector4fc s = state.sunriseAndSunsetColor;
            state.sunriseAndSunsetColor = new Vector4f(s.x(), s.y(), s.z(), (float) (s.w() * (1 - share)));
        }
    }
}
