package com.vcwdfca.ae2ct.tree;

import com.vcwdfca.ae2ct.api.RecipeHelper;

public final class PlanKey {
    private PlanKey() {
    }

    public static String fromRecipeHelper(RecipeHelper helper) {
        StringBuilder sb = new StringBuilder();
        sb.append(helper.output.what().toString()).append(':').append(helper.output.amount());
        sb.append('|').append(helper.recipes.size());
        for (RecipeHelper.Recipe recipe : helper.recipes) {
            sb.append('|').append(recipe.outputs().get(0).what().toString()).append('@').append(recipe.outputs().get(0).amount());
            sb.append(':').append(recipe.inputs().size());
            recipe.inputs().forEach(input -> {
                sb.append(',').append(input.what().toString()).append('@').append(input.amount());
            });
        }
        return Integer.toHexString(sb.toString().hashCode());
    }
}
