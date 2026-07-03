package com.vcwdfca.ae2ct.gui;

final class TreeLinkGeometry {
    private TreeLinkGeometry() {
    }

    static LineSegment vertical(int x, int y1, int y2) {
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2) + 1;
        return new LineSegment(x, top, x + 1, bottom);
    }

    static LineSegment horizontal(int x1, int x2, int y) {
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2) + 1;
        return new LineSegment(left, y, right, y + 1);
    }

    record LineSegment(int left, int top, int right, int bottom) {
    }
}
