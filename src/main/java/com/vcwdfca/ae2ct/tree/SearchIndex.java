package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SearchIndex<K> {
    private final CopyOnWriteArrayList<GraphNode<K>> indexed = new CopyOnWriteArrayList<>();
    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private volatile int totalCount = 0;

    public CompletableFuture<Void> buildAsync(TreeData<K> data, Executor executor) {
        Objects.requireNonNull(executor, "executor");
        indexing.set(true);
        return CompletableFuture.runAsync(() -> buildSync(data), executor)
                .whenComplete((v, t) -> indexing.set(false));
    }

    public void buildSync(TreeData<K> data) {
        indexed.clear();
        totalCount = data.allNodes().size();
        for (GraphNode<K> node : data.allNodes()) {
            indexed.add(node);
        }
    }

    public List<GraphNode<K>> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<GraphNode<K>> results = new ArrayList<>();
        for (GraphNode<K> node : indexed) {
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
