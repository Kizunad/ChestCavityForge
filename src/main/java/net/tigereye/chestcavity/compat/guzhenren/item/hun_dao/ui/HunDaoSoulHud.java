package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 占位 HUD，实现被显式禁用以避免误导玩家。
 *
 * <p>保留空实现方便未来重新启用；当前不会渲染任何元素。
 */
public final class HunDaoSoulHud {

  private HunDaoSoulHud() {}

  public static void render(GuiGraphics guiGraphics, float partialTicks) {
    // UI removed intentionally
  }
}
