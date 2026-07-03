package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TreeCacheTest {
    @Test
    void evictsOldestEntry() {
        TreeCache cache = new TreeCache(2);
        cache.put("a", new TreeCache.CachedTree(null, null, null));
        cache.put("b", new TreeCache.CachedTree(null, null, null));
        cache.put("c", new TreeCache.CachedTree(null, null, null));

        assertNull(cache.get("a"));
        assertNotNull(cache.get("b"));
        assertNotNull(cache.get("c"));
    }
}
