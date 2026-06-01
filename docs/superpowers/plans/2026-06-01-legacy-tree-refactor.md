# Legacy Tree Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor AE2CraftingTree so current-version API integration remains intact while tree data, missing-only filtering, row layout, and navigation follow AE2CT-Legacy behavior.

**Architecture:** Add a Legacy-style lightweight tree model and row layout under `com.vcwdfca.ae2ct.tree`, migrate search/cache/screenshots/GUI to consume it, and keep the current `RecipeHelper` path as fallback. Real AE2 crafting tree extraction is isolated behind accessors and a converter so the GUI does not depend on AE2 internals.

**Tech Stack:** Java 17, ForgeGradle, Sponge Mixin, AE2 1.20.1 Forge APIs, JUnit 5.

---

## File Structure

- Create `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeNode.java`: Legacy-style produced/requested stack node with missing cache, sorting, missing-only copy, and render expansion counters.
- Create `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeProcess.java`: Legacy-style process branch with ordered input nodes and recursive depth sorting.
- Create `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeData.java`: Root plus flattened node list and utility lookups for search/cache.
- Create `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeLayout.java`: Row-layout builder, entries, positions, hit-test map, and row navigation helpers.
- Create `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeSearchIndex.java`: Search index over `LegacyTreeNode`.
- Modify `src/main/java/com/vcwdfca/ae2ct/tree/TreeCache.java`: Store `LegacyTreeData`, layout, and search index instead of old `TreeData/NodeCache/SearchIndex`.
- Modify `src/main/java/com/vcwdfca/ae2ct/tree/TreeDataBuilder.java`: Convert fallback `RecipeHelper` data into `LegacyTreeData`.
- Create `src/main/java/com/vcwdfca/ae2ct/api/LegacyTreePayload.java`: Optional serialized lightweight tree payload attached to plan summaries.
- Modify `src/main/java/com/vcwdfca/ae2ct/api/ICraftingPlanSummary.java`: Add getters/setters for the lightweight tree payload.
- Modify `src/main/java/com/vcwdfca/ae2ct/api/RecipeHelper.java`: Keep existing fallback recipe serialization; do not make it own tree semantics.
- Modify `src/main/java/com/vcwdfca/ae2ct/mixin/AE2CraftingPlanSummary.java`: Write/read the optional lightweight tree payload after `RecipeHelper`.
- Create `src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingPlan.java`, `AccessorCraftingTreeNode.java`, and `AccessorCraftingTreeProcess.java`: Access current AE2 internals when public methods do not expose tree/process data.
- Modify `src/main/resources/ae2ct.mixins.json`: Register accessor mixins.
- Modify `src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeWidget.java`: Render and interact with `LegacyTreeLayout`.
- Modify `src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeScreen.java`: Stop rebuilding after search text changes and let the widget update focus/search state.
- Modify `src/main/java/com/vcwdfca/ae2ct/api/ScreenshotHelper.java`: Draw screenshots from `LegacyTreeLayout`.
- Add tests under `src/test/java/com/vcwdfca/ae2ct/tree/`: model, layout, navigation, search, and fallback builder tests.

---

### Task 1: Legacy Tree Model

**Files:**
- Create: `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeNode.java`
- Create: `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeProcess.java`
- Create: `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeData.java`
- Test: `src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeModelTest.java`

- [ ] **Step 1: Write the failing model tests**

Create `LegacyTreeModelTest.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("root", filtered.output().what());
        assertEquals(1, filtered.inputs().size());
        assertEquals("parent", filtered.inputs().get(0).inputs().get(0).output().what());
        assertEquals("missing", filtered.inputs().get(0).inputs().get(0).inputs().get(0).inputs().get(0).output().what());
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

        assertEquals("deep", root.inputs().get(0).inputs().get(0).output().what());
        assertEquals("shallow", root.inputs().get(1).inputs().get(0).output().what());
    }

    @Test
    void renderExpansionMatchesLegacyShape() {
        LegacyTreeNode root = node("root", 1, 0);
        LegacyTreeNode a = node("a", 1, 0);
        LegacyTreeNode b = node("b", 1, 0);
        LegacyTreeNode c = node("c", 1, 0);
        a.addInput(new LegacyTreeProcess(List.of(c)));
        root.addInput(new LegacyTreeProcess(List.of(a, b)));

        assertEquals(2, root.getRenderExpandNodes());
        assertEquals(0, root.getLastNodeRenderExpandNodes());
    }

    private static LegacyTreeNode node(String key, long amount, long missing) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(key), amount), List.of(), missing, Amounts.empty());
    }
}
```

- [ ] **Step 2: Add a test-only AE key**

Create `src/test/java/com/vcwdfca/ae2ct/tree/TestKey.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.network.chat.Component;

import java.util.Objects;

final class TestKey extends AEKey {
    private final String id;

    TestKey(String id) {
        this.id = id;
    }

    @Override
    public AEKeyType getType() {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(id);
    }

    @Override
    public boolean matches(AEKey other) {
        return equals(other);
    }

    @Override
    protected int computeHashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TestKey other && Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return id;
    }
}
```

If this does not compile because current AE2's `AEKey` has additional abstract methods, inspect the compiler error and implement only those required methods in `TestKey`.

