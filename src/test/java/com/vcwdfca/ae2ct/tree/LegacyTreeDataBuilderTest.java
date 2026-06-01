package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LegacyTreeDataBuilderTest {
    @Test
    void fallbackBuilderCreatesLegacyTreeFromRecipeHelper() {
        GenericStack output = stack("gear", 4);
        RecipeHelper.Recipe gearRecipe = new RecipeHelper.Recipe(
                List.of(stack("iron", 16)),
                List.of(stack("gear", 2)),
                2L
        );
        RecipeHelper helper = new RecipeHelper(output, List.of(gearRecipe));

        LegacyTreeData data = new TreeDataBuilder().buildFallback(helper, List.of());

        assertNotNull(data.root());
        assertEquals("gear", data.root().output().what().toString());
        assertEquals(4, data.root().amount());
        assertEquals(1, data.root().inputs().size());
        assertEquals("iron", data.root().inputs().get(0).inputs().get(0).output().what().toString());
        assertEquals(32, data.root().inputs().get(0).inputs().get(0).amount());
    }

    @Test
    void fallbackBuilderReturnsEmptyDataForMissingHelper() {
        LegacyTreeData data = new TreeDataBuilder().buildFallback(null, List.of());

        assertEquals(0, data.allNodes().size());
    }

    private static GenericStack stack(String key, long amount) {
        return new GenericStack(new TestKey(key), amount);
    }
}
