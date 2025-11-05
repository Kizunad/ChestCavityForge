package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 简易可交互TUI（聊天按钮）。
 */
public final class FlyingSwordTUI {
  private FlyingSwordTUI() {}

  // ===== 主界面 =====
  public static void openMain(ServerPlayer player) {
    // Header
    player.sendSystemMessage(banner("===== 飞剑系统 ====="));
    player.sendSystemMessage(hr());

    // 行为栏
    Component behavior =
        Component.literal("行为: ")
            .append(btn("[攻击]", "/flyingsword mode_selected hunt", "设定指定飞剑为[出击]"))
            .append(space())
            .append(btn("[守护]", "/flyingsword mode_selected guard", "设定指定飞剑为[守护]"))
            .append(space())
            .append(btn("[环绕]", "/flyingsword mode_selected orbit", "设定指定飞剑为[环绕]"))
            .append(space())
            .append(btn("[悬浮]", "/flyingsword mode_selected hover", "设定指定飞剑为[悬浮]"));
    player.sendSystemMessage(behavior);

    // 全体一键
    Component all =
        Component.literal("全体: ")
            .append(btn("[攻击]", "/flyingsword mode hunt", "令所有飞剑[出击]"))
            .append(space())
            .append(btn("[守护]", "/flyingsword mode guard", "令所有飞剑[守护]"))
            .append(space())
            .append(btn("[环绕]", "/flyingsword mode orbit", "令所有飞剑[环绕]"))
            .append(space())
            .append(btn("[悬浮]", "/flyingsword mode hover", "令所有飞剑[悬浮]"));
    player.sendSystemMessage(all);

    // 交互栏
    Component actions =
        Component.literal("交互: ")
            .append(btn("[召回]", "/flyingsword ui_active 1", "管理在场飞剑（召回/选中）"))
            .append(space())
            .append(btn("[召唤]", "/flyingsword ui_storage 1", "从存储中逐个召唤飞剑"))
            .append(space())
            .append(btn("[出击]", "/flyingsword mode hunt", "令所有飞剑[出击]"))
            .append(space())
            .append(btn("[管理]", "/flyingsword list", "列出在场飞剑详情"))
            .append(space())
            .append(btn("[修复]", "/flyingsword repair_selected", "消耗主手物品对选中飞剑修复/赋能"));
    player.sendSystemMessage(actions);
    player.sendSystemMessage(hr());

    // 当前选中
    FlyingSwordEntity selected =
        FlyingSwordController.getSelectedSword(player.serverLevel(), player);
    if (selected != null) {
      String info =
          String.format(
              Locale.ROOT,
              "已选中: Lv.%d 模式:%s 耐久:%.0f/%.0f 距离:%.1fm",
              selected.getSwordLevel(),
              selected.getAIMode().getDisplayName(),
              selected.getDurability(),
              selected.getSwordAttributes().maxDurability,
              selected.distanceTo(player));
      player.sendSystemMessage(dim(info));
    } else {
      player.sendSystemMessage(dim("未选中飞剑，先到[召回]列表中选择"));
    }
  }

  // ===== 在场清单（召回/选中） =====
  public static void openActiveList(ServerPlayer player, int page) {
    ServerLevel level = player.serverLevel();
    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);
    player.sendSystemMessage(banner("== 在场飞剑 =="));
    if (swords.isEmpty()) {
      player.sendSystemMessage(dim("无在场飞剑"));
      player.sendSystemMessage(navBack());
      return;
    }
    final int pageSize = 6;
    int total = swords.size();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int p = Math.min(Math.max(1, page), pages);
    int start = (p - 1) * pageSize;
    int end = Math.min(total, start + pageSize);

    player.sendSystemMessage(pageHeader(p, pages, total));
    player.sendSystemMessage(columnsHeaderActive());

