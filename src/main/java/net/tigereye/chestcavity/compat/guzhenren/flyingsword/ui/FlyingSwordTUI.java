package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import java.util.List;
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
 * 飞剑系统TUI（简洁版，无左右边框）。
 */
public final class FlyingSwordTUI {
  private FlyingSwordTUI() {}

  // ==================== 主界面 ====================

  /**
   * 打开主界面。
   */
  public static void openMain(ServerPlayer player) {
    long nowTick = player.level().getGameTime();

    if (!TUISessionManager.canSendTui(player, nowTick)) {
      double cooldown = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 1000.0;
      player.sendSystemMessage(TUICommandGuard.createRateLimitMessage(cooldown));
      return;
    }

    TUISessionManager.markTuiSent(player, nowTick);
    TUIRefreshOps.clearPrevious(player);

    ServerLevel level = player.serverLevel();
    FlyingSwordEntity selected = FlyingSwordController.getSelectedSword(level, player);

    TUITheme.beginFrame(60);

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder("飞剑系统"));

    // 选中飞剑信息
    if (selected != null) {
      player.sendSystemMessage(createSelectedInfo(selected, player));
    } else {
      player.sendSystemMessage(Component.literal("未选中飞剑 (点击[在场]查看列表)").withStyle(TUITheme.LABEL));
    }

    player.sendSystemMessage(TUITheme.createEmptyLine());

    // 指定飞剑操作
    player.sendSystemMessage(Component.literal("指定飞剑: ").withStyle(TUITheme.ACCENT)
        .append(createModeButton("出击", "/flyingsword mode_selected hunt", "设定选中飞剑为出击模式"))
        .append(sp())
        .append(createModeButton("守护", "/flyingsword mode_selected guard", "设定选中飞剑为守护模式"))
        .append(sp())
        .append(createModeButton("环绕", "/flyingsword mode_selected orbit", "设定选中飞剑为环绕模式"))
        .append(sp())
        .append(createModeButton("悬浮", "/flyingsword mode_selected hover", "设定选中飞剑为悬浮模式"))
        .append(sp())
        .append(createButton("修复", "/flyingsword repair_selected", "消耗主手物品修复选中飞剑")));

    // 全体操作
    player.sendSystemMessage(Component.literal("全体指令: ").withStyle(TUITheme.ACCENT)
        .append(createModeButton("全体出击", "/flyingsword mode hunt", "令所有飞剑出击"))
        .append(sp())
        .append(createModeButton("全体守护", "/flyingsword mode guard", "令所有飞剑守护"))
        .append(sp())
        .append(createModeButton("全体环绕", "/flyingsword mode orbit", "令所有飞剑环绕"))
        .append(sp())
        .append(createModeButton("全体悬浮", "/flyingsword mode hover", "令所有飞剑悬浮"))
        .append(sp())
        .append(createButton("全体召回", "/flyingsword recall", "召回所有飞剑")));

    // 管理导航
    player.sendSystemMessage(Component.literal("管理: ").withStyle(TUITheme.ACCENT)
        .append(createNavButton("在场", "/flyingsword ui_active 1", "查看在场飞剑列表"))
        .append(sp())
        .append(createNavButton("存储", "/flyingsword ui_storage 1", "查看存储飞剑列表"))
        .append(sp())
        .append(createButton("列表", "/flyingsword list", "详细列出所有在场飞剑"))
        .append(sp())
        .append(createButton("状态", "/flyingsword status", "查看飞剑系统状态")));

