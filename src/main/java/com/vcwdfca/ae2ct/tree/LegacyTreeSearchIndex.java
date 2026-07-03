package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LegacyTreeSearchIndex {
    private final CopyOnWriteArrayList<LegacyTreeNode> indexed = new CopyOnWriteArrayList<>();

    public void buildSync(LegacyTreeData data) {
        indexed.clear();
        if (data != null) {
            indexed.addAll(data.allNodes());
        }
    }

    public List<LegacyTreeNode> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<LegacyTreeNode> results = new ArrayList<>();
        for (LegacyTreeNode node : indexed) {
            if (node.displayNameLower().contains(needle)) {
                results.add(node);
            }
        }
        return results;
    }
}
