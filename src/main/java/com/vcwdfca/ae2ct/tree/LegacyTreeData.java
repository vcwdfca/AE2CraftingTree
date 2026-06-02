package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LegacyTreeData {
    private final LegacyTreeNode root;
    private final List<LegacyTreeNode> allNodes = new ArrayList<>();
    private final Map<LegacyTreeNode, Integer> identityIndex = new IdentityHashMap<>();

    public LegacyTreeData(LegacyTreeNode root) {
        this.root = root;
        collect(root);
        for (int i = 0; i < allNodes.size(); i++) {
            identityIndex.put(allNodes.get(i), i);
        }
    }

    public LegacyTreeNode root() {
        return root;
    }

    public List<LegacyTreeNode> allNodes() {
        return Collections.unmodifiableList(allNodes);
    }

    public boolean containsNode(LegacyTreeNode node) {
        return identityIndex.containsKey(node);
    }

    public boolean containsKey(AEKey key) {
        for (LegacyTreeNode node : allNodes) {
            if (node.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public LegacyTreeData filterMissingOnly() {
        if (root == null || !LegacyTreeNode.isMissing(root)) {
            return this;
        }
        return new LegacyTreeData(root.withMissingOnly());
    }

    private void collect(LegacyTreeNode node) {
        if (node == null) {
            return;
        }
        allNodes.add(node);
        for (LegacyTreeProcess process : node.inputs()) {
            for (LegacyTreeNode child : process.inputs()) {
                collect(child);
            }
        }
    }
}
