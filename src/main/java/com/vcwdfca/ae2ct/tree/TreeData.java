package com.vcwdfca.ae2ct.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TreeData<K> {
    private final GraphNode<K> root;
    private final List<GraphNode<K>> allNodes;
    private final Map<K, GraphNode<K>> byKey;

    public TreeData(GraphNode<K> root, List<GraphNode<K>> allNodes) {
        this.root = root;
        this.allNodes = List.copyOf(allNodes);
        this.byKey = new HashMap<>();
        for (GraphNode<K> node : allNodes) {
            byKey.put(node.key(), node);
        }
    }

    public GraphNode<K> root() {
        return root;
    }

    public List<GraphNode<K>> allNodes() {
        return Collections.unmodifiableList(allNodes);
    }

    public boolean containsKey(K key) {
        return byKey.containsKey(key);
    }

    public TreeData<K> filterMissingOnly() {
        if (root == null) {
            return new TreeData<>(null, List.of());
        }

        Map<K, GraphNode<K>> outMap = new HashMap<>();
        List<GraphNode<K>> outList = new ArrayList<>();
        GraphNode<K> filteredRoot = filterMissing(root, outMap, outList);
        if (filteredRoot == null) {
            return new TreeData<>(null, List.of());
        }

        return new TreeData<>(filteredRoot, outList);
    }

    private GraphNode<K> filterMissing(GraphNode<K> node, Map<K, GraphNode<K>> outMap, List<GraphNode<K>> outList) {
        List<GraphNode<K>> filteredChildren = new ArrayList<>();
        for (GraphNode<K> child : node.children()) {
            GraphNode<K> filteredChild = filterMissing(child, outMap, outList);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        if (!node.amounts().hasMissing() && filteredChildren.isEmpty()) {
            return null;
        }

        GraphNode<K> copy = new GraphNode<>(node.key(), node.displayNameLower(), node.amount(), node.amounts());
        for (GraphNode<K> childCopy : filteredChildren) {
            copy.addChild(childCopy);
        }

        outMap.put(copy.key(), copy);
        outList.add(copy);
        return copy;
    }
}
