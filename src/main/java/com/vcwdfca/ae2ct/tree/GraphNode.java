package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GraphNode<K> {
    private final K key;
    private final String displayNameLower;
    private final long amount;
    private final Amounts amounts;
    private final List<GraphNode<K>> children = new ArrayList<>();
    private GraphNode<K> parent;

    public GraphNode(K key, String displayNameLower, long amount, Amounts amounts) {
        this.key = Objects.requireNonNull(key, "key");
        this.displayNameLower = Objects.requireNonNull(displayNameLower, "displayNameLower");
        this.amount = amount;
        this.amounts = Objects.requireNonNull(amounts, "amounts");
    }

    public K key() {
        return key;
    }

    public String displayNameLower() {
        return displayNameLower;
    }

    public long amount() {
        return amount;
    }

    public Amounts amounts() {
        return amounts;
    }

    public GraphNode<K> parent() {
        return parent;
    }

    public List<GraphNode<K>> children() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(GraphNode<K> child) {
        children.add(child);
        child.parent = this;
    }
}
