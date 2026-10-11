package dev.apoapsis.ages;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/** Server-side crafting gate, called from the CraftingMenu mixin before the grid writes its result. */
public final class CraftGate {
    private CraftGate() {}

    /**
     * The Age the withheld result needs, or 0 when the grid's result may be shown to this player. Uses the same recipe
     * lookup as vanilla, so the 3x3 table, the 2x2 inventory grid and the recipe-book fill all pass through it.
     */
    public static int withheldAge(Player player, ServerLevel level, CraftingContainer container,
                                  RecipeHolder<CraftingRecipe> recipeHint) {
        CraftingInput input = container.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);
        if (recipe.isEmpty()) {
            return 0;
        }
        ItemStack result = recipe.get().value().assemble(input);
        return Ages.canUse(player, result) ? 0 : Ages.requiredAge(result);
    }

    /** Shows the "Needs Age N" action-bar line, sharing the use and place gates' per-player cooldown. */
    public static void notifyWithheld(Player player, int required) {
        AgeGates.refuse(player, required);
    }

    /**
     * True when a result assembled with no player (a Crafter's output) is gated and this world is not a sandbox. The
     * Crafter has no player to check, so gated items cannot be automated in Stage 0.
     */
    public static boolean withholdsAutomated(ServerLevel level, ItemStack result) {
        return Ages.requiredAge(result) > 0 && !Ages.sandbox(level);
    }
}