    for (int i = start; i < end; i++) {
      FlyingSwordEntity s = swords.get(i);
      int groupId = s.getGroupId();
      MutableComponent line = Component.empty()
          .append(dim(String.format(Locale.ROOT, "#%d ", i + 1)))
          .append(Component.literal(String.format(Locale.ROOT, "Lv.%d  ", s.getSwordLevel())))
          .append(modePill(s.getAIMode()))
          .append(space())
          .append(dim(String.format(Locale.ROOT, "耐久: %.0f/%.0f  ", s.getDurability(), s.getSwordAttributes().maxDurability)))
          .append(dim(String.format(Locale.ROOT, "距离: %.1fm  ", s.distanceTo(player))))
          .append(dim(Component.literal("组: ").append(groupName(groupId)).append(Component.literal("  "))))
          .append(btn("[选中]", "/flyingsword select index " + (i + 1), "设为指定飞剑"))
          .append(space())
          .append(btn("[修复]", "/flyingsword repair_index " + (i + 1), "消耗主手物品修复/赋能此飞剑"))
          .append(space())
          .append(btn("[召回]", "/flyingsword recall_index " + (i + 1), "召回该飞剑"))
          .append(space())
          .append(btn("[攻]", "/flyingsword mode_index " + (i + 1) + " hunt", "设为出击"))
          .append(space())
          .append(btn("[守]", "/flyingsword mode_index " + (i + 1) + " guard", "设为守护"))
          .append(space())
          .append(btn("[环]", "/flyingsword mode_index " + (i + 1) + " orbit", "设为环绕"))
          .append(space())
          .append(btn("[悬]", "/flyingsword mode_index " + (i + 1) + " hover", "设为悬浮"));
      player.sendSystemMessage(line);

      if (groupId != FlyingSwordEntity.SWARM_GROUP_ID) {
        MutableComponent groupLine = Component.literal("    ")
            .append(dim(Component.translatable("text.guzhenren.jianyingu.sword.group", groupName(groupId))));
        groupLine = groupLine
            .append(space())
            .append(groupButtonForSword(i + 1, 0, groupId == 0, "text.guzhenren.jianyingu.command.group.all"))
            .append(space())
            .append(groupButtonForSword(i + 1, 1, groupId == 1, "text.guzhenren.jianyingu.command.group.g1"))
            .append(space())
            .append(groupButtonForSword(i + 1, 2, groupId == 2, "text.guzhenren.jianyingu.command.group.g2"))
            .append(space())
            .append(groupButtonForSword(i + 1, 3, groupId == 3, "text.guzhenren.jianyingu.command.group.g3"));
        player.sendSystemMessage(groupLine);
      } else {
        MutableComponent groupLine = Component.literal("    ")
            .append(dim(Component.translatable("text.guzhenren.jianyingu.sword.group.locked", groupName(groupId))));
        player.sendSystemMessage(groupLine);
      }
    }

