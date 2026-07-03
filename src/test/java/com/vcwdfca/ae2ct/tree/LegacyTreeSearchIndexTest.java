package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyTreeSearchIndexTest {
    @Test
    void indexesLegacyNodesByLowercaseDisplayName() {
        LegacyTreeNode root = node("root item");
        LegacyTreeNode iron = node("Iron Ingot");
        root.addInput(new LegacyTreeProcess(List.of(iron)));
        LegacyTreeSearchIndex index = new LegacyTreeSearchIndex();

        index.buildAsync(new LegacyTreeData(root), ForkJoinPool.commonPool()).join();

        List<LegacyTreeNode> results = index.search("ing");
        assertFalse(index.isIndexing());
        assertEquals(1, results.size());
        assertEquals(iron, results.get(0));
    }

    @Test
    void emptySearchReturnsNoResults() {
        LegacyTreeSearchIndex index = new LegacyTreeSearchIndex();
        index.buildSync(new LegacyTreeData(node("root item")));

        assertEquals(0, index.search("").size());
        assertEquals(1, index.indexedCount());
        assertEquals(1, index.totalCount());
    }

    private static LegacyTreeNode node(String name) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(name), 1), List.of(), 0, Amounts.empty());
    }
}
