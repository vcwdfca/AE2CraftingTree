package com.vcwdfca.ae2ct.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingTreeNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes AE2 crafting tree process contents for conversion into AE2CT's row layout model.
 */
@Mixin(value = appeng.crafting.CraftingTreeProcess.class, remap = false)
public interface AccessorCraftingTreeProcess {
    /**
     * Returns the pattern details used by this process so AE2CT can calculate pattern output size.
     */
    @Accessor("details")
    IPatternDetails ae2ct$getDetails();

    /**
     * Returns the process input nodes and their AE2 usage multipliers.
     */
    @Accessor("nodes")
    Map<CraftingTreeNode, Long> ae2ct$getNodes();
}
