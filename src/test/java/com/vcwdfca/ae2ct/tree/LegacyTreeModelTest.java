package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTreeModelTest {
    @Test
    void missingOnlyKeepsAncestorsAndDropsCleanBranches() {
        LegacyTreeNode root = node("root", 10, 0);
        LegacyTreeNode clean = node("clean", 4, 0);
        LegacyTreeNode parent = node("parent", 3, 0);
        LegacyTreeNode missing = node("missing", 2, 2);

        parent.addInput(new LegacyTreeProcess(List.of(missing)));
        root.addInput(new LegacyTreeProcess(List.of(clean, parent)));

        LegacyTreeNode filtered = root.withMissingOnly();

        assertNotNull(filtered);
        assertEquals("root", filtered.output().what().toString());
        assertEquals(1, filtered.inputs().size());

        LegacyTreeNode filteredParent = filtered.inputs().get(0).inputs().get(0);
        assertEquals("parent", filteredParent.output().what().toString());
        assertEquals(1, filteredParent.inputs().size());
        assertEquals("missing", filteredParent.inputs().get(0).inputs().get(0).output().what().toString());
    }

    @Test
    void sortPutsDeeperProcessesFirst() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode shallow = node("shallow", 1, 0);
        LegacyTreeNode deep = node("deep", 1, 0);
        deep.addInput(new LegacyTreeProcess(List.of(node("leaf", 1, 0))));

        root.addInput(new LegacyTreeProcess(List.of(shallow)));
        root.addInput(new LegacyTreeProcess(List.of(deep)));

        root.sort();

        assertEquals("deep", root.inputs().get(0).inputs().get(0).output().what().toString());
        assertEquals("shallow", root.inputs().get(1).inputs().get(0).output().what().toString());
    }

    @Test
    void renderExpansionMatchesLegacyShape() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode a = node("a", 1, 0);
        LegacyTreeNode b = node("b", 1, 0);
        LegacyTreeNode c = node("c", 1, 0);
        a.addInput(new LegacyTreeProcess(List.of(c)));
        root.addInput(new LegacyTreeProcess(List.of(a, b)));

        assertEquals(1, root.getRenderExpandNodes());
        assertEquals(0, root.getLastNodeRenderExpandNodes());
    }

    @Test
    void dataFlattensTreeAndFiltersMissingOnly() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode clean = node("clean", 1, 0);
        LegacyTreeNode missing = node("missing", 1, 1);
        root.addInput(new LegacyTreeProcess(List.of(clean, missing)));

        LegacyTreeData data = new LegacyTreeData(root);
        LegacyTreeData filtered = data.filterMissingOnly();

        assertEquals(3, data.allNodes().size());
        assertTrue(data.containsNode(missing));
        assertEquals(2, filtered.allNodes().size());
        assertEquals("missing", filtered.root().inputs().get(0).inputs().get(0).output().what().toString());
    }

    @Test
    void missingOnlyKeepsCleanTreeVisibleWhenNothingIsMissing() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode clean = node("clean", 1, 0);
        root.addInput(new LegacyTreeProcess(List.of(clean)));

        LegacyTreeData filtered = new LegacyTreeData(root).filterMissingOnly();

        assertNotNull(filtered.root());
        assertEquals("root", filtered.root().output().what().toString());
        assertEquals(2, filtered.allNodes().size());
    }

    @Test
    void dataReportsWhetherAnyNodeIsMissing() {
        LegacyTreeNode cleanRoot = node("clean-root", 1, 0);
        cleanRoot.addInput(new LegacyTreeProcess(List.of(node("clean-child", 1, 0))));
        assertFalse(new LegacyTreeData(cleanRoot).hasMissing());

        LegacyTreeNode missingRoot = node("missing-root", 1, 0);
        missingRoot.addInput(new LegacyTreeProcess(List.of(node("missing-child", 1, 1))));
        assertTrue(new LegacyTreeData(missingRoot).hasMissing());
    }

    private static LegacyTreeNode node(String key, long amount, long missing) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(key), amount), List.of(), missing,
                new Amounts(missing, 0, 0));
    }
}
