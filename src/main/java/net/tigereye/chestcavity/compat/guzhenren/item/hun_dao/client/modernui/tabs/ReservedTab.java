package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.IHunDaoPanelTab;

/**
 * Reserved Tab placeholder implementation.
 *
 * <p>Phase 7: Placeholder for future expansion (Phase 8+).
 */
@OnlyIn(Dist.CLIENT)
public class ReservedTab implements IHunDaoPanelTab {

  private static final int COLOR_WHITE = 0xFFFFFFFF;
  private static final int COLOR_GRAY = 0xFF9FA7B3;

  private final String id;
  private final String title;

  /**
   * Create a reserved tab with a custom ID and title.
   *
   * @param id the tab ID
   * @param title the tab title
   */
  public ReservedTab(String id, String title) {
    this.id = id;
    this.title = title;
  }

  @NonNull
  @Override
  public String getId() {
    return id;
  }

  @NonNull
  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void renderContent(@NonNull Canvas canvas, int mouseX, int mouseY, float partialTick) {
    float y = 10;

    // Title
    y = renderText(canvas, "Coming Soon", 10, y, COLOR_WHITE);
    y += 10; // Extra spacing

    // Description
    y = renderText(canvas, "Reserved for Future Use", 10, y, COLOR_GRAY);
    y += 10;

    y = renderText(canvas, "This tab will be implemented", 10, y, COLOR_GRAY);
    y = renderText(canvas, "in a future phase.", 10, y, COLOR_GRAY);
  }

  /**
   * Helper method to render text on canvas and return the next Y position.
   *
   * @param canvas the canvas to draw on
   * @param text the text to render
   * @param x the x position
   * @param y the y position
   * @param color the text color
   * @return the next Y position (y + line height)
   */
  private float renderText(Canvas canvas, String text, float x, float y, int color) {
    Paint paint = Paint.get();
    paint.setColor(color);
    canvas.drawText(text, x, y, paint);
    return y + 20; // Return next line position
  }

  /**
   * Format placeholder content for display.
   *
   * @return formatted placeholder text
   */
  public String formatPlaceholderContent() {
    return "Coming Soon\n\nReserved for Future Use\n\n"
        + "This tab will be implemented in a future phase.";
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    // Reserved tabs are visible but not enabled (non-clickable)
    return false;
  }
}
