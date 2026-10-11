package dev.apoapsis.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a ship: every block, placed by its position relative to the centre of mass and rotated by the orientation.
 * Blocks go through {@link SubmitNodeCollector#submitMovingBlock}, as FallingBlockRenderer draws its block.
 * Client only.
 */
public final class ShipRenderer extends EntityRenderer<ShipEntity, ShipRenderState> {
    /** Ships farther than this from the camera are not drawn, in metres. */
    public static final double RENDER_RANGE = 256.0;

    /** Block lists per ship, so a ship's blocks are not rebuilt every frame. Keys are weak. */
    private final Map<ShipEntity, BlockCache> caches = new WeakHashMap<>();

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ShipRenderState createRenderState() {
        return new ShipRenderState();
    }

    @Override
    public void extractRenderState(ShipEntity ship, ShipRenderState state, float partialTicks) {
        super.extractRenderState(ship, state, partialTicks);
        ShipStructure structure = ship.getStructure();
        BlockCache cache = caches.computeIfAbsent(ship, k -> new BlockCache());
        cache.update(structure);
        state.orientation = ship.getOrientation();
        state.centerOfMass = ship.getCenterOfMassLocal();
        state.blocks = cache.blocks;
        if (ship.level() instanceof ClientLevel level) {
            // Every block takes its light from the ship's position, as FallingBlockRenderer does for its block.
            BlockPos pos = BlockPos.containing(ship.getPosition(partialTicks));
            var biome = level.getBiome(pos);
            var lighting = level.cardinalLighting();
            var lightEngine = level.getLightEngine();
            for (ShipRenderState.ShipBlock block : cache.blocks) {
                MovingBlockRenderState moving = block.moving();
                moving.blockPos = pos;
                moving.biome = biome;
                moving.cardinalLighting = lighting;
                moving.lightEngine = lightEngine;
            }
        }
    }

    @Override
    public void submit(ShipRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Vec3 com = state.centerOfMass;
        poseStack.pushPose();
        // The pose origin is the centre of mass (the entity position); block p's corner sits at R * (p - com).
        poseStack.rotateAround(state.orientation, 0.0F, 0.0F, 0.0F);
        for (ShipRenderState.ShipBlock block : state.blocks) {
            if (block.state().getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            BlockPos p = block.pos();
            poseStack.pushPose();
            poseStack.translate(p.getX() - com.x, p.getY() - com.y, p.getZ() - com.z);
            submitNodeCollector.submitMovingBlock(poseStack, block.moving(), state.outlineColor);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /** Culls against the whole ship, not the 1 m entity box: it is drawn while any part is in view. */
    @Override
    public boolean shouldRender(ShipEntity ship, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        double dx = ship.getX() - camX;
        double dy = ship.getY() - camY;
        double dz = ship.getZ() - camZ;
        if (dx * dx + dy * dy + dz * dz > RENDER_RANGE * RENDER_RANGE) {
            return false;
        }
        return culler.isVisible(getBoundingBoxForCulling(ship, partialTicks).inflate(0.5));
    }

    /** The world-space box around every block, from the rotated local bounds. */
    @Override
    protected AABB getBoundingBoxForCulling(ShipEntity ship, float partialTicks) {
        ShipStructure structure = ship.getStructure();
        AABB local = structure.localBounds();
        Vec3 com = ship.getCenterOfMassLocal();
        Quaternionf q = ship.getOrientation();
        Vec3 origin = ship.getPosition(partialTicks);
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? local.minX : local.maxX;
            double y = (i & 2) == 0 ? local.minY : local.maxY;
            double z = (i & 4) == 0 ? local.minZ : local.maxZ;
            Vector3f c = q.transform(new Vector3f((float) (x - com.x), (float) (y - com.y), (float) (z - com.z)));
            double wx = origin.x + c.x;
            double wy = origin.y + c.y;
            double wz = origin.z + c.z;
            minX = Math.min(minX, wx);
            minY = Math.min(minY, wy);
            minZ = Math.min(minZ, wz);
            maxX = Math.max(maxX, wx);
            maxY = Math.max(maxY, wy);
            maxZ = Math.max(maxZ, wz);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** One ship's block list. Rebuilt only when the structure's blocks differ from the last frame's. */
    private static final class BlockCache {
        private final List<ShipRenderState.ShipBlock> blocks = new ArrayList<>();

        void update(ShipStructure current) {
            if (matches(current)) {
                return;
            }
            blocks.clear();
            for (Map.Entry<BlockPos, BlockState> e : current.blocks().entrySet()) {
                MovingBlockRenderState moving = new MovingBlockRenderState();
                moving.blockState = e.getValue();
                moving.randomSeedPos = e.getKey();
                blocks.add(new ShipRenderState.ShipBlock(e.getKey(), e.getValue(), moving));
            }
        }

        private boolean matches(ShipStructure current) {
            if (current.size() != blocks.size()) {
                return false;
            }
            int i = 0;
            for (Map.Entry<BlockPos, BlockState> e : current.blocks().entrySet()) {
                ShipRenderState.ShipBlock b = blocks.get(i++);
                if (b.state() != e.getValue() || !b.pos().equals(e.getKey())) {
                    return false;
                }
            }
            return true;
        }
    }
}
