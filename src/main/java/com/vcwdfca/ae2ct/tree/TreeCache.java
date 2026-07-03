package com.vcwdfca.ae2ct.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TreeCache {
    private final int maxEntries;
    private final Map<String, CachedTree> cache;

    public TreeCache(int maxEntries) {
        this.maxEntries = maxEntries;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedTree> eldest) {
                return size() > TreeCache.this.maxEntries;
            }
        };
    }

    public synchronized CachedTree get(String key) {
        return cache.get(key);
    }

    public synchronized void put(String key, CachedTree value) {
        cache.put(key, value);
    }

    public record CachedTree(LegacyTreeData data, LegacyTreeLayout layout, LegacyTreeSearchIndex searchIndex) {
    }
}
