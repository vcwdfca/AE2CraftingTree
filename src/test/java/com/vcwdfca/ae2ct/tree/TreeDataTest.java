package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeDataTest {
    @Test
    void filterMissingOnlyKeepsMissingBranch() {
        GraphNode<String> root = new GraphNode<>("root", "root", 1, new Amounts(0, 0, 0));
        GraphNode<String> childA = new GraphNode<>("a", "a", 1, new Amounts(0, 0, 0));
        GraphNode<String> childB = new GraphNode<>("b", "b", 1, new Amounts(5, 0, 0));
        root.addChild(childA);
        root.addChild(childB);

        TreeData<String> data = new TreeData<>(root, List.of(root, childA, childB));
        TreeData<String> filtered = data.filterMissingOnly();

        assertNotNull(filtered.root());
        assertEquals(2, filtered.allNodes().size());
        assertEquals("b", filtered.root().children().get(0).key());
    }

    @Test
    void filterMissingOnlyReturnsNullRootWhenNoMissing() {
        GraphNode<String> root = new GraphNode<>("root", "root", 1, new Amounts(0, 0, 0));
        GraphNode<String> child = new GraphNode<>("c", "c", 1, new Amounts(0, 0, 0));
        root.addChild(child);

        TreeData<String> data = new TreeData<>(root, List.of(root, child));
        TreeData<String> filtered = data.filterMissingOnly();

        assertNull(filtered.root());
        assertEquals(0, filtered.allNodes().size());
    }
}
