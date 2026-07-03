package com.vcwdfca.ae2ct.api;

import appeng.api.networking.crafting.ICraftingPlan;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class LegacyTreePlanStore {
    private static final Map<ICraftingPlan, LegacyTreeData> TREES = Collections.synchronizedMap(new WeakHashMap<>());

    private LegacyTreePlanStore() {
    }

    public static void put(ICraftingPlan plan, LegacyTreeData tree) {
        if (plan != null && tree != null && tree.root() != null) {
            TREES.put(plan, tree);
        }
    }

    public static LegacyTreeData get(ICraftingPlan plan) {
        return plan == null ? null : TREES.get(plan);
    }
}
