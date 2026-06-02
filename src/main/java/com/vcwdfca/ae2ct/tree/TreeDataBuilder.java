package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import com.vcwdfca.ae2ct.mixin.AccessorCraftingTreeNode;
import com.vcwdfca.ae2ct.mixin.AccessorCraftingTreeProcess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class TreeDataBuilder {
    public static LegacyTreeData fromCraftingTree(CraftingTreeNode root, long rootAmount, KeyCounter missingItems) {
        if (root == null) {
            return null;
        }
        Map<AEKey, Long> remainingMissing = new HashMap<>();
        if (missingItems != null) {
            for (Entry<AEKey, Long> entry : missingItems) {
                remainingMissing.put(entry.getKey(), entry.getValue());
            }
        }
        LegacyTreeNode node = fromCraftingNode(root, rootAmount, remainingMissing);
        node.sort();
        return new LegacyTreeData(node);
    }

    public LegacyTreeData buildFallback(RecipeHelper helper, List<CraftingPlanSummaryEntry> entries) {
        if (helper == null || helper.output == null) {
            return new LegacyTreeData(null);
        }

        Map<AEKey, RecipeHelper.Recipe> recipeByOutput = new HashMap<>();
        for (RecipeHelper.Recipe recipe : helper.recipes) {
            for (GenericStack output : recipe.outputs()) {
                recipeByOutput.putIfAbsent(output.what(), recipe);
            }
        }

        Map<AEKey, AmountTracker> amountMap = new HashMap<>();
        for (CraftingPlanSummaryEntry entry : entries) {
            amountMap.put(entry.getWhat(), new AmountTracker(
                    AmountTracker.check(entry.getMissingAmount()),
                    AmountTracker.check(entry.getStoredAmount()),
                    AmountTracker.check(entry.getCraftAmount())));
        }

        LegacyTreeNode root = buildFallbackNode(helper.output, helper.output.amount(), amountMap, recipeByOutput);
        root.sort();
        return new LegacyTreeData(root);
    }

    private static LegacyTreeNode fromCraftingNode(CraftingTreeNode node, long amount, Map<AEKey, Long> remainingMissing) {
        AccessorCraftingTreeNode nodeAccessor = (AccessorCraftingTreeNode) node;
        GenericStack output = new GenericStack(nodeAccessor.ae2ct$getWhat(), amount);
        long missing = consumeMissing(remainingMissing, output.what(), amount);
        LegacyTreeNode converted = new LegacyTreeNode(null, output, List.of(), missing, new Amounts(missing, 0, 0));

        ArrayList<CraftingTreeProcess> processes = nodeAccessor.ae2ct$getNodes();
        if (processes != null) {
            for (CraftingTreeProcess process : processes) {
                LegacyTreeProcess convertedProcess = fromCraftingProcess(process, node, amount, remainingMissing);
                if (convertedProcess != null && !convertedProcess.inputs().isEmpty()) {
                    converted.addInput(convertedProcess);
                }
            }
        }

        return converted;
    }

    private static LegacyTreeProcess fromCraftingProcess(CraftingTreeProcess process, CraftingTreeNode parentNode,
                                                        long parentAmount, Map<AEKey, Long> remainingMissing) {
        AccessorCraftingTreeProcess processAccessor = (AccessorCraftingTreeProcess) process;
        AccessorCraftingTreeNode parentAccessor = (AccessorCraftingTreeNode) parentNode;
        long processTimes = processTimes(processAccessor, parentAccessor.ae2ct$getWhat(), parentAmount);

        List<LegacyTreeNode> inputs = new ArrayList<>();
        for (Entry<CraftingTreeNode, Long> entry : processAccessor.ae2ct$getNodes().entrySet()) {
            AccessorCraftingTreeNode childAccessor = (AccessorCraftingTreeNode) entry.getKey();
            long childAmount = childAccessor.ae2ct$getAmount() * entry.getValue() * processTimes;
            inputs.add(fromCraftingNode(entry.getKey(), childAmount, remainingMissing));
        }

        return inputs.isEmpty() ? null : new LegacyTreeProcess(inputs);
    }

    private static long consumeMissing(Map<AEKey, Long> remainingMissing, AEKey key, long amount) {
        if (remainingMissing == null || remainingMissing.isEmpty()) {
            return 0;
        }
        long remaining = remainingMissing.getOrDefault(key, 0L);
        long consumed = Math.min(Math.max(remaining, 0), amount);
        if (consumed > 0) {
            remainingMissing.put(key, remaining - consumed);
        }
        return consumed;
    }

    private static long processTimes(AccessorCraftingTreeProcess process, AEKey parentKey, long parentAmount) {
        long craftedPerPattern = 0;
        for (GenericStack output : process.ae2ct$getDetails().getOutputs()) {
            if (parentKey.matches(output)) {
                craftedPerPattern += output.amount();
            }
        }
        if (craftedPerPattern <= 0) {
            return 1;
        }
        return ceilDivStatic(parentAmount, craftedPerPattern);
    }

    private LegacyTreeNode buildFallbackNode(GenericStack stack, long amount,
                                             Map<AEKey, AmountTracker> amountMap,
                                             Map<AEKey, RecipeHelper.Recipe> recipeByOutput) {
        AmountTracker tracker = amountMap.getOrDefault(stack.what(), new AmountTracker(0, 0, 0));
        Amounts amounts = new Amounts(tracker.missing, tracker.stored, tracker.craft);
        RecipeHelper.Recipe recipe = recipeByOutput.get(stack.what());
        LegacyTreeNode node = new LegacyTreeNode(null, new GenericStack(stack.what(), amount), List.of(), tracker.missing, amounts);

        if (recipe == null) {
            return node;
        }

        long outputPerPattern = outputAmountFor(recipe, stack.what());
        long times = outputPerPattern <= 0 ? 0 : ceilDiv(amount, outputPerPattern);
        List<LegacyTreeNode> inputs = new ArrayList<>();
        for (GenericStack input : recipe.inputs()) {
            long childAmount = input.amount() * times;
            inputs.add(buildFallbackNode(input, childAmount, amountMap, recipeByOutput));
        }
        if (!inputs.isEmpty()) {
            node.addInput(new LegacyTreeProcess(inputs));
        }
        return node;
    }

    private long outputAmountFor(RecipeHelper.Recipe recipe, AEKey key) {
        long amount = 0;
        for (GenericStack output : recipe.outputs()) {
            if (key.matches(output)) {
                amount += output.amount();
            }
        }
        return amount;
    }

    private long ceilDiv(long value, long divisor) {
        if (divisor == 0) {
            return 0;
        }
        return ceilDivStatic(value, divisor);
    }

    private static long ceilDivStatic(long value, long divisor) {
        if (divisor == 0) {
            return 0;
        }
        return (value + divisor - 1) / divisor;
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
