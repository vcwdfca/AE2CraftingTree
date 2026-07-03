package com.vcwdfca.ae2ct.api;

import com.vcwdfca.ae2ct.tree.LegacyTreeData;

/**
 * Adds AE2CT-specific crafting plan data to AE2's crafting plan summary.
 *
 * <p>The summary is the object serialized from the server-side crafting calculation to the
 * client confirmation screen, so it needs to carry both the recipe fallback data and the
 * captured legacy tree when AE2 exposes one.</p>
 */
public interface ICraftingPlanSummary {
    /**
     * Returns the serialized recipe fallback data used when a captured AE2 tree is unavailable.
     */
    RecipeHelper getJob();

    /**
     * Stores the serialized recipe fallback data on the summary.
     */
    void setJob(RecipeHelper job);

    /**
     * Returns the captured AE2 crafting tree, or null when the plan had to fall back to recipe data.
     */
    LegacyTreeData getLegacyTree();

    /**
     * Stores the captured AE2 crafting tree on the summary for client-side rendering.
     */
    void setLegacyTree(LegacyTreeData tree);
}
