package com.vcwdfca.ae2ct.gui;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vcwdfca.ae2ct.AE2ct;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import com.vcwdfca.ae2ct.api.ScreenshotHelper;
import com.vcwdfca.ae2ct.api.ToolTipText;
import com.vcwdfca.ae2ct.api.xei.Base;
import com.vcwdfca.ae2ct.api.xei.RecipeViewAction;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.LegacyTreeLayout;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeSearchIndex;
import com.vcwdfca.ae2ct.tree.LegacyTreeViewport;
import com.vcwdfca.ae2ct.tree.PlanKey;
import com.vcwdfca.ae2ct.tree.TreeCache;
import com.vcwdfca.ae2ct.tree.TreeDataBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import org.lwjgl.glfw.GLFW;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CraftingTreeWidget {
    private final RecipeHelper data;
    protected final AEBaseScreen<?> screen;

    private static final TreeCache CACHE = new TreeCache(3);

    private final TreeDataBuilder dataBuilder = new TreeDataBuilder();
    private CompletableFuture<LegacyTreeData> dataFuture;

    private LegacyTreeData baseData;
    private LegacyTreeData activeData;
    private LegacyTreeLayout layout;
    private LegacyTreeSearchIndex searchIndex;

    protected boolean isMissingOnly;

    private String activePlanKey;

    private final int outputX = 20;
    private final int outputY = 30;
    private final int spacingX = 30;
    private final int spacingY = 30;
    private final int stackLength = 8;
    private static final int RENDER_GRID_PADDING = 3;
    private LegacyTreeViewport viewport = LegacyTreeViewport.reset(330, 210, 0, 0, outputX, outputY);

    private LegacyTreeLayout.Entry currentMatchEntry = null;
    private int currentMatchIdx = 0;
    private List<LegacyTreeNode> searchResults = List.of();
    private Set<LegacyTreeNode> searchResultSet = Set.of();
    private int lastMouseX;
    private int lastMouseY;

    private LegacyTreeLayout.Entry selectedEntry = null;

    public CraftingTreeWidget(AEBaseScreen<?> screen, RecipeHelper data, LegacyTreeData realTree,
                              List<CraftingPlanSummaryEntry> entries, boolean isMissingOnly) {
        this.screen = screen;
        this.data = data;
        this.isMissingOnly = isMissingOnly;
        if (realTree != null && realTree.root() != null) {
            activePlanKey = PlanKey.fromRecipeHelper(data, realTree);
            TreeCache.CachedTree cached = CACHE.get(activePlanKey);
            if (cached != null && cached.data() != null) {
                baseData = cached.data();
                initializeActiveData();
                return;
            }
            baseData = realTree;
            initializeActiveData();
            CACHE.put(activePlanKey, new TreeCache.CachedTree(baseData, layout, searchIndex));
        } else {
            activePlanKey = PlanKey.fromRecipeHelper(data, entries);
            TreeCache.CachedTree cached = CACHE.get(activePlanKey);
            if (cached != null && cached.data() != null) {
                baseData = cached.data();
                initializeActiveData();
                return;
            }
            dataFuture = CompletableFuture.supplyAsync(() -> dataBuilder.buildFallback(data, entries));
        }
    }

    public void draw(GuiGraphics guiGraphics, @SuppressWarnings("unused") int offsetX,
                     @SuppressWarnings("unused") int offsetY, int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        var board = new Rect2i(screen.getGuiLeft() + 10, screen.getGuiTop() + 20, 330, 210);
        guiGraphics.enableScissor(board.getX(), board.getY(), board.getX() + board.getWidth(), board.getY() + board.getHeight());

        ensureReady();
        if (layout == null || activeData == null || activeData.root() == null) {
            guiGraphics.disableScissor();
            return;
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scroll(), scroll(), scroll());
        LegacyTreeViewport.GridRange range = viewport.visibleGridRange(spacingX, spacingY, RENDER_GRID_PADDING);
        for (LegacyTreeLayout.Entry entry : layout.entriesInRange(range.minColumn(), range.maxColumn(), range.minRow(), range.maxRow(), renderAnchors())) {
            drawNode(guiGraphics, entry);
        }
        poseStack.popPose();
        guiGraphics.disableScissor();

        updateTooltip(guiGraphics, mouseX, mouseY);
        drawIndexingStatus(guiGraphics);
    }

    private void ensureReady() {
        if (layout != null) {
            return;
        }
        if (dataFuture == null || !dataFuture.isDone()) {
            return;
        }
        try {
            baseData = dataFuture.get();
        } catch (Exception ignored) {
            return;
        }

        if (activePlanKey == null) {
            activePlanKey = PlanKey.fromRecipeHelper(data, baseData);
        }
        TreeCache.CachedTree cached = CACHE.get(activePlanKey);
        if (cached != null && cached.data() != null) {
            baseData = cached.data();
        }
        initializeActiveData();
        CACHE.put(activePlanKey, new TreeCache.CachedTree(baseData, layout, searchIndex));
    }

    private void initializeActiveData() {
        if (baseData == null) {
            activeData = null;
            layout = null;
            searchIndex = new LegacyTreeSearchIndex();
            return;
        }
        if (!baseData.hasMissing()) {
            isMissingOnly = false;
        }
        activeData = isMissingOnly ? baseData.filterMissingOnly() : baseData;
        layout = activeData.root() == null ? null : LegacyTreeLayout.build(activeData.root());
        searchIndex = new LegacyTreeSearchIndex();
        searchIndex.buildSync(activeData);
        refreshViewport();

        selectedEntry = layout == null ? null : layout.entry(activeData.root());
        currentMatchEntry = null;
        currentMatchIdx = 0;
        searchResults = List.of();
        searchResultSet = Set.of();
    }

    private List<LegacyTreeLayout.Entry> renderAnchors() {
        List<LegacyTreeLayout.Entry> anchors = new ArrayList<>(2);
        if (currentMatchEntry != null) {
            anchors.add(currentMatchEntry);
        }
        if (selectedEntry != null && selectedEntry != currentMatchEntry) {
            anchors.add(selectedEntry);
        }
        return anchors;
    }

    private void drawNode(GuiGraphics guiGraphics, LegacyTreeLayout.Entry entry) {
        LegacyTreeNode dataNode = entry.node();
        GenericStack stack = dataNode.output();
        var color = FastColor.ARGB32.color(255, 0, 0, 0);

        int x = entry.column() * spacingX + outputX();
        int y = entry.row() * spacingY + outputY();

        if (x * scroll() > screen.getGuiLeft() + screen.width + 10 || y * scroll() > screen.getGuiTop() + screen.height + 10) {
            return;
        }

        if (entry.parent() != null) {
            drawLink(guiGraphics, TreeLinkGeometry.vertical(x + stackLength, y + stackLength - spacingY / 2,
                    y + stackLength), color);
        }
        if (!dataNode.inputs().isEmpty()) {
            drawLink(guiGraphics, TreeLinkGeometry.vertical(x + stackLength, y + stackLength,
                    y + stackLength + spacingY / 2), color);
            if (entry.linkedSubNodes() > 0) {
                drawLink(guiGraphics, TreeLinkGeometry.horizontal(x + stackLength,
                        entry.linkEndColumn() * spacingX + outputX() + stackLength,
                        y + stackLength + spacingY / 2), color);
            }
        }

        if (dataNode.missing() <= 0 && !dataNode.amounts().hasMissing()) {
            guiGraphics.blit(AE2ct.id("icon.png"), x - 3, y - 3, 0, 0, 22, 22);
        } else {
            guiGraphics.blit(AE2ct.id("icon.png"), x - 3, y - 3, 0, 22, 22, 22);
        }

        if (entry == currentMatchEntry) {
            guiGraphics.blit(AE2ct.id("icon.png"), x - 3, y - 3, 0, 44, 22, 22);
        } else if (searchResultSet.contains(dataNode)) {
            guiGraphics.blit(AE2ct.id("icon.png"), x - 3, y - 3, 0, 66, 22, 22);
        }

        AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, x, y, stack.what());
        drawAmount(guiGraphics, stack, x, y, color);
    }

    private void drawLink(GuiGraphics guiGraphics, TreeLinkGeometry.LineSegment segment, int color) {
        guiGraphics.fill(segment.left(), segment.top(), segment.right(), segment.bottom(), color);
    }

    private void drawAmount(GuiGraphics guiGraphics, GenericStack stack, int x, int y, int color) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        var font = Minecraft.getInstance().font;
        var fontX = font.width(getDrawAmount(stack.what(), stack.amount()));
        var fontY = font.lineHeight;
        float scale = 0.4f;
        poseStack.scale(scale, scale, scale);
        guiGraphics.drawString(font, getDrawAmount(stack.what(), stack.amount()), (x + 18 - fontX * scale) / scale,
                (y + 18 - fontY * scale) / scale, color, false);
        poseStack.popPose();
    }

    private void updateTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        LegacyTreeLayout.Entry entry = getMouseEntry(mouseX, mouseY);
        if (entry == null) {
            return;
        }

        LegacyTreeNode node = entry.node();
        if (node == null) {
            return;
        }

        GenericStack stack = node.output();
        var lines = AEKeyRendering.getTooltip(stack.what());
        var a = node.amounts();

        if (node == activeData.root()) {
            lines.add(ToolTipText.OutputAmount.text(stack.what().formatAmount(node.amount(), AmountFormat.FULL)));
        } else if (node.inputs().isEmpty()) {
            lines.add(ToolTipText.InputAmount.text(stack.what().formatAmount(node.amount(), AmountFormat.FULL)));
            lines.add(ToolTipText.Info.text());
            if (a.stored() > 0) {
                lines.add(ToolTipText.StoredAmount.text(stack.what().formatAmount(a.stored(), AmountFormat.FULL)));
            }
            if (a.missing() > 0) {
                lines.add(ToolTipText.MissingAmount.text(stack.what().formatAmount(a.missing(), AmountFormat.FULL)));
            }
        } else {
            lines.add(ToolTipText.MiddenAmount.text(stack.what().formatAmount(node.amount(), AmountFormat.FULL)));
            lines.add(ToolTipText.Info.text());
            if (a.stored() > 0) {
                lines.add(ToolTipText.StoredAmount.text(stack.what().formatAmount(a.stored(), AmountFormat.FULL)));
            }
            if (a.craft() > 0) {
                lines.add(ToolTipText.CraftingAmount.text(stack.what().formatAmount(a.craft(), AmountFormat.FULL)));
            }
        }

        screen.drawTooltipWithHeader(guiGraphics, mouseX - screen.getGuiLeft(), mouseY - screen.getGuiTop(), lines);
    }

    private void drawIndexingStatus(GuiGraphics guiGraphics) {
        if (searchIndex == null || !searchIndex.isIndexing()) {
            return;
        }
        String text = "Indexing " + searchIndex.indexedCount() + "/" + searchIndex.totalCount();
        guiGraphics.drawString(Minecraft.getInstance().font, text, 16, 8, FastColor.ARGB32.color(255, 200, 200, 200));
    }

    public void screenShot() {
        if (layout == null) {
            var player = screen.getMenu().getPlayer();
            player.sendSystemMessage(Component.translatable("ae2ct.screenshot.noready"));
            return;
        }
        ScreenshotHelper.Screenshot(layout, screen.getMenu().getPlayer());
    }

    public static String getDrawAmount(AEKey key, long amount) {
        if (key instanceof AEFluidKey) {
            if (amount >= 1_000) {
                return formatNumber(amount / 1_000.0) + "B";
            }
            return String.valueOf(amount);
        }

        if (amount >= 1_000_000_000) {
            return formatNumber(amount / 1_000_000_000.0) + "G";
        } else if (amount >= 1_000_000) {
            return formatNumber(amount / 1_000_000.0) + "M";
        } else if (amount >= 1_000) {
            return formatNumber(amount / 1_000.0) + "k";
        }
        return String.valueOf(amount);
    }

    public static String formatNumber(double number) {
        if (number == (long) number) {
            return String.format("%d", (long) number);
        }
        return String.format("%.1f", number);
    }

    private Rect2i getArea() {
        return new Rect2i(10, 20, 330, 210);
    }

    private void refreshViewport() {
        viewport = new LegacyTreeViewport(getArea().getWidth(), getArea().getHeight(),
                contentWidth(), contentHeight(), outputX, outputY,
                viewport.offsetX(), viewport.offsetY(), viewport.scale()).clamp();
    }

    private int contentWidth() {
        return layout == null ? 0 : layout.rows().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0) * spacingX + stackLength * 2;
    }

    private int contentHeight() {
        return layout == null ? 0 : layout.rows().size() * spacingY + stackLength * 2;
    }

    private int outputX() {
        return viewport.offsetX();
    }

    private int outputY() {
        return viewport.offsetY();
    }

    private float scroll() {
        return viewport.scale();
    }

    public void mouseScrolled(double mouseX, double mouseY, @SuppressWarnings("unused") double deltaX, double deltaY) {
        if (isMouseOutScreen(mouseX, mouseY)) {
            return;
        }
        viewport = viewport.zoom(deltaY);
    }

    public void mouseDragged(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if (isMouseOutScreen(mouseX, mouseY)) {
            return;
        }
        if (mouseButton == 1 || mouseButton == 0) {
            viewport = viewport.pan(dragX, dragY);
        }
    }

    public void mouseClicked(double xCoord, double yCoord, int btn) {
        if ((btn == 0 || btn == 1) && !isMouseOutScreen(xCoord, yCoord)) {
            LegacyTreeLayout.Entry entry = getMouseEntry(xCoord, yCoord);
            if (entry != null) {
                var stack = entry.node().output();
                openRecipe(stack, btn == 0 ? RecipeViewAction.SHOW_RECIPE : RecipeViewAction.SHOW_USES);
            }
        } else if (btn == 2) {
            viewport = LegacyTreeViewport.reset(getArea().getWidth(), getArea().getHeight(),
                    contentWidth(), contentHeight(), outputX, outputY);
        }
    }

    public boolean handleRecipeViewerKey(int keyCode, int scanCode, int modifiers) {
        if (layout == null) {
            return false;
        }
        LegacyTreeLayout.Entry entry = getMouseEntry(lastMouseX, lastMouseY);
        if (entry == null) {
            return false;
        }
        RecipeViewAction action = Base.getRecipeViewAction(keyCode, scanCode, modifiers);
        if (action == RecipeViewAction.NONE) {
            return false;
        }
        openRecipe(entry.node().output(), action);
        return true;
    }

    private void openRecipe(GenericStack stack, RecipeViewAction action) {
        if (action == RecipeViewAction.SHOW_RECIPE) {
            Base.openRecipe(stack, true);
        } else if (action == RecipeViewAction.SHOW_USES) {
            Base.openRecipe(stack, false);
        }
    }

    private boolean isMouseOutScreen(double mouseX, double mouseY) {
        if (mouseX - screen.getGuiLeft() < getArea().getX() || mouseX - screen.getGuiLeft() > getArea().getX() + getArea().getWidth()) {
            return true;
        }
        return mouseY - screen.getGuiTop() < getArea().getY() || mouseY - screen.getGuiTop() > getArea().getY() + getArea().getHeight();
    }

    private LegacyTreeLayout.Entry getMouseEntry(double mouseX, double mouseY) {
        Point point = getMousePoint(mouseX, mouseY);
        return layout == null ? null : layout.atGrid(point.x, point.y);
    }

    private Point getMousePoint(double mouseX, double mouseY) {
        if (isMouseOutScreen(mouseX, mouseY)) {
            return new Point(-1, -1);
        }
        int x = (int) (mouseX - screen.getGuiLeft() - outputX() * scroll());
        int y = (int) (mouseY - screen.getGuiTop() - outputY() * scroll());
        int sizeX = Math.max(1, Math.round(spacingX * scroll()));
        int sizeY = Math.max(1, Math.round(spacingY * scroll()));
        int i = x / sizeX;
        int j = y / sizeY;
        int left = (int) (i * spacingX * scroll());
        int top = (int) (j * spacingY * scroll());
        int right = (int) ((i * spacingX + stackLength * 2) * scroll());
        int bottom = (int) ((j * spacingY + stackLength * 2) * scroll());
        if (x > left && x < right && y > top && y < bottom) {
            return new Point(i, j);
        }
        return new Point(-1, -1);
    }

    public void keyPressed(int keyCode, @SuppressWarnings("unused") int scanCode, int modifiers) {
        if (layout == null) {
            return;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (keyCode) {
            case InputConstants.KEY_RIGHT -> moveSelection(layout.findRight(getSelectedEntry(), ctrl));
            case InputConstants.KEY_LEFT -> moveSelection(layout.findLeft(getSelectedEntry(), ctrl));
            case InputConstants.KEY_UP -> moveSelection(layout.findUp(getSelectedEntry()));
            case InputConstants.KEY_DOWN -> moveSelection(layout.findDown(getSelectedEntry()));
            default -> {
            }
        }
    }

    private LegacyTreeLayout.Entry getSelectedEntry() {
        if (selectedEntry == null && layout != null && activeData != null && activeData.root() != null) {
            selectedEntry = layout.entry(activeData.root());
        }
        return selectedEntry;
    }

    private void moveSelection(LegacyTreeLayout.Entry next) {
        if (next != null) {
            selectedEntry = next;
            viewport = viewport.focus(next.column(), next.row(), spacingX, spacingY);
        }
    }

    public void reBuild() {
        viewport = LegacyTreeViewport.reset(getArea().getWidth(), getArea().getHeight(),
                contentWidth(), contentHeight(), outputX, outputY);
        currentMatchEntry = null;
        currentMatchIdx = 0;
        searchResults = List.of();
        searchResultSet = Set.of();
        selectedEntry = null;
        initializeActiveData();
    }

    public boolean hasMissing() {
        return baseData != null && baseData.hasMissing();
    }

    public void setMissingOnly(boolean missingOnly) {
        if (missingOnly && !hasMissing()) {
            isMissingOnly = false;
            return;
        }
        if (isMissingOnly == missingOnly) {
            return;
        }
        isMissingOnly = missingOnly;
        reBuild();
    }

    public void toggleMissingOnly() {
        setMissingOnly(!isMissingOnly);
    }

    public void setSearchString(String searchString) {
        if (searchIndex == null) {
            return;
        }
        searchResults = searchIndex.search(searchString);
        searchResultSet = new HashSet<>(searchResults);
        currentMatchIdx = 0;
        updateMatch();
    }

    public void matchSwitch(boolean next) {
        if (searchResults.isEmpty()) {
            return;
        }
        currentMatchIdx += next ? 1 : -1;
        while (currentMatchIdx < 0 || currentMatchIdx >= searchResults.size()) {
            currentMatchIdx = (currentMatchIdx + searchResults.size()) % searchResults.size();
        }
        updateMatch();
    }

    private void updateMatch() {
        if (searchResults.isEmpty() || layout == null) {
            currentMatchEntry = null;
            return;
        }
        LegacyTreeNode matchData = searchResults.get(currentMatchIdx);
        currentMatchEntry = layout.entry(matchData);
        selectedEntry = currentMatchEntry;
        if (currentMatchEntry != null) {
            viewport = viewport.focus(currentMatchEntry.column(), currentMatchEntry.row(), spacingX, spacingY);
        }
    }
}
