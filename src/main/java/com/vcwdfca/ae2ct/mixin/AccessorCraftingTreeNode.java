package com.vcwdfca.ae2ct.mixin;

import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeProcess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(value = appeng.crafting.CraftingTreeNode.class, remap = false)
public interface AccessorCraftingTreeNode {
    @Accessor("what")
    AEKey ae2ct$getWhat();

    @Accessor("amount")
    long ae2ct$getAmount();

    @Accessor("nodes")
    ArrayList<CraftingTreeProcess> ae2ct$getNodes();
}
