package com.vcwdfca.ae2ct.tree;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class TreeBuilder<K> {
    private final TreeData<K> data;
    private final NodeCache<K> cache;
    private final LayoutEngine<K> layoutEngine;
    private LayoutMode layoutMode;
    private final DisplayNode<K> root;
    private final int viewportMargin = 2;

    public TreeBuilder(TreeData<K> data, NodeCache<K> cache, LayoutEngine<K> layoutEngine, LayoutMode layoutMode) {
        this.data = Objects.requireNonNull(data, "data");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.layoutEngine = Objects.requireNonNull(layoutEngine, "layoutEngine");
        this.layoutMode = Objects.requireNonNull(layoutMode, "layoutMode");

        this.root = cache.getOrCreate(data.root());
        if (root.point() == null) {
            root.setPoint(new Point(0, 0));
        }
        layoutEngine.registerRoot(root);
    }

    public DisplayNode<K> root() {
        return root;
    }

    public void setLayoutMode(LayoutMode mode) {
        if (mode != layoutMode) {
            layoutMode = mode;
            rebuildLayout();
        }
    }

    public void ensureViewport(Viewport viewport) {
        if (root.point() == null) {
            return;
        }
        Viewport effective = viewport.expand(viewportMargin);
        Deque<DisplayNode<K>> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            DisplayNode<K> node = queue.removeFirst();
            if (node.point() == null || !effective.contains(node.point())) {
                continue;
            }
            expand(node);
            for (DisplayNode<K> child : node.children()) {
                queue.add(child);
            }
        }
    }

    public DisplayNode<K> expandPath(GraphNode<K> target) {
        List<GraphNode<K>> chain = new ArrayList<>();
        GraphNode<K> cursor = target;
        while (cursor != null) {
            chain.add(0, cursor);
            cursor = cursor.parent();
        }

        DisplayNode<K> display = root;
        for (int i = 1; i < chain.size(); i++) {
            expand(display);
            GraphNode<K> nextData = chain.get(i);
            DisplayNode<K> next = cache.getOrCreate(nextData);
            if (!display.children().contains(next)) {
                display.addChild(next);
                layoutEngine.layoutChildren(display, layoutMode);
            }
            display = next;
        }
        return display;
    }

    public void expandAll() {
        Deque<DisplayNode<K>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            DisplayNode<K> node = queue.removeFirst();
            expand(node);
            for (DisplayNode<K> child : node.children()) {
                queue.add(child);
            }
        }
    }

    private void expand(DisplayNode<K> node) {
        if (node.isExpanded()) {
            return;
        }
        for (GraphNode<K> childData : node.data().children()) {
            DisplayNode<K> child = cache.getOrCreate(childData);
            if (!node.children().contains(child)) {
                node.addChild(child);
            }
        }
        layoutEngine.layoutChildren(node, layoutMode);
        node.setExpanded(true);
    }

    private void rebuildLayout() {
        layoutEngine.clear();
        if (root.point() == null) {
            root.setPoint(new Point(0, 0));
        }
        layoutEngine.registerRoot(root);
        Deque<DisplayNode<K>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            DisplayNode<K> node = queue.removeFirst();
            layoutEngine.layoutChildren(node, layoutMode);
            if (node.isExpanded()) {
                queue.addAll(node.children());
            }
        }
    }
}
