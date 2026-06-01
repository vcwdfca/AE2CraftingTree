package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTreeLayoutTest {
    @Test
    void layoutUsesRowsAndPlaceholderExpansion() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode a = node("a", 1, 0);
        LegacyTreeNode b = node("b", 1, 0);
        LegacyTreeNode c = node("c", 1, 0);
        a.addInput(new LegacyTreeProcess(List.of(c)));
        root.addInput(new LegacyTreeProcess(List.of(a, b)));

        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        assertEquals(3, layout.rows().size());
        assertEquals(root, layout.entry(root).node());
        assertTrue(layout.rows().get(1).size() >= 2);
        assertTrue(layout.rows().get(2).size() >= 2);
    }

    @Test
    void horizontalNavigationSkipsNonMissingWhenRequested() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode clean = node("clean", 1, 0);
        LegacyTreeNode missing = node("missing", 1, 1);
        root.addInput(new LegacyTreeProcess(List.of(clean, missing)));
        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        LegacyTreeLayout.Entry cleanEntry = layout.entry(clean);
        LegacyTreeLayout.Entry nextMissing = layout.findRight(cleanEntry, true);

        assertNotNull(nextMissing);
        assertEquals(missing, nextMissing.node());
    }

    @Test
    void verticalNavigationFindsNearestRenderableColumn() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode a = node("a", 1, 0);
        LegacyTreeNode b = node("b", 1, 0);
        LegacyTreeNode c = node("c", 1, 0);
        a.addInput(new LegacyTreeProcess(List.of(c)));
        root.addInput(new LegacyTreeProcess(List.of(a, b)));
        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        assertEquals(root, layout.findUp(layout.entry(a)).node());
        assertEquals(c, layout.findDown(layout.entry(a)).node());
    }

    private static LegacyTreeNode node(String key, long amount, long missing) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(key), amount), List.of(), missing,
                new Amounts(missing, 0, 0));
    }
}
