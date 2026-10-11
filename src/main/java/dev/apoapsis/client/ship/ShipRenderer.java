package dev.apoapsis.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.apoapsis.grid.ShipParts;
import dev.apoapsis.ship.DescentGear;
import dev.apoapsis.ship.ShipEntity;
import dev.apoapsis.ship.ShipStructure;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a ship: every drawn block, placed by its position relative to the centre of mass and rotated by the
 * interpolated orientation. Blocks go through {@link SubmitNodeCollector#submitMovingBlock}, as FallingBlockRenderer
 * draws its block. Client only.
 */
public final class ShipRenderer extends EntityRenderer<ShipEntity, ShipRenderState> {
    /** Ships farther than this from the camera are not drawn, in metres. */
    public static final double RENDER_RANGE = 256.0;
    /** A block whose cube is nearer the camera than this is not drawn, in metres: twice the 0.05 m near plane. */
    static final double CAMERA_CLEARANCE = 0.1;
    /** Lit canopy fill: vanilla has no minecraft:textures/misc/white.png in 26.x, so NeoForge's 16x16 white is used. */
    private static final RenderType CANOPY_TYPE = RenderTypes.entitySolid(
        Identifier.fromNamespaceAndPath("neoforge", "textures/white.png"));

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
        ShipRenderCache.Entry cache = ShipRenderCache.entry(ship.getId());
        state.orientation = ship.getRenderOrientation(partialTicks);
        state.centerOfMass = ship.getCenterOfMassLocal();
        state.blocks = cache.updateBlocks(ship.getStructure());
        state.chuteInflation = ship.isLanded() ? 0f : (float) ship.chuteInflation(partialTicks);
        ReentryState.Heat heat = ReentryState.of(ship);
        state.heat = heat.intensity();
        state.heatWind = heat.wind();
        AABB localBounds = ship.getStructure().localBounds();
        state.glowRadius = 0.5 * Math.max(localBounds.getXsize(), Math.max(localBounds.getYsize(), localBounds.getZsize())) + 1.0;
        if (ship.level() instanceof ClientLevel level) {
            // Every block takes its light from the ship's position, as FallingBlockRenderer does for its block.
            // The light references are set every frame and never outlive the cache entry.
            BlockPos pos = BlockPos.containing(ship.getPosition(partialTicks));
            var biome = level.getBiome(pos);
            var lighting = level.cardinalLighting();
            var lightEngine = level.getLightEngine();
            for (ShipRenderState.ShipBlock block : state.blocks) {
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
        // The camera in block coordinates, to leave out the block it sits in. The pilot's eye is 0.62 m above the
        // cockpit's top face, which is inside the block stacked on the cockpit; drawn, that block's inner box has its
        // underside a hair above the lens, and the near plane cuts it into a brown bar across the middle of the view.
        Vec3 cam = camera.pos;
        Vector3f eye = new Quaternionf(state.orientation).conjugate()
            .transform(new Vector3f((float) (cam.x - state.x), (float) (cam.y - state.y), (float) (cam.z - state.z)))
            .add((float) com.x, (float) com.y, (float) com.z);
        poseStack.pushPose();
        // The pose origin is the centre of mass (the entity position); block p's corner sits at R * (p - com).
        poseStack.rotateAround(state.orientation, 0.0F, 0.0F, 0.0F);
        for (ShipRenderState.ShipBlock block : state.blocks) {
            if (block.state().getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            BlockPos p = block.pos();
            if (isAtCamera(p, eye.x, eye.y, eye.z)) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(p.getX() - com.x, p.getY() - com.y, p.getZ() - com.z);
            submitNodeCollector.submitMovingBlock(poseStack, block.moving(), state.outlineColor);
            poseStack.popPose();
        }
        float inflation = state.chuteInflation;
        int light = state.lightCoords;
        if (inflation > 0) {
            for (ShipRenderState.ShipBlock block : state.blocks) {
                if (!ShipParts.isParachute(block.state())) {
                    continue;
                }
                BlockPos p = block.pos();
                double ax = p.getX() + 0.5 - com.x;
                double ay = p.getY() + 0.5 - com.y;
                double az = p.getZ() + 0.5 - com.z;
                double hl = Math.hypot(ax, az);
                double ux = hl < 0.3 ? 0.0 : ax / hl;
                double uz = hl < 0.3 ? 0.0 : az / hl;
                submitNodeCollector.submitCustomGeometry(poseStack, CANOPY_TYPE,
                    (pose, buffer) -> ParachuteCanopy.emit(pose, buffer, (float) ax, (float) ay, (float) az,
                        (float) ux, (float) uz, inflation, light));
            }
        }
        poseStack.popPose();
        if (state.heat > 0.05f) {
            ReentryGlow.submit(poseStack, submitNodeCollector, state.heat, state.heatWind, state.glowRadius,
                new Vec3(cam.x - state.x, cam.y - state.y, cam.z - state.z));
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /**
     * True when the camera, at ({@code ex}, {@code ey}, {@code ez}) in block coordinates, is inside the cube of block
     * {@code p} or within {@link #CAMERA_CLEARANCE} of it. Such a block is not drawn: a lens in or against a block sees
     * only slivers of its inner faces, cut by the near plane. Every other block of the ship is drawn as usual.
     */
    static boolean isAtCamera(BlockPos p, double ex, double ey, double ez) {
        return ex > p.getX() - CAMERA_CLEARANCE && ex < p.getX() + 1 + CAMERA_CLEARANCE
            && ey > p.getY() - CAMERA_CLEARANCE && ey < p.getY() + 1 + CAMERA_CLEARANCE
            && ez > p.getZ() - CAMERA_CLEARANCE && ez < p.getZ() + 1 + CAMERA_CLEARANCE;
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

    /** The world-space box around every block, from the rotated local bounds and the orientation drawn this frame. */
    @Override
    protected AABB getBoundingBoxForCulling(ShipEntity ship, float partialTicks) {
        ShipStructure structure = ship.getStructure();
        AABB local = structure.localBounds();
        if (ship.getChuteState() > DescentGear.STOWED && !ship.isLanded()) {
            // The canopies stand about 12 blocks above the pack and splay out about 6 blocks.
            local = local.inflate(6.5, 0.0, 6.5).expandTowards(0.0, 13.0, 0.0);
        }
        Vec3 com = ship.getCenterOfMassLocal();
        Quaternionf q = ship.getRenderOrientation(partialTicks);
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
}
