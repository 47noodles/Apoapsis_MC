package dev.apoapsis.grid;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.context.ContextKeySet;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;
import org.jspecify.annotations.Nullable;

/**
 * Fuel refinery: water buckets and furnace fuel go in by right-click, hydrolox comes out into adjacent fuel tanks.
 * Lit while it refines. Empty-hand right-click shows its status on the action bar.
 */
public class FuelRefineryBlock extends Block implements EntityBlock {
    /** Queried by a fuel item's burn time: the origin of the refinery and the stack itself. */
    private static final ContextKeySet FUEL_QUERY = new ContextKeySet.Builder()
            .required(LootContextParams.ORIGIN)
            .optional(NeoForgeLootContextParams.QUERIED_STACK)
            .build();

    public FuelRefineryBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelRefineryBlockEntity(GridBlocks.FUEL_REFINERY_BE.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != GridBlocks.FUEL_REFINERY_BE.get()) {
            return null;
        }
        BlockEntityTicker<?> ticker = level.isClientSide() ? FuelRefineryBlockEntity.CLIENT_TICKER : FuelRefineryBlockEntity.SERVER_TICKER;
        return (BlockEntityTicker<T>) ticker;
    }

    /** A water bucket adds one bucket; furnace fuel adds its burn time. Each click uses one item outside creative. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.WATER_BUCKET)) {
            return addWater(stack, level, pos, player, hand);
        }
        if (stack.has(DataComponents.COOKING_FUEL)) {
            return addHeat(stack, level, pos, player);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Empty-hand use shows the status line. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof FuelRefineryBlockEntity refinery) {
            player.sendOverlayMessage(statusMessage(level, pos, refinery));
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult addWater(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof FuelRefineryBlockEntity refinery)) {
            return InteractionResult.SUCCESS;
        }
        if (refinery.getWaterBuckets() >= FuelRefineryBlockEntity.MAX_WATER) {
            player.sendOverlayMessage(Component.translatable("message.apoapsis.refinery_full"));
            return InteractionResult.FAIL;
        }
        refinery.setWaterBuckets(refinery.getWaterBuckets() + 1);
        if (!player.isCreative()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
        }
        player.sendOverlayMessage(statusMessage(level, pos, refinery));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult addHeat(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof FuelRefineryBlockEntity refinery)) {
            return InteractionResult.SUCCESS;
        }
        int burn = burnTicks(server, pos, stack);
        if (burn <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (refinery.getHeatTicks() >= FuelRefineryBlockEntity.MAX_HEAT_TICKS) {
            player.sendOverlayMessage(Component.translatable("message.apoapsis.refinery_full"));
            return InteractionResult.FAIL;
        }
        refinery.setHeatTicks(refinery.getHeatTicks() + burn);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.sendOverlayMessage(statusMessage(level, pos, refinery));
        return InteractionResult.SUCCESS;
    }

    /** The burn time of a furnace fuel stack, as the furnace itself reads it; 0 when it is not fuel. */
    static int burnTicks(ServerLevel level, BlockPos pos, ItemStack stack) {
        CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
        if (fuel == null) {
            return 0;
        }
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
                .create(FUEL_QUERY);
        return fuel.burnTime().get(new LootContext.Builder(params).create(Optional.empty()), 0);
    }

    /** "Refinery: 3 water · 20 s heat · 900 kg hydrolox"; water shows the infinite sign beside a source block. */
    static Component statusMessage(Level level, BlockPos pos, FuelRefineryBlockEntity refinery) {
        String water = FuelRefineryBlockEntity.hasSourceWater(level, pos)
                ? "∞"
                : String.valueOf(refinery.getWaterBuckets());
        String heat = String.valueOf(refinery.getHeatTicks() / 20);
        String kg = String.format(Locale.ROOT, "%,d", Math.round(refinery.getHydroloxKg()));
        return Component.translatable("apoapsis.refinery.status", water, heat, kg);
    }
}
