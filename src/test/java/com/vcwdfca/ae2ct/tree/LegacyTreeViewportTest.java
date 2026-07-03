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

    @Test
    void visibleGridRangeConvertsScaledViewportToPaddedRowsAndColumns() {
        LegacyTreeViewport viewport = new LegacyTreeViewport(330, 210, 1200, 900, 20, 30,
                -280, -150, 0.5f).clamp();

        LegacyTreeViewport.GridRange range = viewport.visibleGridRange(30, 30, 2);

        assertEquals(7, range.minColumn());
        assertEquals(33, range.maxColumn());
        assertEquals(3, range.minRow());
        assertEquals(21, range.maxRow());
    }
}
