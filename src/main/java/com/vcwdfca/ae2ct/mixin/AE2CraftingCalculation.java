package com.vcwdfca.ae2ct.mixin;

import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import com.vcwdfca.ae2ct.api.LegacyTreePlanStore;
import com.vcwdfca.ae2ct.tree.TreeDataBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCalculation.class, remap = false)
public class AE2CraftingCalculation {
    @Inject(method = "runCraftAttempt(ZJ)Lappeng/crafting/CraftingPlan;", at = @At("RETURN"))
    private void ae2ct$captureTree(boolean simulate, long amount, CallbackInfoReturnable<CraftingPlan> cir) {
        CraftingPlan plan = cir.getReturnValue();
        if (plan == null) {
            return;
        }

        AccessorCraftingCalculation accessor = (AccessorCraftingCalculation) this;
        LegacyTreePlanStore.put(plan, TreeDataBuilder.fromCraftingTree(accessor.ae2ct$getTree(),
            plan.finalOutput().amount(), accessor.ae2ct$getMissing()));
    }
}
