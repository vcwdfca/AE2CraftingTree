package com.vcwdfca.ae2ct.tree;

import java.util.IdentityHashMap;
import java.util.Map;

public final class NodeCache<K> {
    private final Map<GraphNode<K>, DisplayNode<K>> cache = new IdentityHashMap<>();

    public DisplayNode<K> getOrCreate(GraphNode<K> data) {
        return cache.computeIfAbsent(data, DisplayNode::new);
    }

    public DisplayNode<K> get(GraphNode<K> data) {
        return cache.get(data);
    }

    public void clear() {
        cache.clear();
    }
}
