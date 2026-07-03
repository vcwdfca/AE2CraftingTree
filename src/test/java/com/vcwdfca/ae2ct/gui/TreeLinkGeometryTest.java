package com.vcwdfca.ae2ct.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeLinkGeometryTest {
    @Test
    void verticalSegmentIncludesBothEndpointsWithoutOverrun() {
        TreeLinkGeometry.LineSegment segment = TreeLinkGeometry.vertical(10, 20, 30);

        assertEquals(new TreeLinkGeometry.LineSegment(10, 20, 11, 31), segment);
    }

    @Test
    void horizontalSegmentIncludesBothEndpointsWithoutOverrunAndNormalizesDirection() {
        TreeLinkGeometry.LineSegment segment = TreeLinkGeometry.horizontal(30, 10, 20);

        assertEquals(new TreeLinkGeometry.LineSegment(10, 20, 31, 21), segment);
    }

    @Test
    void branchSegmentDoesNotExceedFirstOrLastChildColumn() {
        int leftChildCenterX = 28;
        int rightChildCenterX = 88;
        TreeLinkGeometry.LineSegment segment = TreeLinkGeometry.horizontal(leftChildCenterX, rightChildCenterX, 53);

        assertEquals(leftChildCenterX, segment.left());
        assertEquals(rightChildCenterX + 1, segment.right());
    }
}