- [ ] **Step 3: Run the tests and verify they fail**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeModelTest
```

Expected: compile fails because `LegacyTreeNode`, `LegacyTreeProcess`, and `LegacyTreeData` do not exist.

- [ ] **Step 4: Implement the model classes**

Implement `LegacyTreeNode.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LegacyTreeNode implements Comparable<LegacyTreeNode> {
    private final LegacyTreeProcess parent;
    private final GenericStack output;
    private final List<LegacyTreeProcess> inputs;
    private final long missing;
    private final Amounts amounts;
    private boolean missingCached;
    private boolean missingCache;

    public LegacyTreeNode(LegacyTreeProcess parent, GenericStack output, List<LegacyTreeProcess> inputs,
                          long missing, Amounts amounts) {
        this.parent = parent;
        this.output = Objects.requireNonNull(output, "output");
        this.inputs = new ArrayList<>(Objects.requireNonNull(inputs, "inputs"));
        this.missing = Math.max(0, missing);
        this.amounts = amounts == null ? new Amounts(this.missing, 0, 0) : amounts;
    }

    public void addInput(LegacyTreeProcess process) {
        inputs.add(process.withParentNodes(this));
        clearMissingCache();
    }

    public LegacyTreeProcess parent() {
        return parent;
    }

    public GenericStack output() {
        return output;
    }

    public AEKey key() {
        return output.what();
    }

    public long amount() {
        return output.amount();
    }

    public List<LegacyTreeProcess> inputs() {
        return inputs;
    }

    public long missing() {
        return missing;
    }

    public Amounts amounts() {
        return amounts;
    }

    public String displayNameLower() {
        return key().getDisplayName().getString().toLowerCase(Locale.ROOT);
    }

    public void sort() {
        inputs.sort(Comparator.reverseOrder());
        for (LegacyTreeProcess input : inputs) {
            input.sort();
            for (LegacyTreeNode subNode : input.inputs()) {
                subNode.sort();
            }
        }
    }

    @Override
    public int compareTo(@Nonnull LegacyTreeNode other) {
        return Integer.compare(diveToDeep(this, 0, new DepthRecorder()), diveToDeep(other, 0, new DepthRecorder()));
    }

    public static int diveToDeep(LegacyTreeNode node, int depth, DepthRecorder recorder) {
        for (LegacyTreeProcess input : node.inputs) {
            for (LegacyTreeNode subNode : input.inputs()) {
                int newDepth = depth + 1;
                recorder.dive(newDepth);
                diveToDeep(subNode, newDepth, recorder);
            }
        }
        return recorder.depth();
    }

    public int totalProcessors() {
        int size = inputs.size();
        for (LegacyTreeProcess input : inputs) {
            for (LegacyTreeNode node : input.inputs()) {
                size += node.totalProcessors();
            }
        }
        return size;
    }

    public int getRenderExpandNodes() {
        int size = Math.max(inputs.size() - 1, 0);
        for (LegacyTreeProcess input : inputs) {
            size += Math.max(input.inputs().size() - 1, 0);
            for (LegacyTreeNode node : input.inputs()) {
                size += node.getRenderExpandNodes();
            }
        }
        return size;
    }

    public int getLastNodeRenderExpandNodes() {
        if (inputs.isEmpty()) {
            return 0;
        }
        LegacyTreeProcess process = inputs.get(inputs.size() - 1);
        if (process.inputs().isEmpty()) {
            return 0;
        }
        LegacyTreeNode node = process.inputs().get(process.inputs().size() - 1);
        return node.getRenderExpandNodes();
    }

    public LegacyTreeNode withMissingOnly() {
        if (!isMissing(this)) {
            return null;
        }

        List<LegacyTreeProcess> missingInputs = new ArrayList<>();
        LegacyTreeNode copy = new LegacyTreeNode(parent, output, List.of(), missing, amounts);
        for (LegacyTreeProcess input : inputs) {
            List<LegacyTreeNode> missingSubNodes = new ArrayList<>();
            for (LegacyTreeNode subNode : input.inputs()) {
                if (isMissing(subNode)) {
                    missingSubNodes.add(subNode.withMissingOnly());
                }
            }
            if (!missingSubNodes.isEmpty()) {
                missingInputs.add(new LegacyTreeProcess(missingSubNodes).withParentNodes(copy));
            }
        }
        copy.inputs.addAll(missingInputs);
        copy.missingCached = true;
        copy.missingCache = true;
        return copy;
    }

    public static boolean isMissing(LegacyTreeNode node) {
        if (node.missingCached) {
            return node.missingCache;
        }
        if (node.missing > 0 || node.amounts.hasMissing()) {
            node.missingCached = true;
            node.missingCache = true;
            return true;
        }
        for (LegacyTreeProcess input : node.inputs) {
            for (LegacyTreeNode subNode : input.inputs()) {
                if (isMissing(subNode)) {
                    node.missingCached = true;
                    node.missingCache = true;
                    return true;
                }
            }
        }
        node.missingCached = true;
        node.missingCache = false;
        return false;
    }

    private void clearMissingCache() {
        missingCached = false;
        missingCache = false;
    }

    public static final class DepthRecorder {
        private int depth;

        void dive(int depth) {
            this.depth = Math.max(this.depth, depth);
        }

        public int depth() {
            return depth;
        }
    }
}
```

Implement `LegacyTreeProcess.java`:

```java
package com.vcwdfca.ae2ct.tree;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LegacyTreeProcess implements Comparable<LegacyTreeProcess> {
    private final List<LegacyTreeNode> inputs;

    public LegacyTreeProcess(List<LegacyTreeNode> inputs) {
        this.inputs = new ArrayList<>(inputs);
    }

    LegacyTreeProcess withParentNodes(LegacyTreeNode parentNode) {
        List<LegacyTreeNode> rebound = new ArrayList<>(inputs.size());
        LegacyTreeProcess process = new LegacyTreeProcess(rebound);
        for (LegacyTreeNode input : inputs) {
            rebound.add(new LegacyTreeNode(process, input.output(), input.inputs(), input.missing(), input.amounts()));
        }
        return process;
    }

    public List<LegacyTreeNode> inputs() {
        return inputs;
    }

    public void sort() {
        inputs.sort(Comparator.reverseOrder());
        for (LegacyTreeNode input : inputs) {
            for (LegacyTreeProcess process : input.inputs()) {
                process.sort();
            }
        }
    }

    @Override
    public int compareTo(@Nonnull LegacyTreeProcess other) {
        return Integer.compare(diveToDeep(this, 0, new LegacyTreeNode.DepthRecorder()),
                diveToDeep(other, 0, new LegacyTreeNode.DepthRecorder()));
    }

    public static int diveToDeep(LegacyTreeProcess process, int depth, LegacyTreeNode.DepthRecorder recorder) {
        for (LegacyTreeNode node : process.inputs) {
            for (LegacyTreeProcess subProcess : node.inputs()) {
                int newDepth = depth + 1;
                recorder.dive(newDepth);
                diveToDeep(subProcess, newDepth, recorder);
            }
        }
        return recorder.depth();
    }

    public int totalNodes() {
        int nodeCount = inputs.size();
        for (LegacyTreeNode node : inputs) {
            for (LegacyTreeProcess input : node.inputs()) {
                nodeCount += input.totalNodes();
            }
        }
        return nodeCount;
    }
}
```

Implement `LegacyTreeData.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LegacyTreeData {
    private final LegacyTreeNode root;
    private final List<LegacyTreeNode> allNodes;
    private final Map<LegacyTreeNode, Integer> identityIndex = new IdentityHashMap<>();

    public LegacyTreeData(LegacyTreeNode root) {
        this.root = root;
        this.allNodes = new ArrayList<>();
        collect(root);
        for (int i = 0; i < allNodes.size(); i++) {
            identityIndex.put(allNodes.get(i), i);
        }
    }

    public LegacyTreeNode root() {
        return root;
    }

    public List<LegacyTreeNode> allNodes() {
        return Collections.unmodifiableList(allNodes);
    }

    public boolean containsNode(LegacyTreeNode node) {
        return identityIndex.containsKey(node);
    }

    public boolean containsKey(AEKey key) {
        for (LegacyTreeNode node : allNodes) {
            if (node.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public LegacyTreeData filterMissingOnly() {
        if (root == null) {
            return new LegacyTreeData(null);
        }
        return new LegacyTreeData(root.withMissingOnly());
    }

    private void collect(LegacyTreeNode node) {
        if (node == null) {
            return;
        }
        allNodes.add(node);
        for (LegacyTreeProcess process : node.inputs()) {
            for (LegacyTreeNode child : process.inputs()) {
                collect(child);
            }
        }
    }
}
```

- [ ] **Step 5: Run the model tests**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeModelTest
```

Expected: PASS after fixing any compile-only differences in `TestKey`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeNode.java src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeProcess.java src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeData.java src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeModelTest.java src/test/java/com/vcwdfca/ae2ct/tree/TestKey.java
git commit -m "feat: add legacy tree model"
```

---

### Task 2: Legacy Row Layout And Navigation

**Files:**
- Create: `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeLayout.java`
- Test: `src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeLayoutTest.java`

- [ ] **Step 1: Write failing row-layout tests**

Create `LegacyTreeLayoutTest.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        return new LegacyTreeNode(null, new GenericStack(new TestKey(key), amount), List.of(), missing, new Amounts(missing, 0, 0));
    }
}
```

- [ ] **Step 2: Run row-layout tests and verify failure**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeLayoutTest
```

Expected: compile fails because `LegacyTreeLayout` does not exist.

- [ ] **Step 3: Implement row layout**

Implement `LegacyTreeLayout.java`:

```java
package com.vcwdfca.ae2ct.tree;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LegacyTreeLayout {
    private final List<List<Entry>> rows = new ArrayList<>();
    private final Map<LegacyTreeNode, Entry> entries = new IdentityHashMap<>();

    public static LegacyTreeLayout build(LegacyTreeNode root) {
        LegacyTreeLayout layout = new LegacyTreeLayout();
        if (root != null) {
            layout.recursiveAddNode(root, 0, null);
            layout.linkRows();
        }
        return layout;
    }

    public List<List<Entry>> rows() {
        return Collections.unmodifiableList(rows);
    }

    public Entry entry(LegacyTreeNode node) {
        return entries.get(node);
    }

    public Entry atGrid(int column, int row) {
        if (row < 0 || row >= rows.size()) {
            return null;
        }
        List<Entry> entries = rows.get(row);
        if (column < 0 || column >= entries.size()) {
            return null;
        }
        Entry entry = entries.get(column);
        return entry.placeholder() ? null : entry;
    }

    public Entry findLeft(Entry entry, boolean missingOnly) {
        Entry cursor = entry == null ? null : entry.previous();
        while (cursor != null) {
            if (!missingOnly || LegacyTreeNode.isMissing(cursor.node())) {
                return cursor;
            }
            cursor = cursor.previous();
        }
        return null;
    }

    public Entry findRight(Entry entry, boolean missingOnly) {
        Entry cursor = entry == null ? null : entry.next();
        while (cursor != null) {
            if (!missingOnly || LegacyTreeNode.isMissing(cursor.node())) {
                return cursor;
            }
            cursor = cursor.next();
        }
        return null;
    }

    public Entry findUp(Entry entry) {
        if (entry == null) {
            return null;
        }
        return findVertical(entry, -1);
    }

    public Entry findDown(Entry entry) {
        if (entry == null) {
            return null;
        }
        return findVertical(entry, 1);
    }

    private Entry findVertical(Entry entry, int deltaRow) {
        int row = entry.row() + deltaRow;
        if (row < 0 || row >= rows.size()) {
            return null;
        }
        List<Entry> targetRow = rows.get(row);
        for (int column = Math.min(entry.column(), targetRow.size() - 1); column >= 0; column--) {
            Entry candidate = targetRow.get(column);
            if (!candidate.placeholder()) {
                return candidate;
            }
        }
        return null;
    }

    private void recursiveAddNode(LegacyTreeNode node, int depth, Entry parent) {
        Entry nodeEntry = addNode(node, depth, parent);
        for (LegacyTreeProcess process : node.inputs()) {
            for (LegacyTreeNode input : process.inputs()) {
                recursiveAddNode(input, depth + 1, nodeEntry);
            }
        }
        int extraRenderNodes = node.getRenderExpandNodes();
        fillEmpty(depth, extraRenderNodes);
        if (extraRenderNodes == 0 && node.inputs().size() > 1) {
            extraRenderNodes += node.inputs().size() - 1;
        }
        nodeEntry.setLinkedSubNodes(extraRenderNodes - node.getLastNodeRenderExpandNodes());
    }

    private Entry addNode(LegacyTreeNode node, int depth, Entry parent) {
        while (rows.size() <= depth) {
            rows.add(new ArrayList<>());
        }
        List<Entry> row = rows.get(depth);
        Entry entry = Entry.node(node, row.size(), depth, parent);
        row.add(entry);
        entries.put(node, entry);
        return entry;
    }

    private void fillEmpty(int depth, int fillNodes) {
        if (fillNodes <= 0 || depth >= rows.size()) {
            return;
        }
        int totalWidth = rows.get(depth).size() + fillNodes;
        for (int i = depth; i < rows.size(); i++) {
            List<Entry> row = rows.get(i);
            while (row.size() < totalWidth) {
                row.add(Entry.placeholder(row.size(), i));
            }
        }
    }

    private void linkRows() {
        for (List<Entry> row : rows) {
            Entry previous = null;
            for (Entry entry : row) {
                if (entry.placeholder()) {
                    continue;
                }
                entry.setPrevious(previous);
                if (previous != null) {
                    previous.setNext(entry);
                }
                previous = entry;
            }
        }
    }

    public static final class Entry {
        private final LegacyTreeNode node;
        private final int column;
        private final int row;
        private final Entry parent;
        private Entry previous;
        private Entry next;
        private int linkedSubNodes;

        private Entry(LegacyTreeNode node, int column, int row, Entry parent) {
            this.node = node;
            this.column = column;
            this.row = row;
            this.parent = parent;
        }

        static Entry node(LegacyTreeNode node, int column, int row, Entry parent) {
            return new Entry(node, column, row, parent);
        }

        static Entry placeholder(int column, int row) {
            return new Entry(null, column, row, null);
        }

        public boolean placeholder() {
            return node == null;
        }

        public LegacyTreeNode node() {
            return node;
        }

        public int column() {
            return column;
        }

        public int row() {
            return row;
        }

        public Entry parent() {
            return parent;
        }

        public Point point() {
            return new Point(column, row);
        }

        public Entry previous() {
            return previous;
        }

        public Entry next() {
            return next;
        }

        public int linkedSubNodes() {
            return linkedSubNodes;
        }

        void setPrevious(Entry previous) {
            this.previous = previous;
        }

        void setNext(Entry next) {
            this.next = next;
        }

        void setLinkedSubNodes(int linkedSubNodes) {
            this.linkedSubNodes = Math.max(0, linkedSubNodes);
        }
    }
}
```

- [ ] **Step 4: Run row-layout tests**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeLayoutTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeLayout.java src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeLayoutTest.java
git commit -m "feat: add legacy row layout"
```

---

### Task 3: Search And Cache Migration

**Files:**
- Create: `src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeSearchIndex.java`
- Modify: `src/main/java/com/vcwdfca/ae2ct/tree/TreeCache.java`
- Test: `src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeSearchIndexTest.java`
- Test: `src/test/java/com/vcwdfca/ae2ct/tree/TreeCacheTest.java`

- [ ] **Step 1: Write failing search test**

Create `LegacyTreeSearchIndexTest.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

class LegacyTreeSearchIndexTest {
    @Test
    void indexesLegacyNodesByLowercaseDisplayName() {
        LegacyTreeNode root = node("root item");
        LegacyTreeNode iron = node("Iron Ingot");
        root.addInput(new LegacyTreeProcess(List.of(iron)));
        LegacyTreeSearchIndex index = new LegacyTreeSearchIndex();

        index.buildAsync(new LegacyTreeData(root), ForkJoinPool.commonPool());
        while (index.isIndexing()) {
            Thread.onSpinWait();
        }

        List<LegacyTreeNode> results = index.search("ing");
        assertEquals(1, results.size());
        assertEquals(iron, results.get(0));
    }

    private static LegacyTreeNode node(String name) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(name), 1), List.of(), 0, Amounts.empty());
    }
}
```

- [ ] **Step 2: Update cache test expectations**

Modify `TreeCacheTest.java` so its cached value uses Legacy types:

```java
package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TreeCacheTest {
    @Test
    void evictsOldEntries() {
        TreeCache cache = new TreeCache(2);
        cache.put("a", new TreeCache.CachedTree(null, null, null));
        cache.put("b", new TreeCache.CachedTree(null, null, null));
        cache.put("c", new TreeCache.CachedTree(null, null, null));

        assertNull(cache.get("a"));
        assertNotNull(cache.get("b"));
        assertNotNull(cache.get("c"));
    }
}
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeSearchIndexTest --tests com.vcwdfca.ae2ct.tree.TreeCacheTest
```

Expected: compile fails because `LegacyTreeSearchIndex` does not exist and `TreeCache.CachedTree` still has old generic fields.

- [ ] **Step 4: Implement search index**

Create `LegacyTreeSearchIndex.java`:

```java
package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LegacyTreeSearchIndex {
    private final CopyOnWriteArrayList<LegacyTreeNode> indexed = new CopyOnWriteArrayList<>();
    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private volatile int totalCount;
    private volatile int indexedCount;

    public void buildAsync(LegacyTreeData data, Executor executor) {
        indexed.clear();
        indexedCount = 0;
        totalCount = data == null ? 0 : data.allNodes().size();
        indexing.set(true);
        executor.execute(() -> {
            try {
                if (data != null) {
                    for (LegacyTreeNode node : data.allNodes()) {
                        indexed.add(node);
                        indexedCount++;
                    }
                }
            } finally {
                indexing.set(false);
            }
        });
    }

    public List<LegacyTreeNode> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<LegacyTreeNode> results = new ArrayList<>();
        for (LegacyTreeNode node : indexed) {
            if (node.displayNameLower().contains(needle)) {
                results.add(node);
            }
        }
        return results;
    }

    public boolean isIndexing() {
        return indexing.get();
    }

    public int totalCount() {
        return totalCount;
    }

    public int indexedCount() {
        return indexedCount;
    }
}
```

- [ ] **Step 5: Modify cache**

Change `TreeCache.java` to:

```java
package com.vcwdfca.ae2ct.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TreeCache {
    private final int maxEntries;
    private final Map<String, CachedTree> entries;

    public TreeCache(int maxEntries) {
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedTree> eldest) {
                return size() > TreeCache.this.maxEntries;
            }
        };
    }

    public synchronized CachedTree get(String key) {
        return entries.get(key);
    }

    public synchronized void put(String key, CachedTree tree) {
        entries.put(key, tree);
    }

    public record CachedTree(LegacyTreeData data, LegacyTreeLayout layout, LegacyTreeSearchIndex searchIndex) {
    }
}
```

- [ ] **Step 6: Run search/cache tests**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeSearchIndexTest --tests com.vcwdfca.ae2ct.tree.TreeCacheTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/tree/LegacyTreeSearchIndex.java src/main/java/com/vcwdfca/ae2ct/tree/TreeCache.java src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeSearchIndexTest.java src/test/java/com/vcwdfca/ae2ct/tree/TreeCacheTest.java
git commit -m "feat: migrate tree search and cache"
```

