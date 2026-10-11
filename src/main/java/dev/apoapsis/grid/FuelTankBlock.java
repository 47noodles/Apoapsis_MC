package dev.apoapsis.grid;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Fuel tank: a block whose block entity holds hydrolox. Canisters refill it; breaking it loses the fuel. */
public class FuelTankBlock extends Block implements EntityBlock {
    /** Hydrolox added by one canister, in kg. */
    public static final double CANISTER_KG = 300.0;

    public FuelTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(GridBlocks.FUEL_TANK_BE.get(), pos, state);
    }

    /** A hydrolox canister adds {@link #CANISTER_KG}, up to the tank's capacity, and is used up outside creative. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(GridBlocks.HYDROLOX_CANISTER.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank)) {
            return InteractionResult.SUCCESS;
        }
        if (tank.getFuelKg() >= FuelTankBlockEntity.CAPACITY_KG) {
            player.sendOverlayMessage(Component.translatable("message.apoapsis.tank_full"));
            return InteractionResult.FAIL;
        }
        tank.setFuelKg(tank.getFuelKg() + CANISTER_KG);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.sendOverlayMessage(fuelMessage(tank));
        return InteractionResult.SUCCESS;
    }

    /** Empty-hand use shows the stored amount. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank) {
            player.sendOverlayMessage(fuelMessage(tank));
        }
        return InteractionResult.SUCCESS;
    }

    /** "Hydrolox tank: 900 / 1,200 kg". */
    static Component fuelMessage(FuelTankBlockEntity tank) {
        return Component.translatable("message.apoapsis.tank_fuel", formatKg(tank.getFuelKg()), formatKg(FuelTankBlockEntity.CAPACITY_KG));
    }

    private static String formatKg(double kg) {
        return String.format(Locale.ROOT, "%,d", Math.round(kg));
    }
}
