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

    private LegacyTreeLayout() {
    }

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
        return entry == null ? null : findVertical(entry, -1);
    }

    public Entry findDown(Entry entry) {
        return entry == null ? null : findVertical(entry, 1);
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
        Entry entry = addNode(node, depth, parent);
        for (LegacyTreeProcess process : node.inputs()) {
            for (LegacyTreeNode input : process.inputs()) {
                recursiveAddNode(input, depth + 1, entry);
            }
        }

        int extraRenderNodes = node.getRenderExpandNodes();
        fillEmpty(depth, extraRenderNodes);
        if (extraRenderNodes == 0 && node.inputs().size() > 1) {
            extraRenderNodes += node.inputs().size() - 1;
        }
        entry.setLinkedSubNodes(extraRenderNodes - node.getLastNodeRenderExpandNodes());
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

        private void setPrevious(Entry previous) {
            this.previous = previous;
        }

        private void setNext(Entry next) {
            this.next = next;
        }

        private void setLinkedSubNodes(int linkedSubNodes) {
            this.linkedSubNodes = Math.max(0, linkedSubNodes);
        }
    }
}
