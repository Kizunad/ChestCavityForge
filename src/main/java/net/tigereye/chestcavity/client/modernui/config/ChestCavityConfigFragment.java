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
import icyllis.modernui.widget.AdapterView;
import icyllis.modernui.widget.ArrayAdapter;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PagerAdapter;
import icyllis.modernui.widget.Spinner;
import icyllis.modernui.widget.TabLayout;
import icyllis.modernui.widget.TextView;
import icyllis.modernui.widget.ViewPager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigActivatePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigRenamePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetOrderPayload;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;

import java.util.List;
import java.util.UUID;

/**
 * Top-level configuration hub for Chest Cavity, rendered with Modern UI.
 * Provides Modern UI configuration hub for Chest Cavity with tabs for
 * home overview, GuScript, and SoulPlayer management.
 */
public class ChestCavityConfigFragment extends Fragment {

    private static final int TAB_COUNT = 3;
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

        private static final SoulAIOrders.Order[] ORDER_VALUES = SoulAIOrders.Order.values();

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

            var titleRow = new LinearLayout(context);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            var nameView = new TextView(context);
            nameView.setText(entry.displayName() + (entry.active() ? "  (当前附身)" : ""));
            nameView.setTextSize(16);
            nameView.setGravity(Gravity.START);
            titleRow.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            var editButton = new Button(context);
            editButton.setText("✏");
            editButton.setMinimumWidth(editButton.dp(36));
            titleRow.addView(editButton, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            card.addView(titleRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            var editRow = new LinearLayout(context);
            editRow.setOrientation(LinearLayout.HORIZONTAL);
            editRow.setGravity(Gravity.CENTER_VERTICAL);
            editRow.setVisibility(View.GONE);

            var editInput = new EditText(context);
            editInput.setSingleLine();
            editInput.setText(entry.displayName());
            editRow.addView(editInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            var saveRename = new Button(context);
            saveRename.setText("保存");
            editRow.addView(saveRename, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            var cancelRename = new Button(context);
            cancelRename.setText("取消");
            editRow.addView(cancelRename, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            var editParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            editParams.topMargin = card.dp(6);
            card.addView(editRow, editParams);

            editButton.setOnClickListener(v -> {
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

            saveRename.setOnClickListener(v -> {
                requestRename(entry.soulId(), editInput.getText().toString());
                editRow.setVisibility(View.GONE);
            });

            cancelRename.setOnClickListener(v -> editRow.setVisibility(View.GONE));

            addCardLine(card, "UUID: " + shortUuid(entry.soulId()));

            double maxHealth = entry.maxHealth();
            addCardLine(card, String.format(java.util.Locale.ROOT,
                    "生命值: %.1f / %.1f (吸收 %.1f)", entry.health(), maxHealth, entry.absorption()));

            addCardLine(card, String.format(java.util.Locale.ROOT,
                    "饱食度: %d  饱和 %.1f", entry.food(), entry.saturation()));

            addCardLine(card, String.format(java.util.Locale.ROOT,
                    "经验等级: %d (进度 %.0f%%)", entry.xpLevel(), entry.xpProgress() * 100f));

            var orderRow = new LinearLayout(context);
            orderRow.setOrientation(LinearLayout.HORIZONTAL);
            orderRow.setGravity(Gravity.CENTER_VERTICAL);
            var orderLabel = new TextView(context);
            orderLabel.setText("指令：");
            orderLabel.setTextSize(13);
            orderRow.addView(orderLabel, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

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
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private boolean ignore = true;

                @Override
                public void onItemSelected(@NonNull AdapterView<?> parent, View view, int position, long id) {
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
                public void onNothingSelected(@NonNull AdapterView<?> parent) { }
            });
            orderRow.addView(spinner, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            var orderParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            orderParams.topMargin = card.dp(8);
            card.addView(orderRow, orderParams);

            var actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.END);
            var button = new Button(context);
            if (entry.active()) {
                button.setText("当前附身");
                button.setEnabled(false);
            } else {
                button.setText(entry.owner() ? "切回本体" : "附身");
                button.setOnClickListener(v -> {
                    requestActivate(entry.soulId());
                    button.setEnabled(false);
                });
            }
            actions.addView(button, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            var params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = card.dp(10);
            card.addView(actions, params);

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
            mc.execute(() -> {
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
            mc.execute(() -> {
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
            mc.execute(() -> {
                ClientPacketListener conn = mc.getConnection();
                if (conn != null) {
                    conn.send(new SoulConfigSetOrderPayload(soulId, order));
                }
            });
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
