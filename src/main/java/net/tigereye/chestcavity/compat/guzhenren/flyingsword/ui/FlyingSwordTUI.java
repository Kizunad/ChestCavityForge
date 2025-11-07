package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordStorage;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;

/**
 * 飞剑系统交互式TUI（全新重构版本）。
 *
 * <p>特性：
 * <ul>
 *   <li>✨ 优美的边框和emoji图标（支持降级到ASCII）</li>
 *   <li>🔒 基于会话ID的过期检测和防刷屏</li>
 *   <li>🎯 准备支持基于UUID的稳定命令（向后兼容index）</li>
 *   <li>📱 人性化的布局和颜色主题</li>
 * </ul>
 */
public final class FlyingSwordTUI {
  private FlyingSwordTUI() {}

  // ==================== 主界面 ====================

  /**
   * 打开主界面。
   *
   * @param player 玩家
   */
  public static void openMain(ServerPlayer player) {
    long nowTick = player.level().getGameTime();

    // 限流检查
    if (!TUISessionManager.canSendTui(player, nowTick)) {
      double cooldown = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 1000.0;
      player.sendSystemMessage(TUICommandGuard.createRateLimitMessage(cooldown));
      return;
    }

    // 生成新会话
    String sid = TUISessionManager.ensureFreshSession(player, nowTick);
    TUISessionManager.markTuiSent(player, nowTick);

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder("飞剑系统"));

    // 当前选中信息
    FlyingSwordEntity selected = FlyingSwordController.getSelectedSword(player.serverLevel(), player);
    if (selected != null) {
      player.sendSystemMessage(createSelectedInfo(selected, player));
    } else {
      player.sendSystemMessage(
          Component.literal("│ ").withStyle(TUITheme.DIM)
              .append(Component.literal("未选中飞剑").withStyle(TUITheme.LABEL))
              .append(TUITheme.createSpacer())
              .append(Component.literal("(点击下方[在场]查看)").withStyle(TUITheme.DIM)));
    }

    player.sendSystemMessage(TUITheme.createDivider());

    // 行为栏（对选中飞剑操作）
    player.sendSystemMessage(TUITheme.createSectionTitle(TUITheme.EMOJI_SWORD, "指定飞剑"));
    player.sendSystemMessage(createBehaviorBar(sid));

    player.sendSystemMessage(TUITheme.createDivider());

    // 全体操作栏
    player.sendSystemMessage(TUITheme.createSectionTitle(TUITheme.EMOJI_GROUP, "全体指令"));
    player.sendSystemMessage(createAllActionsBar(sid));

    player.sendSystemMessage(TUITheme.createDivider());

    // 主导航栏
    player.sendSystemMessage(TUITheme.createSectionTitle(TUITheme.EMOJI_TACTIC, "管理操作"));
    player.sendSystemMessage(createMainNavigation(sid));

