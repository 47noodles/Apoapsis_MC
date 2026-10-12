package dev.apoapsis.client.terrain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.sky.SkyBodies;
import dev.apoapsis.net.TerrainTilePayload;
import dev.apoapsis.orbit.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.PrepareRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Draws the far terrain: the three levels' tiles as static GPU meshes ({@link FarTerrainMesh}), one indexed draw each.
 * Surface dimension: in the opaque feature stage, in world coordinates with the planet's curvature, behind the voxels
 * (see {@code far_terrain.vsh}, which remaps depth so loaded chunks always win). Orbit dimension: in the translucent
 * feature stage, after the planet cap, on the body's sphere under the ship, fading out above 30 km. The shader does the
 * camera-relative placement, curvature, depth, the sun's light on the sphere and the haze; this class only builds
 * meshes when a tile changes and writes a handful of uniforms per frame.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
final class FarTerrainRenderer {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "core/far_terrain");
    /** The level's matrices and fog, a position-colour vertex, translucent blend (the fade), depth tested and written. */
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Apoapsis.MODID, "pipeline/far_terrain"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false)
            .build();
    /** Orbit-dimension axes from inertial: (x, y, z) = (X, Z, -Y), rows (1,0,0) (0,0,1) (0,-1,0). */
    private static final Matrix4f ORBIT_AXES = new Matrix4f(new Matrix3f(1, 0, 0, 0, 0, -1, 0, 1, 0));
    /** The sun's direction in inertial axes, and the unlit side's brightness, as the planet cap. */
    private static final Vector3f SUN = new Vector3f(1, 0, 0);
    private static final float AMBIENT = 0.10f;
    /** Closed buffers wait this many frames, so a frame still in flight is never drawing from freed memory. */
    private static final int RETIRE_FRAMES = 4;

    /** Meshes by mode (surface, orbit) and level. */
    private static final FarTerrainMesh[][] MESHES = new FarTerrainMesh[2][TerrainTilePayload.LEVELS];
    private record Retired(FarTerrainMesh mesh, long frame) {}
    private static final List<Retired> RETIRED = new ArrayList<>();
    /** The mesh being worked out off the render thread for each mode and level, or null. */
    private record Job(FarTerrainMesh.Key key, Future<FarTerrainMesh.Data> result) {}
    private static final Job[][] JOBS = new Job[2][TerrainTilePayload.LEVELS];
    private static final ExecutorService BUILDER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "apoapsis-far-terrain-mesh");
        t.setDaemon(true);
        return t;
    });
    private static long frame;
    /** This frame's view and far plane, set in the prepare stage; null when nothing is drawn. */
    private static @Nullable FarTerrainView view;
    private static float far;
    /** What the level's lightmap gives a block top under open sky, per channel (day, night, Pale's fixed light). */
    private static final Vector3f SKY_TINT = new Vector3f(1, 1, 1);
    /** CPU time of the prepare and draw stages, ns, summed since the last {@link #takeStats}. */
    private static long cpuNanos;
    private static int cpuFrames;

    private FarTerrainRenderer() {}

    /** Closes every mesh (logout). */
    static void clear() {
        for (FarTerrainMesh[] row : MESHES) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null) {
                    row[i].close();
                    row[i] = null;
                }
            }
        }
        for (Job[] row : JOBS) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null) {
                    row[i].result().cancel(true);
                    row[i] = null;
                }
            }
        }
        for (Retired r : RETIRED) {
            r.mesh().close();
        }
        RETIRED.clear();
        view = null;
    }

    /** Average CPU milliseconds per frame of the far terrain since the last call, or -1 when no frame was drawn. */
    static double takeStats() {
        double ms = cpuFrames == 0 ? -1 : cpuNanos / 1e6 / cpuFrames;
        cpuNanos = 0;
        cpuFrames = 0;
        return ms;
    }

    @SubscribeEvent
    static void onPrepare(PrepareRenderBuffersEvent event) {
        long t0 = System.nanoTime();
        frame++;
        RETIRED.removeIf(r -> {
            if (frame - r.frame() > RETIRE_FRAMES) {
                r.mesh().close();
                return true;
            }
            return false;
        });
        view = null;
        if (!FarTerrainClient.enabled || Minecraft.getInstance().level == null) {
            return;
        }
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        FarTerrainView v = FarTerrainView.compute(camera.pos, event.getLevelRenderState().worldPartialTicks, false);
        if (v == null) {
            return;
        }
        view = v;
        far = camera.depthFar;
        updateSkyTint(event.getLevelRenderState().worldPartialTicks);
        int mode = v.mode().ordinal();
        FarTerrainMesh.Key[] want = new FarTerrainMesh.Key[TerrainTilePayload.LEVELS];
        for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
            TerrainTilePayload tile = FarTerrain.tile(v.body().id, level);
            if (tile != null) {
                TerrainTilePayload finer = level == 0 ? null : FarTerrain.tile(v.body().id, level - 1);
                TerrainTilePayload coarser = level == TerrainTilePayload.LEVELS - 1 ? null
                        : FarTerrain.tile(v.body().id, level + 1);
                want[level] = new FarTerrainMesh.Key(tile, v.mode(),
                        finer == null ? null : FarTerrainMesh.Hole.of(finer), coarser);
            }
        }
        syncMeshes(mode, v, want);
        cpuNanos += System.nanoTime() - t0;
        if (FarTerrainClient.debug && frame % 120 == 0) {
            StringBuilder sb = new StringBuilder("[far-terrain] " + v.mode() + " " + v.body().id + " alt "
                    + Math.round(v.altitude()) + " fade " + String.format("%.2f", v.fade()) + " sub "
                    + Math.round(v.subX()) + "," + Math.round(v.subZ()) + " light "
                    + String.format("%.3f %.3f %.3f", SKY_TINT.x, SKY_TINT.y, SKY_TINT.z));
            for (int level = 0; level < TerrainTilePayload.LEVELS; level++) {
                FarTerrainMesh m = MESHES[mode][level];
                TerrainTilePayload t = FarTerrain.tile(v.body().id, level);
                sb.append(" | L").append(level).append(t == null ? " none" : " tile " + t.centerX() + "," + t.centerZ())
                        .append(m == null ? " nomesh" : " idx " + m.indexCount);
            }
            Apoapsis.LOG.info(sb.toString());
        }
    }

    /**
     * Brings the level's meshes in line with the tiles {@code want} (null: no tile). A mesh depends on its own tile, on
     * the finer tile (the hole it leaves for it) and on the coarser tile (the band it blends into), so one arriving tile
     * changes up to three meshes. Those are worked out off the render thread and swapped in together once all are ready,
     * with the old ones drawn meanwhile: a coarse mesh is never given a hole before the fine mesh that fills it is
     * there. Meshes that do not exist yet are built at once, so a new view is not left empty.
     */
    private static void syncMeshes(int mode, FarTerrainView v, FarTerrainMesh.Key[] want) {
        boolean[] stale = new boolean[want.length];
        boolean any = false;
        boolean fresh = false;
        for (int level = 0; level < want.length; level++) {
            FarTerrainMesh current = MESHES[mode][level];
            if (want[level] == null) {
                replace(mode, level, null);
                cancel(mode, level);
            } else if (current == null || !current.key.sameAs(want[level])) {
                stale[level] = true;
                any = true;
                fresh |= current == null;
            } else {
                cancel(mode, level);
            }
        }
        if (!any) {
            return;
        }
        boolean ready = true;
        for (int level = 0; level < want.length; level++) {
            if (!stale[level]) {
                continue;
            }
            Job job = JOBS[mode][level];
            if (job == null || !job.key().sameAs(want[level])) {
                cancel(mode, level);
                job = start(want[level], v, fresh);
                JOBS[mode][level] = job;
            }
            ready &= job.result().isDone();
        }
        if (!ready) {
            return;
        }
        for (int level = 0; level < want.length; level++) {
            if (!stale[level]) {
                continue;
            }
            try {
                replace(mode, level, FarTerrainMesh.upload(JOBS[mode][level].result().get()));
            } catch (InterruptedException | ExecutionException e) {
                Apoapsis.LOG.error("Far terrain mesh failed", e);
                replace(mode, level, null);
            }
            JOBS[mode][level] = null;
        }
    }

    /** Starts working out the mesh for {@code key}: now on this thread when {@code inline}, else on the builder. */
    private static Job start(FarTerrainMesh.Key key, FarTerrainView v, boolean inline) {
        Callable<FarTerrainMesh.Data> work = () -> FarTerrainMesh.compute(key, v.radius(), v.seaLevel(), v.water());
        if (inline) {
            FutureTask<FarTerrainMesh.Data> task = new FutureTask<>(work);
            task.run();
            return new Job(key, task);
        }
        return new Job(key, BUILDER.submit(work));
    }

    private static void cancel(int mode, int level) {
        Job job = JOBS[mode][level];
        if (job != null) {
            job.result().cancel(true);
            JOBS[mode][level] = null;
        }
    }

    /**
     * The vanilla lightmap's colour for full sky light and no block light (see {@code lightmap.fsh}): ambient plus the
     * sky light colour times the sky factor, clamped, then the brightness option's gamma-ish curve. A block top under
     * open sky is its texture times this, so the far terrain, which is flat colours, is multiplied by it too.
     */
    private static void updateSkyTint(float partial) {
        Minecraft mc = Minecraft.getInstance();
        var probe = mc.gameRenderer.mainCamera().attributeProbe();
        float factor = probe.getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partial);
        Vector3fc sky = probe.getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partial);
        Vector3fc ambient = probe.getValue(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, partial);
        float brightness = mc.options.gamma().get().floatValue();
        SKY_TINT.set(ambient.x() + sky.x() * factor, ambient.y() + sky.y() * factor, ambient.z() + sky.z() * factor);
        SKY_TINT.set(Math.clamp(SKY_TINT.x, 0f, 1f), Math.clamp(SKY_TINT.y, 0f, 1f), Math.clamp(SKY_TINT.z, 0f, 1f));
        float max = Math.max(SKY_TINT.x, Math.max(SKY_TINT.y, SKY_TINT.z));
        if (max > 0.0001f) {
            float inv = 1f - max;
            float notGamma = (1f - inv * inv * inv * inv) / max;
            SKY_TINT.set(SKY_TINT.x * (1 + (notGamma - 1) * brightness), SKY_TINT.y * (1 + (notGamma - 1) * brightness),
                    SKY_TINT.z * (1 + (notGamma - 1) * brightness));
        }
    }

    private static void replace(int mode, int level, @Nullable FarTerrainMesh mesh) {
        FarTerrainMesh old = MESHES[mode][level];
        if (old == mesh) {
            return;
        }
        MESHES[mode][level] = mesh;
        if (old != null) {
            RETIRED.add(new Retired(old, frame));
        }
    }

    /** Surface dimension: the terrain behind the voxels, after the opaque features. */
    @SubscribeEvent
    static void onAfterOpaque(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        FarTerrainView v = view;
        if (v != null && v.mode() == FarTerrainView.Mode.SURFACE) {
            draw(event.getRenderPass(), event.getModelViewMatrix(), v);
        }
    }

    /** Orbit dimension: the patch on the sphere, after the planet cap (a translucent custom geometry). */
    @SubscribeEvent
    static void onAfterTranslucent(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        FarTerrainView v = view;
        if (v != null && v.mode() == FarTerrainView.Mode.ORBIT) {
            draw(event.getRenderPass(), event.getModelViewMatrix(), v);
        }
    }

    private static void draw(RenderPass pass, org.joml.Matrix4fc viewMatrix, FarTerrainView v) {
        long t0 = System.nanoTime();
        boolean orbit = v.mode() == FarTerrainView.Mode.ORBIT;
        int mode = v.mode().ordinal();
        Matrix4f modelView = new Matrix4f(viewMatrix);
        if (orbit) {
            modelView.mul(ORBIT_AXES);
        }
        float near;
        float placedFar;
        float scale;
        if (orbit) {
            // Just in front of the planet cap (at 0.9 of the far plane), in distance order inside that band.
            near = 0.86f * far;
            placedFar = 0.895f * far;
            scale = 20_000f;
        } else {
            // Behind every loaded voxel: beyond the chunk reach (horizontal reach times 1.5 for corners, and the height
            // above the ground), and in front of the planet cap.
            double reach = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 1.5;
            double above = Math.max(0, v.camY() - (v.seaLevel() - 30));
            near = (float) Math.max(0.1 * far, Math.min(0.78 * far, Math.sqrt(above * above + reach * reach)));
            placedFar = Math.max(near + 1f, 0.86f * far);
            scale = 8_000f;
        }
        float inv2R = orbit ? 0f : (float) (1.0 / (2 * v.radius()));
        // Verdance's air hazes the distance as the level's fog does, less as the climb thins it out; Pale has none.
        float fogMix = orbit || !v.water() ? 0f
                : (float) (1 - SkyBodies.climbShare(Minecraft.getInstance().level.dimension(), v.camY()));
        boolean drewAny = false;
        pass.pushDebugGroup(() -> "Far terrain");
        pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
        RenderSystem.bindDefaultUniforms(pass);
        for (int level = TerrainTilePayload.LEVELS - 1; level >= 0; level--) {
            FarTerrainMesh mesh = MESHES[mode][level];
            if (mesh == null || mesh.indexCount == 0 || !v.body().id.equals(mesh.tile.body())) {
                continue;
            }
            Vector3f offset;
            Matrix4f params;
            if (orbit) {
                Vec3d p = v.viewer();
                offset = new Vector3f((float) (mesh.originX - p.x()), (float) (mesh.originY - p.y()),
                        (float) (mesh.originZ - p.z()));
                params = new Matrix4f(inv2R, near, placedFar, scale,
                        fogMix, 0f, 0f, 0f,
                        (float) mesh.originX, (float) mesh.originY, (float) mesh.originZ, 1f,
                        SUN.x, SUN.y, SUN.z, AMBIENT);
            } else {
                offset = new Vector3f((float) (mesh.tile.centerX() - v.camX()), (float) -v.camY(),
                        (float) (mesh.tile.centerZ() - v.camZ()));
                params = new Matrix4f(inv2R, near, placedFar, scale,
                        fogMix, 0f, 0f, 0f,
                        0f, 0f, 0f, 0f,
                        SUN.x, SUN.y, SUN.z, AMBIENT);
            }
            GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(modelView,
                    new Vector4f(orbit ? 1f : SKY_TINT.x, orbit ? 1f : SKY_TINT.y, orbit ? 1f : SKY_TINT.z,
                            (float) v.fade()), offset, params);
            pass.setUniform("DynamicTransforms", transform);
            pass.setVertexBuffer(0, mesh.vertices.slice());
            pass.setIndexBuffer(mesh.indices, IndexType.INT);
            pass.drawIndexed(mesh.indexCount, 1, 0, 0, 0);
            drewAny = true;
        }
        pass.popDebugGroup();
        cpuNanos += System.nanoTime() - t0;
        if (drewAny) {
            cpuFrames++;
        }
    }
}
