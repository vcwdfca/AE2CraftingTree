package com.vcwdfca.ae2ct.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TreeCache<K> {
    private final int maxEntries;
    private final Map<String, CachedTree<K>> cache;

    public TreeCache(int maxEntries) {
        this.maxEntries = maxEntries;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedTree<K>> eldest) {
                return size() > TreeCache.this.maxEntries;
            }
        };
    }

    public synchronized CachedTree<K> get(String key) {
        return cache.get(key);
    }

    public synchronized void put(String key, CachedTree<K> value) {
        cache.put(key, value);
    }

    public record CachedTree<K>(TreeData<K> data, NodeCache<K> nodeCache, SearchIndex<K> searchIndex) {
    }
}
