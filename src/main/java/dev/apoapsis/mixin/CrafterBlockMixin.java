package dev.apoapsis.mixin;

import dev.apoapsis.ages.CraftGate;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Withholds a Crafter's output of an item the world's Ages do not allow to be automated. A Crafter has no player, so its
 * result cannot be checked against a player's Age. Injects at the head of {@code CrafterBlock.dispenseFrom}, the only
 * place a Crafter assembles its result. Sandbox worlds are not affected.
 */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {

    @Inject(
            method = "dispenseFrom(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void apoapsis$withholdGatedResult(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (!(level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter)) {
            return;
        }
        CraftingInput input = crafter.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = CrafterBlock.getPotentialResults(level, input);
        if (recipe.isPresent() && CraftGate.withholdsAutomated(level, recipe.get().value().assemble(input))) {
            // The vanilla outcome for a crafter with no result: the failure sound, and nothing is dispensed.
            level.levelEvent(1050, pos, 0);
            ci.cancel();
        }
    }
}