    // 底部边框
    player.sendSystemMessage(TUITheme.createBottomBorder());
  }

  /**
   * 打开在场飞剑列表。
   */
  public static void openActiveList(ServerPlayer player, int page) {
    long nowTick = player.level().getGameTime();

    if (!TUISessionManager.canSendTui(player, nowTick)) {
      double cooldown = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 1000.0;
      player.sendSystemMessage(TUICommandGuard.createRateLimitMessage(cooldown));
      return;
    }

    TUISessionManager.markTuiSent(player, nowTick);
    TUIRefreshOps.clearPrevious(player);

    ServerLevel level = player.serverLevel();
    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);

    TUITheme.beginFrame(60);

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder("在场飞剑"));

    if (swords.isEmpty()) {
      player.sendSystemMessage(Component.literal("暂无在场飞剑").withStyle(TUITheme.LABEL));
      player.sendSystemMessage(TUITheme.createBottomBorder());
      player.sendSystemMessage(createNavButton("返回", "/flyingsword ui", "返回主界面"));
      return;
    }

    // 分页
    final int pageSize = FlyingSwordTuning.TUI_PAGE_SIZE;
    int total = swords.size();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int p = Math.min(Math.max(1, page), pages);
    int start = (p - 1) * pageSize;
    int end = Math.min(total, start + pageSize);

    // 分页导航
    player.sendSystemMessage(createPagination(p, pages, true));

    // 列表项
    for (int i = start; i < end; i++) {
      FlyingSwordEntity sword = swords.get(i);
      int idx = i + 1;
      player.sendSystemMessage(createSwordListItem(sword, idx, player, p));
    }

    // 底部
    player.sendSystemMessage(createPagination(p, pages, true));
    player.sendSystemMessage(TUITheme.createBottomBorder());
    player.sendSystemMessage(createNavButton("返回", "/flyingsword ui", "返回主界面"));
  }

  /**
   * 打开存储飞剑列表。
   */
  public static void openStorageList(ServerPlayer player, int page) {
    long nowTick = player.level().getGameTime();

    if (!TUISessionManager.canSendTui(player, nowTick)) {
      double cooldown = FlyingSwordTuning.TUI_MIN_REFRESH_MILLIS / 1000.0;
      player.sendSystemMessage(TUICommandGuard.createRateLimitMessage(cooldown));
      return;
    }

    TUISessionManager.markTuiSent(player, nowTick);
    TUIRefreshOps.clearPrevious(player);

    var storage = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);
    var list = storage.getRecalledSwords();

    TUITheme.beginFrame(60);

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder("存储飞剑"));

    if (list.isEmpty()) {
      player.sendSystemMessage(Component.literal("存储中暂无飞剑").withStyle(TUITheme.LABEL));
      player.sendSystemMessage(TUITheme.createBottomBorder());
      player.sendSystemMessage(createNavButton("返回", "/flyingsword ui", "返回主界面"));
      return;
    }

    // 分页
    final int pageSize = FlyingSwordTuning.TUI_PAGE_SIZE;
    int total = list.size();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int p = Math.min(Math.max(1, page), pages);
    int start = (p - 1) * pageSize;
    int end = Math.min(total, start + pageSize);

    // 分页导航
    player.sendSystemMessage(createPagination(p, pages, false));

    // 列表项
    for (int i = start; i < end; i++) {
      FlyingSwordStorage.RecalledSword recalled = list.get(i);
      int idx = i + 1;
      player.sendSystemMessage(createStorageListItem(recalled, idx, player, p));
    }

    // 底部
    player.sendSystemMessage(createPagination(p, pages, false));
    player.sendSystemMessage(TUITheme.createBottomBorder());
    player.sendSystemMessage(createNavButton("返回", "/flyingsword ui", "返回主界面"));
  }

  // ==================== 组件构建 ====================

  private static Component createSelectedInfo(FlyingSwordEntity selected, ServerPlayer player) {
    double ratio = selected.getDurability() / selected.getSwordAttributes().maxDurability;
    ChatFormatting color = ratio > 0.6 ? ChatFormatting.GREEN :
                          ratio > 0.3 ? ChatFormatting.YELLOW :
                          ChatFormatting.RED;

    return Component.literal("已选中: ").withStyle(TUITheme.ACCENT)
        .append(Component.literal("Lv." + selected.getSwordLevel()).withStyle(TUITheme.VALUE))
        .append(Component.literal(" | ").withStyle(TUITheme.DIM))
        .append(Component.literal("[" + selected.getAIMode().getDisplayName() + "]").withStyle(getModeColor(selected.getAIMode().getDisplayName())))
        .append(Component.literal(" | ").withStyle(TUITheme.DIM))
        .append(Component.literal(String.format("%.0f/%.0f", selected.getDurability(), selected.getSwordAttributes().maxDurability)).withStyle(color))
        .append(Component.literal(" | ").withStyle(TUITheme.DIM))
        .append(Component.literal(String.format("%.1fm", selected.distanceTo(player))).withStyle(TUITheme.LABEL));
  }

  private static Component createSwordListItem(FlyingSwordEntity sword, int idx, ServerPlayer player, int currentPage) {
    double ratio = sword.getDurability() / sword.getSwordAttributes().maxDurability;

    MutableComponent line = Component.literal(String.format("#%-2d ", idx)).withStyle(TUITheme.LABEL)
        .append(Component.literal("Lv." + sword.getSwordLevel()).withStyle(TUITheme.VALUE))
        .append(sp())
        .append(Component.literal("[" + sword.getAIMode().getDisplayName() + "]").withStyle(getModeColor(sword.getAIMode().getDisplayName())))
        .append(sp())
        .append(Component.literal(String.format("%.0f%%", ratio * 100)).withStyle(ratio > 0.5 ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
        .append(sp())
        .append(Component.literal(String.format("%.0fm", sword.distanceTo(player))).withStyle(TUITheme.LABEL));

    // 操作按钮（带刷新）
    line.append(Component.literal("  "))
        .append(createRefreshButton("选", "/flyingsword select index " + idx, "选中", currentPage, true))
        .append(sp())
        .append(createRefreshButton("修", "/flyingsword repair_index " + idx, "修复", currentPage, true))
        .append(sp())
        .append(createRefreshButton("回", "/flyingsword recall_index " + idx, "召回", currentPage, true))
        .append(sp())
        .append(createRefreshButton("攻", "/flyingsword mode_index " + idx + " hunt", "出击", currentPage, true))
        .append(sp())
        .append(createRefreshButton("守", "/flyingsword mode_index " + idx + " guard", "守护", currentPage, true))
        .append(sp())
        .append(createRefreshButton("环", "/flyingsword mode_index " + idx + " orbit", "环绕", currentPage, true))
        .append(sp())
        .append(createRefreshButton("悬", "/flyingsword mode_index " + idx + " hover", "悬浮", currentPage, true));

    return line;
  }

  private static Component createStorageListItem(FlyingSwordStorage.RecalledSword recalled, int idx, ServerPlayer player, int currentPage) {
    String name = FlyingSwordTUIOps.getStoredDisplayName(player.serverLevel(), recalled);
    double ratio = recalled.durability / recalled.attributes.maxDurability;

    MutableComponent line = Component.literal(String.format("#%-2d ", idx)).withStyle(TUITheme.LABEL)
        .append(Component.literal("Lv." + recalled.level).withStyle(TUITheme.VALUE))
        .append(sp())
        .append(Component.literal(String.format("%.0f%%", ratio * 100)).withStyle(ratio > 0.5 ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
        .append(sp())
        .append(Component.literal(name).withStyle(TUITheme.VALUE));

    if (recalled.itemWithdrawn) {
      line.append(Component.literal("  "))
          .append(createRefreshButton("放回", "/flyingsword deposit_index " + idx, "放回物品", currentPage, false))
          .append(sp())
          .append(Component.literal("(已取出)").withStyle(TUITheme.WARNING))
          .append(sp())
          .append(createRefreshButton("删除", "/flyingsword delete_storage " + idx, "删除", currentPage, false));
    } else {
      line.append(Component.literal("  "))
          .append(createRefreshButton("召唤", "/flyingsword restore_index " + idx, "召唤", currentPage, false))
          .append(sp())
          .append(createRefreshButton("取出", "/flyingsword withdraw_index " + idx, "取出物品", currentPage, false))
          .append(sp())
          .append(createRefreshButton("删除", "/flyingsword delete_storage " + idx, "删除", currentPage, false));
    }

    return line;
  }

  private static Component createPagination(int page, int pages, boolean isActive) {
    MutableComponent nav = Component.empty();

    if (page > 1) {
      String cmd = isActive ? "/flyingsword ui_active " + (page - 1) : "/flyingsword ui_storage " + (page - 1);
      nav.append(createNavButton("< 上一页", cmd, "上一页"));
    } else {
      nav.append(Component.literal("< 上一页").withStyle(TUITheme.DIM));
    }

    nav.append(Component.literal(" | ").withStyle(TUITheme.DIM));
    nav.append(Component.literal("第 " + page + "/" + pages + " 页").withStyle(TUITheme.LABEL));
    nav.append(Component.literal(" | ").withStyle(TUITheme.DIM));

    if (page < pages) {
      String cmd = isActive ? "/flyingsword ui_active " + (page + 1) : "/flyingsword ui_storage " + (page + 1);
      nav.append(createNavButton("下一页 >", cmd, "下一页"));
    } else {
      nav.append(Component.literal("下一页 >").withStyle(TUITheme.DIM));
    }

    return nav;
  }

  // ==================== 按钮工具 ====================

  private static MutableComponent createButton(String label, String command, String hover) {
    return Component.literal("[" + label + "]")
        .withStyle(TUITheme.BUTTON)
        .withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal(hover).withStyle(ChatFormatting.GRAY))));
  }

  private static MutableComponent createModeButton(String label, String command, String hover) {
    return createButton(label, command, hover);
  }

  private static MutableComponent createNavButton(String label, String command, String hover) {
    return Component.literal("[" + label + "]")
        .withStyle(TUITheme.ACCENT)
        .withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal(hover).withStyle(ChatFormatting.GRAY))));
  }

  private static MutableComponent createRefreshButton(String label, String command, String hover, int page, boolean isActive) {
    // 组合命令：先执行操作，再刷新当前页面
    String refreshCmd = isActive ? "/flyingsword ui_active " + page : "/flyingsword ui_storage " + page;
    String combinedCmd = command + " && " + refreshCmd;

    return Component.literal("[" + label + "]")
        .withStyle(TUITheme.BUTTON)
        .withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal(hover).withStyle(ChatFormatting.GRAY))));
  }

  private static ChatFormatting getModeColor(String mode) {
    return switch (mode.toLowerCase()) {
      case "hunt", "出击" -> TUITheme.MODE_HUNT;
      case "guard", "守护" -> TUITheme.MODE_GUARD;
      case "orbit", "环绕" -> TUITheme.MODE_ORBIT;
      case "hover", "悬浮" -> TUITheme.MODE_HOVER;
      default -> TUITheme.LABEL;
    };
  }

  private static Component sp() {
    return Component.literal(" ");
  }
}
