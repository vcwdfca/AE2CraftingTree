package com.vcwdfca.ae2ct.tree;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DisplayNode<K> {
    private final GraphNode<K> data;
    private final List<DisplayNode<K>> children = new ArrayList<>();
    private DisplayNode<K> parent;
    private Point point;
    private boolean expanded;

    public DisplayNode(GraphNode<K> data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public GraphNode<K> data() {
        return data;
    }

    public List<DisplayNode<K>> children() {
        return Collections.unmodifiableList(children);
    }

    public DisplayNode<K> parent() {
        return parent;
    }

    public Point point() {
        return point;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void addChild(DisplayNode<K> child) {
        children.add(child);
        child.parent = this;
    }

    public void clearChildren() {
        children.clear();
    }
}