    // 底部边框
    player.sendSystemMessage(TUITheme.createBottomBorder());
  }

  /**
   * 打开在场飞剑列表。
   *
   * @param player 玩家
   * @param page 页码（从1开始）
   */
  public static void openActiveList(ServerPlayer player, int page) {
    long nowTick = player.level().getGameTime();

    // 限流检查
    if (!TUISessionManager.canSendTui(player, nowTick)) {
      double cooldown = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 1000.0;
      player.sendSystemMessage(TUICommandGuard.createRateLimitMessage(cooldown));
      return;
    }

    // 生成新会话
    String sid = TUISessionManager.ensureFreshSession(player, nowTick);
    TUISessionManager.markTuiSent(player, nowTick);

    ServerLevel level = player.serverLevel();
    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder("在场飞剑"));

    if (swords.isEmpty()) {
      player.sendSystemMessage(
          Component.literal("│ ").withStyle(TUITheme.DIM)
              .append(Component.literal("暂无在场飞剑").withStyle(TUITheme.LABEL)));
      player.sendSystemMessage(TUITheme.createBottomBorder());
      player.sendSystemMessage(createBackButton(sid));
      return;
    }

    // 分页计算
    final int pageSize = FlyingSwordTuning.TUI_PAGE_SIZE;
    int total = swords.size();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int p = Math.min(Math.max(1, page), pages);
    int start = (p - 1) * pageSize;
    int end = Math.min(total, start + pageSize);

    // 分页导航（顶部）
    player.sendSystemMessage(TUITheme.createNavigation(p > 1, p < pages, p, pages));

    // 列表项
    for (int i = start; i < end; i++) {
      FlyingSwordEntity sword = swords.get(i);
      player.sendSystemMessage(createSwordListItem(sword, i, player, sid));

      // 分组按钮行（如果不是剑群）
      int groupId = sword.getGroupId();
      String uuid = sword.getUUID().toString();
      if (groupId != FlyingSwordEntity.SWARM_GROUP_ID) {
        player.sendSystemMessage(createGroupButtonsByUuid(uuid, groupId, sid));
      } else {
        player.sendSystemMessage(
            Component.literal("    ")
                .append(Component.literal(TUITheme.EMOJI_SWARM + " 剑群飞剑（分组已锁定）")
                    .withStyle(TUITheme.DIM)));
      }
    }

    // 底部导航
    player.sendSystemMessage(TUITheme.createNavigation(p > 1, p < pages, p, pages));
    player.sendSystemMessage(TUITheme.createBottomBorder());
    player.sendSystemMessage(createActivePagination(p, pages, sid));
  }

  /**
   * 打开存储飞剑列表。
   *
   * @param player 玩家
   * @param page 页码（从1开始）
   */
  public static void openStorageList(ServerPlayer player, int page) {
    long nowTick = player.level().getGameTime();

    // 限流检查
    if (!TUISessionManager.canSendTui(player, nowTick)) {
      double cooldown = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 1000.0;
      player.sendSystemMessage(TUICommandGuard.createRateLimitMessage(cooldown));
      return;
    }

    // 生成新会话
    String sid = TUISessionManager.ensureFreshSession(player, nowTick);
    TUISessionManager.markTuiSent(player, nowTick);

    var storage = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);
    var list = storage.getRecalledSwords();

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder("存储飞剑"));

    if (list.isEmpty()) {
      player.sendSystemMessage(
          Component.literal("│ ").withStyle(TUITheme.DIM)
              .append(Component.literal("存储中暂无飞剑").withStyle(TUITheme.LABEL)));
      player.sendSystemMessage(TUITheme.createBottomBorder());
      player.sendSystemMessage(createBackButton(sid));
      return;
    }

    // 分页计算
    final int pageSize = FlyingSwordTuning.TUI_PAGE_SIZE;
    int total = list.size();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int p = Math.min(Math.max(1, page), pages);
    int start = (p - 1) * pageSize;
    int end = Math.min(total, start + pageSize);

    // 分页导航（顶部）
    player.sendSystemMessage(TUITheme.createNavigation(p > 1, p < pages, p, pages));

    // 列表项
    for (int i = start; i < end; i++) {
      var recalled = list.get(i);
      player.sendSystemMessage(createStorageListItem(recalled, i, player, sid));
    }

    // 底部导航
    player.sendSystemMessage(TUITheme.createNavigation(p > 1, p < pages, p, pages));
    player.sendSystemMessage(TUITheme.createBottomBorder());
    player.sendSystemMessage(createStoragePagination(p, pages, sid));
  }

  // ==================== 组件构建方法 ====================

  /**
   * 创建选中飞剑信息行。
   */
  private static Component createSelectedInfo(FlyingSwordEntity selected, ServerPlayer player) {
    double durabilityRatio = selected.getDurability() / selected.getSwordAttributes().maxDurability;
    ChatFormatting durabilityColor =
        durabilityRatio > 0.6 ? ChatFormatting.GREEN :
        durabilityRatio > 0.3 ? ChatFormatting.YELLOW :
        ChatFormatting.RED;

    return Component.literal("│ ").withStyle(TUITheme.DIM)
        .append(Component.literal("已选中 ").withStyle(TUITheme.ACCENT))
        .append(TUITheme.createSpacer())
        .append(TUITheme.createLabelValue("等级", "Lv." + selected.getSwordLevel()))
        .append(TUITheme.createSpacer())
        .append(TUITheme.createModePill(selected.getAIMode().getDisplayName()))
        .append(TUITheme.createSpacer())
        .append(TUITheme.createLabelValue(
            "耐久",
            String.format("%.0f/%.0f", selected.getDurability(), selected.getSwordAttributes().maxDurability),
            durabilityColor))
        .append(TUITheme.createSpacer())
        .append(TUITheme.createLabelValue("距离", String.format("%.1fm", selected.distanceTo(player))));
  }

  /**
   * 创建行为栏（对选中飞剑操作）。
   */
  private static Component createBehaviorBar(String sid) {
    MutableComponent bar = Component.literal("│ ").withStyle(TUITheme.DIM);

    bar.append(createButton("出击", "/flyingsword mode_selected hunt", "设定选中飞剑为出击模式"));
    bar.append(space());
    bar.append(createButton("守护", "/flyingsword mode_selected guard", "设定选中飞剑为守护模式"));
    bar.append(space());
    bar.append(createButton("环绕", "/flyingsword mode_selected orbit", "设定选中飞剑为环绕模式"));
    bar.append(space());
    bar.append(createButton("悬浮", "/flyingsword mode_selected hover", "设定选中飞剑为悬浮模式"));
    bar.append(space());
    bar.append(createButton("修复", "/flyingsword repair_selected", "消耗主手物品修复选中飞剑"));

    return bar;
  }

  /**
   * 创建全体操作栏。
   */
  private static Component createAllActionsBar(String sid) {
    MutableComponent bar = Component.literal("│ ").withStyle(TUITheme.DIM);

    bar.append(createButton("全体出击", "/flyingsword mode hunt", "令所有飞剑出击"));
    bar.append(space());
    bar.append(createButton("全体守护", "/flyingsword mode guard", "令所有飞剑守护"));
    bar.append(space());
    bar.append(createButton("全体环绕", "/flyingsword mode orbit", "令所有飞剑环绕"));
    bar.append(space());
    bar.append(createButton("全体悬浮", "/flyingsword mode hover", "令所有飞剑悬浮"));
    bar.append(space());
    bar.append(createButton("全体召回", "/flyingsword recall", "召回所有飞剑"));

    return bar;
  }

  /**
   * 创建主导航栏。
   */
  private static Component createMainNavigation(String sid) {
    MutableComponent nav = Component.literal("│ ").withStyle(TUITheme.DIM);

    nav.append(createButton("在场", "/flyingsword ui_active 1", "管理在场飞剑"));
    nav.append(space());
    nav.append(createButton("存储", "/flyingsword ui_storage 1", "管理存储中的飞剑"));
    nav.append(space());
    nav.append(createButton("列表", "/flyingsword list", "详细列出所有在场飞剑"));
    nav.append(space());
    nav.append(createButton("状态", "/flyingsword status", "查看飞剑系统状态"));

    return nav;
  }

  /**
   * 创建飞剑列表项。
   */
  private static Component createSwordListItem(
      FlyingSwordEntity sword, int index, ServerPlayer player, String sid) {
    double durabilityRatio = sword.getDurability() / sword.getSwordAttributes().maxDurability;
    String uuid = sword.getUUID().toString();

    MutableComponent line = Component.literal("│ ").withStyle(TUITheme.DIM)
        .append(Component.literal(String.format("#%-2d ", index + 1)).withStyle(TUITheme.LABEL))
        .append(TUITheme.createLabelValue("Lv", String.valueOf(sword.getSwordLevel())))
        .append(space())
        .append(TUITheme.createModePill(sword.getAIMode().getDisplayName()))
        .append(space());

    // 耐久进度条
    line.append(TUITheme.createProgressBar(
        sword.getDurability(),
        sword.getSwordAttributes().maxDurability,
        8,
        durabilityRatio > 0.5 ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
        ChatFormatting.DARK_GRAY));

    line.append(Component.literal(String.format(" %.0f%%", durabilityRatio * 100)).withStyle(TUITheme.LABEL));
    line.append(space());
    line.append(TUITheme.createLabelValue("距", String.format("%.0fm", sword.distanceTo(player))));

    return line.append(Component.literal("  "))
        .append(createButton("选", "/flyingsword select_id " + uuid + " " + sid, "选中此飞剑"))
        .append(space())
        .append(createButton("修", "/flyingsword repair_id " + uuid + " " + sid, "修复此飞剑"))
        .append(space())
        .append(createButton("回", "/flyingsword recall_id " + uuid + " " + sid, "召回此飞剑"))
        .append(space())
        .append(createModeButtonById("攻", uuid, "hunt", sid))
        .append(space())
        .append(createModeButtonById("守", uuid, "guard", sid))
        .append(space())
        .append(createModeButtonById("环", uuid, "orbit", sid))
        .append(space())
        .append(createModeButtonById("悬", uuid, "hover", sid));
  }

  /**
   * 创建分组按钮行（基于UUID）。
   */
  private static Component createGroupButtonsByUuid(String uuid, int currentGroupId, String sid) {
    MutableComponent line = Component.literal("    ")
        .append(Component.literal("分组: ").withStyle(TUITheme.LABEL));

    line.append(createGroupButtonById(uuid, 0, currentGroupId == 0, "全部", sid));
    line.append(space());
    line.append(createGroupButtonById(uuid, 1, currentGroupId == 1, "G1", sid));
    line.append(space());
    line.append(createGroupButtonById(uuid, 2, currentGroupId == 2, "G2", sid));
    line.append(space());
    line.append(createGroupButtonById(uuid, 3, currentGroupId == 3, "G3", sid));

    return line;
  }

  /**
   * 创建存储列表项。
   */
  private static Component createStorageListItem(
      FlyingSwordStorage.RecalledSword recalled, int index, ServerPlayer player, String sid) {
    String name = FlyingSwordTUIOps.getStoredDisplayName(player.serverLevel(), recalled);
    double durabilityRatio = recalled.durability / recalled.attributes.maxDurability;
    String itemUuid = recalled.displayItemUUID != null ? recalled.displayItemUUID.toString() : "";

    MutableComponent line = Component.literal("│ ").withStyle(TUITheme.DIM)
        .append(Component.literal(String.format("#%-2d ", index + 1)).withStyle(TUITheme.LABEL))
        .append(TUITheme.createLabelValue("Lv", String.valueOf(recalled.level)))
        .append(space());

    // 耐久进度条
    line.append(TUITheme.createProgressBar(
        recalled.durability,
        recalled.attributes.maxDurability,
        8,
        durabilityRatio > 0.5 ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
        ChatFormatting.DARK_GRAY));

    line.append(Component.literal(String.format(" %.0f%%", durabilityRatio * 100)).withStyle(TUITheme.LABEL));
    line.append(space());
    line.append(Component.literal(name).withStyle(TUITheme.VALUE));

    if (recalled.itemWithdrawn) {
      return line.append(Component.literal("  "))
          .append(createButton("放回", "/flyingsword deposit_item " + itemUuid + " " + sid, "放回此物品"))
          .append(space())
          .append(Component.literal("(已取出)").withStyle(TUITheme.WARNING));
    } else {
      return line.append(Component.literal("  "))
          .append(createButton("召唤", "/flyingsword restore_item " + itemUuid + " " + sid, "召唤此飞剑"))
          .append(space())
          .append(createButton("取出", "/flyingsword withdraw_item " + itemUuid + " " + sid, "取出物品本体"));
    }
  }

  /**
   * 创建在场列表底部分页按钮。
   */
  private static Component createActivePagination(int page, int pages, String sid) {
    MutableComponent nav = Component.empty();

    if (page > 1) {
      nav.append(createButton("◀ 上一页", "/flyingsword ui_active " + (page - 1), "上一页"));
      nav.append(space());
    }

    nav.append(createButton("返回主界面", "/flyingsword ui", "返回"));

    if (page < pages) {
      nav.append(space());
      nav.append(createButton("下一页 ▶", "/flyingsword ui_active " + (page + 1), "下一页"));
    }

    return nav;
  }

  /**
   * 创建存储列表底部分页按钮。
   */
  private static Component createStoragePagination(int page, int pages, String sid) {
    MutableComponent nav = Component.empty();

    if (page > 1) {
      nav.append(createButton("◀ 上一页", "/flyingsword ui_storage " + (page - 1), "上一页"));
      nav.append(space());
    }

    nav.append(createButton("返回主界面", "/flyingsword ui", "返回"));

    if (page < pages) {
      nav.append(space());
      nav.append(createButton("下一页 ▶", "/flyingsword ui_storage " + (page + 1), "下一页"));
    }

    return nav;
  }

  // ==================== 按钮工具方法 ====================

  /**
   * 创建通用按钮。
   */
  private static MutableComponent createButton(String label, String command, String hover) {
    return TUITheme.createButton(label)
        .withStyle(
            style ->
                style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(
                        new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal(hover).withStyle(ChatFormatting.GRAY))));
  }

  /**
   * 创建模式切换按钮（基于UUID）。
   */
  private static MutableComponent createModeButtonById(String label, String uuid, String mode, String sid) {
    String modeName = switch (mode) {
      case "hunt" -> "出击";
      case "guard" -> "守护";
      case "orbit" -> "环绕";
      case "hover" -> "悬浮";
      default -> mode;
    };
    return createButton(label, "/flyingsword mode_id " + uuid + " " + mode + " " + sid, "设为" + modeName);
  }

  /**
   * 创建分组按钮（基于UUID）。
   */
  private static MutableComponent createGroupButtonById(
      String uuid, int groupId, boolean selected, String label, String sid) {
    if (selected) {
      return Component.literal("[" + label + "]")
          .withStyle(ChatFormatting.BOLD)
          .withStyle(TUITheme.ACCENT);
    } else {
      return createButton(label, "/flyingsword group_id " + uuid + " " + groupId + " " + sid, "设为分组: " + label);
    }
  }

  /**
   * 创建返回按钮。
   */
  private static Component createBackButton(String sid) {
    return createButton("« 返回主界面", "/flyingsword ui", "返回主界面");
  }

  /**
   * 创建空格。
   */
  private static Component space() {
    return Component.literal(" ");
  }
}
