package dev.apoapsis.client.sky;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.sky.SkyMath.Axes;
import dev.apoapsis.orbit.MeridianSystem;
import dev.apoapsis.orbit.Vec3d;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The space sky: a baked star field, a faint Milky Way band and the sun, drawn right after the vanilla sky pass in the
 * orbit dimension, on Pale, and fading in with the climb on Verdance. Everything is static or analytic: the stars are
 * one static vertex buffer built once from {@code stars.bin} (see {@code tools/art/gen_sky.py}), the band is a texture
 * sampled by direction, and the sun with its glow and glare is evaluated per pixel by the dome shader. The vanilla
 * stars, sun and moon are switched off for these skies (see {@link #onExtract}). The bodies ({@link SkyBodies}) draw
 * afterwards and cover whatever is behind them. Client only; every method runs on the render thread.
 */
public final class SpaceSky {
    private static final Logger LOG = LogUtils.getLogger();

    private static final Identifier STARS_FILE = id("textures/environment/stars.bin");
    private static final Identifier BAND_TEXTURE = id("textures/environment/milky_way.png");
    private static final Identifier BAND_SHADER = id("core/space_band");
    private static final Identifier SUN_SHADER = id("core/space_sun");
    private static final Identifier STARS_SHADER = id("core/space_stars");
    private static final Identifier ORBIT_ID = id("orbit");
    private static final Identifier PALE_ID = id("pale");

    /** The sun's direction in inertial axes: +X (as in {@link SkyBodies}). */
    private static final Vec3d SUN = new Vec3d(1, 0, 0);
    /** The sun's drawn angular radius, radians: a little over the real 0.27 degrees, so it reads as a disc. */
    private static final float SUN_RADIUS = (float) Math.toRadians(0.75);
    /** Brightness of the band texture's peak on screen, 0..1. */
    private static final float BAND_STRENGTH = 0.24f;
    /** Strength of the sun's disc and glow (1 is a white disc), and of the screen-wide glare when facing it. */
    private static final float SUN_STRENGTH = 1.0f;
    private static final float GLARE_STRENGTH = 0.025f;
    /** Star vertex: position, corner, colour: 24 bytes, four per star. */
    private static final int STAR_VERTEX_BYTES = 24;
    /** Where the stars sit, as a share of the far plane (any value inside the view volume works: nothing is depth tested). */
    private static final float SKY_DISTANCE = 0.5f;
    /** The band strip's quads round the galactic equator and across its latitude. */
    private static final int BAND_COLUMNS = 128;
    private static final int BAND_ROWS = 12;

    /** The band: a strip of textured quads on the sky sphere, additive. */
    private static final RenderPipeline BAND_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withLocation(id("pipeline/space_band"))
            .withVertexShader(BAND_SHADER)
            .withFragmentShader(BAND_SHADER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();
    /** The sun: one quad round its direction, disc and glow evaluated per pixel, additive. */
    private static final RenderPipeline SUN_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withLocation(id("pipeline/space_sun"))
            .withVertexShader(SUN_SHADER)
            .withFragmentShader(SUN_SHADER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();
    /** The stars: screen-space sized quads on the sphere, additive. */
    private static final RenderPipeline STARS_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withLocation(id("pipeline/space_stars"))
            .withVertexShader(STARS_SHADER)
            .withFragmentShader(STARS_SHADER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    /** Environment switches for measuring: no space sky at all (vanilla's stays), and a log of CPU and GPU time. */
    private static final boolean DISABLED = System.getenv("APOAPSIS_SKY_OFF") != null;
    /** With {@code APOAPSIS_SKY_NOSKY}: only the space sky is off (the finer planet maps stay), to time them apart. */
    private static final boolean NO_SKY = System.getenv("APOAPSIS_SKY_NOSKY") != null;
    private static final boolean STATS = System.getenv("APOAPSIS_SKY_STATS") != null;

    private static @Nullable GpuBuffer starBuffer;
    private static int starCount;
    /** Rows: the galactic frame's axes in inertial axes (the band texture is in the galactic frame). */
    private static final Matrix3f GALACTIC = new Matrix3f();
    private static @Nullable GpuBuffer bandBuffer;
    private static int bandQuads;
    private static @Nullable GpuBuffer sunBuffer;
    private static boolean loadFailed;
    private static @Nullable GpuSampler bandSampler;
    private static @Nullable GpuQueryPool timestamps;
    private static final int WARMUP_FRAMES = 200;
    private static long cpuNanos;
    private static int cpuFrames;
    private static long frameNanos;
    private static int frames;
    private static int warm;
    private static long lastFrame;
    private static long gpuNanos;
    private static long domeNanos;
    private static int gpuFrames;

    private SpaceSky() {}

    /** True with {@code APOAPSIS_SKY_OFF} set: the space sky and the finer planet maps are off, for before/after runs. */
    static boolean off() {
        return DISABLED;
    }

    /** Row {@code i} of the galactic frame (0 centre, 1 east, 2 north pole) in inertial axes. */
    static Vec3d galacticAxis(int i) {
        return new Vec3d(GALACTIC.getRowColumn(i, 0), GALACTIC.getRowColumn(i, 1), GALACTIC.getRowColumn(i, 2));
    }

    /** The galactic north pole in inertial axes (the direction farthest from the band). */
    static Vec3d galacticPole() {
        return new Vec3d(GALACTIC.m02(), GALACTIC.m12(), GALACTIC.m22());
    }

    /** The galactic centre's direction in inertial axes (the band's brightest part), once the catalogue is loaded. */
    static Vec3d galacticCentre() {
        return new Vec3d(GALACTIC.m00(), GALACTIC.m10(), GALACTIC.m20());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Apoapsis.MODID, path);
    }

    /** The sky of the level at camera {@code cam}: the frame it is drawn in and how much of it shows, or null. */
    record View(Axes axes, double visibility) {
    }

    /**
     * The sky frame for a camera at {@code cam} in {@code level}: the orbit dimension's axes, or a surface's tangent axes
     * (the same map as the bodies use), with the share of the space sky that shows. Null where there is none: the
     * overworld below the climb, and every other dimension.
     */
    static @Nullable View view(Level level, Vec3 cam) {
        Identifier dim = level.dimension().identifier();
        if (dim.equals(ORBIT_ID)) {
            return new View(SkyMath.ORBIT, 1);
        }
        boolean overworld = Level.OVERWORLD.equals(level.dimension());
        if (!overworld && !dim.equals(PALE_ID)) {
            return null;
        }
        double visibility = overworld ? SkyBodies.climbShare(level.dimension(), cam.y()) : 1;
        if (visibility <= 0) {
            return null;
        }
        double radius = (overworld ? MeridianSystem.VERDANCE : MeridianSystem.PALE).radius;
        return new View(SkyMath.surfaceAxes(cam.x() / radius, -cam.z() / radius), visibility);
    }

    /**
     * Runs after the level's sky state is extracted: where the space sky shows, the vanilla stars, sun and moon are
     * switched off (the space sky has its own); on Verdance's climb they fade out over the first third of it.
     */
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        if (DISABLED || NO_SKY) {
            return;
        }
        SkyRenderState sky = event.getRenderState().skyRenderState;
        View view = view(event.getLevel(), event.getCamera().position());
        if (view == null) {
            return;
        }
        float vanilla = Level.OVERWORLD.equals(event.getLevel().dimension())
                ? (float) (1 - Math.min(1, view.visibility() * 3)) : 0f;
        sky.starBrightness *= vanilla;
        sky.rainBrightness *= vanilla;
    }

    /** Draws the space sky after the vanilla sky. */
    public static void onAfterSky(RenderLevelStageEvent.AfterSky event) {
        if (STATS) {
            frameStats();
        }
        if (DISABLED || NO_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LevelRenderState state = event.getLevelRenderState();
        CameraRenderState camera = state.cameraRenderState;
        if (mc.level == null) {
            return;
        }
        View view = view(mc.level, camera.pos);
        if (view == null) {
            return;
        }
        long t0 = System.nanoTime();
        ensureBuffers();
        if (starBuffer == null || bandBuffer == null || sunBuffer == null) {
            return;
        }
        GpuTextureView band = bandView();
        if (band == null) {
            return;
        }

        // View-from-inertial rotation: the camera's view rotation after the frame's axes.
        Axes ax = view.axes();
        Matrix3f axes = new Matrix3f(
                (float) ax.a().x(), (float) ax.b().x(), (float) ax.c().x(),
                (float) ax.a().y(), (float) ax.b().y(), (float) ax.c().y(),
                (float) ax.a().z(), (float) ax.b().z(), (float) ax.c().z());
        Matrix3f viewRot = new Matrix3f().set(event.getModelViewMatrix());
        Matrix3f viewFromInertial = new Matrix3f(viewRot).mul(axes);
        Vector3f sunView = viewFromInertial.transform(new Vector3f((float) SUN.x(), (float) SUN.y(), (float) SUN.z()));
        float vis = (float) view.visibility();
        float distance = SKY_DISTANCE * camera.depthFar;
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Space sky",
                target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            if (STATS && timestamps != null) {
                pass.writeTimestamp(timestamps, 0);
            }
            RenderSystem.bindDefaultUniforms(pass);

            // Band: a galactic unit vector to the sky sphere in view space; TextureMat[0] carries the sun in view space.
            Matrix4f galacticToView = new Matrix4f(viewRot).mul(new Matrix4f(axes))
                    .mul(new Matrix4f(new Matrix3f(GALACTIC).transpose())).scale(distance);
            Matrix4f sunParams = new Matrix4f(
                    sunView.x, sunView.y, sunView.z, 0f,
                    0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 1f);
            pass.pushDebugGroup(() -> "Band");
            pass.setPipeline(RenderSystem.getCompiledPipeline(BAND_PIPELINE));
            pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(galacticToView,
                    new Vector4f(vis * BAND_STRENGTH, SUN_RADIUS, 0f, 0f), new Vector3f(), sunParams));
            pass.setUniform("Sampler0", band, bandSampler());
            pass.setVertexBuffer(0, bandBuffer.slice());
            RenderSystem.AutoStorageIndexBuffer bandIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            pass.setIndexBuffer(bandIndices.getBuffer(6 * bandQuads), bandIndices.type());
            pass.drawIndexed(6 * bandQuads, 1, 0, 0, 0);
            pass.popDebugGroup();

            // Sun: its direction in view space, drawn unless it is far behind the camera (its glow reaches 28 degrees).
            if (sunView.z < 0.5f) {
                Matrix4f sunMv = new Matrix4f(
                        sunView.x, sunView.y, sunView.z, SUN_RADIUS,
                        SUN_STRENGTH, GLARE_STRENGTH, 0f, 0f,
                        0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 1f);
                pass.pushDebugGroup(() -> "Sun");
                pass.setPipeline(RenderSystem.getCompiledPipeline(SUN_PIPELINE));
                pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(sunMv,
                        new Vector4f(vis, 0f, distance, 0f), new Vector3f(), new Matrix4f()));
                pass.setVertexBuffer(0, sunBuffer.slice());
                pass.draw(6, 1, 0, 0);
                pass.popDebugGroup();
            }
            if (STATS && timestamps != null) {
                pass.writeTimestamp(timestamps, 2);
            }

            // Stars: the model-view maps an inertial unit vector onto the sky sphere; the sun (inertial) dims them.
            Matrix4f modelView = new Matrix4f(viewRot).mul(new Matrix4f(axes)).scale(distance);
            pass.pushDebugGroup(() -> "Stars");
            pass.setPipeline(RenderSystem.getCompiledPipeline(STARS_PIPELINE));
            pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(modelView,
                    new Vector4f(vis, SUN_RADIUS, 0f, 0f), new Vector3f((float) SUN.x(), (float) SUN.y(), (float) SUN.z()),
                    new Matrix4f()));
            pass.setVertexBuffer(0, starBuffer.slice());
            RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            pass.setIndexBuffer(quads.getBuffer(6 * starCount), quads.type());
            pass.drawIndexed(6 * starCount, 1, 0, 0, 0);
            pass.popDebugGroup();
            if (STATS && timestamps != null) {
                pass.writeTimestamp(timestamps, 1);
            }
        }
        if (STATS && warm > WARMUP_FRAMES) {
            cpuNanos += System.nanoTime() - t0;
            cpuFrames++;
        }
    }

    /**
     * Stats mode, once per frame before the pass: the frame time since the last frame, the GPU time of the previous
     * frame's pass from its two timestamps, and a log line every 300 frames after a warm-up (shader compile, JIT).
     */
    private static void frameStats() {
        long now = System.nanoTime();
        if (lastFrame != 0 && ++warm > WARMUP_FRAMES) {
            frameNanos += now - lastFrame;
            frames++;
        }
        lastFrame = now;
        GpuQueryPool pool = timestamps;
        if (pool != null && warm > WARMUP_FRAMES) {
            OptionalLong a = pool.getValue(0);
            OptionalLong b = pool.getValue(1);
            OptionalLong m = pool.getValue(2);
            if (a.isPresent() && b.isPresent() && m.isPresent() && b.getAsLong() >= a.getAsLong()) {
                gpuNanos += b.getAsLong() - a.getAsLong();
                domeNanos += m.getAsLong() - a.getAsLong();
                gpuFrames++;
            }
        }
        if (frames >= 300) {
            LOG.info("[space sky] over {} frames: frame {} ms, sky CPU {} ms, sky GPU {} ms (dome {} ms) ({} samples)", frames,
                    String.format("%.3f", frameNanos / 1e6 / frames),
                    cpuFrames == 0 ? "n/a" : String.format("%.3f", cpuNanos / 1e6 / cpuFrames),
                    gpuFrames == 0 ? "n/a" : String.format("%.3f", gpuNanos / 1e6 / gpuFrames),
                    gpuFrames == 0 ? "n/a" : String.format("%.3f", domeNanos / 1e6 / gpuFrames), gpuFrames);
            frames = 0;
            frameNanos = 0;
            cpuNanos = 0;
            cpuFrames = 0;
            gpuNanos = 0;
            domeNanos = 0;
            gpuFrames = 0;
        }
    }

    /** The band texture's view, or null while it is not loaded. */
    private static @Nullable GpuTextureView bandView() {
        try {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(BAND_TEXTURE);
            return texture.getTextureView();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Linear filtering, wrapping around the galactic longitude and clamped at the poles. */
    private static GpuSampler bandSampler() {
        GpuSampler s = bandSampler;
        if (s == null) {
            s = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.LINEAR, false);
            bandSampler = s;
        }
        return s;
    }

    /** Builds the static buffers on first use: the star quads from the catalogue, and the dome's triangle. */
    private static void ensureBuffers() {
        if (sunBuffer == null) {
            float[] corners = {-1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f, -1f, -1f, 0f, 1f, 1f, 0f, -1f, 1f, 0f};
            ByteBuffer t = ByteBuffer.allocateDirect(corners.length * 4).order(ByteOrder.nativeOrder());
            for (float f : corners) {
                t.putFloat(f);
            }
            t.flip();
            sunBuffer = RenderSystem.getDevice().createBuffer(() -> "Space sun quad", GpuBuffer.USAGE_VERTEX, t);
        }
        if (bandBuffer == null) {
            buildBand();
        }
        if (starBuffer != null || loadFailed) {
            return;
        }
        try {
            loadStars();
        } catch (IOException | RuntimeException e) {
            loadFailed = true;
            LOG.error("[space sky] could not load {}: no stars will draw", STARS_FILE, e);
        }
    }

    /**
     * The band strip as quads: galactic longitude in BAND_COLUMNS columns, sin(latitude) from -0.45 to 0.45 in
     * BAND_ROWS rows. UV matches the texture (longitude across, sin(latitude) down); the alpha fades the strip to
     * nothing at its edges.
     */
    private static void buildBand() {
        double limit = 0.45;
        ByteBuffer out = ByteBuffer.allocateDirect(BAND_COLUMNS * BAND_ROWS * 4 * STAR_VERTEX_BYTES)
                .order(ByteOrder.nativeOrder());
        int[] dl = {0, 1, 1, 0};
        int[] dr = {0, 0, 1, 1};
        for (int row = 0; row < BAND_ROWS; row++) {
            for (int col = 0; col < BAND_COLUMNS; col++) {
                for (int c = 0; c < 4; c++) {
                    double z = -limit + 2 * limit * (row + dr[c]) / BAND_ROWS;
                    double lon = 2 * Math.PI * (col + dl[c]) / BAND_COLUMNS;
                    double ring = Math.sqrt(1 - z * z);
                    double t = Math.min(1, Math.max(0, (Math.abs(z) - 0.20) / 0.25));
                    double fade = 1 - t * t * (3 - 2 * t);
                    out.putFloat((float) (ring * Math.cos(lon))).putFloat((float) (ring * Math.sin(lon)))
                            .putFloat((float) z);
                    out.putFloat((float) ((col + dl[c]) / (double) BAND_COLUMNS)).putFloat((float) (0.5 - 0.5 * z));
                    out.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) Math.round(255 * fade));
                }
            }
        }
        out.flip();
        bandBuffer = RenderSystem.getDevice().createBuffer(() -> "Space band", GpuBuffer.USAGE_VERTEX, out);
        bandQuads = BAND_COLUMNS * BAND_ROWS;
    }

    private static void loadStars() throws IOException {
        byte[] bytes;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(STARS_FILE)) {
            bytes = in.readAllBytes();
        }
        ByteBuffer in = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (in.getInt() != 0x4B535041 /* "APSK" read little endian */) {
            throw new IOException("bad magic");
        }
        int version = in.getInt();
        int count = in.getInt();
        if (version != 1 || count < 1 || in.remaining() != 36 + 16 * count) {
            throw new IOException("bad header: version " + version + ", count " + count);
        }
        float[] g = new float[9];
        for (int i = 0; i < 9; i++) {
            g[i] = in.getFloat();
        }
        // Rows gx, gy, gz; the Matrix3f constructor takes columns.
        GALACTIC.set(g[0], g[3], g[6], g[1], g[4], g[7], g[2], g[5], g[8]);

        ByteBuffer out = ByteBuffer.allocateDirect(count * 4 * STAR_VERTEX_BYTES).order(ByteOrder.nativeOrder());
        float[] cornerU = {-1f, 1f, 1f, -1f};
        float[] cornerV = {-1f, -1f, 1f, 1f};
        for (int s = 0; s < count; s++) {
            float x = in.getFloat();
            float y = in.getFloat();
            float z = in.getFloat();
            int magCode = in.get() & 0xFF;
            byte r = in.get();
            byte gr = in.get();
            byte b = in.get();
            // Apparent magnitude, then a flux with a soft slope (the eye is not linear): J = 1 at magnitude 1.
            double m = magCode / 25.0 - 2.0;
            double flux = Math.pow(10, -0.4 * 0.313 * (m - 1.0));
            int alpha = (int) Math.round(255 * Math.sqrt(Math.min(flux, 2.1) / 2.1));
            for (int c = 0; c < 4; c++) {
                out.putFloat(x).putFloat(y).putFloat(z).putFloat(cornerU[c]).putFloat(cornerV[c]);
                out.put(r).put(gr).put(b).put((byte) alpha);
            }
        }
        out.flip();
        starBuffer = RenderSystem.getDevice().createBuffer(() -> "Space stars", GpuBuffer.USAGE_VERTEX, out);
        starCount = count;
        if (STATS) {
            timestamps = RenderSystem.getDevice().createTimestampQueryPool(3);
        }
        LOG.info("[space sky] {} stars loaded", count);
    }
}
