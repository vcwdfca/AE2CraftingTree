package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LegacyTreeNode implements Comparable<LegacyTreeNode> {
    private final LegacyTreeProcess parent;
    private final GenericStack output;
    private final List<LegacyTreeProcess> inputs;
    private final long missing;
    private final Amounts amounts;
    private boolean missingCached;
    private boolean missingCache;

    public LegacyTreeNode(LegacyTreeProcess parent, GenericStack output, List<LegacyTreeProcess> inputs,
                          long missing, Amounts amounts) {
        this.parent = parent;
        this.output = Objects.requireNonNull(output, "output");
        this.inputs = new ArrayList<>(Objects.requireNonNull(inputs, "inputs"));
        this.missing = Math.max(0, missing);
        this.amounts = amounts == null ? new Amounts(this.missing, 0, 0) : amounts;
    }

    public void addInput(LegacyTreeProcess process) {
        inputs.add(process);
        clearMissingCache();
    }

    public LegacyTreeProcess parent() {
        return parent;
    }

    public GenericStack output() {
        return output;
    }

    public AEKey key() {
        return output.what();
    }

    public long amount() {
        return output.amount();
    }

    public List<LegacyTreeProcess> inputs() {
        return inputs;
    }

    public long missing() {
        return missing;
    }

    public Amounts amounts() {
        return amounts;
    }

    public String displayNameLower() {
        return key().getDisplayName().getString().toLowerCase(Locale.ROOT);
    }

    public void sort() {
        inputs.sort(Comparator.reverseOrder());
        for (LegacyTreeProcess input : inputs) {
            input.sort();
            for (LegacyTreeNode subNode : input.inputs()) {
                subNode.sort();
            }
        }
    }

    @Override
    public int compareTo(LegacyTreeNode other) {
        return Integer.compare(diveToDeep(this, 0, new DepthRecorder()), diveToDeep(other, 0, new DepthRecorder()));
    }

    public static int diveToDeep(LegacyTreeNode node, int depth, DepthRecorder recorder) {
        for (LegacyTreeProcess input : node.inputs) {
            for (LegacyTreeNode subNode : input.inputs()) {
                int newDepth = depth + 1;
                recorder.dive(newDepth);
                diveToDeep(subNode, newDepth, recorder);
            }
        }
        return recorder.depth();
    }

    public int totalProcessors() {
        int size = inputs.size();
        for (LegacyTreeProcess input : inputs) {
            for (LegacyTreeNode node : input.inputs()) {
                size += node.totalProcessors();
            }
        }
        return size;
    }

    public int getRenderExpandNodes() {
        int size = Math.max(inputs.size() - 1, 0);
        for (LegacyTreeProcess input : inputs) {
            size += Math.max(input.inputs().size() - 1, 0);
            for (LegacyTreeNode node : input.inputs()) {
                size += node.getRenderExpandNodes();
            }
        }
        return size;
    }

    public int getLastNodeRenderExpandNodes() {
        if (inputs.isEmpty()) {
            return 0;
        }
        LegacyTreeProcess process = inputs.get(inputs.size() - 1);
        if (process.inputs().isEmpty()) {
            return 0;
        }
        return process.inputs().get(process.inputs().size() - 1).getRenderExpandNodes();
    }

    public LegacyTreeNode withMissingOnly() {
        if (!isMissing(this)) {
            return null;
        }

        LegacyTreeNode copy = new LegacyTreeNode(parent, output, List.of(), missing, amounts);
        for (LegacyTreeProcess input : inputs) {
            List<LegacyTreeNode> missingSubNodes = new ArrayList<>();
            for (LegacyTreeNode subNode : input.inputs()) {
                if (isMissing(subNode)) {
                    missingSubNodes.add(subNode.withMissingOnly());
                }
            }
            if (!missingSubNodes.isEmpty()) {
                copy.inputs.add(new LegacyTreeProcess(missingSubNodes));
            }
        }
        copy.missingCached = true;
        copy.missingCache = true;
        return copy;
    }

    public static boolean isMissing(LegacyTreeNode node) {
        if (node.missingCached) {
            return node.missingCache;
        }
        if (node.missing > 0 || node.amounts.hasMissing()) {
            node.missingCached = true;
            node.missingCache = true;
            return true;
        }
        for (LegacyTreeProcess input : node.inputs) {
            for (LegacyTreeNode subNode : input.inputs()) {
                if (isMissing(subNode)) {
                    node.missingCached = true;
                    node.missingCache = true;
                    return true;
                }
            }
        }
        node.missingCached = true;
        node.missingCache = false;
        return false;
    }

    private void clearMissingCache() {
        missingCached = false;
        missingCache = false;
    }

    public static final class DepthRecorder {
        private int depth;

        void dive(int depth) {
            this.depth = Math.max(this.depth, depth);
        }

        public int depth() {
            return depth;
        }
    }
}
