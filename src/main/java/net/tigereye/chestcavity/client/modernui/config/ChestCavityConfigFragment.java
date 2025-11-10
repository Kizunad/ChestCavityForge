package net.tigereye.chestcavity.client.modernui.config;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewParent;
import icyllis.modernui.widget.AdapterView;
import icyllis.modernui.widget.ArrayAdapter;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.CheckBox;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PagerAdapter;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.SeekBar;
import icyllis.modernui.widget.Spinner;
import icyllis.modernui.widget.TabLayout;
import icyllis.modernui.widget.TextView;
import icyllis.modernui.widget.ViewPager;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.data.PlayerPreferenceClientState;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;
import net.tigereye.chestcavity.client.modernui.config.docs.CategoryTranslations;
import net.tigereye.chestcavity.client.modernui.config.docs.DocEntry;
import net.tigereye.chestcavity.client.modernui.config.docs.DocRegistry;
import net.tigereye.chestcavity.client.modernui.config.network.PlayerPreferenceUpdatePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigActivatePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigForceTeleportPayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigRenamePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetOrderPayload;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarClientData;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarKey;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarKeyBinding;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarState;
import net.tigereye.chestcavity.client.modernui.widget.SimpleSkillSlotView;
import net.tigereye.chestcavity.client.ui.ModernUiClientState;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;

/**
 * Top-level configuration hub for Chest Cavity, rendered with Modern UI. Provides Modern UI
 * configuration hub for Chest Cavity with tabs for home overview, GuScript, and SoulPlayer
 * management.
 */
public class ChestCavityConfigFragment extends Fragment {

