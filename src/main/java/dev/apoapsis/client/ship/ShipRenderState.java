package dev.apoapsis.client.ship;

import java.util.List;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Per-frame data the ship renderer submits: pose, centre of mass and the ship's blocks. Client only. */
public final class ShipRenderState extends EntityRenderState {
    /** Body axes to dimension axes. */
    public Quaternionf orientation = new Quaternionf();
    /** Centre of mass in local block coordinates; the entity position is this point in the world. */
    public Vec3 centerOfMass = Vec3.ZERO;
    /** The ship's blocks, shared with the renderer's cache. */
    public List<ShipBlock> blocks = List.of();
    /** How open the parachutes are, 0 to 1; 0 when stowed, torn or landed. */
    public float chuteInflation;
    /** Re-entry heating, 0 to 1; the plasma sheath is drawn above 0.05. */
    public float heat;
    /** Unit direction of motion in dimension axes, which the air flows against; zero when cold. */
    public Vec3 heatWind = Vec3.ZERO;
    /** Half the ship's size in metres, plus a margin: the radius the plasma sheath is drawn at. */
    public double glowRadius;

    /** One block: its local position, state, and the reusable moving-block state that draws it. */
    public record ShipBlock(BlockPos pos, BlockState state, MovingBlockRenderState moving) {}
}
