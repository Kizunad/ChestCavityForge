package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.view.View;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom view for rendering tab content using Modern UI Canvas.
 *
 * <p>Phase 7.1: Implements proper canvas-based rendering that calls the active tab's renderContent
 * method.
 */
@OnlyIn(Dist.CLIENT)
public class CanvasContentView extends View {

  private IHunDaoPanelTab activeTab;

  public CanvasContentView(@NonNull Context context) {
    super(context);
  }

  /**
   * Set the active tab whose content should be rendered.
   *
   * @param tab the active tab to render
   */
  public void setActiveTab(@Nullable IHunDaoPanelTab tab) {
    this.activeTab = tab;
    invalidate(); // Trigger redraw
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    if (activeTab != null) {
      // Call the active tab's renderContent method
      // Mouse coordinates are not available in onDraw, so we pass 0, 0
      activeTab.renderContent(canvas, 0, 0, 0f);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Set a reasonable minimum size for the content area
    int minWidth = dp(400);
    int minHeight = dp(200);

    int width = Math.max(minWidth, MeasureSpec.getSize(widthMeasureSpec));
    int height = Math.max(minHeight, MeasureSpec.getSize(heightMeasureSpec));

    setMeasuredDimension(width, height);
  }
}