  private static final int TAB_COUNT = 5;
  // 引用技能、图鉴页中的图标容器，切页时显式隐藏以规避 SurfaceView 叠绘
  @Nullable private icyllis.modernui.view.View skillIconScrollRef;
  @Nullable private icyllis.modernui.view.View docsIconScrollRef;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable DataSet savedInstanceState) {
    var context = requireContext();
    // 1. 将根布局改为 LinearLayout
    var root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL); // 2. 设置为垂直方向
    root.setLayoutParams(
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    // 防止子视图在切页时越界绘制
    root.setClipToPadding(true);
    root.setClipChildren(true);

    // --- 1. 配置 TabLayout (标签栏) ---
    var tabs = new TabLayout(context);
    tabs.setTabMode(TabLayout.MODE_AUTO);
    tabs.setTabGravity(TabLayout.GRAVITY_CENTER);

    // 监听选项卡切换，控制含 SurfaceView 页的显隐，避免跨页叠绘
    tabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            updateIconSurfaceVisibility(tab.getPosition());
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });

    // 为 Tabs 设置布局参数，高度为自适应
    var tabParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    // 2. 先将 Tabs 添加到 root
    root.addView(tabs, tabParams);

    // --- 2. 配置 ViewPager (内容页) ---
    var pager = new ViewPager(context);
    pager.setId(View.generateViewId());
    pager.setAdapter(new ConfigPagerAdapter());
    // 防越界绘制至相邻页
    pager.setClipToPadding(true);
    pager.setClipChildren(true);

    // 3. 将 ViewPager 链接到 Tabs
    tabs.setupWithViewPager(pager);

    // 4. 为 ViewPager 设置布局参数，使其填充所有剩余空间
    var pagerParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0); // 高度设为 0
    pagerParams.weight = 1.0f; // 设置权重为 1

    // 5. 在 Tabs 之后将 Pager 添加到 root
    root.addView(pager, pagerParams);

    // 初始化一次
    updateIconSurfaceVisibility(0);

    return root;
  }

  private final class ConfigPagerAdapter extends PagerAdapter {

    private static final SoulAIOrders.Order[] ORDER_VALUES = SoulAIOrders.Order.values();

    @Override
    public int getCount() {
      return TAB_COUNT;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
      View layout =
          switch (position) {
            case 0 -> createHomePage(container);
            case 1 -> createGuScriptPage(container);
            case 2 -> createSkillHotbarPage(container);
            case 3 -> createDocsPage(container);
            default -> createSoulPlayerPage(container);
          };
      // Only add if the view doesn't already have a parent to avoid "child already has a parent"
      if (layout.getParent() == null) {
        container.addView(layout);
      }
      return layout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      // Defensive check to avoid removing a view that's not a child of this container
      if (object instanceof View v && v.getParent() == container) {
        container.removeView(v);
      }
      if (position == 2) {
        skillIconScrollRef = null;
      } else if (position == 3) {
        docsIconScrollRef = null;
      }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
      return switch (position) {
        case 0 -> "主页";
        case 1 -> "自定义杀招";
        case 2 -> "蛊虫技能";
        case 3 -> "图鉴";
        default -> "分魂";
      };
    }

    private LinearLayout createHomePage(ViewGroup container) {
      var context = container.getContext();
      var layout = baseLayout(context);
      addHeadline(layout, "Chest Cavity 设置总览", 18);
      addBody(
          layout, "· 规划全局配置入口，未来将同步 ModMenu / NeoForge Config。\n" + "· 在此页面添加常用开关、QoL 选项与快速跳转。");
      PlayerPreferenceClientState.requestSync();

      // 屏蔽 Reaction 调试/提示输出（实时）
      var row = new LinearLayout(context);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      var label = new TextView(context);
      label.setText("显示反应提示/调试输出");
      label.setTextSize(13);
      label.setTextColor(0xFFDEE5F4); // Light color for labels
      row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      var toggle = new CheckBox(context);
      boolean init = true;
      try {
        CCConfig cfg = ChestCavity.config;
        if (cfg != null) init = cfg.REACTION.debugReactions;
      } catch (Throwable ignored) {
      }
      toggle.setChecked(init);
      toggle.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            try {
              var holder = AutoConfig.getConfigHolder(CCConfig.class);
              CCConfig cfg = holder.getConfig();
              cfg.REACTION.debugReactions = isChecked;
              holder.save();
            } catch (Throwable ignored) {
            }
          });
      row.addView(
          toggle,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.addView(
          row,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var rouRow = new LinearLayout(context);
      rouRow.setOrientation(LinearLayout.HORIZONTAL);
      rouRow.setGravity(Gravity.CENTER_VERTICAL);
      var rouLabel = new TextView(context);
      rouLabel.setText("肉白骨：启用被动器官恢复");
      rouLabel.setTextSize(13);
      rouLabel.setTextColor(0xFFDEE5F4);
      rouRow.addView(
          rouLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      var rouToggle = new CheckBox(context);
      boolean rouInit =
          PlayerPreferenceClientState.get(
              PlayerPreferenceOps.ROU_BAIGU_PASSIVE_RESTORATION,
              PlayerPreferenceOps.defaultRouBaiguPassive());
      rouToggle.setChecked(rouInit);
      rouToggle.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            PlayerPreferenceClientState.setLocal(
                PlayerPreferenceOps.ROU_BAIGU_PASSIVE_RESTORATION, isChecked);
            sendPreferenceUpdate(PlayerPreferenceOps.ROU_BAIGU_PASSIVE_RESTORATION, isChecked);
          });
      rouRow.addView(
          rouToggle,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.addView(
          rouRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var rouVacancyRow = new LinearLayout(context);
      rouVacancyRow.setOrientation(LinearLayout.HORIZONTAL);
      rouVacancyRow.setGravity(Gravity.CENTER_VERTICAL);
      var rouVacancyLabel = new TextView(context);
      rouVacancyLabel.setText("肉白骨：胸腔无空位时不生成新器官");
      rouVacancyLabel.setTextSize(13);
      rouVacancyLabel.setTextColor(0xFFDEE5F4);
      rouVacancyRow.addView(
          rouVacancyLabel,
          new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      var rouVacancyToggle = new CheckBox(context);
      boolean rouVacancyInit =
          PlayerPreferenceClientState.get(
              PlayerPreferenceOps.ROU_BAIGU_REQUIRE_EMPTY_SLOT,
              PlayerPreferenceOps.defaultRouBaiguRequireEmpty());
      rouVacancyToggle.setChecked(rouVacancyInit);
      rouVacancyToggle.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            PlayerPreferenceClientState.setLocal(
                PlayerPreferenceOps.ROU_BAIGU_REQUIRE_EMPTY_SLOT, isChecked);
            sendPreferenceUpdate(PlayerPreferenceOps.ROU_BAIGU_REQUIRE_EMPTY_SLOT, isChecked);
          });
      rouVacancyRow.addView(
          rouVacancyToggle,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.addView(
          rouVacancyRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var swordBlockRow = new LinearLayout(context);
      swordBlockRow.setOrientation(LinearLayout.HORIZONTAL);
      swordBlockRow.setGravity(Gravity.CENTER_VERTICAL);
      var swordBlockLabel = new TextView(context);
      swordBlockLabel.setText("飞剑：允许破坏方块");
      swordBlockLabel.setTextSize(13);
      swordBlockLabel.setTextColor(0xFFDEE5F4);
      swordBlockRow.addView(
          swordBlockLabel,
          new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      var swordBlockToggle = new CheckBox(context);
      boolean swordBlockInit =
          PlayerPreferenceClientState.get(
              PlayerPreferenceOps.SWORD_SLASH_BLOCK_BREAK,
              PlayerPreferenceOps.defaultSwordSlashBlockBreak());
      swordBlockToggle.setChecked(swordBlockInit);
      swordBlockToggle.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            PlayerPreferenceClientState.setLocal(
                PlayerPreferenceOps.SWORD_SLASH_BLOCK_BREAK, isChecked);
            sendPreferenceUpdate(PlayerPreferenceOps.SWORD_SLASH_BLOCK_BREAK, isChecked);
          });
      swordBlockRow.addView(
          swordBlockToggle,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.addView(
          swordBlockRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      return layout;
    }

    private LinearLayout createGuScriptPage(ViewGroup container) {
      var context = container.getContext();
      var layout = baseLayout(context);
      addHeadline(layout, "自定义杀招 (GuScript)", 18);
      addBody(layout, "· 预留布局用于编辑与管理 GuScript 方案。\n" + "· 后续可在此嵌入脚本列表、快捷启动与资源统计。");
      return layout;
    }

    private View createSkillHotbarPage(ViewGroup container) {
      var context = container.getContext();
      var scroll = new ScrollView(context);
      scroll.setClipToPadding(true);
      scroll.setFillViewport(true);
      var layout = baseLayout(context);
      scroll.addView(
          layout,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var pageRoot = new FrameLayout(context);
      pageRoot.setClipToPadding(true);
      pageRoot.setClipChildren(true);
      pageRoot.addView(
          scroll,
          new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

      addHeadline(layout, "蛊虫主动技快捷键", 18);
      addBody(
          layout,
          "· 绑定 ModernUI 技能槽后，可在无 GUI 状态下直接释放蛊真人主动技。\n"
              + "· 点击文件夹按钮浏览不同道派的技能。\n"
              + "· 可使用 /testmodernUI keylisten true/false 快速启用或停用监听。");

      var statusView = new TextView(context);
      statusView.setTextSize(12);
      statusView.setTextColor(0xFFB5C7E3);
      var statusParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      statusParams.topMargin = layout.dp(4);
      layout.addView(statusView, statusParams);

      ActiveSkillRegistry.bootstrap();
      ComboSkillRegistry.bootstrap();
      DocRegistry.reload();

      var selectedSlotRef = new AtomicReference<SimpleSkillSlotView>();
      var selectedSkillIdRef = new AtomicReference<ResourceLocation>();

      // Mode switcher: "organs" (蛊虫器官) or "combos" (蛊虫杀招)
      var currentMode = new AtomicReference<String>("organs");

      // Navigation state: [category, subcategory]
      var currentCategory = new AtomicReference<String>("");
      var currentSubcategory = new AtomicReference<String>("");

      // Mode switcher buttons (蛊虫器官 / 蛊虫杀招)
      var modeSwitcherRow = new LinearLayout(context);
      modeSwitcherRow.setOrientation(LinearLayout.HORIZONTAL);
      modeSwitcherRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
      var modeSwitcherParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      modeSwitcherParams.topMargin = layout.dp(12);
      modeSwitcherParams.bottomMargin = layout.dp(8);
      layout.addView(modeSwitcherRow, modeSwitcherParams);

      var organsModeButton = new Button(context);
      organsModeButton.setText("蛊虫器官");
      organsModeButton.setGravity(Gravity.CENTER);
      organsModeButton.setPadding(layout.dp(16), layout.dp(8), layout.dp(16), layout.dp(8));

      var combosModeButton = new Button(context);
      combosModeButton.setText("蛊虫杀招");
      combosModeButton.setGravity(Gravity.CENTER);
      combosModeButton.setPadding(layout.dp(16), layout.dp(8), layout.dp(16), layout.dp(8));

      // Style helper for mode buttons
      Consumer<Button> updateModeButtonStyle =
          button -> {
            boolean isSelected =
                button == organsModeButton
                    ? currentMode.get().equals("organs")
                    : currentMode.get().equals("combos");
            var bgDrawable = new ShapeDrawable();
            if (isSelected) {
              bgDrawable.setColor(0xFF4080FF); // Bright blue for selected
              bgDrawable.setStroke(layout.dp(2), 0xFF80AFFF);
            } else {
              bgDrawable.setColor(0x40406080); // Dimmed for unselected
              bgDrawable.setStroke(layout.dp(1), 0x606BAEFF);
            }
            bgDrawable.setCornerRadius(layout.dp(6));
            button.setBackground(bgDrawable);
            button.setTextColor(isSelected ? 0xFFFFFFFF : 0xFFB5C7E3);
          };

      var modeSwitcherButtonParams = new LinearLayout.LayoutParams(layout.dp(100), layout.dp(36));
      modeSwitcherButtonParams.rightMargin = layout.dp(8);
      modeSwitcherRow.addView(organsModeButton, modeSwitcherButtonParams);
      modeSwitcherRow.addView(combosModeButton, modeSwitcherButtonParams);

      // Initialize button styles
      updateModeButtonStyle.accept(organsModeButton);
      updateModeButtonStyle.accept(combosModeButton);

      // Breadcrumb navigation with clickable segments
      var breadcrumbRow = new LinearLayout(context);
      breadcrumbRow.setOrientation(LinearLayout.HORIZONTAL);
      breadcrumbRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
      var breadcrumbParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      breadcrumbParams.topMargin = layout.dp(8);
      breadcrumbParams.bottomMargin = layout.dp(4);
      layout.addView(breadcrumbRow, breadcrumbParams);

      // Folder container
      var folderGrid = new LinearLayout(context);
      folderGrid.setOrientation(LinearLayout.VERTICAL);
      folderGrid.setGravity(Gravity.START);
      folderGrid.setPadding(folderGrid.dp(4), folderGrid.dp(4), folderGrid.dp(4), folderGrid.dp(4));
      var folderGridParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      folderGridParams.topMargin = layout.dp(8);
      layout.addView(folderGrid, folderGridParams);

      // Icon container
      var iconGrid = new LinearLayout(context);
      iconGrid.setOrientation(LinearLayout.VERTICAL);
      iconGrid.setGravity(Gravity.START);
      iconGrid.setPadding(iconGrid.dp(4), iconGrid.dp(4), iconGrid.dp(4), iconGrid.dp(4));
      var iconGridParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      iconGridParams.topMargin = layout.dp(8);
      layout.addView(iconGrid, iconGridParams);
      ChestCavityConfigFragment.this.skillIconScrollRef = iconGrid;

      record SkillIcon(
          SimpleSkillSlotView view,
          Object entry, // Either ActiveSkillEntry or ComboSkillEntry
          String type, // "active" or "combo"
          String category,
          String subcategory) {}

      var allIconRecords = new ArrayList<SkillIcon>();

      // Forward reference arrays for rebuild function
      final Runnable[] rebuildSkillIconsRef = new Runnable[1];

      // Build skill icons based on current mode
      Runnable rebuildSkillIcons =
          () -> {
            allIconRecords.clear();
            String mode = currentMode.get();

            if (mode.equals("organs")) {
              // Build active skill icons
              var skillToCategoryMap = new java.util.HashMap<ResourceLocation, DocEntry>();
              for (DocEntry doc : DocRegistry.all()) {
                skillToCategoryMap.put(doc.id(), doc);
              }

              var registryEntries = new ArrayList<>(ActiveSkillRegistry.entries());
              registryEntries.sort(Comparator.comparing(e -> e.skillId().toString()));

              for (ActiveSkillRegistry.ActiveSkillEntry entry : registryEntries) {
                ResourceLocation organItem = entry.organId();
                Item item =
                    organItem != null
                        ? net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getOptional(organItem)
                            .orElse(null)
                        : null;
                if (item == null) {
                  continue;
                }

                // Get category info from DocRegistry with smart matching:
                // 1. Try exact skillId match
                // 2. Try organId match (for multi-skill organs like qing_feng_lun_gu/dash)
                // 3. Try ability ID match
                DocEntry docEntry = skillToCategoryMap.get(entry.skillId());
                if (docEntry == null && entry.organId() != null) {
                  docEntry = skillToCategoryMap.get(entry.organId());
                }
                if (docEntry == null && entry.abilityId() != null) {
                  docEntry = skillToCategoryMap.get(entry.abilityId());
                }

                String category = docEntry != null ? docEntry.category() : "";
                String subcategory = docEntry != null ? docEntry.subcategory() : "";
                // 若来自 docs/combo 下的文档导致分类被解析为 "combo"，这里忽略它，
                // 避免在“蛊虫器官”模式下出现额外的 combo 类别按钮。
                if ("combo".equals(category)) {
                  category = "";
                  subcategory = "";
                }

                // If still no category found, try to infer from organId path
                if (category.isEmpty() && entry.organId() != null) {
                  String organPath = entry.organId().toString();
                  // Check if it's a guzhenren organ
                  if (organPath.startsWith("guzhenren:")) {
                    // Assume it's human dao by default for guzhenren organs without docs
                    category = "human";
                    subcategory = ""; // Will show in category root
                  }
                }

                ItemStack iconStack = new ItemStack(item);
                String label = iconStack.getHoverName().getString();
                var slotView =
                    new SimpleSkillSlotView(
                        context,
                        entry.skillId(),
                        iconStack,
                        label,
                        statusView,
                        layout.dp(36),
                        clicked -> {
                          // Add click animation for skill icon
                          animateButtonPress(clicked);

                          SimpleSkillSlotView previous = selectedSlotRef.getAndSet(clicked);
                          if (previous != null && previous != clicked) {
                            previous.setSelected(false);
                          }
                          clicked.setSelected(true);
                          selectedSkillIdRef.set(entry.skillId());
                          statusView.setText(
                              "选中技能：" + entry.skillId() + " ｜ " + entry.description());
                        });
                allIconRecords.add(new SkillIcon(slotView, entry, "active", category, subcategory));
              }
            } else if (mode.equals("combos")) {
              // Build combo skill icons
              var comboEntries = new ArrayList<>(ComboSkillRegistry.entries());
              comboEntries.sort(Comparator.comparing(e -> e.skillId().toString()));

              for (ComboSkillRegistry.ComboSkillEntry entry : comboEntries) {
                // Use the category and subcategory from the entry directly
                String category = entry.category() != null ? entry.category() : "";
                String subcategory = entry.subcategory() != null ? entry.subcategory() : "";

                // Create a fallback ItemStack (use first required organ)
                ItemStack iconStack = ItemStack.EMPTY;
                if (!entry.requiredOrgans().isEmpty()) {
                  ResourceLocation firstOrgan = entry.requiredOrgans().get(0);
                  Item item =
                      net.minecraft.core.registries.BuiltInRegistries.ITEM
                          .getOptional(firstOrgan)
                          .orElse(null);
                  if (item != null) {
                    iconStack = new ItemStack(item);
                  }
                }

                // Use displayName from entry
                String label = entry.displayName();
                var slotView =
                    new SimpleSkillSlotView(
                        context,
                        entry.skillId(),
                        iconStack,
                        entry.iconLocation(), // 使用PNG图标
                        label,
                        statusView,
                        layout.dp(36),
                        clicked -> {
                          // Add click animation for combo skill icon
                          animateButtonPress(clicked);

                          SimpleSkillSlotView previous = selectedSlotRef.getAndSet(clicked);
                          if (previous != null && previous != clicked) {
                            previous.setSelected(false);
                          }
                          clicked.setSelected(true);
                          selectedSkillIdRef.set(entry.skillId());

                          // Show combo skill info with organ requirements
                          var player = Minecraft.getInstance().player;
                          if (player != null) {
                            var checkResult = ComboSkillRegistry.checkOrgans(player, entry);
                            String status = checkResult.canActivate() ? "✓可激活" : "✗未满足";
                            statusView.setText(
                                String.format(
                                    "选中杀招：%s %s | 必需器官：%d/%d | 锚点：%d/%d | %s",
                                    entry.displayName(),
                                    status,
                                    checkResult.equippedRequired(),
                                    checkResult.totalRequired(),
                                    checkResult.equippedOptional(),
                                    checkResult.totalOptional(),
                                    entry.description()));
                          } else {
                            statusView.setText(
                                "选中杀招：" + entry.displayName() + " ｜ " + entry.description());
                          }
                        });
                allIconRecords.add(new SkillIcon(slotView, entry, "combo", category, subcategory));
              }
            }
          };

      // Assign to forward reference array
      rebuildSkillIconsRef[0] = rebuildSkillIcons;

      // Initial build of skill icons
      rebuildSkillIcons.run();

      final int slotSizePx = layout.dp(36);
      final int cellWidthPx = Math.max(slotSizePx + layout.dp(12), layout.dp(72));
      final int folderButtonWidth = layout.dp(120);
      final int folderButtonHeight = layout.dp(40);

      // Use arrays to allow forward references in lambdas
      final Runnable[] renderFoldersRef = new Runnable[1];
      final Runnable[] renderIconsRef = new Runnable[1];
      final Runnable[] updateBreadcrumbRef = new Runnable[1];

      // Render folders based on current navigation level
      Runnable renderFolders =
          () -> {
            folderGrid.removeAllViews();
            String category = currentCategory.get();
            String subcategory = currentSubcategory.get();

            int available = layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();
            if (available <= 0) {
              available = layout.dp(300); // fallback
            }
            int gap = layout.dp(6);
            int colWidth = folderButtonWidth + gap * 2;
            int columns = Math.max(1, available / colWidth);

            if (category.isEmpty()) {
              // Show top-level categories
              var categories =
                  allIconRecords.stream()
                      .map(SkillIcon::category)
                      .filter(c -> !c.isEmpty())
                      .distinct()
                      .sorted()
                      .toList();

              LinearLayout currentRow = null;
              int col = 0;
              for (String cat : categories) {
                if (currentRow == null || col >= columns) {
                  currentRow = new LinearLayout(context);
                  currentRow.setOrientation(LinearLayout.HORIZONTAL);
                  currentRow.setGravity(Gravity.START);
                  var rowParams =
                      new LinearLayout.LayoutParams(
                          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                  if (folderGrid.getChildCount() > 0) {
                    rowParams.topMargin = gap;
                  }
                  folderGrid.addView(currentRow, rowParams);
                  col = 0;
                }

                var folderButton = new Button(context);
                folderButton.setText(CategoryTranslations.getCategoryName(cat));
                folderButton.setGravity(Gravity.CENTER); // Center text

                // Add visual styling to folder buttons
                folderButton.setTextColor(0xFFFFFFFF);
                // Create a drawable background with rounded corners
                var bgDrawable = new ShapeDrawable();
                bgDrawable.setColor(0x50406080); // Semi-transparent blue
                bgDrawable.setCornerRadius(layout.dp(6)); // Rounded corners
                bgDrawable.setStroke(layout.dp(1), 0x806BAEFF); // Light blue border
                folderButton.setBackground(bgDrawable);
                folderButton.setPadding(layout.dp(12), layout.dp(8), layout.dp(12), layout.dp(8));

                // Pop-in animation: start small and scale up
                folderButton.setScaleX(0.3f);
                folderButton.setScaleY(0.3f);
                folderButton.setAlpha(0f);
                int animDelay = col * 50; // Stagger animation for each button
                folderButton.postDelayed(
                    () -> {
                      // Simple animation using postDelayed
                      animateButton(folderButton, 0.3f, 1f, 0f, 1f, 200);
                    },
                    animDelay);

                String finalCat = cat;
                folderButton.setOnClickListener(
                    v -> {
                      // Press animation effect
                      animateButtonPress(v);

                      currentCategory.set(finalCat);
                      currentSubcategory.set("");
                      renderFoldersRef[0].run();
                      renderIconsRef[0].run();
                      updateBreadcrumbRef[0].run();
                    });
                var params = new LinearLayout.LayoutParams(folderButtonWidth, folderButtonHeight);
                params.leftMargin = gap;
                params.rightMargin = gap;
                params.bottomMargin = gap;
                currentRow.addView(folderButton, params);
                col++;
              }
            } else if (subcategory.isEmpty()) {
              // Show subcategories for selected category
              var subcategories =
                  allIconRecords.stream()
                      .filter(icon -> icon.category().equals(category))
                      .map(SkillIcon::subcategory)
                      .filter(sc -> !sc.isEmpty())
                      .distinct()
                      .sorted()
                      .toList();

              LinearLayout currentRow = null;
              int col = 0;
              for (String subcat : subcategories) {
                if (currentRow == null || col >= columns) {
                  currentRow = new LinearLayout(context);
                  currentRow.setOrientation(LinearLayout.HORIZONTAL);
                  currentRow.setGravity(Gravity.START);
                  var rowParams =
                      new LinearLayout.LayoutParams(
                          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                  if (folderGrid.getChildCount() > 0) {
                    rowParams.topMargin = gap;
                  }
                  folderGrid.addView(currentRow, rowParams);
                  col = 0;
                }

                var folderButton = new Button(context);
                folderButton.setText(CategoryTranslations.getSubcategoryName(subcat));
                folderButton.setGravity(Gravity.CENTER); // Center text

                // Add visual styling to folder buttons
                folderButton.setTextColor(0xFFFFFFFF);
                // Create a drawable background with rounded corners
                var bgDrawable = new ShapeDrawable();
                bgDrawable.setColor(0x50406080); // Semi-transparent blue
                bgDrawable.setCornerRadius(layout.dp(6)); // Rounded corners
                bgDrawable.setStroke(layout.dp(1), 0x806BAEFF); // Light blue border
                folderButton.setBackground(bgDrawable);
                folderButton.setPadding(layout.dp(12), layout.dp(8), layout.dp(12), layout.dp(8));

                // Pop-in animation: start small and scale up
                folderButton.setScaleX(0.3f);
                folderButton.setScaleY(0.3f);
                folderButton.setAlpha(0f);
                int animDelay = col * 50; // Stagger animation for each button
                folderButton.postDelayed(
                    () -> {
                      // Simple animation using postDelayed
                      animateButton(folderButton, 0.3f, 1f, 0f, 1f, 200);
                    },
                    animDelay);

                String finalSubcat = subcat;
                folderButton.setOnClickListener(
                    v -> {
                      // Press animation effect
                      animateButtonPress(v);

                      currentSubcategory.set(finalSubcat);
                      renderFoldersRef[0].run();
                      renderIconsRef[0].run();
                      updateBreadcrumbRef[0].run();
                    });
                var params = new LinearLayout.LayoutParams(folderButtonWidth, folderButtonHeight);
                params.leftMargin = gap;
                params.rightMargin = gap;
                params.bottomMargin = gap;
                currentRow.addView(folderButton, params);
                col++;
              }
            } else {
              // At leaf level, no folders to show
              folderGrid.setVisibility(View.GONE);
              return;
            }

            folderGrid.setVisibility(View.VISIBLE);
          };

      // Render icons based on current filter
      Runnable renderIcons =
          () -> {
            String category = currentCategory.get();
            String subcategory = currentSubcategory.get();

            // Filter icons
            List<SkillIcon> filteredIcons;
            if (category.isEmpty()) {
              // Show all or none at root
              filteredIcons = List.of();
            } else if (subcategory.isEmpty()) {
              // Show all skills in category
              filteredIcons =
                  allIconRecords.stream().filter(icon -> icon.category().equals(category)).toList();
            } else {
              // Show skills in subcategory
              filteredIcons =
                  allIconRecords.stream()
                      .filter(
                          icon ->
                              icon.category().equals(category)
                                  && icon.subcategory().equals(subcategory))
                      .toList();
            }

            int available = layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();
            if (available <= 0) {
              return;
            }
            int gap = layout.dp(6);
            int colWidth = cellWidthPx + gap * 2;
            int columns = Math.max(1, available / colWidth);

            iconGrid.removeAllViews();
            if (filteredIcons.isEmpty()) {
              if (!category.isEmpty() && !subcategory.isEmpty()) {
                var emptyView = new TextView(context);
                emptyView.setText("此分类下暂无技能");
                emptyView.setTextSize(13);
                emptyView.setTextColor(0xFFB5C7E3);
                iconGrid.addView(
                    emptyView,
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
              }
              return;
            }

            LinearLayout currentRow = null;
            int col = 0;
            for (SkillIcon icon : filteredIcons) {
              if (currentRow == null || col >= columns) {
                currentRow = new LinearLayout(context);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.START);
                var rowParams =
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (iconGrid.getChildCount() > 0) {
                  rowParams.topMargin = gap;
                }
                iconGrid.addView(currentRow, rowParams);
                col = 0;
              }
              var params =
                  new LinearLayout.LayoutParams(cellWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
              params.leftMargin = gap;
              params.rightMargin = gap;
              params.bottomMargin = gap;
              var slotView = icon.view();
              ViewParent parent = slotView.getParent();
              if (parent instanceof ViewGroup parentGroup) {
                parentGroup.removeView(slotView);
              }
              currentRow.addView(slotView, params);
              icon.view().setSelected(icon.view() == selectedSlotRef.get());

              // Add pop-in animation for skill icons
              slotView.setScaleX(0.3f);
              slotView.setScaleY(0.3f);
              slotView.setAlpha(0f);
              int iconAnimDelay = col * 40; // Stagger animation
              slotView.postDelayed(
                  () -> {
                    animateButton(slotView, 0.3f, 1f, 0f, 1f, 180);
                  },
                  iconAnimDelay);

              col++;
            }

            if (!filteredIcons.isEmpty()) {
              filteredIcons.get(0).view().performClick();
            }
          };

      // Update breadcrumb with clickable navigation segments
      Runnable updateBreadcrumb =
          () -> {
            breadcrumbRow.removeAllViews();

            String category = currentCategory.get();
            String subcategory = currentSubcategory.get();

            // Helper to add breadcrumb segment
            var addSegment =
                new Object() {
                  void add(String text, boolean clickable, Runnable onClick) {
                    var textView = new TextView(context);
                    textView.setText(text);
                    textView.setTextSize(14);
                    textView.setPadding(layout.dp(4), layout.dp(2), layout.dp(4), layout.dp(2));

                    if (clickable) {
                      // Clickable segment (link style)
                      textView.setTextColor(0xFF6BAEFF); // Light blue
                      textView.setOnClickListener(v -> onClick.run());
                    } else {
                      // Current/non-clickable segment
                      textView.setTextColor(0xFFFFFFFF); // White
                    }

                    breadcrumbRow.addView(
                        textView,
                        new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                  }

                  void addSeparator() {
                    var sep = new TextView(context);
                    sep.setText(" > ");
                    sep.setTextSize(14);
                    sep.setTextColor(0xFFB5C7E3);
                    breadcrumbRow.addView(
                        sep,
                        new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                  }
                };

            // Always show "全部" as clickable unless we're at root
            boolean atRoot = category.isEmpty();
            addSegment.add(
                "全部",
                !atRoot,
                () -> {
                  currentCategory.set("");
                  currentSubcategory.set("");
                  renderFoldersRef[0].run();
                  renderIconsRef[0].run();
                  updateBreadcrumbRef[0].run();
                });

            // Show category if selected
            if (!category.isEmpty()) {
              addSegment.addSeparator();
              boolean inSubcategory = !subcategory.isEmpty();
              addSegment.add(
                  CategoryTranslations.getCategoryName(category),
                  inSubcategory,
                  () -> {
                    currentSubcategory.set("");
                    renderFoldersRef[0].run();
                    renderIconsRef[0].run();
                    updateBreadcrumbRef[0].run();
                  });
            }

            // Show subcategory if selected
            if (!subcategory.isEmpty()) {
              addSegment.addSeparator();
              addSegment.add(CategoryTranslations.getSubcategoryName(subcategory), false, null);
            }
          };

      // Assign to arrays to allow forward references
      renderFoldersRef[0] = renderFolders;
      renderIconsRef[0] = renderIcons;
      updateBreadcrumbRef[0] = updateBreadcrumb;

      // Add onClick handlers for mode switcher buttons
      organsModeButton.setOnClickListener(
          v -> {
            if (currentMode.get().equals("organs")) {
              return; // Already in organs mode
            }
            animateButtonPress(v);
            currentMode.set("organs");
            currentCategory.set("");
            currentSubcategory.set("");
            rebuildSkillIconsRef[0].run();
            updateModeButtonStyle.accept(organsModeButton);
            updateModeButtonStyle.accept(combosModeButton);
            renderFoldersRef[0].run();
            renderIconsRef[0].run();
            updateBreadcrumbRef[0].run();
          });

      combosModeButton.setOnClickListener(
          v -> {
            if (currentMode.get().equals("combos")) {
              return; // Already in combos mode
            }
            animateButtonPress(v);
            currentMode.set("combos");
            currentCategory.set("");
            currentSubcategory.set("");
            rebuildSkillIconsRef[0].run();
            updateModeButtonStyle.accept(organsModeButton);
            updateModeButtonStyle.accept(combosModeButton);
            renderFoldersRef[0].run();
            renderIconsRef[0].run();
            updateBreadcrumbRef[0].run();
          });

      // Initial render
      layout.post(
          () -> {
            renderFoldersRef[0].run();
            renderIconsRef[0].run();
            updateBreadcrumbRef[0].run();
          });

      var sections = new java.util.LinkedHashMap<SkillHotbarKey, SkillSection>();
      for (SkillHotbarKey key : SkillHotbarKey.values()) {
        SkillSection section = new SkillSection(context, key, statusView, selectedSkillIdRef::get);
        sections.put(key, section);
        layout.addView(section.root(), section.layoutParams(layout));
      }

      var captureOverlay = new KeyCaptureOverlay(context, statusView, sections);
      pageRoot.addView(
          captureOverlay.root(),
          new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

      var buttonRow = new LinearLayout(context);
      buttonRow.setOrientation(LinearLayout.HORIZONTAL);
      buttonRow.setGravity(Gravity.END);

      var resetButton = new Button(context);
      resetButton.setText("恢复默认");
      resetButton.setOnClickListener(
          v -> {
            SkillHotbarClientData.resetToDefault();
            statusView.setText("快捷键已重置为空。");
          });
      buttonRow.addView(
          resetButton,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var toggleButton = new Button(context);
      toggleButton.setText("当前：" + (ModernUiClientState.isKeyListenEnabled() ? "监听开启" : "监听关闭"));
      toggleButton.setOnClickListener(
          v -> {
            boolean next = !ModernUiClientState.isKeyListenEnabled();
            ModernUiClientState.setKeyListenEnabled(next);
            toggleButton.setText("当前：" + (next ? "监听开启" : "监听关闭"));
            statusView.setText(next ? "已启用 ModernUI 快捷键监听。" : "已关闭 ModernUI 快捷键监听。");
          });
      var toggleParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      toggleParams.leftMargin = buttonRow.dp(8);
      buttonRow.addView(toggleButton, toggleParams);

      var buttonParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      buttonParams.topMargin = layout.dp(12);
      layout.addView(buttonRow, buttonParams);

      SkillHotbarClientData.Listener listener =
          newState ->
              layout.post(
                  () -> {
                    for (SkillSection section : sections.values()) {
                      section.refresh(newState);
                    }
                  });

      layout.addOnAttachStateChangeListener(
          new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
              SkillHotbarClientData.addListener(listener);
              SkillHotbarClientData.addCaptureListener(captureOverlay);
              for (SkillSection section : sections.values()) {
                section.refresh(SkillHotbarClientData.state());
              }
              captureOverlay.onCaptureStatusChanged(null);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
              SkillHotbarClientData.removeListener(listener);
              SkillHotbarClientData.removeCaptureListener(captureOverlay);
              captureOverlay.onCaptureStatusChanged(null);
            }
          });

      for (SkillSection section : sections.values()) {
        section.refresh(SkillHotbarClientData.state());
      }

      captureOverlay.onCaptureStatusChanged(null);

      return pageRoot;
    }

    private View createDocsPage(ViewGroup container) {
      var context = container.getContext();

      var root = new FrameLayout(context);

      var scroll = new ScrollView(context);
      scroll.setClipToPadding(true);
      scroll.setFillViewport(true);
      root.addView(
          scroll,
          new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

      var layout = baseLayout(context);
      scroll.addView(
          layout,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      addHeadline(layout, "器官与技能图鉴", 18);

      var search = new EditText(context);
      search.setHint("搜索（名称 / 标签 / 描述 / id）");
      search.setSingleLine(true);
      var searchParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      searchParams.topMargin = layout.dp(6);
      layout.addView(search, searchParams);

      var splitRow = new LinearLayout(context);
      splitRow.setOrientation(LinearLayout.HORIZONTAL);
      splitRow.setGravity(Gravity.START | Gravity.TOP);
      splitRow.setPadding(splitRow.dp(4), splitRow.dp(6), splitRow.dp(4), splitRow.dp(4));
      var splitParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      splitParams.topMargin = layout.dp(8);
      layout.addView(splitRow, splitParams);
      final int detailPanelWidth = splitRow.dp(320);
      final int detailPanelSpacing = splitRow.dp(12);

      var leftColumn = new LinearLayout(context);
      leftColumn.setOrientation(LinearLayout.VERTICAL);
      var leftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
      leftParams.rightMargin = splitRow.dp(8);
      splitRow.addView(leftColumn, leftParams);

      var spacer = new View(context);
      spacer.setVisibility(View.INVISIBLE);
      var spacerParams =
          new LinearLayout.LayoutParams(
              detailPanelWidth + detailPanelSpacing, ViewGroup.LayoutParams.MATCH_PARENT);
      splitRow.addView(spacer, spacerParams);

      var iconGrid = new LinearLayout(context);
      iconGrid.setOrientation(LinearLayout.VERTICAL);
      iconGrid.setGravity(Gravity.START);
      iconGrid.setPadding(iconGrid.dp(4), iconGrid.dp(4), iconGrid.dp(4), iconGrid.dp(4));
      leftColumn.addView(
          iconGrid,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      ChestCavityConfigFragment.this.docsIconScrollRef = iconGrid;

      var detailScroll = new ScrollView(context);
      detailScroll.setClipToPadding(true);
      detailScroll.setFillViewport(true);
      detailScroll.setScrollbarFadingEnabled(false);

      var detailColumn = new LinearLayout(context);
      detailColumn.setOrientation(LinearLayout.VERTICAL);
      detailColumn.setPadding(
          detailColumn.dp(8), detailColumn.dp(8), detailColumn.dp(8), detailColumn.dp(8));
      ShapeDrawable detailBg = new ShapeDrawable();
      detailBg.setCornerRadius(detailColumn.dp(10));
      detailBg.setColor(0x331C2A3A);
      detailColumn.setBackground(detailBg);
      detailScroll.addView(
          detailColumn,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var detailOverlayParams =
          new FrameLayout.LayoutParams(detailPanelWidth, ViewGroup.LayoutParams.MATCH_PARENT);
      detailOverlayParams.gravity = Gravity.TOP | Gravity.END;
      detailOverlayParams.rightMargin = detailPanelSpacing;
      detailOverlayParams.topMargin = 0;
      root.addView(detailScroll, detailOverlayParams);

      Runnable renderEmpty =
          () -> {
            detailColumn.removeAllViews();
            detailScroll.scrollTo(0, 0);
            var hint = new TextView(context);
            hint.setText("请选择左侧条目查看详情。");
            hint.setTextSize(12);
            hint.setTextColor(0xFFCBD8EC);
            detailColumn.addView(
                hint,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          };

      Consumer<DocEntry> renderDetail =
          entry -> {
            detailColumn.removeAllViews();
            detailScroll.scrollTo(0, 0);

            var titleView = new TextView(context);
            titleView.setText(entry.title());
            titleView.setTextSize(16);
            titleView.setTextColor(0xFFEEF5FF);
            detailColumn.addView(
                titleView,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (!entry.summary().isBlank()) {
              var summaryView = new TextView(context);
              summaryView.setText(entry.summary());
              summaryView.setTextSize(13);
              summaryView.setTextColor(0xFFC8D6F2);
              summaryView.setPadding(0, detailColumn.dp(4), 0, 0);
              detailColumn.addView(
                  summaryView,
                  new LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            for (String line : entry.details()) {
              if (line == null || line.isBlank()) {
                continue;
              }
              var lineView = new TextView(context);
              lineView.setText("· " + line);
              lineView.setTextSize(12);
              lineView.setTextColor(0xFFB7C6E8);
              lineView.setPadding(0, detailColumn.dp(3), 0, 0);
              detailColumn.addView(
                  lineView,
                  new LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            if (!entry.tags().isEmpty()) {
              var tagsView = new TextView(context);
              tagsView.setText("标签：" + String.join(" ｜ ", entry.tags()));
              tagsView.setTextSize(11);
              tagsView.setTextColor(0xFFA7BADF);
              tagsView.setPadding(0, detailColumn.dp(6), 0, 0);
              detailColumn.addView(
                  tagsView,
                  new LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
          };

      DocRegistry.reload();
      var baseEntries = new ArrayList<>(DocRegistry.all());
      baseEntries.sort(Comparator.comparing(e -> e.title().toLowerCase(Locale.ROOT)));

      record IconRecord(DocEntry entry, SimpleSkillSlotView view) {}

      var iconRecords = new ArrayList<IconRecord>();
      var selectedIdRef = new AtomicReference<ResourceLocation>();
      var displayedRef = new AtomicReference<List<DocEntry>>(List.copyOf(baseEntries));

      final int slotSizePx = layout.dp(36);
      final int cellWidthPx = Math.max(slotSizePx + layout.dp(12), layout.dp(72));
      final int gap = layout.dp(6);

      Consumer<List<DocEntry>> rebuild =
          entries -> {
            displayedRef.set(List.copyOf(entries));
            iconRecords.clear();
            iconGrid.removeAllViews();

            if (entries.isEmpty()) {
              var emptyView = new TextView(context);
              emptyView.setText("未找到匹配条目");
              emptyView.setTextSize(12);
              emptyView.setTextColor(0xFF9FB4D9);
              iconGrid.addView(
                  emptyView,
                  new LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
              selectedIdRef.set(null);
              renderEmpty.run();
              return;
            }

            int available =
                iconGrid.getWidth() - iconGrid.getPaddingLeft() - iconGrid.getPaddingRight();
            if (available <= 0) {
              available =
                  leftColumn.getWidth()
                      - leftColumn.getPaddingLeft()
                      - leftColumn.getPaddingRight();
            }
            if (available <= 0) {
              available = layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();
            }
            int colWidth = cellWidthPx + gap * 2;
            int columns = available > 0 ? Math.max(1, available / colWidth) : 1;
            LinearLayout currentRow = null;
            int col = 0;
            ResourceLocation currentSelected = selectedIdRef.get();
            boolean foundSelection = false;

            for (DocEntry entry : entries) {
              var slot =
                  new SimpleSkillSlotView(
                      context,
                      entry.id(),
                      entry.icon(),
                      entry.iconTexture(),
                      entry.title(),
                      null,
                      slotSizePx,
                      clicked -> {
                        selectedIdRef.set(entry.id());
                        for (IconRecord record : iconRecords) {
                          record.view().setSelected(record.entry().id().equals(entry.id()));
                        }
                        renderDetail.accept(entry);
                      });

              boolean isSelected = currentSelected != null && currentSelected.equals(entry.id());
              slot.setSelected(isSelected);
              if (isSelected) {
                foundSelection = true;
              }

              if (currentRow == null || col >= columns) {
                currentRow = new LinearLayout(context);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.START);
                var rowParams =
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (iconGrid.getChildCount() > 0) {
                  rowParams.topMargin = gap;
                }
                iconGrid.addView(currentRow, rowParams);
                col = 0;
              }

              var cellParams =
                  new LinearLayout.LayoutParams(cellWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
              cellParams.leftMargin = gap;
              cellParams.rightMargin = gap;
              cellParams.bottomMargin = gap;
              currentRow.addView(slot, cellParams);

              iconRecords.add(new IconRecord(entry, slot));
              col++;
            }

            if (!foundSelection) {
              if (!iconRecords.isEmpty()) {
                IconRecord first = iconRecords.get(0);
                selectedIdRef.set(first.entry().id());
                first.view().setSelected(true);
                renderDetail.accept(first.entry());
              } else {
                selectedIdRef.set(null);
                renderEmpty.run();
              }
            } else {
              for (IconRecord record : iconRecords) {
                if (record.entry().id().equals(selectedIdRef.get())) {
                  renderDetail.accept(record.entry());
                  break;
                }
              }
            }
          };

      Runnable alignDetailPanel =
          () -> {
            FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) detailScroll.getLayoutParams();
            int desiredTop = splitRow.getTop();
            if (params.topMargin != desiredTop) {
              params.topMargin = desiredTop;
              detailScroll.setLayoutParams(params);
            }
          };
      scroll.post(alignDetailPanel);

      layout.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int newWidth = right - left;
            int oldWidth = oldRight - oldLeft;
            if (newWidth != oldWidth) {
              List<DocEntry> current = displayedRef.get();
              if (current != null) {
                rebuild.accept(new ArrayList<>(current));
              }
            }
            alignDetailPanel.run();
          });

      iconGrid.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int newWidth = right - left;
            int oldWidth = oldRight - oldLeft;
            if (newWidth != oldWidth) {
              List<DocEntry> current = displayedRef.get();
              if (current != null) {
                rebuild.accept(new ArrayList<>(current));
              }
            }
          });

      splitRow.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
              alignDetailPanel.run());

      Consumer<String> applyQuery =
          query -> {
            String text = query == null ? "" : query.trim();
            List<DocEntry> filtered =
                text.isEmpty()
                    ? new ArrayList<>(baseEntries)
                    : new ArrayList<>(DocRegistry.search(text));
            rebuild.accept(filtered);
          };

      boolean watcherAttached = false;
      try {
        Class<?> watcherClass = Class.forName("icyllis.modernui.text.TextWatcher");
        Object watcher =
            Proxy.newProxyInstance(
                watcherClass.getClassLoader(),
                new Class<?>[] {watcherClass},
                (proxy, method, args) -> {
                  if ("onTextChanged".equals(method.getName()) && args != null && args.length > 0) {
                    CharSequence seq = (CharSequence) args[0];
                    applyQuery.accept(seq == null ? "" : seq.toString());
                  }
                  return null;
                });
        search.getClass().getMethod("addTextChangedListener", watcherClass).invoke(search, watcher);
        watcherAttached = true;
      } catch (Throwable ignored) {
      }

      if (!watcherAttached) {
        search.setOnKeyListener(
            (v, keyCode, event) -> {
              CharSequence text = search.getText();
              applyQuery.accept(text == null ? "" : text.toString());
              return false;
            });
      }

      applyQuery.accept("");

      return root;
    }

    private final class SkillSection {

      private final SkillHotbarKey key;
      private final LinearLayout root;
      private final LinearLayout listContainer;
      private final EditText input;
      private final TextView statusView;
      private final TextView keyLabel;
      private final Button changeKeyButton;
      private final Button iconBindButton;
      private final icyllis.modernui.core.Context context;
      private final Supplier<ResourceLocation> selectedSkillSupplier;

      SkillSection(
          icyllis.modernui.core.Context context,
          SkillHotbarKey key,
          TextView statusView,
          Supplier<ResourceLocation> selectedSkillSupplier) {
        this.context = context;
        this.key = key;
        this.statusView = statusView;
        this.selectedSkillSupplier = selectedSkillSupplier;
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(root.dp(12), root.dp(10), root.dp(12), root.dp(10));
        ShapeDrawable background = new ShapeDrawable();
        background.setCornerRadius(root.dp(10));
        background.setColor(0xFF1C2A3A);
        root.setBackground(background);

        var header = new TextView(context);
        header.setText("键位 " + (key.ordinal() + 1));
        header.setTextSize(16);
        root.addView(
            header,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        var keyRow = new LinearLayout(context);
        keyRow.setOrientation(LinearLayout.HORIZONTAL);
        keyRow.setGravity(Gravity.CENTER_VERTICAL);

        keyLabel = new TextView(context);
        keyLabel.setTextSize(13);
        keyLabel.setTextColor(0xFFDEE5F4);
        keyRow.addView(
            keyLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        changeKeyButton = new Button(context);
        changeKeyButton.setText("设置键位");
        keyRow.addView(
            changeKeyButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        var keyParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        keyParams.topMargin = root.dp(6);
        root.addView(keyRow, keyParams);

        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        var listParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        listParams.topMargin = root.dp(6);
        root.addView(listContainer, listParams);

        var inputRow = new LinearLayout(context);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(context);
        input.setHint("skillId");
        inputRow.addView(
            input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        var bindButton = new Button(context);
        bindButton.setText("绑定");
        inputRow.addView(
            bindButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        iconBindButton = new Button(context);
        iconBindButton.setText("图标绑定");
        var iconParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iconParams.leftMargin = inputRow.dp(6);
        inputRow.addView(iconBindButton, iconParams);

        var clearButton = new Button(context);
        clearButton.setText("清空");
        var clearParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clearParams.leftMargin = inputRow.dp(6);
        inputRow.addView(clearButton, clearParams);

        var inputParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = root.dp(8);
        root.addView(inputRow, inputParams);

        bindButton.setOnClickListener(
            v -> {
              String raw = input.getText().toString().trim();
              if (raw.isEmpty()) {
                statusView.setText("请输入 skillId（示例：guzhenren:jiu_chong）");
                return;
              }
              ResourceLocation id = ResourceLocation.tryParse(raw);
              if (id == null) {
                statusView.setText("无效的 skillId: " + raw);
                return;
              }
              SkillHotbarClientData.addSkill(key, id);
              input.setText("");
              statusView.setText("已绑定 " + key.label() + " -> " + id);
            });

        iconBindButton.setOnClickListener(
            v -> {
              ResourceLocation selected = selectedSkillSupplier.get();
              if (selected == null) {
                statusView.setText("请先点击上方技能图标进行选择。");
                return;
              }
              if (SkillHotbarClientData.state().getSkills(key).contains(selected)) {
                statusView.setText(selected + " 已绑定在 " + key.label() + "。");
                return;
              }
              SkillHotbarClientData.addSkill(key, selected);
              statusView.setText("已绑定 " + key.label() + " -> " + selected);
            });

        clearButton.setOnClickListener(
            v -> {
              SkillHotbarClientData.clearKey(key);
              statusView.setText("已清空 " + key.label() + " 快捷键。");
            });

        changeKeyButton.setOnClickListener(
            v -> {
              if (!changeKeyButton.isEnabled()) {
                return;
              }
              boolean accepted =
                  SkillHotbarClientData.requestKeyCapture(
                      key,
                      binding -> {
                        SkillHotbarKeyBinding safe =
                            binding == null ? SkillHotbarKeyBinding.UNBOUND : binding;
                        SkillHotbarClientData.setBinding(key, safe);
                        root.post(
                            () -> {
                              changeKeyButton.setEnabled(true);
                              statusView.setText(
                                  "已将 "
                                      + key.label()
                                      + " 绑定到 "
                                      + SkillHotbarClientData.describeBinding(safe));
                              refresh(SkillHotbarClientData.state());
                            });
                      });
              if (accepted) {
                changeKeyButton.setEnabled(false);
                statusView.setText("正在录制 " + key.label() + " 快捷键…");
              } else {
                statusView.setText("键位捕获已在进行中，请先完成再重试。");
              }
            });
      }

      View root() {
        return root;
      }

      LinearLayout.LayoutParams layoutParams(LinearLayout parent) {
        var params =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = parent.dp(10);
        return params;
      }

      void refresh(SkillHotbarState state) {
        listContainer.removeAllViews();
        keyLabel.setText(
            "当前键位：" + SkillHotbarClientData.describeBinding(SkillHotbarClientData.getBinding(key)));

        if (!SkillHotbarClientData.isCapturing()) {
          changeKeyButton.setEnabled(true);
        }

        List<ResourceLocation> skills = state.getSkills(key);
        if (skills.isEmpty()) {
          var placeholder = new TextView(context);
          placeholder.setText("尚未绑定技能");
          placeholder.setTextSize(13);
          placeholder.setTextColor(0xFF8AA2C2);
          listContainer.addView(
              placeholder,
              new LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          return;
        }

        for (ResourceLocation skillId : skills) {
          var row = new LinearLayout(context);
          row.setOrientation(LinearLayout.HORIZONTAL);
          row.setGravity(Gravity.CENTER_VERTICAL);

          var label = new TextView(context);
          label.setText(skillId.toString());
          label.setTextSize(13);
          row.addView(
              label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

          var removeButton = new Button(context);
          removeButton.setText("移除");
          removeButton.setOnClickListener(
              v -> {
                SkillHotbarClientData.removeSkill(key, skillId);
                statusView.setText("已移除 " + skillId);
              });
          row.addView(
              removeButton,
              new LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

          var rowParams =
              new LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
          rowParams.bottomMargin = row.dp(4);
          listContainer.addView(row, rowParams);
        }
      }

      void updateCaptureState(SkillHotbarClientData.KeyCaptureStatus status) {
        if (status == null || !status.active()) {
          changeKeyButton.setEnabled(true);
          return;
        }
        changeKeyButton.setEnabled(false);
      }
    }

    /**
     * Semi-transparent modal overlay rendered during key capture. It mirrors the live preview and
     * exposes explicit actions (save / retry / cancel) so the player remains in control of the
     * binding flow.
     */
    private final class KeyCaptureOverlay implements SkillHotbarClientData.CaptureListener {

      private final FrameLayout root;
      private final TextView titleView;
      private final TextView combinationView;
      private final TextView hintView;
      private final TextView countdownView;
      private final Button saveButton;
      private final Button retryButton;
      private final Button cancelButton;
      private final TextView statusView;
      private final Map<SkillHotbarKey, SkillSection> sections;

      KeyCaptureOverlay(
          icyllis.modernui.core.Context context,
          TextView statusView,
          Map<SkillHotbarKey, SkillSection> sections) {
        this.statusView = statusView;
        this.sections = sections;
        root = new FrameLayout(context);
        root.setVisibility(View.GONE);
        root.setClickable(true);
        root.setFocusable(true);
        ShapeDrawable overlayBackground = new ShapeDrawable();
        overlayBackground.setColor(0x99000000);
        root.setBackground(overlayBackground);

        var card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(card.dp(18), card.dp(18), card.dp(18), card.dp(16));
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        ShapeDrawable background = new ShapeDrawable();
        background.setCornerRadius(card.dp(12));
        background.setColor(0xFF1F2F42);
        background.setStroke(card.dp(1), 0xFF4A90E2);
        card.setBackground(background);

        titleView = new TextView(context);
        titleView.setTextSize(16);
        titleView.setTextColor(0xFFE1EBFF);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(
            titleView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        combinationView = new TextView(context);
        combinationView.setTextSize(14);
        combinationView.setTextColor(0xFFB4C6E6);
        combinationView.setGravity(Gravity.CENTER_HORIZONTAL);
        combinationView.setPadding(0, card.dp(10), 0, card.dp(6));
        card.addView(
            combinationView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        hintView = new TextView(context);
        hintView.setTextSize(13);
        hintView.setTextColor(0xFFDEE5F4);
        hintView.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(
            hintView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        countdownView = new TextView(context);
        countdownView.setTextSize(12);
        countdownView.setTextColor(0xFFA5B3C9);
        countdownView.setGravity(Gravity.CENTER_HORIZONTAL);
        countdownView.setPadding(0, card.dp(6), 0, card.dp(10));
        card.addView(
            countdownView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        var buttonRow = new LinearLayout(context);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER_HORIZONTAL);
        buttonRow.setPadding(0, card.dp(6), 0, 0);

        saveButton = new Button(context);
        saveButton.setText("保存");
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(
            v -> {
              if (!SkillHotbarClientData.confirmKeyCapture()) {
                statusView.setText("保存失败，请重新尝试录制。");
              }
            });
        buttonRow.addView(
            saveButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        retryButton = new Button(context);
        retryButton.setText("重新录制");
        retryButton.setOnClickListener(v -> SkillHotbarClientData.restartKeyCapture());
        var retryParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryParams.leftMargin = buttonRow.dp(6);
        buttonRow.addView(retryButton, retryParams);

        cancelButton = new Button(context);
        cancelButton.setText("取消");
        cancelButton.setOnClickListener(
            v -> {
              SkillHotbarClientData.cancelKeyCapture();
              statusView.setText("已取消快捷键录制。");
            });
        var cancelParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cancelParams.leftMargin = buttonRow.dp(6);
        buttonRow.addView(cancelButton, cancelParams);

        card.addView(
            buttonRow,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        var cardParams =
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(card, cardParams);

        update(null);
      }

      View root() {
        return root;
      }

      @Override
      public void onCaptureStatusChanged(SkillHotbarClientData.KeyCaptureStatus status) {
        root.post(() -> update(status));
      }

      private void update(SkillHotbarClientData.KeyCaptureStatus status) {
        for (SkillSection section : sections.values()) {
          section.updateCaptureState(status);
        }

        if (status == null) {
          root.setVisibility(View.GONE);
          saveButton.setEnabled(false);
          combinationView.setText("尚未记录");
          hintView.setText("等待开始录制。");
          countdownView.setText("");
          return;
        }

        if (!status.active()) {
          root.setVisibility(View.GONE);
          saveButton.setEnabled(false);
          if (status.timedOut()) {
            statusView.setText("录制超时，已取消。");
          } else if (status.cancelled()) {
            statusView.setText("已取消快捷键录制。");
          }
          return;
        }

        root.setVisibility(View.VISIBLE);
        SkillHotbarKey target = status.target();
        titleView.setText("录制快捷键：" + (target != null ? target.label() : "未知"));

        if (status.preview() != null) {
          combinationView.setText(SkillHotbarClientData.describeBinding(status.preview()));
        } else {
          combinationView.setText("尚未记录");
        }

        if (status.waitingForInitialRelease()) {
          hintView.setText("请先释放所有按键以开始录制。");
          countdownView.setText("剩余时间：--");
        } else if (status.awaitingConfirmation()) {
          hintView.setText("捕获完成，点击“保存”确认或“重新录制”。");
          countdownView.setText("剩余时间：--");
        } else if (!status.hasCandidate()) {
          hintView.setText("请按下目标快捷键组合…");
          countdownView.setText("剩余时间：" + Math.max(0L, status.inactivityMillisRemaining()) + " ms");
        } else {
          hintView.setText("已检测到组合，松开所有按键以完成录制。");
          countdownView.setText("剩余时间：" + Math.max(0L, status.inactivityMillisRemaining()) + " ms");
        }

        saveButton.setEnabled(status.awaitingConfirmation() && status.preview() != null);
      }
    }

    private View createSoulPlayerPage(ViewGroup container) {
      var context = container.getContext();
      var scroll = new ScrollView(context);
      var layout = baseLayout(context);
      scroll.addView(
          layout,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      addHeadline(layout, "分魂系统", 18);
      addRefreshRow(layout);

      layout.addOnAttachStateChangeListener(
          new View.OnAttachStateChangeListener() {
            final SoulConfigDataClient.Listener listener =
                snapshot -> layout.post(() -> populateSoulLayout(layout, snapshot));

            @Override
            public void onViewAttachedToWindow(View v) {
              SoulConfigDataClient.INSTANCE.addListener(listener);
              populateSoulLayout(layout, SoulConfigDataClient.INSTANCE.snapshot());
              if (SoulConfigDataClient.INSTANCE.snapshot().isEmpty()) {
                SoulConfigDataClient.INSTANCE.requestSync();
              }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
              SoulConfigDataClient.INSTANCE.removeListener(listener);
            }
          });

      populateSoulLayout(layout, SoulConfigDataClient.INSTANCE.snapshot());
      if (SoulConfigDataClient.INSTANCE.snapshot().isEmpty()) {
        SoulConfigDataClient.INSTANCE.requestSync();
      }

      return scroll;
    }

    private void addRefreshRow(LinearLayout layout) {
      var row = new LinearLayout(layout.getContext());
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.END);
      var refresh = new Button(layout.getContext());
      refresh.setText("刷新数据");
      refresh.setOnClickListener(v -> SoulConfigDataClient.INSTANCE.requestSync());
      row.addView(
          refresh,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      var params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = layout.dp(6);
      layout.addView(row, params);
    }

    private void populateSoulLayout(LinearLayout layout, SoulConfigDataClient.Snapshot snapshot) {
      // Preserve headline (index 0) and refresh row (index 1)
      while (layout.getChildCount() > 2) {
        layout.removeViewAt(2);
      }

      if (snapshot.entries().isEmpty()) {
        addBody(layout, "正在加载分魂数据… 如果长时间无响应，请确认服务器已启用 /soul 控制。");
        return;
      }

      for (SoulConfigDataClient.SoulEntry entry : snapshot.entries()) {
        layout.addView(createSoulCard(layout.getContext(), entry), soulCardLayoutParams(layout));
      }

      // 底部设置栏（全局）：掉落物吸取、跟随/传送调优
      layout.addView(createBottomSettings(layout), bottomSettingsLayoutParams(layout));
    }

    private LinearLayout.LayoutParams soulCardLayoutParams(LinearLayout parent) {
      var params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = parent.dp(10);
      return params;
    }

    private View createSoulCard(
        icyllis.modernui.core.Context context, SoulConfigDataClient.SoulEntry entry) {
      var card = new LinearLayout(context);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(card.dp(14), card.dp(12), card.dp(14), card.dp(12));
      ShapeDrawable background = new ShapeDrawable();
      background.setCornerRadius(card.dp(10));
      background.setColor(entry.active() ? 0xFF1F3B4D : 0xFF1B2836);
      card.setBackground(background);

      var titleRow = new LinearLayout(context);
      titleRow.setOrientation(LinearLayout.HORIZONTAL);
      titleRow.setGravity(Gravity.CENTER_VERTICAL);

      var nameView = new TextView(context);
      nameView.setText(entry.displayName() + (entry.active() ? "  (当前附身)" : ""));
      nameView.setTextSize(16);
      nameView.setGravity(Gravity.START);
      titleRow.addView(
          nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

      var editButton = new Button(context);
      editButton.setText("✏");
      editButton.setMinimumWidth(editButton.dp(36));
      editButton.setEnabled(!entry.active());
      titleRow.addView(
          editButton,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      card.addView(
          titleRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var editRow = new LinearLayout(context);
      editRow.setOrientation(LinearLayout.HORIZONTAL);
      editRow.setGravity(Gravity.CENTER_VERTICAL);
      editRow.setVisibility(View.GONE);

      var editInput = new EditText(context);
      editInput.setSingleLine();
      editInput.setText(entry.displayName());
      editRow.addView(
          editInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

      var saveRename = new Button(context);
      saveRename.setText("保存");
      editRow.addView(
          saveRename,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var cancelRename = new Button(context);
      cancelRename.setText("取消");
      editRow.addView(
          cancelRename,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var editParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      editParams.topMargin = card.dp(6);
      card.addView(editRow, editParams);

      editButton.setOnClickListener(
          v -> {
            if (entry.active()) {
              return;
            }
            boolean show = editRow.getVisibility() == View.GONE;
            if (show) {
              editInput.setText(entry.displayName());
              editRow.setVisibility(View.VISIBLE);
              editInput.requestFocus();
            } else {
              editRow.setVisibility(View.GONE);
            }
          });

      saveRename.setOnClickListener(
          v -> {
            if (entry.active()) {
              editRow.setVisibility(View.GONE);
              return;
            }
            String candidate = editInput.getText().toString().trim();
            if (candidate.length() > 16) {
              candidate = candidate.substring(0, 16);
            }
            if (!candidate.equals(entry.displayName())) {
              requestRename(entry.soulId(), candidate);
            }
            editRow.setVisibility(View.GONE);
          });

      cancelRename.setOnClickListener(v -> editRow.setVisibility(View.GONE));

      addCardLine(card, "UUID: " + shortUuid(entry.soulId()));

      double maxHealth = entry.maxHealth();
      addCardLine(
          card,
          String.format(
              java.util.Locale.ROOT,
              "生命值: %.1f / %.1f (吸收 %.1f)",
              entry.health(),
              maxHealth,
              entry.absorption()));

      addCardLine(
          card,
          String.format(
              java.util.Locale.ROOT, "饱食度: %d  饱和 %.1f", entry.food(), entry.saturation()));

      addCardLine(
          card,
          String.format(
              java.util.Locale.ROOT,
              "经验等级: %d (进度 %.0f%%)",
              entry.xpLevel(),
              entry.xpProgress() * 100f));

      var orderRow = new LinearLayout(context);
      orderRow.setOrientation(LinearLayout.HORIZONTAL);
      orderRow.setGravity(Gravity.CENTER_VERTICAL);
      var orderLabel = new TextView(context);
      orderLabel.setText("指令：");
      orderLabel.setTextSize(13);
      orderRow.addView(
          orderLabel,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var spinner = new Spinner(context);
      spinner.setMinimumWidth(spinner.dp(160));
      String[] labels = new String[ORDER_VALUES.length];
      for (int i = 0; i < ORDER_VALUES.length; i++) {
        labels[i] = orderLabel(ORDER_VALUES[i]);
      }
      var adapter = new ArrayAdapter<>(context, labels);
      spinner.setAdapter(adapter);
      spinner.setSelection(entry.order().ordinal());
      spinner.setEnabled(!entry.active());
      spinner.setOnItemSelectedListener(
          new AdapterView.OnItemSelectedListener() {
            private boolean ignore = true;

            @Override
            public void onItemSelected(
                @NonNull AdapterView<?> parent, View view, int position, long id) {
              if (entry.active()) {
                return;
              }
              if (ignore) {
                ignore = false;
                return;
              }
              SoulAIOrders.Order order = ORDER_VALUES[position];
              requestOrderChange(entry.soulId(), order);
            }

            @Override
            public void onNothingSelected(@NonNull AdapterView<?> parent) {}
          });
      orderRow.addView(
          spinner,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      var orderParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      orderParams.topMargin = card.dp(8);
      card.addView(orderRow, orderParams);

      var actions = new LinearLayout(context);
      actions.setOrientation(LinearLayout.HORIZONTAL);
      actions.setGravity(Gravity.END);
      var teleportButton = new Button(context);
      teleportButton.setText("强制传送");
      teleportButton.setEnabled(!entry.owner());
      teleportButton.setOnClickListener(
          v -> {
            if (!entry.owner()) {
              requestForceTeleport(entry.soulId());
            }
          });
      var tpParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      tpParams.rightMargin = actions.dp(6);
      actions.addView(teleportButton, tpParams);

      var button = new Button(context);
      if (entry.active()) {
        button.setText("当前附身");
        button.setEnabled(false);
      } else {
        button.setText(entry.owner() ? "切回本体" : "附身");
        button.setOnClickListener(
            v -> {
              requestActivate(entry.soulId());
              button.setEnabled(false);
            });
      }
      actions.addView(
          button,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      var params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = card.dp(10);
      card.addView(actions, params);

      return card;
    }

    private LinearLayout.LayoutParams bottomSettingsLayoutParams(LinearLayout parent) {
      var params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = parent.dp(14);
      params.bottomMargin = parent.dp(18);
      return params;
    }

    private View createBottomSettings(LinearLayout parent) {
      var context = parent.getContext();
      var panel = new LinearLayout(context);
      panel.setOrientation(LinearLayout.VERTICAL);
      panel.setPadding(panel.dp(10), panel.dp(10), panel.dp(10), panel.dp(10));
      ShapeDrawable bg = new ShapeDrawable();
      bg.setCornerRadius(panel.dp(10));
      bg.setColor(0xFF17202A);
      panel.setBackground(bg);

      // 标题 + 保存按钮
      var headerRow = new LinearLayout(context);
      headerRow.setOrientation(LinearLayout.HORIZONTAL);
      headerRow.setGravity(Gravity.CENTER_VERTICAL);
      var title = new TextView(context);
      title.setText("设置");
      title.setTextSize(16);
      headerRow.addView(
          title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      var saveButton = new Button(context);
      saveButton.setText("保存");
      saveButton.setEnabled(false);
      headerRow.addView(
          saveButton,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      panel.addView(
          headerRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      // ---- 本地暂存状态 ----
      var vacuumInit = SoulConfigDataClient.INSTANCE.vacuum();
      final boolean[] baseVacEnabled = {vacuumInit.enabled()};
      final double[] baseVacRadius = {clampVacuumRadius(vacuumInit.radius())};
      final boolean[] stagedVacEnabled = {baseVacEnabled[0]};
      final double[] stagedVacRadius = {baseVacRadius[0]};

      var initTuning = SoulConfigDataClient.INSTANCE.followTp();
      final boolean[] baseTpEnabled = {initTuning.teleportEnabled()};
      final double[] baseFollowDist = {clampFollowDist(initTuning.followDist())};
      final double[] baseTeleportDist = {clampTeleportDist(initTuning.teleportDist())};
      final boolean[] stagedTpEnabled = {baseTpEnabled[0]};
      final double[] stagedFollowDist = {baseFollowDist[0]};
      final double[] stagedTeleportDist = {baseTeleportDist[0]};

      Runnable updateSaveEnabled =
          () -> {
            boolean changed =
                stagedVacEnabled[0] != baseVacEnabled[0]
                    || Math.abs(stagedVacRadius[0] - baseVacRadius[0]) > 1.0e-6
                    || stagedTpEnabled[0] != baseTpEnabled[0]
                    || Math.abs(stagedFollowDist[0] - baseFollowDist[0]) > 1.0e-6
                    || Math.abs(stagedTeleportDist[0] - baseTeleportDist[0]) > 1.0e-6;
            saveButton.setEnabled(changed);
          };

      // 吸取开关
      var row1 = new LinearLayout(context);
      row1.setOrientation(LinearLayout.HORIZONTAL);
      row1.setGravity(Gravity.CENTER_VERTICAL);
      var label1 = new TextView(context);
      label1.setText("掉落物吸取");
      label1.setTextSize(13);
      row1.addView(
          label1, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      var toggle = new CheckBox(context);
      toggle.setChecked(stagedVacEnabled[0]);
      row1.addView(
          toggle,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      panel.addView(
          row1,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      // 半径滑动条
      var row2 = new LinearLayout(context);
      row2.setOrientation(LinearLayout.VERTICAL);
      var label2 = new TextView(context);
      label2.setText(String.format(java.util.Locale.ROOT, "吸取半径: %.1f", stagedVacRadius[0]));
      label2.setTextSize(13);
      row2.addView(
          label2,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      var seek = new SeekBar(context);
      seek.setMax(235); // 映射 0.5..24.0 -> 0..235 (步长0.1)
      int progress = (int) Math.round((stagedVacRadius[0] - 0.5) * 10.0);
      seek.setProgress(progress);
      row2.addView(
          seek,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      panel.addView(
          row2,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      toggle.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            stagedVacEnabled[0] = isChecked;
            updateSaveEnabled.run();
          });
      seek.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
              double radius = 0.5 + (progress / 10.0);
              label2.setText(String.format(java.util.Locale.ROOT, "吸取半径: %.1f", radius));
              if (fromUser) {
                stagedVacRadius[0] = radius;
                updateSaveEnabled.run();
              }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
          });
      // --- 分魂跟随/传送 ---
      addHeadline(panel, "分魂跟随/传送", 15);
      var tpRow = new LinearLayout(context);
      tpRow.setOrientation(LinearLayout.HORIZONTAL);
      tpRow.setGravity(Gravity.CENTER_VERTICAL);
      var tpToggle = new CheckBox(context);
      tpToggle.setText("超距传送到主人身边（>阈值）");
      tpToggle.setChecked(stagedTpEnabled[0]);
      tpRow.addView(
          tpToggle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      panel.addView(
          tpRow,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var followLabel = new TextView(context);
      followLabel.setText(
          String.format(java.util.Locale.ROOT, "启动跟随距离: %.1fr", stagedFollowDist[0]));
      followLabel.setTextSize(13);
      panel.addView(
          followLabel,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      var followSeek = new SeekBar(context);
      followSeek.setMax(70); // 1.0 ~ 8.0，以 0.1 步进
      int initFollowProgress = (int) Math.round((stagedFollowDist[0] - 1.0) * 10.0);
      followSeek.setProgress(initFollowProgress);
      panel.addView(
          followSeek,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      var tpDistLabel = new TextView(context);
      tpDistLabel.setText(
          String.format(java.util.Locale.ROOT, "超距传送阈值: %.1fr", stagedTeleportDist[0]));
      tpDistLabel.setTextSize(13);
      panel.addView(
          tpDistLabel,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      var tpDistSeek = new SeekBar(context);
      tpDistSeek.setMax(1200); // 8.0 ~ 128.0，以 0.1 步进
      int initTpProgress = (int) Math.round((stagedTeleportDist[0] - 8.0) * 10.0);
      tpDistSeek.setProgress(initTpProgress);
      panel.addView(
          tpDistSeek,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      tpToggle.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            stagedTpEnabled[0] = isChecked;
            updateSaveEnabled.run();
          });
      followSeek.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
              double followDist = 1.0 + (progress / 10.0);
              followLabel.setText(
                  String.format(java.util.Locale.ROOT, "启动跟随距离: %.1fr", followDist));
              if (fromUser) {
                stagedFollowDist[0] = followDist;
                updateSaveEnabled.run();
              }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
          });
      tpDistSeek.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
              double tpDist = 8.0 + (progress / 10.0);
              tpDistLabel.setText(String.format(java.util.Locale.ROOT, "超距传送阈值: %.1fr", tpDist));
              if (fromUser) {
                stagedTeleportDist[0] = tpDist;
                updateSaveEnabled.run();
              }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
          });

      saveButton.setOnClickListener(
          v -> {
            if (!saveButton.isEnabled()) {
              return;
            }
            var mc = net.minecraft.client.Minecraft.getInstance();
            var conn = mc.getConnection();
            if (conn != null) {
              conn.send(
                  new net.tigereye.chestcavity.client.modernui.config.network
                      .SoulConfigSetVacuumPayload(stagedVacEnabled[0], stagedVacRadius[0]));
              conn.send(
                  new net.tigereye.chestcavity.client.modernui.config.network
                      .SoulConfigSetFollowTeleportPayload(
                      stagedTpEnabled[0], stagedFollowDist[0], stagedTeleportDist[0]));
            }
            // 更新本地基线
            baseVacEnabled[0] = stagedVacEnabled[0];
            baseVacRadius[0] = stagedVacRadius[0];
            baseTpEnabled[0] = stagedTpEnabled[0];
            baseFollowDist[0] = stagedFollowDist[0];
            baseTeleportDist[0] = stagedTeleportDist[0];
            SoulConfigDataClient.INSTANCE.updateVacuum(
                new SoulConfigDataClient.VacuumTuning(stagedVacEnabled[0], stagedVacRadius[0]));
            SoulConfigDataClient.INSTANCE.updateFollowTp(
                new SoulConfigDataClient.FollowTpTuning(
                    stagedTpEnabled[0], stagedFollowDist[0], stagedTeleportDist[0]));
            updateSaveEnabled.run();
          });

      return panel;
    }

    private double clampVacuumRadius(double value) {
      double clamped = Math.max(0.5, Math.min(24.0, value));
      return 0.5 + Math.round((clamped - 0.5) * 10.0) / 10.0;
    }

    private double clampFollowDist(double value) {
      double clamped = Math.max(1.0, Math.min(8.0, value));
      return 1.0 + Math.round((clamped - 1.0) * 10.0) / 10.0;
    }

    private double clampTeleportDist(double value) {
      double clamped = Math.max(8.0, Math.min(128.0, value));
      return 8.0 + Math.round((clamped - 8.0) * 10.0) / 10.0;
    }

    private void addCardLine(LinearLayout card, String text) {
      var tv = new TextView(card.getContext());
      tv.setText(text);
      tv.setTextSize(13);
      tv.setGravity(Gravity.START);
      var params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = card.dp(6);
      card.addView(tv, params);
    }

    private String shortUuid(UUID soulId) {
      String id = soulId.toString();
      return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private String orderLabel(SoulAIOrders.Order order) {
      return switch (order) {
        case IDLE -> "待命";
        case FOLLOW -> "跟随";
        case GUARD -> "守卫";
        case FORCE_FIGHT -> "强制战斗";
      };
    }

    private void requestActivate(UUID soulId) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (connection == null) {
        return;
      }
      mc.execute(
          () -> {
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
              conn.send(new SoulConfigActivatePayload(soulId));
            }
          });
    }

    private void requestRename(UUID soulId, String newName) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (connection == null) {
        return;
      }
      mc.execute(
          () -> {
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
              conn.send(new SoulConfigRenamePayload(soulId, newName));
            }
          });
    }

    private void requestOrderChange(UUID soulId, SoulAIOrders.Order order) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (connection == null) {
        return;
      }
      mc.execute(
          () -> {
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
              conn.send(new SoulConfigSetOrderPayload(soulId, order));
            }
          });
    }

    private void requestForceTeleport(UUID soulId) {
      if (soulId == null) {
        return;
      }
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (connection == null) {
        return;
      }
      mc.execute(
          () -> {
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
              conn.send(new SoulConfigForceTeleportPayload(soulId));
            }
          });
    }

    private LinearLayout baseLayout(icyllis.modernui.core.Context context) {
      var layout = new LinearLayout(context);
      layout.setOrientation(LinearLayout.VERTICAL);
      int padding = layout.dp(16);
      layout.setPadding(padding, padding, padding, padding);
      layout.setClipToPadding(true);
      layout.setClipChildren(true);
      layout.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      return layout;
    }

    private void addHeadline(LinearLayout layout, String text, int sizeSp) {
      var tv = new TextView(layout.getContext());
      tv.setText(text);
      tv.setTextSize(sizeSp);
      tv.setTextColor(0xFFEEF5FF); // Light blue-white color for headings
      tv.setGravity(Gravity.START);
      layout.addView(
          tv,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addBody(LinearLayout layout, String text) {
      var tv = new TextView(layout.getContext());
      tv.setText(text);
      tv.setTextSize(14);
      tv.setTextColor(0xFFCBD8EC); // Light gray-blue color for body text
      tv.setLineSpacing(0, 1.1f);
      tv.setGravity(Gravity.START);
      var params =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.topMargin = layout.dp(8);
      layout.addView(tv, params);
    }
  }

  private void updateIconSurfaceVisibility(int pageIndex) {
    if (skillIconScrollRef != null) {
      skillIconScrollRef.setVisibility(pageIndex == 2 ? View.VISIBLE : View.GONE);
    }
    if (docsIconScrollRef != null) {
      docsIconScrollRef.setVisibility(pageIndex == 3 ? View.VISIBLE : View.GONE);
    }
  }

  /**
   * Animates a view from start scale/alpha to end scale/alpha over a duration.
   *
   * @param view The view to animate
   * @param startScale Initial scale (both X and Y)
   * @param endScale Target scale (both X and Y)
   * @param startAlpha Initial alpha (0-1)
   * @param endAlpha Target alpha (0-1)
   * @param durationMs Animation duration in milliseconds
   */
  private void animateButton(
      View view,
      float startScale,
      float endScale,
      float startAlpha,
      float endAlpha,
      long durationMs) {
    long startTime = System.currentTimeMillis();
    Runnable animator =
        new Runnable() {
          @Override
          public void run() {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, elapsed / (float) durationMs);

            // Ease-out cubic interpolation for smooth animation
            float eased = 1 - (float) Math.pow(1 - progress, 3);

            float currentScale = startScale + (endScale - startScale) * eased;
            float currentAlpha = startAlpha + (endAlpha - startAlpha) * eased;

            view.setScaleX(currentScale);
            view.setScaleY(currentScale);
            view.setAlpha(currentAlpha);

            if (progress < 1f) {
              view.postDelayed(this, 16); // ~60fps
            }
          }
        };
    view.post(animator);
  }

  /**
   * Plays a press animation: scale down then back up.
   *
   * @param view The view to animate
   */
  private void animateButtonPress(View view) {
    // Scale down
    animateButton(view, 1f, 0.92f, 1f, 1f, 100);
    // Scale back up after delay
    view.postDelayed(() -> animateButton(view, 0.92f, 1f, 1f, 1f, 100), 100);
  }

  private void sendPreferenceUpdate(ResourceLocation key, boolean value) {
    var mc = Minecraft.getInstance();
    var connection = mc.getConnection();
    if (connection == null) {
      return;
    }
    mc.execute(() -> connection.send(new PlayerPreferenceUpdatePayload(key, value)));
  }
}