---

### Task 4: Fallback RecipeHelper Builder

**Files:**
- Modify: `src/main/java/com/vcwdfca/ae2ct/tree/TreeDataBuilder.java`
- Test: `src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeDataBuilderTest.java`

- [ ] **Step 1: Write fallback builder test**

Create `LegacyTreeDataBuilderTest.java`:

```java
package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.GenericStack;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyTreeDataBuilderTest {
    @Test
    void fallbackBuilderCreatesLegacyTreeFromRecipeHelper() {
        GenericStack output = stack("gear", 4);
        RecipeHelper.Recipe gearRecipe = new RecipeHelper.Recipe(
                List.of(stack("iron", 16)),
                List.of(stack("gear", 2)),
                2L
        );
        RecipeHelper helper = new RecipeHelper(output, List.of(gearRecipe));

        LegacyTreeData data = new TreeDataBuilder().buildFallback(helper, List.of());

        assertNotNull(data.root());
        assertEquals("gear", data.root().output().what().toString());
        assertEquals(4, data.root().amount());
        assertEquals(1, data.root().inputs().size());
        assertEquals("iron", data.root().inputs().get(0).inputs().get(0).output().what().toString());
        assertEquals(32, data.root().inputs().get(0).inputs().get(0).amount());
    }

    private static GenericStack stack(String key, long amount) {
        return new GenericStack(new TestKey(key), amount);
    }
}
```

