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
 * Custom view for rendering tab content using TextView.
 *
 * <p>Phase 7.1: Implements proper text-based rendering that displays the active tab's content.
 */
@OnlyIn(Dist.CLIENT)
public class TabContentView extends LinearLayout {

  private final TextView contentTextView;

  public TabContentView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);

    contentTextView = new TextView(context);
    contentTextView.setTextSize(14);
    contentTextView.setTextColor(0xFFDFDFDF);
    contentTextView.setGravity(Gravity.START | Gravity.TOP);

    addView(
        contentTextView,
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
  }

  /**
   * Set the active tab whose content should be rendered.
   *
   * @param tab the active tab to render
   */
  public void setActiveTab(@Nullable IHunDaoPanelTab tab) {
    if (tab != null) {
      String content = tab.getFormattedContent();
      contentTextView.setText(content);
    } else {
      contentTextView.setText("");
    }
  }
}
