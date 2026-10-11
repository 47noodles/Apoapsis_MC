package dev.apoapsis.mixin;

import dev.apoapsis.ages.CraftGate;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Withholds the crafting result of an item the player's Age does not allow yet. Injects at the head of
 * {@code CraftingMenu.slotChangedCraftingGrid}, which computes the result for the 3x3 table, the 2x2 inventory grid
 * (through {@code InventoryMenu}) and the recipe-book fill (through {@code finishPlacingRecipe}).
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(
            method = "slotChangedCraftingGrid(Lnet/minecraft/world/inventory/AbstractContainerMenu;"
                    + "Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/inventory/CraftingContainer;"
                    + "Lnet/minecraft/world/inventory/ResultContainer;"
                    + "Lnet/minecraft/world/item/crafting/RecipeHolder;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void apoapsis$withholdLockedResult(AbstractContainerMenu menu, ServerLevel level, Player player,
                                                      CraftingContainer container, ResultContainer resultSlots,
                                                      RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci) {
        int required = CraftGate.withheldAge(player, level, container, recipeHint);
        if (required <= 0) {
            return;
        }
        // Clear the result slot and tell the client, as the vanilla method does at its end, then skip the rest.
        resultSlots.setItem(0, ItemStack.EMPTY);
        menu.setRemoteSlot(0, ItemStack.EMPTY);
        ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
        CraftGate.notifyWithheld(player, required);
        ci.cancel();
    }
}
