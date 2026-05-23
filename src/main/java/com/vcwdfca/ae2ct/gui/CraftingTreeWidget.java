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
import com.vcwdfca.ae2ct.Config;
import com.vcwdfca.ae2ct.api.RecipeHelper;
import com.vcwdfca.ae2ct.api.ScreenshotHelper;
import com.vcwdfca.ae2ct.api.ToolTipText;
import com.vcwdfca.ae2ct.api.xei.Base;
import com.vcwdfca.ae2ct.tree.DisplayNode;
import com.vcwdfca.ae2ct.tree.GraphNode;
import com.vcwdfca.ae2ct.tree.LayoutEngine;
import com.vcwdfca.ae2ct.tree.LayoutMode;
import com.vcwdfca.ae2ct.tree.NodeCache;
import com.vcwdfca.ae2ct.tree.PlanKey;
import com.vcwdfca.ae2ct.tree.SearchIndex;
import com.vcwdfca.ae2ct.tree.TreeBuilder;
import com.vcwdfca.ae2ct.tree.TreeCache;
import com.vcwdfca.ae2ct.tree.TreeData;
import com.vcwdfca.ae2ct.tree.TreeDataBuilder;
import com.vcwdfca.ae2ct.tree.Viewport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class CraftingTreeWidget {
    private final RecipeHelper data;
    protected final AEBaseScreen<?> screen;

    private static final TreeCache<AEKey> CACHE = new TreeCache<>(3);

    private final TreeDataBuilder dataBuilder = new TreeDataBuilder();
    private CompletableFuture<TreeData<AEKey>> dataFuture;

    private TreeData<AEKey> baseData;
    private TreeData<AEKey> activeData;

    private NodeCache<AEKey> nodeCache;
    private LayoutEngine<AEKey> layoutEngine;
    private TreeBuilder<AEKey> treeBuilder;
    private SearchIndex<AEKey> searchIndex;

    protected boolean isMissingOnly = false;

    private final String planKey;

    private int outputX = 20;
    private int outputY = 30;
    private int spacingX = 30;
    private int spacingY = 30;
    private int stackLength = 8;
    private float scroll = 1.0f;

    private DisplayNode<AEKey> currentMatchDisplay = null;
    private int currentMatchIdx = 0;
    private List<GraphNode<AEKey>> searchResults = List.of();
    private Set<GraphNode<AEKey>> searchResultSet = Set.of();

    private DisplayNode<AEKey> selectedNode = null;
    private int selectedNodeIdx = 0;

    public CraftingTreeWidget(AEBaseScreen<?> screen, RecipeHelper data, List<CraftingPlanSummaryEntry> entries, boolean isMissingOnly) {
        this.screen = screen;
        this.data = data;
        this.isMissingOnly = isMissingOnly;
        this.planKey = PlanKey.fromRecipeHelper(data);

        TreeCache.CachedTree<AEKey> cached = CACHE.get(planKey);
        if (cached != null && cached.data() != null) {
            baseData = cached.data();
            nodeCache = cached.nodeCache() != null ? cached.nodeCache() : new NodeCache<>();
            searchIndex = cached.searchIndex() != null ? cached.searchIndex() : new SearchIndex<>();
            initializeActiveData();
        } else {
            dataFuture = CompletableFuture.supplyAsync(() -> dataBuilder.build(data, entries));
        }
    }

    public void draw(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        var board = new Rect2i(screen.getGuiLeft() + 10, screen.getGuiTop() + 20, 330, 210);
        guiGraphics.enableScissor(board.getX(), board.getY(), board.getX() + board.getWidth(), board.getY() + board.getHeight());

        ensureReady();
        if (treeBuilder == null || activeData == null || activeData.root() == null) {
            guiGraphics.disableScissor();
            return;
        }

        treeBuilder.setLayoutMode(getLayoutMode());

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        treeBuilder.ensureViewport(getViewport());

        poseStack.scale(scroll, scroll, scroll);
        drawNode(guiGraphics, treeBuilder.root());
        poseStack.popPose();
        guiGraphics.disableScissor();

        updateTooltip(guiGraphics, mouseX, mouseY);
        drawIndexingStatus(guiGraphics);
    }

    private void ensureReady() {
        if (treeBuilder != null) {
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

        initializeActiveData();
        CACHE.put(planKey, new TreeCache.CachedTree<>(baseData, nodeCache, searchIndex));
    }

    private void initializeActiveData() {
        activeData = isMissingOnly ? baseData.filterMissingOnly() : baseData;
        nodeCache = new NodeCache<>();
        layoutEngine = new LayoutEngine<>();
        if (activeData.root() == null) {
            treeBuilder = null;
            return;
        }
        treeBuilder = new TreeBuilder<>(activeData, nodeCache, layoutEngine, getLayoutMode());
        searchIndex = new SearchIndex<>();
        searchIndex.buildAsync(activeData, ForkJoinPool.commonPool());

        selectedNode = null;
        selectedNodeIdx = 0;
        currentMatchDisplay = null;
        currentMatchIdx = 0;
        searchResults = List.of();
        searchResultSet = Set.of();
    }

    private LayoutMode getLayoutMode() {
        return Config.USE_COMPACT_TREE.get() ? LayoutMode.COMPACT : LayoutMode.LOOSE;
    }

    private void drawNode(GuiGraphics guiGraphics, DisplayNode<AEKey> node) {
        GraphNode<AEKey> dataNode = node.data();
        GenericStack stack = new GenericStack(dataNode.key(), dataNode.amount());
        var color = FastColor.ARGB32.color(255, 0, 0, 0);

        int x = node.point().x * spacingX + outputX;
        int y = node.point().y * spacingY + outputY;

        if (x * scroll > screen.getGuiLeft() + screen.width + 10 || y * scroll > screen.getGuiTop() + screen.height + 10) {
            return;
        }

        if (!node.children().isEmpty()) {
            guiGraphics.vLine(x + stackLength, y + stackLength, y + stackLength + spacingY / 2, color);
        }

        if (dataNode.amounts().missing() <= 0) {
            guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3, 0, 0, 22, 22);
        } else {
            guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3, 0, 22, 22, 22);
        }

        if (node == currentMatchDisplay) {
            guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3, 0, 44, 22, 22);
        } else if (searchResultSet.contains(dataNode)) {
            guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x - 3, y - 3, 0, 66, 22, 22);
        }

        AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, x, y, stack.what());
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        var font = Minecraft.getInstance().font;
        var fontX = font.width(getDrawAmount(stack.what(), dataNode.amount()));
        var fontY = font.lineHeight;
        float scale = 0.4f;
        poseStack.scale(scale, scale, scale);
        guiGraphics.drawString(font, getDrawAmount(stack.what(), dataNode.amount()), (x + 18 - fontX * scale) / scale,
                (y + 18 - fontY * scale) / scale, color, false);
        poseStack.popPose();

        for (DisplayNode<AEKey> child : node.children()) {
            var p = child.point();
            var pX = p.x * spacingX + outputX;
            var pY = p.y * spacingY + outputY;
            guiGraphics.vLine(pX + stackLength, y + stackLength + spacingY / 2, pY + stackLength, color);
            drawNode(guiGraphics, child);
        }

        if (!node.children().isEmpty()) {
            int maxX = node.children().stream().mapToInt(c -> c.point().x).max().orElse(node.point().x);
            guiGraphics.hLine(x + stackLength, maxX * spacingX + outputX + stackLength, y + stackLength + spacingY / 2, color);
        }
    }

    private void updateTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Point p = getMousePoint(mouseX, mouseY);
        if (layoutEngine == null || !layoutEngine.occupancy().containsKey(p)) {
            return;
        }

        DisplayNode<AEKey> node = layoutEngine.occupancy().get(p);
        if (node == null || treeBuilder == null) {
            return;
        }

        GenericStack stack = new GenericStack(node.data().key(), node.data().amount());
        var lines = AEKeyRendering.getTooltip(stack.what());
        var a = node.data().amounts();

        if (node == treeBuilder.root()) {
            lines.add(ToolTipText.OutputAmount.text(stack.what().formatAmount(node.data().amount(), AmountFormat.FULL)));
        } else if (node.children().isEmpty()) {
            lines.add(ToolTipText.InputAmount.text(stack.what().formatAmount(node.data().amount(), AmountFormat.FULL)));
            lines.add(ToolTipText.Info.text());
            if (a.stored() > 0) {
                lines.add(ToolTipText.StoredAmount.text(stack.what().formatAmount(a.stored(), AmountFormat.FULL)));
            }
            if (a.missing() > 0) {
                lines.add(ToolTipText.MissingAmount.text(stack.what().formatAmount(a.missing(), AmountFormat.FULL)));
            }
        } else {
            lines.add(ToolTipText.MiddenAmount.text(stack.what().formatAmount(node.data().amount(), AmountFormat.FULL)));
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
        if (treeBuilder == null) {
            var player = screen.getMenu().getPlayer();
            player.sendSystemMessage(Component.translatable("ae2ct.screenshot.noready"));
            return;
        }
        treeBuilder.expandAll();
        ScreenshotHelper.Screenshot(treeBuilder.root(), screen.getMenu().getPlayer());
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

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isMouseOutScreen(mouseX, mouseY)) {
            return true;
        }
        scroll += (float) (deltaY * 0.1);
        if (scroll <= 0.1f) {
            scroll = 0.1f;
        }
        if (scroll >= 10f) {
            scroll = 10f;
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if (isMouseOutScreen(mouseX, mouseY)) {
            return true;
        }
        if (mouseButton == 1 || mouseButton == 0) {
            if (scroll <= 0.3f) {
                outputX += (int) (dragX * 2.5);
                outputY += (int) (dragY * 2.5);
            } else {
                outputX += (int) dragX;
                outputY += (int) dragY;
            }
        }
        return true;
    }

    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        if ((btn == 0 || btn == 1) && !isMouseOutScreen(xCoord, yCoord)) {
            Point p = getMousePoint(xCoord, yCoord);
            if (layoutEngine != null && layoutEngine.occupancy().containsKey(p)) {
                var node = layoutEngine.occupancy().get(p);
                var stack = new GenericStack(node.data().key(), node.data().amount());
                if (btn == 0) {
                    Base.openRecipe(stack, true);
                } else {
                    Base.openRecipe(stack, false);
                }
            }
        } else if (btn == 2) {
            scroll = 1.0f;
            outputX = 20;
            outputY = 30;
        }
        return true;
    }

    private boolean isMouseOutScreen(double mouseX, double mouseY) {
        if (mouseX - screen.getGuiLeft() < getArea().getX() || mouseX - screen.getGuiLeft() > getArea().getX() + getArea().getWidth()) {
            return true;
        }
        if (mouseY - screen.getGuiTop() < getArea().getY() || mouseY - screen.getGuiTop() > getArea().getY() + getArea().getHeight()) {
            return true;
        }
        return false;
    }

    private Point getMousePoint(double mouseX, double mouseY) {
        try {
            if (isMouseOutScreen(mouseX, mouseY)) {
                return new Point(-1, -1);
            }
            int x = (int) (mouseX - screen.getGuiLeft() - outputX * scroll);
            int y = (int) (mouseY - screen.getGuiTop() - outputY * scroll);
            int sizeX = (int) (spacingX * scroll);
            int sizeY = (int) (spacingY * scroll);
            int i = x / sizeX;
            int j = y / sizeY;
            int left = (int) (i * spacingX * scroll);
            int top = (int) (j * spacingY * scroll);
            int right = (int) ((i * spacingX + stackLength * 2) * scroll);
            int bottom = (int) ((j * spacingY + stackLength * 2) * scroll);
            if (x > left && x < right && y > top && y < bottom) {
                return new Point(i, j);
            }
            return new Point(-1, -1);
        } catch (Exception e) {
            return new Point(-1, -1);
        }
    }

    private Viewport getViewport() {
        Rect2i area = getArea();
        int left = (int) ((area.getX() - outputX * scroll) / (spacingX * scroll));
        int right = (int) ((area.getX() + area.getWidth() - outputX * scroll) / (spacingX * scroll));
        int top = (int) ((area.getY() - outputY * scroll) / (spacingY * scroll));
        int bottom = (int) ((area.getY() + area.getHeight() - outputY * scroll) / (spacingY * scroll));
        return new Viewport(left, top, right, bottom);
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (treeBuilder == null) {
            return;
        }
        switch (keyCode) {
            case InputConstants.KEY_RIGHT -> moveSelection(1);
            case InputConstants.KEY_LEFT -> moveSelection(-1);
            case InputConstants.KEY_UP -> moveToParent();
            case InputConstants.KEY_DOWN -> moveToFirstChild();
            default -> {
            }
        }
    }

    private DisplayNode<AEKey> getSelectedNode() {
        if (selectedNode == null) {
            selectedNode = treeBuilder.root();
            selectedNodeIdx = 0;
        }
        return selectedNode;
    }

    private void updatePosition() {
        if (selectedNode != null) {
            outputX = 20 - selectedNode.point().x * spacingX;
            outputY = 30 - selectedNode.point().y * spacingY;
        }
    }

    private void moveSelection(int delta) {
        DisplayNode<AEKey> node = getSelectedNode();
        if (node.parent() == null) {
            return;
        }
        DisplayNode<AEKey> parent = node.parent();
        int next = selectedNodeIdx + delta;
        if (next >= 0 && next < parent.children().size()) {
            selectedNodeIdx = next;
            selectedNode = parent.children().get(selectedNodeIdx);
            updatePosition();
        }
    }

    private void moveToParent() {
        DisplayNode<AEKey> node = getSelectedNode();
        if (node.parent() != null) {
            selectedNode = node.parent();
            if (selectedNode.parent() != null) {
                selectedNodeIdx = selectedNode.parent().children().indexOf(selectedNode);
            } else {
                selectedNodeIdx = 0;
            }
            updatePosition();
        }
    }

    private void moveToFirstChild() {
        DisplayNode<AEKey> node = getSelectedNode();
        if (!node.children().isEmpty()) {
            selectedNode = node.children().get(0);
            selectedNodeIdx = 0;
            updatePosition();
        }
    }

    public void reBuild() {
        outputX = 20;
        outputY = 30;
        currentMatchDisplay = null;
        currentMatchIdx = 0;
        searchResults = List.of();
        searchResultSet = Set.of();
        selectedNode = null;
        selectedNodeIdx = 0;
        initializeActiveData();
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
        if (searchResults.isEmpty() || treeBuilder == null) {
            currentMatchDisplay = null;
            return;
        }
        GraphNode<AEKey> matchData = searchResults.get(currentMatchIdx);
        currentMatchDisplay = treeBuilder.expandPath(matchData);
        outputX = 20 - currentMatchDisplay.point().x * spacingX;
        outputY = 30 - currentMatchDisplay.point().y * spacingY;
    }
}