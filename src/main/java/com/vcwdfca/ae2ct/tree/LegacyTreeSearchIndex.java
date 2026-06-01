package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LegacyTreeSearchIndex {
    private final CopyOnWriteArrayList<LegacyTreeNode> indexed = new CopyOnWriteArrayList<>();
    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private volatile int totalCount;

    public CompletableFuture<Void> buildAsync(LegacyTreeData data, Executor executor) {
        Objects.requireNonNull(executor, "executor");
        indexing.set(true);
        return CompletableFuture.runAsync(() -> buildSync(data), executor)
                .whenComplete((value, throwable) -> indexing.set(false));
    }

    public void buildSync(LegacyTreeData data) {
        indexed.clear();
        totalCount = data == null ? 0 : data.allNodes().size();
        if (data == null) {
            return;
        }
        indexed.addAll(data.allNodes());
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

    public boolean isIndexing() {
        return indexing.get();
    }

    public int indexedCount() {
        return indexed.size();
    }

    public int totalCount() {
        return totalCount;
    }
}
