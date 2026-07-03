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

    @Test
    void linkSpanStopsBeforeLastChildSubtreeExpansion() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode left = node("left", 1, 0);
        LegacyTreeNode leftLeaf = node("left-leaf", 1, 0);
        LegacyTreeNode right = node("right", 1, 0);
        LegacyTreeNode deepA = node("deep-a", 1, 0);
        LegacyTreeNode deepB = node("deep-b", 1, 0);
        left.addInput(new LegacyTreeProcess(List.of(leftLeaf)));
        right.addInput(new LegacyTreeProcess(List.of(deepA, deepB)));
        root.addInput(new LegacyTreeProcess(List.of(left, right)));

        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        assertEquals(layout.entry(right).column(), layout.entry(root).linkEndColumn());
        assertTrue(layout.entry(deepB).column() > layout.entry(root).linkEndColumn());
    }

    @Test
    void renderableNodeCountIgnoresPlaceholders() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode a = node("a", 1, 0);
        LegacyTreeNode b = node("b", 1, 0);
        LegacyTreeNode c = node("c", 1, 0);
        a.addInput(new LegacyTreeProcess(List.of(c)));
        root.addInput(new LegacyTreeProcess(List.of(a, b)));

        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        assertEquals(4, layout.renderableNodeCount());
        assertTrue(layout.rows().stream().mapToInt(List::size).sum() > layout.renderableNodeCount());
    }

    @Test
    void entriesInRangeSkipsPlaceholdersAndOutOfRangeNodes() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode left = node("left", 1, 0);
        LegacyTreeNode right = node("right", 1, 0);
        LegacyTreeNode deep = node("deep", 1, 0);
        left.addInput(new LegacyTreeProcess(List.of(deep)));
        root.addInput(new LegacyTreeProcess(List.of(left, right)));

        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        assertEquals(List.of(root, left), layout.entriesInRange(0, 0, 0, 1).stream().map(LegacyTreeLayout.Entry::node).toList());
        assertEquals(List.of(right), layout.entriesInRange(1, 3, 1, 1).stream().map(LegacyTreeLayout.Entry::node).toList());
        assertTrue(layout.entriesInRange(2, 2, 0, 2).isEmpty());
    }

    @Test
    void entriesInRangeCanIncludeAnchorsOutsideViewportForImmediateSearchRendering() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode left = node("left", 1, 0);
        LegacyTreeNode right = node("right", 1, 0);
        LegacyTreeNode deep = node("deep", 1, 0);
        left.addInput(new LegacyTreeProcess(List.of(deep)));
        root.addInput(new LegacyTreeProcess(List.of(left, right)));

        LegacyTreeLayout layout = LegacyTreeLayout.build(root);

        List<LegacyTreeNode> nodes = layout.entriesInRange(0, 0, 0, 0, List.of(layout.entry(deep), layout.entry(root))).stream()
                .map(LegacyTreeLayout.Entry::node)
                .toList();

        assertEquals(List.of(root, deep), nodes);
    }

    private static LegacyTreeNode node(String key, long amount, long missing) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(key), amount), List.of(), missing,
                new Amounts(missing, 0, 0));
    }
}
