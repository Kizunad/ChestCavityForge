package net.tigereye.chestcavity.guscript.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptBindingTogglePayload;

public class GuScriptScreen extends AbstractContainerScreen<GuScriptMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    private Button bindingTargetButton;
    private Button listenerButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button addPageButton;

    public GuScriptScreen(GuScriptMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + menu.getRows() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        int buttonWidth = 98;
        int offset = 4;
        int slotSize = 18;
        int rightX = this.leftPos + this.imageWidth - offset - slotSize;
        int baseY = this.topPos - 24 - slotSize - slotSize / 2;

        bindingTargetButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendToggle(GuScriptBindingTogglePayload.Operation.TOGGLE_TARGET))
                .bounds(rightX - buttonWidth, baseY, buttonWidth, 20)
                .build());

        listenerButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendToggle(GuScriptBindingTogglePayload.Operation.CYCLE_LISTENER))
                .bounds(rightX - buttonWidth, baseY + 22, buttonWidth, 20)
                .build());

        int pageButtonWidth = 20;
        int pageButtonY = this.topPos - 18;
        int pageButtonX = this.leftPos + 8;

        prevPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> sendPageChange(false))
                .bounds(pageButtonX, pageButtonY, pageButtonWidth, 20)
                .build());

        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> sendPageChange(true))
                .bounds(pageButtonX + pageButtonWidth + 2, pageButtonY, pageButtonWidth, 20)
                .build());

        addPageButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> sendAddPage())
                .bounds(pageButtonX + (pageButtonWidth + 2) * 2, pageButtonY, pageButtonWidth, 20)
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
        graphics.drawString(this.font, pageInfo, this.leftPos + 60, this.topPos - 12, 0xFFFFFF, false);
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
}
