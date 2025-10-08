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
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PagerAdapter;
import icyllis.modernui.widget.TabLayout;
import icyllis.modernui.widget.TextView;
import icyllis.modernui.widget.ViewPager;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot;
import net.tigereye.chestcavity.soul.profile.SoulProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Top-level configuration hub for Chest Cavity, rendered with Modern UI.
 * Provides Modern UI configuration hub for Chest Cavity with tabs for
 * home overview, GuScript, and SoulPlayer management.
 */
public class ChestCavityConfigFragment extends Fragment {

    private static final int TAB_COUNT = 3;
    private static final ResourceLocation MAX_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        var context = requireContext();
        var root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        var pager = new ViewPager(context);
        pager.setId(View.generateViewId());
        pager.setAdapter(new ConfigPagerAdapter());

        var pagerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        pagerParams.gravity = Gravity.CENTER;
        pagerParams.topMargin = root.dp(52);
        root.addView(pager, pagerParams);

        var tabs = new TabLayout(context);
        tabs.setTabMode(TabLayout.MODE_AUTO);
        tabs.setTabGravity(TabLayout.GRAVITY_CENTER);
        tabs.setupWithViewPager(pager);

        var tabParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tabParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        root.addView(tabs, tabParams);

        return root;
    }

    private final class ConfigPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LinearLayout layout = switch (position) {
                case 0 -> createHomePage(container);
                case 1 -> createGuScriptPage(container);
                default -> createSoulPlayerPage(container);
            };
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
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
                default -> "分魂";
            };
        }

        private LinearLayout createHomePage(ViewGroup container) {
            var context = container.getContext();
            var layout = baseLayout(context);
            addHeadline(layout, "Chest Cavity 设置总览", 18);
            addBody(layout,
                    "· 规划全局配置入口，未来将同步 ModMenu / NeoForge Config。\n" +
                            "· 在此页面添加常用开关、QoL 选项与快速跳转。");
            return layout;
        }

        private LinearLayout createGuScriptPage(ViewGroup container) {
            var context = container.getContext();
            var layout = baseLayout(context);
            addHeadline(layout, "自定义杀招 (GuScript)", 18);
            addBody(layout,
                    "· 预留布局用于编辑与管理 GuScript 方案。\n" +
                            "· 后续可在此嵌入脚本列表、快捷启动与资源统计。");
            return layout;
        }

        private LinearLayout createSoulPlayerPage(ViewGroup container) {
            var context = container.getContext();
            var layout = baseLayout(context);

            addHeadline(layout, "分魂系统", 18);
            addRefreshRow(layout);

            layout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                final SoulConfigDataClient.Listener listener = snapshot -> layout.post(() -> populateSoulLayout(layout, snapshot));

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

            return layout;
        }

        private void addRefreshRow(LinearLayout layout) {
            var row = new LinearLayout(layout.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.END);
            var refresh = new Button(layout.getContext());
            refresh.setText("刷新数据");
            refresh.setOnClickListener(v -> SoulConfigDataClient.INSTANCE.requestSync());
            row.addView(refresh, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
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
        }

        private LinearLayout.LayoutParams soulCardLayoutParams(LinearLayout parent) {
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = parent.dp(10);
            return params;
        }

        private View createSoulCard(icyllis.modernui.core.Context context,
                                    SoulConfigDataClient.SoulEntry entry) {
            var card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(card.dp(14), card.dp(12), card.dp(14), card.dp(12));
            ShapeDrawable background = new ShapeDrawable();
            background.setCornerRadius(card.dp(10));
            background.setColor(entry.active() ? 0xFF1F3B4D : 0xFF1B2836);
            card.setBackground(background);

            var title = new TextView(context);
            title.setText(entry.displayName() + (entry.active() ? "  (当前附身)" : ""));
            title.setTextSize(16);
            title.setGravity(Gravity.START);
            card.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            addCardLine(card, "UUID: " + shortUuid(entry.soulId()));

            double maxHealth = entry.maxHealth();
            addCardLine(card, String.format(java.util.Locale.ROOT,
                    "生命值: %.1f / %.1f (吸收 %.1f)", entry.health(), maxHealth, entry.absorption()));

            addCardLine(card, String.format(java.util.Locale.ROOT,
                    "饱食度: %d  饱和 %.1f", entry.food(), entry.saturation()));

            addCardLine(card, String.format(java.util.Locale.ROOT,
                    "经验等级: %d (进度 %.0f%%)", entry.xpLevel(), entry.xpProgress() * 100f));

            return card;
        }

        private void addCardLine(LinearLayout card, String text) {
            var tv = new TextView(card.getContext());
            tv.setText(text);
            tv.setTextSize(13);
            tv.setGravity(Gravity.START);
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = card.dp(6);
            card.addView(tv, params);
        }

        private String defaultSoulLabel(UUID soulId) {
            String id = soulId.toString();
            return "Soul " + (id.length() > 8 ? id.substring(0, 8) : id);
        }

        private String shortUuid(UUID soulId) {
            String id = soulId.toString();
            return id.length() > 8 ? id.substring(0, 8) : id;
        }

        private LinearLayout baseLayout(icyllis.modernui.core.Context context) {
            var layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            int padding = layout.dp(16);
            layout.setPadding(padding, padding, padding, padding);
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return layout;
        }

        private void addHeadline(LinearLayout layout, String text, int sizeSp) {
            var tv = new TextView(layout.getContext());
            tv.setText(text);
            tv.setTextSize(sizeSp);
            tv.setGravity(Gravity.START);
            layout.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        private void addBody(LinearLayout layout, String text) {
            var tv = new TextView(layout.getContext());
            tv.setText(text);
            tv.setTextSize(14);
            tv.setLineSpacing(0, 1.1f);
            tv.setGravity(Gravity.START);
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = layout.dp(8);
            layout.addView(tv, params);
        }
    }
}
