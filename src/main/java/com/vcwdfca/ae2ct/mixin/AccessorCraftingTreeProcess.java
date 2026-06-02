package com.vcwdfca.ae2ct.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingTreeNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = appeng.crafting.CraftingTreeProcess.class, remap = false)
public interface AccessorCraftingTreeProcess {
    @Accessor("details")
    IPatternDetails ae2ct$getDetails();

    @Accessor("nodes")
    Map<CraftingTreeNode, Long> ae2ct$getNodes();
}
