package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PlanKeyTest {
    @Test
    void legacyTreeAmountsAffectCacheKey() {
        RecipeHelper helper = new RecipeHelper(stack("gear", 4), List.of());
        LegacyTreeData cleanTree = new LegacyTreeData(node("gear", 4, 0));
        LegacyTreeData missingTree = new LegacyTreeData(node("gear", 4, 2));

        String cleanKey = PlanKey.fromRecipeHelper(helper, cleanTree);
        String missingKey = PlanKey.fromRecipeHelper(helper, missingTree);

        assertNotEquals(cleanKey, missingKey);
    }

    @Test
    void fallbackSummaryAmountsAffectCacheKeyBeforeTreeBuild() {
        RecipeHelper helper = new RecipeHelper(stack("gear", 4), List.of());
        CraftingPlanSummaryEntry cleanEntry = new CraftingPlanSummaryEntry(stack("gear", 4).what(), 0, 4, 0);
        CraftingPlanSummaryEntry missingEntry = new CraftingPlanSummaryEntry(stack("gear", 4).what(), 2, 0, 2);

        String cleanKey = PlanKey.fromRecipeHelper(helper, List.of(cleanEntry));
        String missingKey = PlanKey.fromRecipeHelper(helper, List.of(missingEntry));

        assertNotEquals(cleanKey, missingKey);
    }

    private static LegacyTreeNode node(String key, long amount, long missing) {
        return new LegacyTreeNode(null, stack(key, amount), List.of(), missing, new Amounts(missing, 0, 0));
    }

    private static GenericStack stack(String key, long amount) {
        return new GenericStack(new TestKey(key), amount);
    }
}