If `CraftingPlanSummaryEntry` is difficult to construct in tests, keep the test with `List.of()` and verify amount propagation only. Add a second test for amounts after a factory helper for summary entries exists.

- [ ] **Step 2: Run fallback builder test and verify failure**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeDataBuilderTest
```

Expected: compile fails because `TreeDataBuilder.buildFallback` does not exist or returns old `TreeData`.

- [ ] **Step 3: Implement fallback builder**

Replace `TreeDataBuilder` public surface with `LegacyTreeData` methods while keeping private amount tracking:

```java
public LegacyTreeData buildFallback(RecipeHelper helper, List<CraftingPlanSummaryEntry> entries) {
    if (helper == null || helper.output == null) {
        return new LegacyTreeData(null);
    }
    Map<AEKey, RecipeHelper.Recipe> recipeByOutput = new HashMap<>();
    for (RecipeHelper.Recipe recipe : helper.recipes) {
        for (GenericStack output : recipe.outputs()) {
            recipeByOutput.putIfAbsent(output.what(), recipe);
        }
    }
    Map<AEKey, AmountTracker> amountMap = buildAmountMap(entries);
    LegacyTreeNode root = buildFallbackNode(helper.output, helper.output.amount(), amountMap, recipeByOutput);
    root.sort();
    return new LegacyTreeData(root);
}
```

Use this child amount rule in `buildFallbackNode`:

```java
long outputPerPattern = outputAmountFor(recipe, stack.what());
long times = outputPerPattern <= 0 ? 0 : ceilDiv(amount, outputPerPattern);
long childAmount = input.amount() * times;
```

Create `Amounts` with missing/stored/craft from `CraftingPlanSummaryEntry` when present; otherwise use `new Amounts(missing, 0, 0)`.

- [ ] **Step 4: Run fallback builder test**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.tree.LegacyTreeDataBuilderTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/tree/TreeDataBuilder.java src/test/java/com/vcwdfca/ae2ct/tree/LegacyTreeDataBuilderTest.java
git commit -m "feat: build fallback legacy tree data"
```

---

### Task 5: Real Tree Payload And Plan Summary Serialization

**Files:**
- Create: `src/main/java/com/vcwdfca/ae2ct/api/LegacyTreePayload.java`
- Modify: `src/main/java/com/vcwdfca/ae2ct/api/ICraftingPlanSummary.java`
- Modify: `src/main/java/com/vcwdfca/ae2ct/mixin/AE2CraftingPlanSummary.java`
- Test: `src/test/java/com/vcwdfca/ae2ct/api/LegacyTreePayloadTest.java`

