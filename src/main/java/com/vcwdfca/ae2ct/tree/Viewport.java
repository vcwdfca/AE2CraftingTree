package com.vcwdfca.ae2ct.tree;

import java.awt.Point;

public record Viewport(int minX, int minY, int maxX, int maxY) {
    public boolean contains(Point p) {
        return p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY;
    }

    public Viewport expand(int margin) {
        return new Viewport(minX - margin, minY - margin, maxX + margin, maxY + margin);
    }
}
