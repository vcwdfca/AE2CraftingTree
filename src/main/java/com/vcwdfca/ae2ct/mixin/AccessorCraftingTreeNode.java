package com.vcwdfca.ae2ct.mixin;

import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeProcess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

/**
 * Exposes AE2 crafting tree node contents for conversion into AE2CT's serializable tree model.
 */
@Mixin(value = appeng.crafting.CraftingTreeNode.class, remap = false)
public interface AccessorCraftingTreeNode {
    /**
     * Returns the stack key produced or requested by this AE2 tree node.
     */
    @Accessor("what")
    AEKey ae2ct$getWhat();

    /**
     * Returns the per-node amount stored by AE2 before AE2CT multiplies it by process usage.
     */
    @Accessor("amount")
    long ae2ct$getAmount();

    /**
     * Returns child crafting processes attached to this node.
     */
    @Accessor("nodes")
    ArrayList<CraftingTreeProcess> ae2ct$getNodes();
}
