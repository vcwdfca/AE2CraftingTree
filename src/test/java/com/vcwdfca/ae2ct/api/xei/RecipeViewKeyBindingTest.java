package com.vcwdfca.ae2ct.api.xei;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewKeyBindingTest {
    @Test
    void recipeActionWinsWhenBothExternalBindingsMatch() {
        RecipeViewKeyBinding binding = new FixedBinding(true, true);

        assertEquals(RecipeViewAction.SHOW_RECIPE, binding.resolve(82, 0, 0));
    }

    @Test
    void usesActionIsReturnedWhenOnlyUsesBindingMatches() {
        RecipeViewKeyBinding binding = new FixedBinding(false, true);

        assertEquals(RecipeViewAction.SHOW_USES, binding.resolve(85, 0, 0));
    }

    @Test
    void noneActionIsReturnedWhenNoExternalBindingMatches() {
        RecipeViewKeyBinding binding = new FixedBinding(false, false);

        assertEquals(RecipeViewAction.NONE, binding.resolve(65, 0, 0));
    }

    private record FixedBinding(boolean recipe, boolean uses) implements RecipeViewKeyBinding {
        @Override
        public boolean matchesShowRecipe(int keyCode, int scanCode, int modifiers) {
            return recipe;
        }

        @Override
        public boolean matchesShowUses(int keyCode, int scanCode, int modifiers) {
            return uses;
        }
    }
}
