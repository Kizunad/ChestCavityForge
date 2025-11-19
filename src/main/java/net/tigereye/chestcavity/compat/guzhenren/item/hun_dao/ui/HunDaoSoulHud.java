package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Placeholder for a Heads-Up Display (HUD) related to the soul system.
 *
 * <p>This implementation is intentionally left empty to avoid misleading players, as the UI features
 * are currently disabled. It is preserved as a placeholder for potential future reenabling. At
 * present, it does not render any elements on the screen.
 */
public final class HunDaoSoulHud {

  private HunDaoSoulHud() {}

  /**
   * Renders the soul HUD. This method is a no-op as the UI is intentionally disabled.
   *
   * @param guiGraphics The GuiGraphics context used for rendering.
   * @param partialTicks The fraction of a tick that has passed since the last complete tick, used
   *     for smooth animation.
   */
  public static void render(GuiGraphics guiGraphics, float partialTicks) {
    // UI removed intentionally
  }
}