- [ ] **Step 1: Write payload serialization test**

Create `LegacyTreePayloadTest.java`:

```java
package com.vcwdfca.ae2ct.api;

import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.tree.Amounts;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
import com.vcwdfca.ae2ct.tree.TestKey;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyTreePayloadTest {
    @Test
    void roundTripsLegacyTreeData() {
        LegacyTreeNode root = node("root", 5, 0);
        root.addInput(new LegacyTreeProcess(List.of(node("missing", 2, 2))));

        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        LegacyTreePayload.write(buffer, new LegacyTreeData(root));
        LegacyTreeData read = LegacyTreePayload.read(buffer);

        assertNotNull(read.root());
        assertEquals("root", read.root().output().what().toString());
        assertEquals(5, read.root().amount());
        assertEquals("missing", read.root().inputs().get(0).inputs().get(0).output().what().toString());
        assertEquals(2, read.root().inputs().get(0).inputs().get(0).missing());
    }

    private static LegacyTreeNode node(String key, long amount, long missing) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(key), amount), List.of(), missing, new Amounts(missing, 0, 0));
    }
}
```

- [ ] **Step 2: Run payload test and verify failure**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.api.LegacyTreePayloadTest
```

Expected: compile fails because `LegacyTreePayload` does not exist.

- [ ] **Step 3: Implement payload serialization**

Create `LegacyTreePayload.java`:

```java
package com.vcwdfca.ae2ct.api;

import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.tree.Amounts;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class LegacyTreePayload {
    private LegacyTreePayload() {
    }

    public static void writeNullable(FriendlyByteBuf buffer, LegacyTreeData data) {
        buffer.writeBoolean(data != null && data.root() != null);
        if (data != null && data.root() != null) {
            write(buffer, data);
        }
    }

    public static LegacyTreeData readNullable(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }
        return read(buffer);
    }

    public static void write(FriendlyByteBuf buffer, LegacyTreeData data) {
        writeNode(buffer, data.root());
    }

    public static LegacyTreeData read(FriendlyByteBuf buffer) {
        return new LegacyTreeData(readNode(buffer, null));
    }

    private static void writeNode(FriendlyByteBuf buffer, LegacyTreeNode node) {
        GenericStack.writeBuffer(node.output(), buffer);
        buffer.writeVarLong(node.missing());
        buffer.writeVarLong(node.amounts().missing());
        buffer.writeVarLong(node.amounts().stored());
        buffer.writeVarLong(node.amounts().craft());
        buffer.writeVarInt(node.inputs().size());
        for (LegacyTreeProcess process : node.inputs()) {
            writeProcess(buffer, process);
        }
    }

    private static LegacyTreeNode readNode(FriendlyByteBuf buffer, LegacyTreeProcess parent) {
        GenericStack output = GenericStack.readBuffer(buffer);
        long missing = buffer.readVarLong();
        Amounts amounts = new Amounts(buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong());
        int processCount = buffer.readVarInt();
        LegacyTreeNode node = new LegacyTreeNode(parent, output, List.of(), missing, amounts);
        for (int i = 0; i < processCount; i++) {
            node.addInput(readProcess(buffer, node));
        }
        return node;
    }

    private static void writeProcess(FriendlyByteBuf buffer, LegacyTreeProcess process) {
        buffer.writeVarInt(process.inputs().size());
        for (LegacyTreeNode input : process.inputs()) {
            writeNode(buffer, input);
        }
    }

    private static LegacyTreeProcess readProcess(FriendlyByteBuf buffer, LegacyTreeNode parentNode) {
        int nodeCount = buffer.readVarInt();
        List<LegacyTreeNode> nodes = new ArrayList<>(nodeCount);
        LegacyTreeProcess process = new LegacyTreeProcess(nodes);
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(readNode(buffer, process));
        }
        return process;
    }
}
```

- [ ] **Step 4: Extend summary interface and mixin**

Change `ICraftingPlanSummary.java`:

```java
package com.vcwdfca.ae2ct.api;

import com.vcwdfca.ae2ct.tree.LegacyTreeData;

public interface ICraftingPlanSummary {
    RecipeHelper getJob();
    void setJob(RecipeHelper job);
    LegacyTreeData getLegacyTree();
    void setLegacyTree(LegacyTreeData tree);
}
```

Update `AE2CraftingPlanSummary`:

```java
@Unique
private LegacyTreeData legacyTree;

@Inject(at = @At("TAIL"), method = "fromJob", cancellable = true, remap = false)
private static void buildEX(IGrid grid, IActionSource actionSource, ICraftingPlan job, CallbackInfoReturnable<CraftingPlanSummary> cir) {
    var r = cir.getReturnValue();
    CraftingPlan plan = (CraftingPlan) job;
    ((ICraftingPlanSummary) r).setJob(RecipeHelper.fromCraftingPlan(plan));
    ((ICraftingPlanSummary) r).setLegacyTree(TreeDataBuilder.fromCraftingPlan(plan));
    cir.setReturnValue(r);
}

@Inject(at = @At("TAIL"), method = "write", remap = false)
private void write(FriendlyByteBuf buffer, CallbackInfo ci) {
    jobs.write(buffer);
    LegacyTreePayload.writeNullable(buffer, legacyTree);
}

@Inject(at = @At("TAIL"), method = "read", cancellable = true, remap = false)
private static void read(FriendlyByteBuf buffer, CallbackInfoReturnable<CraftingPlanSummary> cir) {
    var r = cir.getReturnValue();
    ((ICraftingPlanSummary) r).setJob(RecipeHelper.read(buffer));
    ((ICraftingPlanSummary) r).setLegacyTree(LegacyTreePayload.readNullable(buffer));
    cir.setReturnValue(r);
}
```

Add the missing imports when editing:

```java
import com.vcwdfca.ae2ct.api.LegacyTreePayload;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.TreeDataBuilder;
```

- [ ] **Step 5: Run payload test**

Run:

```bash
./gradlew.bat test --tests com.vcwdfca.ae2ct.api.LegacyTreePayloadTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/api/LegacyTreePayload.java src/main/java/com/vcwdfca/ae2ct/api/ICraftingPlanSummary.java src/main/java/com/vcwdfca/ae2ct/mixin/AE2CraftingPlanSummary.java src/test/java/com/vcwdfca/ae2ct/api/LegacyTreePayloadTest.java
git commit -m "feat: serialize legacy tree payload"
```

---

### Task 6: Real AE2 Tree Conversion

**Files:**
- Create: `src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingPlan.java`
- Create: `src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingTreeNode.java`
- Create: `src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingTreeProcess.java`
- Modify: `src/main/resources/ae2ct.mixins.json`
- Modify: `src/main/java/com/vcwdfca/ae2ct/tree/TreeDataBuilder.java`

- [ ] **Step 1: Inspect current AE2 class names before coding**

Run:

```bash
./gradlew.bat compileJava
```

If compilation fails because accessor targets do not exist after later edits, use the compiler error to identify the exact AE2 package/class names in this dependency. The expected Supergiant-like names are:

```java
appeng.crafting.CraftingPlan
appeng.crafting.CraftingTreeNode
appeng.crafting.CraftingTreeProcess
```

- [ ] **Step 2: Add accessor mixins**

Create `AccessorCraftingPlan.java`:

```java
package com.vcwdfca.ae2ct.mixin;

