package dev.apoapsis.ages;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/** Server-side crafting gate, called from the CraftingMenu mixin before the grid writes its result. */
public final class CraftGate {
    private CraftGate() {}

    /**
     * True when the grid would craft a result the player's Age does not allow yet. Uses the same recipe lookup as
     * vanilla, so the 3x3 table, the 2x2 inventory grid and the recipe-book fill all pass through it.
     */
    public static boolean withholds(Player player, ServerLevel level, CraftingContainer container,
                                    RecipeHolder<CraftingRecipe> recipeHint) {
        CraftingInput input = container.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);
        return recipe.isPresent() && !Ages.canUse(player, recipe.get().value().assemble(input));
    }
}
