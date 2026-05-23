package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class SearchIndexTest {
    private TreeData<String> sampleData() {
        GraphNode<String> root = new GraphNode<>("root", "root", 1, Amounts.empty());
        GraphNode<String> iron = new GraphNode<>("iron", "iron ingot", 1, Amounts.empty());
        GraphNode<String> gear = new GraphNode<>("gear", "gear", 1, Amounts.empty());
        root.addChild(iron);
        iron.addChild(gear);
        return new TreeData<>(root, List.of(root, iron, gear));
    }

    @Test
    void buildSyncAndSearchReturnsMatches() {
        SearchIndex<String> index = new SearchIndex<>();
        index.buildSync(sampleData());

        List<GraphNode<String>> results = index.search("ing");
        assertEquals(1, results.size());
        assertEquals("iron", results.get(0).key());
    }

    @Test
    void buildAsyncUpdatesIndexingState() {
        SearchIndex<String> index = new SearchIndex<>();
        CompletableFuture<Void> future = index.buildAsync(sampleData(), Runnable::run);
        future.join();

        assertFalse(index.isIndexing());
        assertEquals(3, index.indexedCount());
        assertEquals(3, index.totalCount());
    }
}
