package com.vcwdfca.ae2ct.mixin;

import appeng.crafting.CraftingTreeNode;
import appeng.api.stacks.KeyCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = appeng.crafting.CraftingCalculation.class, remap = false)
public interface AccessorCraftingCalculation {
    @Accessor("tree")
    CraftingTreeNode ae2ct$getTree();

    @Accessor("missing")
    KeyCounter ae2ct$getMissing();
}
