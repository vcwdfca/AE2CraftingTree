package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.*;

class LayoutEngineTest {
    @Test
    void layoutChildrenAssignsDistinctPositions() {
        GraphNode<String> rootData = new GraphNode<>("root", "root", 1, Amounts.empty());
        GraphNode<String> aData = new GraphNode<>("a", "a", 1, Amounts.empty());
        GraphNode<String> bData = new GraphNode<>("b", "b", 1, Amounts.empty());
        rootData.addChild(aData);
        rootData.addChild(bData);

        DisplayNode<String> root = new DisplayNode<>(rootData);
        DisplayNode<String> a = new DisplayNode<>(aData);
        DisplayNode<String> b = new DisplayNode<>(bData);
        root.setPoint(new Point(0, 0));
        root.addChild(a);
        root.addChild(b);

        LayoutEngine<String> engine = new LayoutEngine<>();
        engine.registerRoot(root);
        engine.layoutChildren(root, LayoutMode.COMPACT);

        assertNotNull(a.point());
        assertNotNull(b.point());
        assertEquals(1, a.point().y);
        assertEquals(1, b.point().y);
        assertNotEquals(a.point().x, b.point().x);
    }

    @Test
    void viewportContainsPoint() {
        Viewport viewport = new Viewport(0, 0, 2, 2);
        assertTrue(viewport.contains(new Point(1, 1)));
        assertFalse(viewport.contains(new Point(3, 3)));
    }
}
