package net.tigereye.chestcavity.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Shared HUD/Toast paint helpers to keep a consistent look (card background, simple drop shadow,
 * icon blit, and colors).
 */
public final class HudUiPaint {
  private HudUiPaint() {}

  // Standard palette
  public static final int CARD_BG = 0xE0161618; // dark semi-opaque
  public static final int CARD_SHADOW = 0x4014171A; // soft shadow
  public static final int TEXT_TITLE = 0xFFFFFFFF;
  public static final int TEXT_SUB = 0xFFE0E0E0;
  public static final int PROGRESS_BG = 0x8024282C; // dark track
  public static final int PROGRESS_FG = 0xFFB0B0B0; // light gray fill

  /** Draw a simple shadowed card (square corners; rounded can be added later via 9-slice/mesh). */
  public static void drawCard(GuiGraphics g, int x, int y, int w, int h) {
    // Shadow offset
    g.fill(x + 2, y + 2, x + w + 2, y + h + 2, CARD_SHADOW);
    // Background
    g.fill(x, y, x + w, y + h, CARD_BG);
  }

  /** Draw a 24x24 icon at given position. */
  public static void drawIcon24(GuiGraphics g, ResourceLocation icon, int x, int y) {
    if (icon != null) {
      g.blit(icon, x, y, 0, 0, 24, 24, 24, 24);
    }
  }

  /** Draw an item stack icon inside a 24x24 box. Uses the vanilla 16x16 render centered. */
  public static void drawItem24(GuiGraphics g, ItemStack stack, int x, int y) {
    if (stack == null || stack.isEmpty()) return;
    int ix = x + 4; // center 16px in 24px box
    int iy = y + 4;
    var font = Minecraft.getInstance().font;
    g.renderItem(stack, ix, iy);
    g.renderItemDecorations(font, stack, ix, iy);
  }

  /** Draw two lines of text next to the icon. */
  public static void drawTwoLineText(GuiGraphics g, String title, String sub, int x, int y) {
    var font = Minecraft.getInstance().font;
    g.drawString(font, title, x, y, TEXT_TITLE, false);
    g.drawString(font, sub, x, y + 12, TEXT_SUB, false);
  }

  /** Draw a horizontal progress bar with a small shadow. progress in [0,1]. */
  public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h, float progress) {
    // Shadow
    g.fill(x + 1, y + 1, x + w + 1, y + h + 1, CARD_SHADOW);
    // Track
    g.fill(x, y, x + w, y + h, PROGRESS_BG);
    // Fill (clamped)
    float p = Math.max(0f, Math.min(1f, progress));
    int fw = Math.round(w * p);
    if (fw > 0) {
      g.fill(x, y, x + fw, y + h, PROGRESS_FG);
    }
  }
}