import appeng.crafting.CraftingTreeNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.crafting.CraftingPlan", remap = false)
public interface AccessorCraftingPlan {
    @Accessor("tree")
    CraftingTreeNode ae2ct$getTree();
}
```

Create `AccessorCraftingTreeNode.java`:

```java
package com.vcwdfca.ae2ct.mixin;

import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingTreeProcess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = appeng.crafting.CraftingTreeNode.class, remap = false)
public interface AccessorCraftingTreeNode {
    @Accessor("what")
    AEKey ae2ct$getWhat();

    @Accessor("amount")
    long ae2ct$getAmount();

    @Accessor("nodes")
    List<CraftingTreeProcess> ae2ct$getNodes();
}
```

Create `AccessorCraftingTreeProcess.java`:

```java
package com.vcwdfca.ae2ct.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingTreeNode;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = appeng.crafting.CraftingTreeProcess.class, remap = false)
public interface AccessorCraftingTreeProcess {
    @Accessor("details")
    IPatternDetails ae2ct$getDetails();

    @Accessor("nodes")
    Object2LongLinkedOpenHashMap<CraftingTreeNode> ae2ct$getNodes();
}
```

If current AE2 exposes public getters matching Supergiant, prefer those methods in `TreeDataBuilder` and keep accessors only for fields that are not public.

- [ ] **Step 3: Register accessors**

Modify `ae2ct.mixins.json`:

```json
"mixins": [
  "Ae2CraftConfirmScreen",
  "AE2CraftingPlanSummary",
  "AccessorCraftingPlan",
  "AccessorCraftingTreeNode",
  "AccessorCraftingTreeProcess"
]
```

- [ ] **Step 4: Add converter methods**

Add methods to `TreeDataBuilder`:

```java
public static LegacyTreeData fromCraftingPlan(CraftingPlan plan) {
    try {
        CraftingTreeNode root = ((AccessorCraftingPlan) (Object) plan).ae2ct$getTree();
        if (root == null) {
            return null;
        }
        LegacyTreeNode node = fromCraftingNode(root, null, plan.finalOutput().amount(), plan.missingItems());
        node.sort();
        return new LegacyTreeData(node);
    } catch (Throwable ignored) {
        return null;
    }
}

private static LegacyTreeNode fromCraftingNode(CraftingTreeNode node, LegacyTreeProcess parent,
                                               long amount, KeyCounter missingItems) {
    AccessorCraftingTreeNode nodeAccessor = (AccessorCraftingTreeNode) node;
    GenericStack output = new GenericStack(nodeAccessor.ae2ct$getWhat(), amount);
    long missing = missingItems == null ? 0 : missingItems.get(output.what());
    LegacyTreeNode converted = new LegacyTreeNode(parent, output, List.of(), missing, new Amounts(missing, 0, 0));
    List<CraftingTreeProcess> processes = nodeAccessor.ae2ct$getNodes();
    if (processes != null) {
        for (CraftingTreeProcess process : processes) {
            LegacyTreeProcess convertedProcess = fromCraftingProcess(process, node, amount);
            if (convertedProcess != null && !convertedProcess.inputs().isEmpty()) {
                converted.addInput(convertedProcess);
            }
        }
    }
    return converted;
}
```

Add `fromCraftingProcess` using Supergiant's process-times rule:

```java
private static LegacyTreeProcess fromCraftingProcess(CraftingTreeProcess process, CraftingTreeNode parentNode,
                                                     long parentAmount) {
    AccessorCraftingTreeProcess processAccessor = (AccessorCraftingTreeProcess) process;
    AccessorCraftingTreeNode parentAccessor = (AccessorCraftingTreeNode) parentNode;
    long processTimes = processTimes(processAccessor.ae2ct$getDetails().getOutputs(), parentAccessor.ae2ct$getWhat(), parentAmount);
    List<LegacyTreeNode> inputs = new ArrayList<>();
    LegacyTreeProcess converted = new LegacyTreeProcess(inputs);
    for (Object2LongMap.Entry<CraftingTreeNode> entry : processAccessor.ae2ct$getNodes().object2LongEntrySet()) {
        AccessorCraftingTreeNode childAccessor = (AccessorCraftingTreeNode) entry.getKey();
        long childAmount = childAccessor.ae2ct$getAmount() * entry.getLongValue() * processTimes;
        inputs.add(fromCraftingNode(entry.getKey(), converted, childAmount, null));
    }
    return inputs.isEmpty() ? null : converted;
}
```

Use `processTimes = 1` when no matching output exists, matching Supergiant's fallback behavior.

- [ ] **Step 5: Compile**

Run:

```bash
./gradlew.bat compileJava
```

Expected: PASS. If field names differ in the current AE2 dependency, adjust accessor names to the compiler-confirmed names.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingPlan.java src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingTreeNode.java src/main/java/com/vcwdfca/ae2ct/mixin/AccessorCraftingTreeProcess.java src/main/resources/ae2ct.mixins.json src/main/java/com/vcwdfca/ae2ct/tree/TreeDataBuilder.java
git commit -m "feat: convert real ae2 crafting tree"
```

---

### Task 7: GUI Migration To Legacy Layout

**Files:**
- Modify: `src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeWidget.java`
- Modify: `src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeScreen.java`

- [ ] **Step 1: Replace widget fields**

In `CraftingTreeWidget`, replace old fields:

```java
private static final TreeCache CACHE = new TreeCache(3);
private final TreeDataBuilder dataBuilder = new TreeDataBuilder();
private CompletableFuture<LegacyTreeData> dataFuture;
private LegacyTreeData baseData;
private LegacyTreeData activeData;
private LegacyTreeLayout layout;
private LegacyTreeSearchIndex searchIndex;
private LegacyTreeLayout.Entry currentMatchEntry;
private List<LegacyTreeNode> searchResults = List.of();
private Set<LegacyTreeNode> searchResultSet = Set.of();
private LegacyTreeLayout.Entry selectedEntry;
```

Change the constructor to accept the optional real tree:

```java
public CraftingTreeWidget(AEBaseScreen<?> screen, RecipeHelper data, LegacyTreeData realTree,
                          List<CraftingPlanSummaryEntry> entries, boolean isMissingOnly) {
    this.screen = screen;
    this.data = data;
    this.isMissingOnly = isMissingOnly;
    this.planKey = PlanKey.fromRecipeHelper(data);
    TreeCache.CachedTree cached = CACHE.get(planKey);
    if (cached != null && cached.data() != null) {
        baseData = cached.data();
        initializeActiveData();
    } else if (realTree != null && realTree.root() != null) {
        baseData = realTree;
        initializeActiveData();
        CACHE.put(planKey, new TreeCache.CachedTree(baseData, layout, searchIndex));
    } else {
        dataFuture = CompletableFuture.supplyAsync(() -> dataBuilder.buildFallback(data, entries));
    }
}
```

