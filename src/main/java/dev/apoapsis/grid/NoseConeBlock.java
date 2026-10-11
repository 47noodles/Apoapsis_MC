package dev.apoapsis.grid;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Nose cone: a tapered cap. FACING is the direction the tip points. Placement points the tip away from the face aimed
 * at (up when placed on a top surface), so it can face up or down. FACING is the standard property, so a ship turned
 * by its snap rotation turns the cap with it.
 */
public class NoseConeBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public NoseConeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    /** Only up or down: the tip points away from the surface the player is aiming at. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingVerticalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
