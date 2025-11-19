package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom view for rendering tab content using TextView or custom View.
 *
 * <p>Phase 7.1: Implements proper text-based rendering that displays the active tab's content.
 *
 * <p>Phase 7.2: Supports custom View layouts from tabs for complex two-column grids and cards.
 */
@OnlyIn(Dist.CLIENT)
public class TabContentView extends LinearLayout {

  private TextView contentTextView;

  public TabContentView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
  }

  /**
   * Set the active tab whose content should be rendered.
   *
   * <p>Phase 7.2: If the tab provides a custom view via createContentView(), use that. Otherwise,
   * fall back to text-based rendering.
   *
   * @param tab the active tab to render
   */
  public void setActiveTab(@Nullable IHunDaoPanelTab tab) {
    // Clear existing content
    removeAllViews();

    if (tab == null) {
      return;
    }

    // Phase 7.2: Try to get custom view first
    var customView = tab.createContentView(getContext());
    if (customView != null) {
      addView(customView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
      return;
    }

    // Fall back to text-based rendering
    if (contentTextView == null) {
      contentTextView = new TextView(getContext());
      contentTextView.setTextSize(14);
      contentTextView.setTextColor(0xFFDFDFDF);
      contentTextView.setGravity(Gravity.START | Gravity.TOP);
    }

    String content = tab.getFormattedContent();
    contentTextView.setText(content);
    addView(
        contentTextView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
  }
}