- [ ] **Step 2: Update screen constructor call**

In `CraftingTreeScreen`, change widget construction:

```java
var summary = (ICraftingPlanSummary) parent.getMenu().getPlan();
craftingTreeWidget = new CraftingTreeWidget(this, summary.getJob(), summary.getLegacyTree(),
        parent.getMenu().getPlan().getEntries(), Config.SHOW_MISSING_ONLY_BY_DEFAULT.get());
```

In `setSearchText`, remove `craftingTreeWidget.reBuild();` so search does not reset layout after every keypress:

```java
private void setSearchText(String text) {
    craftingTreeWidget.setSearchString(text);
}
```

- [ ] **Step 3: Replace initialize flow**

In `CraftingTreeWidget.initializeActiveData()`:

```java
activeData = isMissingOnly ? baseData.filterMissingOnly() : baseData;
layout = activeData.root() == null ? null : LegacyTreeLayout.build(activeData.root());
searchIndex = new LegacyTreeSearchIndex();
searchIndex.buildAsync(activeData, ForkJoinPool.commonPool());
selectedEntry = layout == null ? null : layout.entry(activeData.root());
currentMatchEntry = null;
currentMatchIdx = 0;
searchResults = List.of();
searchResultSet = Set.of();
```

- [ ] **Step 4: Replace rendering loops**

Replace `drawNode(GuiGraphics, DisplayNode<AEKey>)` with:

```java
private void drawNode(GuiGraphics guiGraphics, LegacyTreeLayout.Entry entry) {
    LegacyTreeNode dataNode = entry.node();
    GenericStack stack = dataNode.output();
    int color = FastColor.ARGB32.color(255, 0, 0, 0);
    int x = entry.column() * spacingX + outputX;
    int y = entry.row() * spacingY + outputY;

    if (x * scroll > screen.getGuiLeft() + screen.width + 10 || y * scroll > screen.getGuiTop() + screen.height + 10) {
        return;
    }

    for (LegacyTreeProcess process : dataNode.inputs()) {
        for (LegacyTreeNode child : process.inputs()) {
            LegacyTreeLayout.Entry childEntry = layout.entry(child);
            if (childEntry == null) {
                continue;
            }
            int childX = childEntry.column() * spacingX + outputX;
            int childY = childEntry.row() * spacingY + outputY;
            guiGraphics.vLine(childX + stackLength, y + stackLength + spacingY / 2, childY + stackLength, color);
        }
    }
    if (!dataNode.inputs().isEmpty()) {
        guiGraphics.vLine(x + stackLength, y + stackLength, y + stackLength + spacingY / 2, color);
        int maxColumn = maxChildColumn(dataNode);
        guiGraphics.hLine(x + stackLength, maxColumn * spacingX + outputX + stackLength, y + stackLength + spacingY / 2, color);
    }

    guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3,
            dataNode.missing() <= 0 ? 0 : 22, 0, 22, 22);
    if (entry == currentMatchEntry) {
        guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3, 0, 44, 22, 22);
    } else if (searchResultSet.contains(dataNode)) {
        guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3, 0, 66, 22, 22);
    }

    AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, x, y, stack.what());
    drawAmount(guiGraphics, stack, x, y, color);
}
```

Add `drawAmount` by moving the current amount-label code unchanged.

In `draw`, after scaling, iterate all rows:

```java
for (List<LegacyTreeLayout.Entry> row : layout.rows()) {
    for (LegacyTreeLayout.Entry entry : row) {
        if (!entry.placeholder()) {
            drawNode(guiGraphics, entry);
        }
    }
}
```

- [ ] **Step 5: Replace hit detection and tooltip mapping**

Change `getMousePoint` callers to use `LegacyTreeLayout.Entry`:

```java
private LegacyTreeLayout.Entry getMouseEntry(double mouseX, double mouseY) {
    Point point = getMousePoint(mouseX, mouseY);
    return layout == null ? null : layout.atGrid(point.x, point.y);
}
```

In `updateTooltip`, use `LegacyTreeNode node = entry.node();` and replace `node.data().key()` with `node.key()`, `node.data().amount()` with `node.amount()`, and `node.data().amounts()` with `node.amounts()`.

In `mouseClicked`, open recipes from `entry.node().output()`.

- [ ] **Step 6: Replace keyboard navigation**

Update `keyPressed`:

```java
boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
switch (keyCode) {
    case InputConstants.KEY_RIGHT -> moveSelection(layout.findRight(getSelectedEntry(), ctrl));
    case InputConstants.KEY_LEFT -> moveSelection(layout.findLeft(getSelectedEntry(), ctrl));
    case InputConstants.KEY_UP -> moveSelection(layout.findUp(getSelectedEntry()));
    case InputConstants.KEY_DOWN -> moveSelection(layout.findDown(getSelectedEntry()));
    default -> {
    }
}
```

Add:

```java
private LegacyTreeLayout.Entry getSelectedEntry() {
    if (selectedEntry == null && layout != null && activeData != null && activeData.root() != null) {
        selectedEntry = layout.entry(activeData.root());
    }
    return selectedEntry;
}

private void moveSelection(LegacyTreeLayout.Entry next) {
    if (next == null) {
        return;
    }
    selectedEntry = next;
    outputX = 20 - next.column() * spacingX;
    outputY = 30 - next.row() * spacingY;
}
```

- [ ] **Step 7: Compile GUI migration**

Run:

```bash
./gradlew.bat compileJava
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeWidget.java src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeScreen.java
git commit -m "feat: render legacy row tree"
```

---

### Task 8: Screenshot Migration

**Files:**
- Modify: `src/main/java/com/vcwdfca/ae2ct/api/ScreenshotHelper.java`
- Modify: `src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeWidget.java`

- [ ] **Step 1: Change screenshot entry point**

Change `CraftingTreeWidget.screenShot()`:

```java
public void screenShot() {
    if (layout == null || activeData == null || activeData.root() == null) {
        var player = screen.getMenu().getPlayer();
        player.sendSystemMessage(Component.translatable("ae2ct.screenshot.noready"));
        return;
    }
    ScreenshotHelper.Screenshot(layout, screen.getMenu().getPlayer());
}
```

- [ ] **Step 2: Change ScreenshotHelper signatures**

Replace `DisplayNode<AEKey>` imports and signatures with:

```java
import com.vcwdfca.ae2ct.tree.LegacyTreeLayout;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
```

Change public method:

```java
public static void Screenshot(LegacyTreeLayout layout, Player player)
```

Collect renderable entries from rows:

```java
List<LegacyTreeLayout.Entry> nodes = layout.rows().stream()
        .flatMap(List::stream)
        .filter(entry -> !entry.placeholder())
        .toList();
Set<AEKey> keys = nodes.stream().map(entry -> entry.node().key()).collect(Collectors.toSet());
```

Compute width/height from `entry.column()` and `entry.row()`.

- [ ] **Step 3: Replace screenshot draw recursion**

Use this draw method:

