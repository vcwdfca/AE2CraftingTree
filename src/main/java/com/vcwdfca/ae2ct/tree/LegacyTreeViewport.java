package com.vcwdfca.ae2ct.tree;

public record LegacyTreeViewport(int viewportWidth, int viewportHeight, int contentWidth, int contentHeight,
                                 int originX, int originY, int offsetX, int offsetY, float scale) {
    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 1.0f;

    public static LegacyTreeViewport reset(int viewportWidth, int viewportHeight, int contentWidth, int contentHeight,
                                           int originX, int originY) {
        return new LegacyTreeViewport(viewportWidth, viewportHeight, contentWidth, contentHeight,
                originX, originY, originX, originY, MAX_SCALE).clamp();
    }

    public LegacyTreeViewport zoom(double wheelDelta) {
        float nextScale = clampScale(scale + (wheelDelta < 0 ? -0.05f : 0.05f));
        return new LegacyTreeViewport(viewportWidth, viewportHeight, contentWidth, contentHeight,
                originX, originY, offsetX, offsetY, nextScale).clamp();
    }

    public LegacyTreeViewport pan(double dragX, double dragY) {
        return new LegacyTreeViewport(viewportWidth, viewportHeight, contentWidth, contentHeight,
                originX, originY, offsetX + Math.round((float) (dragX / scale)),
                offsetY + Math.round((float) (dragY / scale)), scale).clamp();
    }

    public LegacyTreeViewport focus(int column, int row, int spacingX, int spacingY) {
        return new LegacyTreeViewport(viewportWidth, viewportHeight, contentWidth, contentHeight,
                originX, originY, originX - column * spacingX, originY - row * spacingY, scale).clamp();
    }

    public LegacyTreeViewport clamp() {
        return new LegacyTreeViewport(viewportWidth, viewportHeight, contentWidth, contentHeight,
                originX, originY, clampOffset(offsetX, viewportWidth, contentWidth, originX),
                clampOffset(offsetY, viewportHeight, contentHeight, originY), scale);
    }

    private static int clampOffset(int offset, int viewportSize, int contentSize, int origin) {
        if (contentSize <= viewportSize) {
            return origin;
        }
        int min = origin - (contentSize - viewportSize);
        return Math.max(min, Math.min(origin, offset));
    }

    private static float clampScale(float value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }
}
