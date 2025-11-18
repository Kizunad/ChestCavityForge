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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs.ReservedTab;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs.SoulOverviewTab;

/**
 * Hun Dao Modern UI panel fragment.
 *
 * <p>Phase 7: Multi-tab panel for soul system information display.
 */
@OnlyIn(Dist.CLIENT)
public class HunDaoModernPanelFragment extends Fragment {

  private final List<IHunDaoPanelTab> tabs = new ArrayList<>();
  private int activeTabIndex = 0;
  private TabContentView contentView;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable DataSet savedInstanceState) {
    var context = requireContext();
    var minecraft = Minecraft.getInstance();

    // Initialize tabs
    tabs.add(new SoulOverviewTab());
    tabs.add(new ReservedTab("reserved_1", "Reserved"));
    tabs.add(new ReservedTab("reserved_2", "Reserved"));

    // Root container
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
    title.setText("Hun Dao Modern Panel (Phase 7)");
    title.setTextSize(18);
    title.setTextColor(0xFFFFFFFF);
    title.setGravity(Gravity.CENTER_HORIZONTAL);
    root.addView(
        title,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Tab bar
    var tabBar = new LinearLayout(context);
    tabBar.setOrientation(LinearLayout.HORIZONTAL);
    tabBar.setGravity(Gravity.CENTER_HORIZONTAL);
    var tabBarParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

      // Add spacing between tabs
      if (i > 0) {
        var spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(root.dp(8), 1));
        tabBar.addView(spacer);
      }

      tabBar.addView(tabButton);
    }

    root.addView(tabBar, tabBarParams);

    // Tab content area - renders via custom text view
    contentView = new TabContentView(context);
    contentView.setPadding(0, root.dp(20), 0, root.dp(20));
    // Initialize with the first tab
    if (!tabs.isEmpty()) {
      contentView.setActiveTab(tabs.get(0));
    }
    root.addView(
        contentView,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Close button
    var closeButton = new Button(context);
    closeButton.setText("Close Panel");
    closeButton.setOnClickListener(v -> minecraft.execute(() -> minecraft.setScreen(null)));
    var closeParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    closeParams.topMargin = root.dp(8);
    closeParams.gravity = Gravity.CENTER_HORIZONTAL;
    root.addView(closeButton, closeParams);

    // Center the layout
    var layoutParams =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER);
    root.setLayoutParams(layoutParams);

    return root;
  }

  private void switchTab(int index) {
    if (index >= 0 && index < tabs.size()) {
      activeTabIndex = index;
      // Update content view to render the new active tab
      if (contentView != null) {
        contentView.setActiveTab(tabs.get(index));
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
