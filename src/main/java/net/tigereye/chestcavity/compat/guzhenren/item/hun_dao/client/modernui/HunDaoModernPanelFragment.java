package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs.ReservedTab;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs.SoulOverviewTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hun Dao Modern UI panel fragment.
 *
 * <p>Phase 7.3: Wide-screen panel layout with full i18n support.
 */
@OnlyIn(Dist.CLIENT)
public class HunDaoModernPanelFragment extends Fragment {

  private static final Logger LOGGER = LoggerFactory.getLogger(HunDaoModernPanelFragment.class);

  private final List<IHunDaoPanelTab> tabs = new ArrayList<>();
  private int activeTabIndex = 0;
  private TabContentView contentView;

  // Phase 7.2: Fixed panel dimensions for wide-screen layout
  private static final int PANEL_WIDTH_DP = 450;
  private static final int PANEL_MIN_HEIGHT_DP = 300;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable DataSet savedInstanceState) {
    var context = requireContext();
    var minecraft = Minecraft.getInstance();

    LOGGER.debug("Opening Hun Dao Modern Panel");

    // Initialize tabs
    tabs.add(new SoulOverviewTab());
    tabs.add(
        new ReservedTab("reserved_1", I18n.get("gui.chestcavity.hun_dao_modern_panel.reserved")));
    tabs.add(
        new ReservedTab("reserved_2", I18n.get("gui.chestcavity.hun_dao_modern_panel.reserved")));

    // Phase 7.2: Main container with fixed width
    var root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    int padding = root.dp(18);
    root.setPadding(padding, padding, padding, padding);

    var background = new ShapeDrawable();
    background.setCornerRadius(root.dp(12));
    background.setColor(0xCC151A1F);
    background.setStroke(root.dp(1), 0xFF4A90E2);
    root.setBackground(background);

    // Title
    var title = new TextView(context);
    title.setText(I18n.get("gui.chestcavity.hun_dao_modern_panel.title"));
    title.setTextSize(18);
    title.setTextColor(0xFFFFFFFF);
    title.setGravity(Gravity.CENTER_HORIZONTAL);
    root.addView(
        title,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Phase 7.2: Tab bar with weighted layout (equal distribution)
    var tabBar = new LinearLayout(context);
    tabBar.setOrientation(LinearLayout.HORIZONTAL);
    var tabBarParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    tabBarParams.topMargin = root.dp(12);
    tabBarParams.bottomMargin = root.dp(12);

    for (int i = 0; i < tabs.size(); i++) {
      IHunDaoPanelTab tab = tabs.get(i);
      if (!tab.isVisible()) {
        continue;
      }

      final int tabIndex = i;
      var tabButton = new Button(context);
      tabButton.setText(tab.getTitle());
      tabButton.setEnabled(tab.isEnabled());
      tabButton.setOnClickListener(v -> switchTab(tabIndex));

      // Phase 7.2: Use weighted layout params for equal width distribution
      var tabButtonParams =
          new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);

      // Add spacing between tabs
      if (i > 0) {
        tabButtonParams.leftMargin = root.dp(8);
      }

      tabBar.addView(tabButton, tabButtonParams);
    }

    root.addView(tabBar, tabBarParams);

    // Phase 7.2: Content area with card-style background
    contentView = new TabContentView(context);
    int contentPadding = root.dp(16);
    contentView.setPadding(contentPadding, contentPadding, contentPadding, contentPadding);

    // Add rounded rectangle card background
    var contentBackground = new ShapeDrawable();
    contentBackground.setCornerRadius(root.dp(8));
    contentBackground.setColor(0xDD1A1F26); // Slightly lighter than main panel
    contentBackground.setStroke(root.dp(1), 0xFF3A7BC8); // Subtle border
    contentView.setBackground(contentBackground);

    // Initialize with the first tab
    if (!tabs.isEmpty()) {
      contentView.setActiveTab(tabs.get(0));
    }

    var contentParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    contentParams.topMargin = root.dp(8);
    contentParams.bottomMargin = root.dp(8);
    root.addView(contentView, contentParams);

    // Close button
    var closeButton = new Button(context);
    closeButton.setText(I18n.get("gui.chestcavity.hun_dao_modern_panel.close"));
    closeButton.setOnClickListener(
        v -> {
          LOGGER.debug("Closing Hun Dao Modern Panel");
          minecraft.execute(() -> minecraft.setScreen(null));
        });
    var closeParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    closeParams.topMargin = root.dp(8);
    closeParams.gravity = Gravity.CENTER_HORIZONTAL;
    root.addView(closeButton, closeParams);

    // Phase 7.2: Set minimum height on root view
    root.setMinimumHeight(root.dp(PANEL_MIN_HEIGHT_DP));

    // Phase 7.2: Center the layout with fixed width
    var layoutParams =
        new FrameLayout.LayoutParams(
            root.dp(PANEL_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
    root.setLayoutParams(layoutParams);

    return root;
  }

  private void switchTab(int index) {
    if (index >= 0 && index < tabs.size()) {
      activeTabIndex = index;
      IHunDaoPanelTab tab = tabs.get(index);
      LOGGER.debug("Switching to tab: {} (index: {})", tab.getTitle(), index);
      // Update content view to render the new active tab
      if (contentView != null) {
        contentView.setActiveTab(tab);
      }
    }
  }

  public IHunDaoPanelTab getActiveTab() {
    if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
      return tabs.get(activeTabIndex);
    }
    return null;
  }
}
