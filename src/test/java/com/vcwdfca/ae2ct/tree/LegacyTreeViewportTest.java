package com.vcwdfca.ae2ct.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyTreeViewportTest {
    @Test
    void zoomClampsToLegacyRange() {
        LegacyTreeViewport viewport = LegacyTreeViewport.reset(330, 210, 600, 420, 20, 30);

        for (int i = 0; i < 100; i++) {
            viewport = viewport.zoom(-1);
        }
        assertEquals(0.25f, viewport.scale());

        for (int i = 0; i < 100; i++) {
            viewport = viewport.zoom(1);
        }
        assertEquals(1.0f, viewport.scale());
    }

    @Test
    void panKeepsContentBoundedWithMargins() {
        LegacyTreeViewport viewport = LegacyTreeViewport.reset(330, 210, 600, 420, 20, 30);

        viewport = viewport.pan(500, 500);
        assertEquals(20, viewport.offsetX());
        assertEquals(30, viewport.offsetY());

        viewport = viewport.pan(-1000, -1000);
        assertEquals(-250, viewport.offsetX());
        assertEquals(-180, viewport.offsetY());
    }
}
