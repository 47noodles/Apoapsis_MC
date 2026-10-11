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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.UseRemainder;
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

    /**
     * A water bucket adds one bucket; furnace fuel adds its burn time. Each click uses one item outside creative.
     * Fuel is refused when more than half its burn would be wasted against the heat cap; otherwise the item's
     * crafting remainder (for example an empty bucket) is returned to the hand.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.WATER_BUCKET)) {
            return addWater(stack, level, pos, player, hand);
        }
        if (stack.has(DataComponents.COOKING_FUEL)) {
            return addHeat(stack, level, pos, player, hand);
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

    /**
     * Adds furnace fuel's burn time to the heat buffer. The item is refused when more than half its burn would be
     * wasted against the cap. Otherwise the heat is clamped to the cap and the item's crafting remainder (if any)
     * replaces the item in the hand; with no remainder, one item is consumed.
     */
    private static InteractionResult addHeat(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof FuelRefineryBlockEntity refinery)) {
            return InteractionResult.SUCCESS;
        }
        int burn = burnTicks(server, pos, stack);
        if (burn <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        long total = (long) refinery.getHeatTicks() + burn;
        long wasted = Math.max(0L, total - FuelRefineryBlockEntity.MAX_HEAT_TICKS);
        if (wasted * 2 > burn) { // a full buffer wastes the whole burn, so this covers it too
            player.sendOverlayMessage(Component.translatable("message.apoapsis.refinery_full"));
            return InteractionResult.FAIL;
        }
        refinery.setHeatTicks((int) Math.min(total, FuelRefineryBlockEntity.MAX_HEAT_TICKS));
        if (!player.isCreative()) {
            ItemStack remainder = remainderOf(stack);
            if (remainder.isEmpty()) {
                stack.shrink(1);
            } else {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, remainder));
            }
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

    /**
     * What a used fuel item leaves behind: its use-remainder component, else its crafting remainder (a lava bucket
     * leaves a bucket), else empty.
     */
    static ItemStack remainderOf(ItemStack stack) {
        UseRemainder use = stack.get(DataComponents.USE_REMAINDER);
        if (use != null) {
            return use.convertInto().create();
        }
        ItemStackTemplate crafting = stack.getItem().getCraftingRemainder();
        return crafting == null ? ItemStack.EMPTY : crafting.create();
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
