package com.vcwdfca.ae2ct.tree;

import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.vcwdfca.ae2ct.api.RecipeHelper;

import java.util.Comparator;
import java.util.List;

public final class PlanKey {
    private PlanKey() {
    }

    public static String fromRecipeHelper(RecipeHelper helper, LegacyTreeData tree) {
        StringBuilder sb = new StringBuilder();
        appendRecipeHelper(sb, helper);
        if (tree != null) {
            sb.append("|tree:");
            for (LegacyTreeNode node : tree.allNodes()) {
                sb.append(node.key()).append('@').append(node.amount());
                sb.append('/').append(node.missing());
                sb.append('/').append(node.amounts().missing());
                sb.append('/').append(node.amounts().stored());
                sb.append('/').append(node.amounts().craft());
                sb.append(';');
            }
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    public static String fromRecipeHelper(RecipeHelper helper, List<CraftingPlanSummaryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        appendRecipeHelper(sb, helper);
        if (entries != null && !entries.isEmpty()) {
            sb.append("|summary:");
            entries.stream()
                .sorted(Comparator.comparing(entry -> entry.getWhat().toString()))
                .forEach(entry -> {
                    sb.append(entry.getWhat());
                    sb.append('/').append(entry.getMissingAmount());
                    sb.append('/').append(entry.getStoredAmount());
                    sb.append('/').append(entry.getCraftAmount());
                    sb.append(';');
                });
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    private static void appendRecipeHelper(StringBuilder sb, RecipeHelper helper) {
        sb.append(helper.output.what()).append(':').append(helper.output.amount());
        sb.append('|').append(helper.recipes.size());
        for (RecipeHelper.Recipe recipe : helper.recipes) {
            sb.append('|').append(recipe.outputs().getFirst().what()).append('@').append(recipe.outputs().getFirst().amount());
            sb.append(':').append(recipe.inputs().size());
            recipe.inputs().forEach(input -> sb.append(',').append(input.what()).append('@').append(input.amount()));
        }
    }
}
