package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LegacyTreeProcess implements Comparable<LegacyTreeProcess> {
    private final List<LegacyTreeNode> inputs;

    public LegacyTreeProcess(List<LegacyTreeNode> inputs) {
        this.inputs = new ArrayList<>(inputs);
    }

    public List<LegacyTreeNode> inputs() {
        return inputs;
    }

    public void sort() {
        inputs.sort(Comparator.reverseOrder());
        for (LegacyTreeNode input : inputs) {
            for (LegacyTreeProcess process : input.inputs()) {
                process.sort();
            }
        }
    }

    @Override
    public int compareTo(LegacyTreeProcess other) {
        return Integer.compare(diveToDeep(this, 0, new LegacyTreeNode.DepthRecorder()),
                diveToDeep(other, 0, new LegacyTreeNode.DepthRecorder()));
    }

    public static int diveToDeep(LegacyTreeProcess process, int depth, LegacyTreeNode.DepthRecorder recorder) {
        for (LegacyTreeNode node : process.inputs) {
            for (LegacyTreeProcess subProcess : node.inputs()) {
                int newDepth = depth + 1;
                recorder.dive(newDepth);
                diveToDeep(subProcess, newDepth, recorder);
            }
        }
        return recorder.depth();
    }

    public int totalNodes() {
        int nodeCount = inputs.size();
        for (LegacyTreeNode node : inputs) {
            for (LegacyTreeProcess input : node.inputs()) {
                nodeCount += input.totalNodes();
            }
        }
        return nodeCount;
    }
}
