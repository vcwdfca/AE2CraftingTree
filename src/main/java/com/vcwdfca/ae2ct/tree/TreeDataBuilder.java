package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.vcwdfca.ae2ct.api.RecipeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TreeDataBuilder {
    public TreeData<AEKey> build(RecipeHelper helper, List<CraftingPlanSummaryEntry> entries) {
        Map<AEKey, RecipeHelper.Recipe> recipeByOutput = new HashMap<>();
        for (RecipeHelper.Recipe recipe : helper.recipes) {
            recipeByOutput.put(recipe.outputs().get(0).what(), recipe);
        }

        Map<AEKey, AmountTracker> amountMap = new HashMap<>();
        for (CraftingPlanSummaryEntry entry : entries) {
            amountMap.put(entry.getWhat(), new AmountTracker(entry.getMissingAmount(), entry.getStoredAmount(), entry.getCraftAmount()));
        }

        GenericStack output = helper.output;
        RecipeHelper.Recipe rootRecipe = recipeByOutput.get(output.what());
        long outputAmount = rootRecipe != null ? rootRecipe.outputs().get(0).amount() : output.amount();
        long times = outputAmount == 0 ? 0 : ceilDiv(output.amount(), outputAmount);
        List<GenericStack> inputs = rootRecipe != null ? rootRecipe.inputs() : List.of();

        GraphNode<AEKey> root = buildNode(output, output.amount(), inputs, times, outputAmount, amountMap, recipeByOutput);
        List<GraphNode<AEKey>> all = new ArrayList<>();
        collect(root, all);
        return new TreeData<>(root, all);
    }

    private GraphNode<AEKey> buildNode(GenericStack stack, long amount, List<GenericStack> inputs, long times, long outputAmount,
                                      Map<AEKey, AmountTracker> amountMap,
                                      Map<AEKey, RecipeHelper.Recipe> recipeByOutput) {
        AmountTracker tracker = amountMap.getOrDefault(stack.what(), new AmountTracker(0, Long.MAX_VALUE, Long.MAX_VALUE));
        Amounts nodeAmounts;

        if (inputs.isEmpty()) {
            long stored = AmountTracker.check(amount - tracker.missing);
            nodeAmounts = new Amounts(AmountTracker.check(amount - stored), stored, 0);
            tracker.missing = AmountTracker.check(tracker.missing - amount);
        } else if (times == 0) {
            nodeAmounts = new Amounts(0, amount, 0);
            tracker.stored = AmountTracker.check(tracker.stored - amount);
        } else {
            nodeAmounts = new Amounts(0, AmountTracker.check(amount - times * outputAmount), times * outputAmount);
        }

        GraphNode<AEKey> node = new GraphNode<>(stack.what(), displayNameLower(stack.what()), amount, nodeAmounts);

        for (GenericStack input : inputs) {
            long neededAmount = input.amount() * times;
            RecipeHelper.Recipe childRecipe = recipeByOutput.get(input.what());
            long childOutputAmount = childRecipe != null ? childRecipe.outputs().get(0).amount() : 0;

            long needCraft;
            AmountTracker childTracker = amountMap.getOrDefault(input.what(), new AmountTracker(0, Long.MAX_VALUE, Long.MAX_VALUE));
            needCraft = AmountTracker.check(neededAmount - childTracker.stored);

            long childTimes = childOutputAmount == 0 ? 0 : ceilDiv(needCraft, childOutputAmount);
            List<GenericStack> childInputs = childRecipe != null ? childRecipe.inputs() : List.of();

            GraphNode<AEKey> child = buildNode(input, neededAmount, childInputs, childTimes, childOutputAmount, amountMap, recipeByOutput);
            node.addChild(child);
        }

        return node;
    }

    private void collect(GraphNode<AEKey> node, List<GraphNode<AEKey>> out) {
        if (node == null) {
            return;
        }
        out.add(node);
        for (GraphNode<AEKey> child : node.children()) {
            collect(child, out);
        }
    }

    private long ceilDiv(long value, long divisor) {
        if (divisor == 0) {
            return 0;
        }
        return (value + divisor - 1) / divisor;
    }

    private String displayNameLower(AEKey key) {
        return key.getDisplayName().getString().toLowerCase(Locale.ROOT);
    }

    private static final class AmountTracker {
        long missing;
        long stored;
        long craft;

        AmountTracker(long missing, long stored, long craft) {
            this.missing = missing;
            this.stored = stored;
            this.craft = craft;
        }

        static long check(long value) {
            return value < 0 ? 0 : value;
        }
    }
}