    // 分页与返回
    net.minecraft.network.chat.MutableComponent pager = Component.empty();
    if (p > 1) {
      pager = pager.append(btn("[上一页]", "/flyingsword ui_active " + (p - 1), "上一页"));
    }
    pager = pager.append(space()).append(btn("[返回]", "/flyingsword ui", "返回主界面"));
    if (p < pages) {
      pager = pager.append(space()).append(btn("[下一页]", "/flyingsword ui_active " + (p + 1), "下一页"));
    }
    player.sendSystemMessage(pager);
  }

  // ===== 存储清单（逐个召唤） =====
  public static void openStorageList(ServerPlayer player, int page) {
    var storage = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);
    var list = storage.getRecalledSwords();
    player.sendSystemMessage(banner("== 存储飞剑 =="));
    if (list.isEmpty()) {
      player.sendSystemMessage(dim("存储中没有飞剑"));
      player.sendSystemMessage(navBack());
      return;
    }
    final int pageSize = 6;
    int total = list.size();
    int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
    int p = Math.min(Math.max(1, page), pages);
    int start = (p - 1) * pageSize;
    int end = Math.min(total, start + pageSize);
    player.sendSystemMessage(pageHeader(p, pages, total));

    for (int i = start; i < end; i++) {
      var s = list.get(i);
      String name = net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui
          .FlyingSwordTUIOps.getStoredDisplayName(player.serverLevel(), s);
      MutableComponent line = Component.empty()
          .append(dim(String.format(Locale.ROOT, "#%d ", i + 1)))
          .append(Component.literal(String.format(Locale.ROOT, "Lv.%d  ", s.level)))
          .append(dim(String.format(Locale.ROOT, "耐久: %.0f/%.0f  ", s.durability, s.attributes.maxDurability)))
          .append(dim(String.format(Locale.ROOT, "名称: %s  ", name)));

      if (s.itemWithdrawn) {
        line = line
            .append(btn("[放回]", "/flyingsword deposit_index " + (i + 1), "放回存储（UUID 必须匹配）"))
            .append(space())
            .append(dim("（已取出，无法召唤）"));
      } else {
        line = line.append(btn("[召唤]", "/flyingsword restore_index " + (i + 1), "召唤该飞剑"))
            .append(space())
            .append(btn("[取出]", "/flyingsword withdraw_index " + (i + 1), "拿出本体物品（仅认UUID）"));
      }
      player.sendSystemMessage(line);
    }

    // 分页与返回
    net.minecraft.network.chat.MutableComponent pager = Component.empty();
    if (p > 1) {
      pager = pager.append(btn("[上一页]", "/flyingsword ui_storage " + (p - 1), "上一页"));
    }
    pager = pager.append(space()).append(btn("[返回]", "/flyingsword ui", "返回主界面"));
    if (p < pages) {
      pager = pager.append(space()).append(btn("[下一页]", "/flyingsword ui_storage " + (p + 1), "下一页"));
    }
    player.sendSystemMessage(pager);
  }

  // ===== 小工具 =====
  private static Component banner(String text) {
    return Component.literal(text).withStyle(Style.EMPTY.withColor(Theme.ACCENT));
  }

  private static Component space() {
    return Component.literal(" ");
  }

  private static Component navBack() {
    return btn("[返回]", "/flyingsword ui", "返回主界面");
  }

  private static Component btn(String label, String command, String hover) {
    return Component.literal(label)
        .withStyle(
            Style.EMPTY
                .withColor(Theme.BUTTON)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
  }

  // ===== 主题与样式 =====
  private static final class Theme {
    static final int ACCENT = 0xFFD27F; // 标题/分隔
    static final int BUTTON = 0x55FFFF; // 按钮
    static final int DIM = 0xA0A0A0; // 次要信息

    static final int MODE_HUNT = 0xFF5555;
    static final int MODE_GUARD = 0xFFAA00;
    static final int MODE_ORBIT = 0x55AAFF;
    static final int MODE_HOVER = 0x55FFFF;
    static final int MODE_RECALL = 0xAA55FF; // 紫色 - 召回模式
  }

  private static Component dim(String text) {
    return Component.literal(text).withStyle(Style.EMPTY.withColor(Theme.DIM));
  }

  private static Component dim(Component component) {
    return component.copy().withStyle(Style.EMPTY.withColor(Theme.DIM));
  }

  private static Component hr() {
    return Component.literal("——————————————").withStyle(Style.EMPTY.withColor(Theme.DIM));
  }

  private static Component pageHeader(int page, int pages, int total) {
    String txt = String.format(Locale.ROOT, "第 %d/%d 页 · 共 %d 把", page, Math.max(1, pages), total);
    return Component.literal(txt).withStyle(Style.EMPTY.withColor(Theme.ACCENT));
  }

  private static Component modePill(net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode mode) {
    String label = switch (mode) {
      case HUNT -> "[出击]";
      case GUARD -> "[守护]";
      case ORBIT -> "[环绕]";
      case HOVER -> "[悬浮]";
      case RECALL -> "[召回]";
      case SWARM -> "[集群]";
    };
    int color = switch (mode) {
      case HUNT -> Theme.MODE_HUNT;
      case GUARD -> Theme.MODE_GUARD;
      case ORBIT -> Theme.MODE_ORBIT;
      case HOVER -> Theme.MODE_HOVER;
      case RECALL -> Theme.MODE_RECALL;
      case SWARM -> Theme.MODE_GUARD; // 使用守护模式的颜色
    };
    return Component.literal(label).withStyle(Style.EMPTY.withColor(color));
  }

  private static Component columnsHeaderActive() {
    MutableComponent c = Component.empty();
    c = c.append(Component.literal("# ").withStyle(Style.EMPTY.withColor(Theme.ACCENT)));
    c = c.append(Component.literal("等级  ").withStyle(Style.EMPTY.withColor(Theme.ACCENT)));
    c = c.append(Component.literal("模式  ").withStyle(Style.EMPTY.withColor(Theme.ACCENT)));
    c = c.append(Component.literal("耐久        ").withStyle(Style.EMPTY.withColor(Theme.ACCENT)));
    c = c.append(Component.literal("距离   ").withStyle(Style.EMPTY.withColor(Theme.ACCENT)));
    c = c.append(Component.literal("操作").withStyle(Style.EMPTY.withColor(Theme.ACCENT)));
    return c;
  }

  private static Component groupName(int groupId) {
    if (groupId == 0) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.all");
    }
    if (groupId == 1) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.g1");
    }
    if (groupId == 2) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.g2");
    }
    if (groupId == 3) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.g3");
    }
    if (groupId == FlyingSwordEntity.SWARM_GROUP_ID) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.swarm");
    }
    return Component.literal(String.format(Locale.ROOT, "#%d", groupId));
  }

  private static MutableComponent groupButtonForSword(
      int index, int groupId, boolean selected, String nameKey) {
    MutableComponent label =
        Component.literal("[")
            .append(Component.translatable(nameKey))
            .append(Component.literal("]"));
    Style style =
        (selected ? Style.EMPTY.withColor(Theme.BUTTON).withBold(true) : Style.EMPTY.withColor(Theme.DIM))
            .withClickEvent(
                new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    String.format(Locale.ROOT, "/flyingsword group_index %d %d", index, groupId)))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.translatable(
                        "text.guzhenren.jianyingu.sword.group.button.hover", groupName(groupId))));
    return label.withStyle(style);
  }
}
