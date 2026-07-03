package com.vcwdfca.ae2ct.api.xei;

import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.api.xei.emi.EmiItem;
import com.vcwdfca.ae2ct.api.xei.jei.JeiItem;
import net.neoforged.fml.ModList;

public class Base {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static void openRecipe(GenericStack itemStack, Boolean isOutput ){
        if(isModLoaded("emi")){
            EmiItem.openRecipe(itemStack, isOutput);
        } else if(isModLoaded("jei")){
            JeiItem.openRecipe(itemStack, isOutput);
        }
    }

    public static RecipeViewAction getRecipeViewAction(int keyCode, int scanCode, int modifiers) {
        if (isModLoaded("emi")) {
            RecipeViewAction action = EmiItem.getRecipeViewAction(keyCode, scanCode, modifiers);
            if (action != RecipeViewAction.NONE) {
                return action;
            }
        }
        if (isModLoaded("jei")) {
            return JeiItem.getRecipeViewAction(keyCode, scanCode, modifiers);
        }
        return RecipeViewAction.NONE;
    }
}
