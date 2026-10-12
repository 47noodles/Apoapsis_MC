package dev.apoapsis.client.ship.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One effect sprite: moves in a straight line with some drag, grows from the start to the end size, goes through three
 * colours (hot, mid, cool) and fades in quickly and out slowly. Additive by default (plume, glow, sparks), or normally
 * blended (dust). A streak is turned so its long axis follows its screen-space motion. Always full bright and without
 * collisions. Client only.
 */
public final class FxParticle extends SingleQuadParticle {
    private final SingleQuadParticle.Layer layer;
    private float size0 = 0.5F;
    private float size1 = 0.5F;
    private final float[] colors = {1, 1, 1, 1, 1, 1, 1, 1, 1};
    private float peak = 1.0F;
    private float fadePower = 1.0F;
    private boolean streak;
    private final Vector3f local = new Vector3f();

    public FxParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite,
            SingleQuadParticle.Layer layer) {
        super(level, x, y, z, sprite);
        this.layer = layer;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.lifetime = 8;
    }

    /** Sets the drift in blocks per tick, the drag per tick (1 = none) and the lifetime in ticks. */
    public FxParticle motion(double vx, double vy, double vz, float drag, int lifetimeTicks) {
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = drag;
        this.lifetime = Math.max(1, lifetimeTicks);
        update();
        return this;
    }

    /** Half-width of the sprite at birth and at death, in blocks. */
    public FxParticle sizes(float start, float end) {
        this.size0 = start;
        this.size1 = end;
        return this;
    }

    /** The colour at birth, middle and death, as 0..1 floats; the middle is reached after a third of the life. */
    public FxParticle colors(float r0, float g0, float b0, float r1, float g1, float b1, float r2, float g2, float b2) {
        colors[0] = r0;
        colors[1] = g0;
        colors[2] = b0;
        colors[3] = r1;
        colors[4] = g1;
        colors[5] = b1;
        colors[6] = r2;
        colors[7] = g2;
        colors[8] = b2;
        update();
        return this;
    }

    /** Peak alpha, and the power of the fade-out (1 linear, 2 quick). */
    public FxParticle alpha(float peakAlpha, float fadeOutPower) {
        this.peak = peakAlpha;
        this.fadePower = fadeOutPower;
        update();
        return this;
    }

    /** Turns the sprite along its screen-space motion; for the streak texture. */
    public FxParticle streak() {
        this.streak = true;
        return this;
    }

    /** A random spin so a puff does not look the same every time. */
    public FxParticle spin() {
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        update();
    }

    private void update() {
        float t = Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
        int from = t < 0.35F ? 0 : 3;
        float k = t < 0.35F ? t / 0.35F : (t - 0.35F) / 0.65F;
        this.rCol = Mth.lerp(k, colors[from], colors[from + 3]);
        this.gCol = Mth.lerp(k, colors[from + 1], colors[from + 4]);
        this.bCol = Mth.lerp(k, colors[from + 2], colors[from + 5]);
        float fadeIn = Math.min(1.0F, t / 0.04F);
        this.alpha = peak * fadeIn * (float) Math.pow(1.0F - t, fadePower);
    }

    @Override
    public float getQuadSize(float partial) {
        return Mth.lerp(Mth.clamp((this.age + partial) / this.lifetime, 0.0F, 1.0F), size0, size1);
    }

    @Override
    protected int getLightCoords(float partial) {
        return 0xF000F0;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return layer;
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partial) {
        if (!streak) {
            super.extract(state, camera, partial);
            return;
        }
        Quaternionf rotation = new Quaternionf(camera.rotation());
        local.set((float) xd, (float) yd, (float) zd);
        rotation.conjugate(new Quaternionf()).transform(local);
        if (local.x * local.x + local.y * local.y > 1.0e-10F) {
            rotation.rotateZ((float) Math.atan2(local.y, local.x) - Mth.HALF_PI);
        }
        extractRotatedQuad(state, camera, rotation, partial);
    }
}
