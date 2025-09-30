package net.tigereye.chestcavity.guscript.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptBindingTogglePayload;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;

public class GuScriptScreen extends AbstractContainerScreen<GuScriptMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    private Button bindingTargetButton;
    private Button listenerButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button addPageButton;
    private Button simulateButton;
    private int pageInfoY;
    private int navButtonHeight;

    public GuScriptScreen(GuScriptMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + menu.getRows() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        CCConfig.GuScriptUIConfig ui = ChestCavity.config.GUSCRIPT_UI;
        int slotSize = computeSlotSize();

        int bindingSpacing = Math.max(ui.minBindingSpacingPx, (int) Math.round(slotSize * ui.bindingVerticalSpacingSlots));
        int bindingButtonHeight = Math.max(1, ui.bindingButtonHeightPx);
        int bindingStackHeight = bindingButtonHeight * 2 + bindingSpacing;
        int bindingTopPadding = Math.max(ui.minTopGutterPx, (int) Math.round(slotSize * ui.bindingTopPaddingSlots));
        int bindingBaseY = this.topPos - bindingTopPadding - bindingStackHeight + ui.bindingVerticalOffsetPx;
        int maxBindingBaseY = this.topPos - bindingStackHeight - ui.minTopGutterPx;
        bindingBaseY = Math.min(bindingBaseY, maxBindingBaseY);

        int rightPadding = Math.max(ui.minHorizontalGutterPx, (int) Math.round(slotSize * ui.bindingRightPaddingSlots));
        int bindingButtonWidth = (int) Math.round(this.imageWidth * ui.bindingButtonWidthFraction);
        int minBindingWidth = Math.max(ui.minBindingButtonWidthPx, 1);
        int maxBindingWidth = Math.max(this.imageWidth - ui.minHorizontalGutterPx - rightPadding, minBindingWidth);
        bindingButtonWidth = Mth.clamp(bindingButtonWidth, minBindingWidth, maxBindingWidth);

        int minAnchorX = this.leftPos + ui.minHorizontalGutterPx;
        int maxAnchorX = this.leftPos + this.imageWidth - bindingButtonWidth - ui.minHorizontalGutterPx;
        if (maxAnchorX < minAnchorX) {
            maxAnchorX = minAnchorX;
        }
        int bindingAnchorX = this.leftPos + this.imageWidth - rightPadding - bindingButtonWidth;
        bindingAnchorX = Mth.clamp(bindingAnchorX, minAnchorX, maxAnchorX);

        bindingTargetButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendToggle(GuScriptBindingTogglePayload.Operation.TOGGLE_TARGET))
                .bounds(bindingAnchorX, bindingBaseY, bindingButtonWidth, bindingButtonHeight)
                .build());

        listenerButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendToggle(GuScriptBindingTogglePayload.Operation.CYCLE_LISTENER))
                .bounds(bindingAnchorX, bindingBaseY + bindingButtonHeight + bindingSpacing, bindingButtonWidth, bindingButtonHeight)
                .build());

        int pageButtonWidth = Math.max(1, ui.pageButtonWidthPx);
        this.navButtonHeight = Math.max(1, ui.pageButtonHeightPx);
        int pageButtonSpacing = Math.max(ui.minPageButtonSpacingPx, (int) Math.round(slotSize * ui.pageButtonHorizontalSpacingSlots));
        int pageButtonRowWidth = pageButtonWidth * 3 + pageButtonSpacing * 2;
        int pageButtonLeftPadding = Math.max(ui.minHorizontalGutterPx, (int) Math.round(slotSize * ui.pageButtonLeftPaddingSlots));
        int preferredPageButtonX = this.leftPos + pageButtonLeftPadding;
        int minPageButtonX = this.leftPos + ui.minHorizontalGutterPx;
        int maxPageButtonX = this.leftPos + this.imageWidth - pageButtonRowWidth - ui.minHorizontalGutterPx;
        if (maxPageButtonX < minPageButtonX) {
            maxPageButtonX = minPageButtonX;
        }
        int pageButtonX = Mth.clamp(preferredPageButtonX + ui.pageButtonHorizontalOffsetPx, minPageButtonX, maxPageButtonX);

        int pageButtonTopPadding = Math.max(ui.minTopGutterPx, (int) Math.round(slotSize * ui.pageButtonTopPaddingSlots));
        int pageButtonY = this.topPos - pageButtonTopPadding - this.navButtonHeight;

        prevPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> sendPageChange(false))
                .bounds(pageButtonX, pageButtonY, pageButtonWidth, this.navButtonHeight)
                .build());

        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> sendPageChange(true))
                .bounds(pageButtonX + pageButtonWidth + pageButtonSpacing, pageButtonY, pageButtonWidth, this.navButtonHeight)
                .build());

        addPageButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> sendAddPage())
                .bounds(pageButtonX + (pageButtonWidth + pageButtonSpacing) * 2, pageButtonY, pageButtonWidth, this.navButtonHeight)
                .build());

        int pageInfoSpacing = Math.max(0, ui.pageInfoBelowNavSpacingPx);
        this.pageInfoY = pageButtonY + this.navButtonHeight + pageInfoSpacing;

        // Simulate button: horizontally aligned with paging buttons, to the left of the prevPageButton
        int simulateWidth = Math.max(60, pageButtonWidth * 3);
        int simulateY = pageButtonY; // same row as paging buttons
        // Move left by exactly one simulate button width from the previous placement
        int preferredSimulateX = pageButtonX - (pageButtonSpacing + simulateWidth);
        simulateButton = addRenderableWidget(Button.builder(Component.literal("模拟编译"), b -> sendSimulateCompile())
                .bounds(preferredSimulateX, simulateY, simulateWidth, this.navButtonHeight)
                .build());

        updateButtonStates();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int containerHeight = this.menu.getRows() * 18 + 17;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, containerHeight);
        graphics.blit(BACKGROUND, x, y + containerHeight, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        Component pageInfo = Component.translatable("gui.chestcavity.guscript.page", this.menu.getPageIndex() + 1, Math.max(1, this.menu.getPageCount()));
        int pageInfoX = this.leftPos + (this.imageWidth - this.font.width(pageInfo)) / 2;
        graphics.drawString(this.font, pageInfo, pageInfoX, this.pageInfoY, 0xFFFFFF, false);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonStates();
    }

    private void updateButtonStates() {
        BindingTarget bindingTarget = this.menu.getBindingTarget();
        ListenerType listenerType = this.menu.getListenerType();

        if (bindingTargetButton != null) {
            bindingTargetButton.setMessage(bindingTarget.label());
        }

        if (listenerButton != null) {
            listenerButton.setMessage(listenerType.label());
            listenerButton.active = bindingTarget == BindingTarget.LISTENER;
        }

        if (prevPageButton != null) {
            prevPageButton.active = this.menu.getPageIndex() > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = this.menu.getPageIndex() < this.menu.getPageCount() - 1;
        }
    }

    private void sendToggle(GuScriptBindingTogglePayload.Operation operation) {
        if (operation == GuScriptBindingTogglePayload.Operation.CYCLE_LISTENER
                && this.menu.getBindingTarget() != BindingTarget.LISTENER) {
            return;
        }
        if (minecraft == null) {
            return;
        }
        var connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }
        connection.send(new GuScriptBindingTogglePayload(operation));
    }

    private void sendPageChange(boolean forward) {
        if (minecraft == null) {
            return;
        }
        int target = this.menu.getPageIndex() + (forward ? 1 : -1);
        if (target < 0 || target >= this.menu.getPageCount()) {
            return;
        }
        var connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(new net.tigereye.chestcavity.guscript.network.packets.GuScriptPageChangePayload(net.tigereye.chestcavity.guscript.network.packets.GuScriptPageChangePayload.Operation.SET, target));
        }
    }

    private void sendAddPage() {
        if (minecraft == null) {
            return;
        }
        var connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(new net.tigereye.chestcavity.guscript.network.packets.GuScriptPageChangePayload(net.tigereye.chestcavity.guscript.network.packets.GuScriptPageChangePayload.Operation.ADD, 0));
        }
    }

    private void sendSimulateCompile() {
        if (minecraft == null) {
            return;
        }
        var connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(new net.tigereye.chestcavity.guscript.network.packets.GuScriptSimulateCompilePayload(this.menu.getPageIndex()));
        }
    }

    private int computeSlotSize() {
        int rows = this.menu.getRows();
        if (rows <= 0) {
            return 18;
        }
        int contentHeight = this.imageHeight - 114;
        if (contentHeight <= 0) {
            return 18;
        }
        return Math.max(1, contentHeight / rows);
    }
}
