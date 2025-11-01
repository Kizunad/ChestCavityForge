package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordRepairTuning;

/**
 * 飞剑修复/赋能 操作集合。
 *
 * <p>通过“主手物品 → 处理器”的注册表实现扩展，每次消耗1个物品，对选中或按序号的在场飞剑
 * 进行修复（恢复部分耐久）并施加属性加成。
 */
public final class RepairOps {

  /** 物品ID → 处理器 映射。 */
  private static final Map<ResourceLocation, RepairHandler> REGISTRY = new LinkedHashMap<>();

  private RepairOps() {}

  /**
   * 处理“修复选中飞剑”。
   */
  public static boolean repairSelected(ServerLevel level, ServerPlayer player) {
    FlyingSwordEntity sword = FlyingSwordController.getSelectedSword(level, player);
    if (sword == null) {
      player.sendSystemMessage(Component.literal("[飞剑] 未选中飞剑"));
      return false;
    }
    return applyRepair(level, player, sword);
  }

  /**
   * 处理“按在场序号修复”。
   */
  public static boolean repairByIndex(ServerLevel level, ServerPlayer player, int index1) {
    var list = FlyingSwordController.getPlayerSwords(level, player);
    if (index1 < 1 || index1 > list.size()) {
      player.sendSystemMessage(Component.literal("[飞剑] 序号超出范围"));
      return false;
    }
    return applyRepair(level, player, list.get(index1 - 1));
  }

  private static boolean applyRepair(ServerLevel level, ServerPlayer player, FlyingSwordEntity sword) {
    ItemStack main = player.getMainHandItem();
    if (main == null || main.isEmpty()) {
      player.sendSystemMessage(Component.literal("[飞剑] 主手没有可用物品"));
      return false;
    }

    Item item = main.getItem();
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
    RepairHandler handler = REGISTRY.get(id);
    if (handler == null) {
      player.sendSystemMessage(
          Component.literal(
              String.format(Locale.ROOT, "[飞剑] %s 不支持用于修复/赋能", id.toString())));
      return false;
    }

    // 消耗一个物品
    main.shrink(1);

    // 执行处理器（内部完成属性修改与修复）
    Result res = handler.apply(level, player, sword);
    if (!res.success) {
      // 失败时不返还物品（保持简单，避免复制漏洞）
      player.sendSystemMessage(Component.literal("[飞剑] 修复失败"));
      return false;
    }

    // 成功反馈
    player.sendSystemMessage(
        Component.literal(
            String.format(
                Locale.ROOT,
                "[飞剑] 修复完成：+%.0f耐久%s",
                res.repaired,
                res.extraNote.isEmpty() ? "" : ("，" + res.extraNote))));
    return true;
  }

  // ===== 注册表与默认处理器 =====

  /** 注册一个修复处理器。 */
  public static void register(ResourceLocation itemId, RepairHandler handler) {
    REGISTRY.put(itemId, handler);
  }

  /** 初始化默认处理器（铁/金/红石/绿宝石/钻石/下界合金方块）。 */
  public static void initDefaults() {
    // 铁块：最大耐久 +1，修复一定比例
    register(ResourceLocation.withDefaultNamespace("iron_block"), (level, player, sword) -> {
      var attrs = sword.getSwordAttributes();
      attrs.maxDurability = Math.max(1.0, attrs.maxDurability + 1.0);
      sword.setSwordAttributes(attrs); // 同步生命与上限
      double repaired = doRepairPercent(sword, FlyingSwordRepairTuning.REPAIR_PERCENT_PER_USE);
      return Result.ok(repaired, "最大耐久+1");
    });

    // 金块：基础速度 +0.001
    register(ResourceLocation.withDefaultNamespace("gold_block"), (level, player, sword) -> {
      var attrs = sword.getSwordAttributes();
      attrs.speedBase += 0.001;
      sword.setSwordAttributes(attrs);
      double repaired = doRepairPercent(sword, FlyingSwordRepairTuning.REPAIR_PERCENT_PER_USE);
      return Result.ok(repaired, "基础速度+0.001");
    });

    // 红石块：基础伤害 +0.001
    register(ResourceLocation.withDefaultNamespace("redstone_block"), (level, player, sword) -> {
      var attrs = sword.getSwordAttributes();
      attrs.damageBase += 0.001;
      sword.setSwordAttributes(attrs);
      double repaired = doRepairPercent(sword, FlyingSwordRepairTuning.REPAIR_PERCENT_PER_USE);
      return Result.ok(repaired, "基础伤害+0.001");
    });

    // 绿宝石块：耐久损耗效率 -0.001（下限保护 ≥ 0）
    register(ResourceLocation.withDefaultNamespace("emerald_block"), (level, player, sword) -> {
      var attrs = sword.getSwordAttributes();
      attrs.duraLossRatio = Math.max(0.0, attrs.duraLossRatio - 0.001);
      sword.setSwordAttributes(attrs);
      double repaired = doRepairPercent(sword, FlyingSwordRepairTuning.REPAIR_PERCENT_PER_USE);
      return Result.ok(repaired, "耐久损耗效率-0.001");
    });

    // 钻石块：基础伤害 +0.01
    register(ResourceLocation.withDefaultNamespace("diamond_block"), (level, player, sword) -> {
      var attrs = sword.getSwordAttributes();
      attrs.damageBase += 0.01;
      sword.setSwordAttributes(attrs);
      double repaired = doRepairPercent(sword, FlyingSwordRepairTuning.REPAIR_PERCENT_PER_USE);
      return Result.ok(repaired, "基础伤害+0.01");
    });

    // 下界合金块：每次攻击追加真伤 +0.01
    register(ResourceLocation.withDefaultNamespace("netherite_block"), (level, player, sword) -> {
      var attrs = sword.getSwordAttributes();
      attrs.trueDamagePerHit += 0.01;
      sword.setSwordAttributes(attrs);
      double repaired = doRepairPercent(sword, FlyingSwordRepairTuning.REPAIR_PERCENT_PER_USE);
      return Result.ok(repaired, "每次追加真伤+0.01");
    });
  }

  private static double doRepairPercent(FlyingSwordEntity sword, double percent) {
    percent = Math.max(0.0, percent);
    double max = sword.getSwordAttributes().maxDurability;
    double amount = max * percent;
    float before = sword.getDurability();
    sword.setDurability((float) Math.min(max, before + amount));
    return sword.getDurability() - before;
  }

  // ===== 类型定义 =====

  @FunctionalInterface
  public interface RepairHandler {
    Result apply(ServerLevel level, ServerPlayer player, FlyingSwordEntity sword);
  }

  public static final class Result {
    public final boolean success;
    public final double repaired;
    public final String extraNote;

    private Result(boolean success, double repaired, String extraNote) {
      this.success = success;
      this.repaired = repaired;
      this.extraNote = extraNote == null ? "" : extraNote;
    }

    public static Result ok(double repaired, String note) {
      return new Result(true, repaired, note);
    }
  }

  // 静态块：默认注册
  static {
    initDefaults();
  }
}

