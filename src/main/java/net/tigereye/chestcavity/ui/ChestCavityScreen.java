package net.tigereye.chestcavity.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** A screen for the chest cavity. */
public class ChestCavityScreen extends AbstractContainerScreen<ChestCavityScreenHandler> {
  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

  /**
   * Creates a new ChestCavityScreen.
   *
   * @param handler The screen handler.
   * @param inventory The player inventory.
   * @param title The screen title.
   */
  public ChestCavityScreen(ChestCavityScreenHandler handler, Inventory inventory, Component title) {
    super(handler, inventory, title);
    this.imageHeight = 114 + handler.getRows() * 18;
    this.inventoryLabelY = this.imageHeight - 94;
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
    int x = (width - imageWidth) / 2;
    int y = (height - imageHeight) / 2;
    int containerHeight = this.menu.getRows() * 18 + 17;
    graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, containerHeight);
    graphics.blit(TEXTURE, x, y + containerHeight, 0, 126, imageWidth, 96);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    renderBackground(graphics, mouseX, mouseY, delta);
    super.render(graphics, mouseX, mouseY, delta);
    renderTooltip(graphics, mouseX, mouseY);
  }

  @Override
  protected void init() {
    super.init();
    // Center the title
    titleLabelX = (imageWidth - font.width(title)) / 2;
  }
}