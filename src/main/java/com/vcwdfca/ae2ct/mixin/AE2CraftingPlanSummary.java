package com.vcwdfca.ae2ct.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.CraftingPlan;
import appeng.menu.me.crafting.CraftingPlanSummary;
import com.vcwdfca.ae2ct.api.ICraftingPlanSummary;
import com.vcwdfca.ae2ct.api.LegacyTreePayload;
import com.vcwdfca.ae2ct.api.LegacyTreePlanStore;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingPlanSummary.class)
public class AE2CraftingPlanSummary implements ICraftingPlanSummary {
    @Unique
    private RecipeHelper jobs;
    @Unique
    private LegacyTreeData legacyTree;

    @Inject(at = @At("TAIL"), method = "fromJob", cancellable = true, remap = false)
    private static void buildEX(IGrid grid, IActionSource actionSource, ICraftingPlan job, CallbackInfoReturnable<CraftingPlanSummary> cir) {
        var r = cir.getReturnValue();
        ((ICraftingPlanSummary) r).setJob(RecipeHelper.fromCraftingPlan((CraftingPlan) job));
        ((ICraftingPlanSummary) r).setLegacyTree(LegacyTreePlanStore.get(job));
        cir.setReturnValue(r);
    }

    @Inject(at = @At("TAIL"), method = "write", remap = false)
    private void write(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        jobs.write(buffer);
        LegacyTreePayload.writeNullable(buffer, legacyTree);
    }

    @Inject(at = @At("TAIL"), method = "read", cancellable = true, remap = false)
    private static void read(RegistryFriendlyByteBuf buffer, CallbackInfoReturnable<CraftingPlanSummary> cir) {
        var r = cir.getReturnValue();
        var h = RecipeHelper.read(buffer);
        ((ICraftingPlanSummary) r).setJob(h);
        ((ICraftingPlanSummary) r).setLegacyTree(LegacyTreePayload.readNullable(buffer));
        cir.setReturnValue(r);

    }

    @Override
    public RecipeHelper getJob() {
        return jobs;
    }

    @Override
    public void setJob(RecipeHelper job) {
        this.jobs = job;
    }

    @Override
    public LegacyTreeData getLegacyTree() {
        return legacyTree;
    }

    @Override
    public void setLegacyTree(LegacyTreeData tree) {
        this.legacyTree = tree;
    }

}
