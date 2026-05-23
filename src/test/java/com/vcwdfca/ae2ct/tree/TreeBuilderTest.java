package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeBuilderTest {
    private TreeData<String> sampleData() {
        GraphNode<String> root = new GraphNode<>("root", "root", 1, Amounts.empty());
        GraphNode<String> child = new GraphNode<>("child", "child", 1, Amounts.empty());
        GraphNode<String> grand = new GraphNode<>("grand", "grand", 1, Amounts.empty());
        root.addChild(child);
        child.addChild(grand);
        return new TreeData<>(root, List.of(root, child, grand));
    }

    @Test
    void ensureViewportExpandsVisibleNodes() {
        TreeData<String> data = sampleData();
        NodeCache<String> cache = new NodeCache<>();
        LayoutEngine<String> layout = new LayoutEngine<>();

        TreeBuilder<String> builder = new TreeBuilder<>(data, cache, layout, LayoutMode.COMPACT);
        builder.ensureViewport(new Viewport(0, 0, 2, 2));

        DisplayNode<String> rootDisplay = builder.root();
        assertTrue(rootDisplay.isExpanded());
        assertEquals(1, rootDisplay.children().size());
    }

    @Test
    void expandPathBuildsAncestors() {
        TreeData<String> data = sampleData();
        NodeCache<String> cache = new NodeCache<>();
        LayoutEngine<String> layout = new LayoutEngine<>();

        TreeBuilder<String> builder = new TreeBuilder<>(data, cache, layout, LayoutMode.COMPACT);
        GraphNode<String> target = data.allNodes().get(2);
        DisplayNode<String> display = builder.expandPath(target);

        assertNotNull(display.point());
        assertEquals("grand", display.data().key());
    }
}