```java
private static void draw(Graphics2D graphics, BufferedImage stackImage, LegacyTreeLayout layout,
                         LegacyTreeLayout.Entry entry, Map<AEKey, Point> map) {
    int spacing = 110;
    int output = 10;
    int stackLength = 44;
    int x = entry.column() * spacing + output;
    int y = entry.row() * spacing + output;
    LegacyTreeNode node = entry.node();

    if (!node.inputs().isEmpty()) {
        graphics.drawLine(x + stackLength, y + stackLength, x + stackLength, y + stackLength + spacing / 2);
    }

    Point pos = map.get(node.key());
    BufferedImage subImage = stackImage.getSubimage(pos.x * 88, pos.y * 88, 88, 88);
    graphics.drawImage(subImage, x, y, null);

    if (Config.SCREENSHOT_SHOW_COUNT.get()) {
        String text = CraftingTreeWidget.getDrawAmount(node.key(), node.amount());
        var fm = graphics.getFontMetrics();
        graphics.drawString(text, x + 80 - fm.stringWidth(text), y + 92 - fm.getHeight());
    }

    int maxColumn = entry.column();
    for (LegacyTreeProcess process : node.inputs()) {
        for (LegacyTreeNode child : process.inputs()) {
            LegacyTreeLayout.Entry childEntry = layout.entry(child);
            if (childEntry == null) {
                continue;
            }
            int childX = childEntry.column() * spacing + output;
            int childY = childEntry.row() * spacing + output;
            graphics.drawLine(childX + stackLength, y + stackLength + spacing / 2, childX + stackLength, childY + stackLength);
            maxColumn = Math.max(maxColumn, childEntry.column());
            draw(graphics, stackImage, layout, childEntry, map);
        }
    }
    if (!node.inputs().isEmpty()) {
        graphics.drawLine(x + stackLength, y + stackLength + spacing / 2,
                maxColumn * spacing + output + stackLength, y + stackLength + spacing / 2);
    }
}
```

- [ ] **Step 4: Compile screenshot migration**

Run:

```bash
./gradlew.bat compileJava
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/vcwdfca/ae2ct/api/ScreenshotHelper.java src/main/java/com/vcwdfca/ae2ct/gui/CraftingTreeWidget.java
git commit -m "feat: adapt screenshots to legacy layout"
```

---

### Task 9: Remove Old Tree Consumers And Verify

**Files:**
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/GraphNode.java`
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/DisplayNode.java`
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/LayoutEngine.java`
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/LayoutMode.java`
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/NodeCache.java`
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/SearchIndex.java`
- Delete when no longer referenced: `src/main/java/com/vcwdfca/ae2ct/tree/TreeBuilder.java`
- Delete or rewrite old tests: `src/test/java/com/vcwdfca/ae2ct/tree/TreeBuilderTest.java`, `LayoutEngineTest.java`, `SearchIndexTest.java`, `TreeDataTest.java`

- [ ] **Step 1: Check references**

Run:

```bash
rg -n "GraphNode|DisplayNode|LayoutEngine|LayoutMode|NodeCache|SearchIndex|TreeBuilder|TreeData<" src/main/java src/test/java
```

Expected: no main-code references after Tasks 1-8. Test references can be removed or rewritten.

- [ ] **Step 2: Delete old classes and obsolete tests**

Delete only files with no remaining main-code references:

```bash
git rm src/main/java/com/vcwdfca/ae2ct/tree/GraphNode.java src/main/java/com/vcwdfca/ae2ct/tree/DisplayNode.java src/main/java/com/vcwdfca/ae2ct/tree/LayoutEngine.java src/main/java/com/vcwdfca/ae2ct/tree/LayoutMode.java src/main/java/com/vcwdfca/ae2ct/tree/NodeCache.java src/main/java/com/vcwdfca/ae2ct/tree/SearchIndex.java src/main/java/com/vcwdfca/ae2ct/tree/TreeBuilder.java src/test/java/com/vcwdfca/ae2ct/tree/TreeBuilderTest.java src/test/java/com/vcwdfca/ae2ct/tree/LayoutEngineTest.java src/test/java/com/vcwdfca/ae2ct/tree/SearchIndexTest.java src/test/java/com/vcwdfca/ae2ct/tree/TreeDataTest.java
```

Keep `TreeData.java` only if still needed by compatibility code. If no references remain, delete it too.

- [ ] **Step 3: Run full tests**

Run:

```bash
./gradlew.bat test
```

Expected: PASS.

- [ ] **Step 4: Run compile**

Run:

```bash
./gradlew.bat compileJava
```

Expected: PASS.

- [ ] **Step 5: Commit cleanup**

```bash
git add src/main/java src/test/java
git commit -m "refactor: remove old tree pipeline"
```

---

### Task 10: Manual Runtime Smoke Test

**Files:**
- No source changes expected unless the runtime smoke test reveals a bug.

- [ ] **Step 1: Start the client**

Run:

```bash
./gradlew.bat runClient
```

Expected: Minecraft client launches with AE2CraftingTree loaded.

- [ ] **Step 2: Open a crafting plan and tree screen**

In-game:

- Open an AE2 crafting confirm screen.
- Click the crafting-tree toolbar button.
- Verify the tree opens without a crash.
- Verify the first visible tree matches row-style Legacy layout.
- Toggle missing-only and confirm only missing paths remain.
- Search an item name and switch next/previous match.
- Use arrow keys and Ctrl+left/right to navigate.
- Use mouse wheel and drag to zoom/pan.
- Run screenshot action and confirm a PNG is created.

- [ ] **Step 3: Fix smoke-test issues if present**

For each runtime issue, add a focused regression test when possible, then fix the smallest related code path. Run:

```bash
./gradlew.bat test
./gradlew.bat compileJava
```

Expected: PASS after fixes.

- [ ] **Step 4: Commit runtime fixes**

If fixes were needed:

```bash
git add src/main/java src/test/java
git commit -m "fix: polish legacy tree runtime behavior"
```

If no fixes were needed, do not create an empty commit.

---

## Self-Review

Spec coverage:

- Legacy-style model: Tasks 1 and 3.
- Real AE2 tree extraction/conversion: Tasks 5 and 6.
- RecipeHelper fallback: Task 4.
- Row layout and navigation: Tasks 2 and 7.
- Search/cache: Task 3 plus Task 7 integration.
- Screenshot: Task 8.
- Error handling and empty tree behavior: Tasks 4, 5, 7, and 8.
- Tests and verification: Tasks 1-4, 9, and 10.

Type consistency:

- `LegacyTreeData`, `LegacyTreeNode`, `LegacyTreeProcess`, `LegacyTreeLayout`, and `LegacyTreeSearchIndex` are introduced before consumers use them.
- `CraftingTreeWidget` receives `LegacyTreeData realTree` from `ICraftingPlanSummary.getLegacyTree()`.
- `TreeCache.CachedTree` uses Legacy types after Task 3, before GUI migration in Task 7.

Known execution risks:

- Current AE2 dependency may expose `CraftingPlan.tree()` publicly or may store it as a private field. Task 6 starts with compilation and instructs using accessors only where needed.
- `TestKey` may need small abstract-method additions depending on the exact AE2 `AEKey` class in this dependency.
- Runtime smoke testing needs an actual AE2 crafting setup; automated unit tests cover model/layout behavior, not the full Minecraft GUI.
