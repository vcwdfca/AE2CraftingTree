package com.vcwdfca.ae2ct.api.xei;

/**
 * Describes recipe viewer key bindings that AE2CT should honor while a tree node is hovered.
 */
public interface RecipeViewKeyBinding {
    /**
     * Returns whether the current key press matches the viewer's configured "show recipe" binding.
     */
    boolean matchesShowRecipe(int keyCode, int scanCode, int modifiers);

    /**
     * Returns whether the current key press matches the viewer's configured "show uses" binding.
     */
    boolean matchesShowUses(int keyCode, int scanCode, int modifiers);

    /**
     * Converts the viewer-specific key binding result into the action AE2CT should execute.
     */
    default RecipeViewAction resolve(int keyCode, int scanCode, int modifiers) {
        return resolve(matchesShowRecipe(keyCode, scanCode, modifiers),
            matchesShowUses(keyCode, scanCode, modifiers));
    }

    /**
     * Resolves raw recipe and uses matches, preferring recipes when both bindings match.
     */
    static RecipeViewAction resolve(boolean showRecipe, boolean showUses) {
        if (showRecipe) {
            return RecipeViewAction.SHOW_RECIPE;
        }
        if (showUses) {
            return RecipeViewAction.SHOW_USES;
        }
        return RecipeViewAction.NONE;
    }
}
