package com.vcwdfca.ae2ct.api;

import com.vcwdfca.ae2ct.tree.LegacyTreeData;

public interface ICraftingPlanSummary {
    RecipeHelper getJob();

    void setJob(RecipeHelper job);

    LegacyTreeData getLegacyTree();

    void setLegacyTree(LegacyTreeData tree);
}
