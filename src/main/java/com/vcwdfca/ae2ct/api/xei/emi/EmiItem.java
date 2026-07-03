package com.vcwdfca.ae2ct.api.xei.emi;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.api.xei.RecipeViewAction;
import com.vcwdfca.ae2ct.api.xei.RecipeViewKeyBinding;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public final class EmiItem {
    private EmiItem() {
    }

    public static void openRecipe(GenericStack itemStack, boolean isOutput) {
        if (itemStack == null) {
            return;
        }

        EmiIngredient emiIngredient;
        var what = itemStack.what();
        if (what instanceof AEItemKey itemKey) {
            ItemStack stack = itemKey.getReadOnlyStack();
            emiIngredient = EmiStack.of(stack);
        } else if (what instanceof AEFluidKey fluidKey) {
            FluidStack stack = fluidKey.toStack(1000);
            emiIngredient = EmiStack.of(stack.getFluid());
        } else {
            return;
        }

        if (isOutput) {
            EmiApi.displayRecipes(emiIngredient);
        } else {
            EmiApi.displayUses(emiIngredient);
        }
    }

    public static RecipeViewAction getRecipeViewAction(int keyCode, int scanCode, @SuppressWarnings("unused") int modifiers) {
        return RecipeViewKeyBinding.resolve(
            EmiConfig.viewRecipes.matchesKey(keyCode, scanCode),
            EmiConfig.viewUses.matchesKey(keyCode, scanCode)
        );
    }
}
