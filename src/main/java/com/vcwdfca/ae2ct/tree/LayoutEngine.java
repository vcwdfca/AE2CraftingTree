package com.vcwdfca.ae2ct.tree;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public final class LayoutEngine<K> {
    private final Map<Point, DisplayNode<K>> occupancy = new HashMap<>();

    public void clear() {
        occupancy.clear();
    }

    public Map<Point, DisplayNode<K>> occupancy() {
        return occupancy;
    }

    public void registerRoot(DisplayNode<K> root) {
        if (root.point() != null) {
            occupancy.put(root.point(), root);
        }
    }

    public void layoutChildren(DisplayNode<K> parent, LayoutMode mode) {
        if (parent.children().isEmpty() || parent.point() == null) {
            return;
        }

        int y = parent.point().y + 1;
        int nextX = parent.point().x;

        for (DisplayNode<K> child : parent.children()) {
            if (child.point() != null) {
                nextX = Math.max(nextX, child.point().x + 1);
                continue;
            }

            if (mode == LayoutMode.COMPACT) {
                nextX = nextFreeX(nextX, y);
                Point p = new Point(nextX, y);
                child.setPoint(p);
                occupancy.put(p, child);
                nextX++;
            } else {
                Point p = new Point(nextX, y);
                while (occupancy.containsKey(p)) {
                    nextX++;
                    p = new Point(nextX, y);
                }
                child.setPoint(p);
                occupancy.put(p, child);
                nextX += 2;
            }
        }
    }

    private int nextFreeX(int startX, int y) {
        int x = startX;
        while (occupancy.containsKey(new Point(x, y))) {
            x++;
        }
        return x;
    }
}
