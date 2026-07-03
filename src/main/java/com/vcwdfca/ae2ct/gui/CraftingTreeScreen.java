package com.vcwdfca.ae2ct.gui;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.me.crafting.CraftConfirmScreen;

import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.mojang.blaze3d.platform.InputConstants;
import com.vcwdfca.ae2ct.Config;
import com.vcwdfca.ae2ct.api.ICraftingPlanSummary;
import com.vcwdfca.ae2ct.api.ToolTipText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class CraftingTreeScreen extends AESubScreen<CraftConfirmMenu, CraftConfirmScreen> {
    private final CraftingTreeWidget craftingTreeWidget;
    private final AETextField searchField;
    private static final Component PRE = Component.literal("<");
    private static final Component NXT = Component.literal(">");
    private final Button[] btns = new Button[2];
    private final ChangeButton missingOnlyButton;

    public CraftingTreeScreen(CraftConfirmScreen parent) {
        super(parent, "/screens/crafting_tree.json");
        var plan = Objects.requireNonNull(parent.getMenu().getPlan(), "crafting plan");
        var summary = (ICraftingPlanSummary) plan;
        craftingTreeWidget = new CraftingTreeWidget(this, summary.getJob(), summary.getLegacyTree(),
            plan.getEntries(), Config.SHOW_MISSING_ONLY_BY_DEFAULT.get());
        addBackButton();
        this.addToLeftToolbar(new ChangeButton(this::changeSetting, Icon.COG, ToolTipText.Setting));
        this.addToLeftToolbar(new ChangeButton(craftingTreeWidget::screenShot, Icon.STORAGE_FILTER_EXTRACTABLE_ONLY, ToolTipText.Screenshot));
        missingOnlyButton = new ChangeButton(this::showMissingOnly, Icon.INVALID, ToolTipText.ShowMissingOnly);
        this.addToLeftToolbar(missingOnlyButton);

        searchField = widgets.addTextField("searchField");
        searchField.setPlaceholder(GuiText.SearchPlaceholder.text());
        searchField.setResponder(this::setSearchText);

        btns[0] = widgets.addButton("pre", PRE, action -> craftingTreeWidget.matchSwitch(false));
        btns[1] = widgets.addButton("nxt", NXT, action -> craftingTreeWidget.matchSwitch(true));
    }

    private void addBackButton() {
        var label = menu.getHost().getMainMenuIcon().getHoverName();
        TabButton button = new TabButton(Icon.BACK, label, btn -> returnToParent());
        widgets.add("back", button);
    }

    private void showMissingOnly() {
        if (craftingTreeWidget == null) return;
        craftingTreeWidget.toggleMissingOnly();
    }


    private void changeSetting() {
        switchToScreen(new SettingScreen(this));
    }


    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        craftingTreeWidget.draw(guiGraphics, offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);

        missingOnlyButton.active = craftingTreeWidget.hasMissing();
        searchField.render(guiGraphics, mouseX, mouseY, partialTicks);
        btns[0].render(guiGraphics, mouseX, mouseY, partialTicks);
        btns[1].render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        craftingTreeWidget.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        craftingTreeWidget.mouseDragged(mouseX, mouseY, mouseButton, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, mouseButton, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        craftingTreeWidget.mouseClicked(xCoord, yCoord, btn);

        if (searchField.isMouseOver(xCoord, yCoord) && btn == 1) {
            searchField.setValue("");
            setSearchText("");
        }

        return super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_BACKSPACE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (searchField.isFocused() && keyCode == GLFW.GLFW_KEY_ENTER) {
            searchField.setFocused(false);
            return true;
        }

        if (!searchField.isFocused() && craftingTreeWidget.handleRecipeViewerKey(keyCode, scanCode, modifiers)) {
            return true;
        }
        craftingTreeWidget.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (character == ' ' && this.searchField.getValue().isEmpty()) {
            return true;
        }

        return super.charTyped(character, modifiers);
    }

    private void setSearchText(String text) {
        craftingTreeWidget.setSearchString(text);
    }
}
