package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianLiaoGuTuning;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 剑疗蛊相关的纯计算函数。
 */
public final class JianLiaoGuCalc {

  private JianLiaoGuCalc() {}

  /** 读取玩家当前的剑道道痕（缺省0）。 */
  public static double readSwordScar(ServerPlayer player) {
    if (player == null) {
      return 0.0;
    }
    return GuzhenrenResourceBridge
        .open(player)
        .flatMap(
            handle -> {
              OptionalDouble value = handle.read("daohen_jiandao");
              return value.isPresent() ? Optional.of(value.getAsDouble()) : Optional.empty();
            })
        .orElse(0.0);
  }

  /**
   * 心跳治疗量：基础值 + 道痕增强，并限制在最大生命的固定比例以内。
   */
  public static float heartbeatHeal(float maxHealth, double swordScar) {
    double bonus = Math.max(0.0, swordScar) / 100.0 * JianLiaoGuTuning.HEARTBEAT_HEAL_PER_100_SCAR;
    double heal = Math.max(0.0, JianLiaoGuTuning.HEARTBEAT_HEAL_BASE + bonus);
    double healCap = Math.max(0.2, maxHealth * JianLiaoGuTuning.HEARTBEAT_HEAL_CAP_RATIO);
    return (float) Math.min(heal, healCap);
  }

  /** 判定飞剑是否为低耐久。 */
  public static boolean isLowDurability(FlyingSwordEntity sword) {
    if (sword == null) {
      return false;
    }
    double max = Math.max(1.0, sword.getSwordAttributes().maxDurability);
    double ratio = sword.getDurability() / max;
    return ratio < JianLiaoGuTuning.LOW_DURABILITY_THRESHOLD;
  }

  /** 健康飞剑单次可扣减的耐久量（不足时返回0）。 */
  public static double donorCost(FlyingSwordEntity donor) {
    if (donor == null) {
      return 0.0;
    }
    FlyingSwordAttributes attrs = donor.getSwordAttributes();
    double max = Math.max(1.0, attrs.maxDurability);
    double cost = JianLiaoGuTuning.DONOR_COST_FRACTION * max;
    if (donor.getDurability() <= cost) {
      return 0.0;
    }
    return cost;
  }

  /**
   * 捐献池可用于修复的净值（扣除税率）。
   */
  public static double donorNetFromCost(double cost) {
    if (cost <= 0.0) {
      return 0.0;
    }
    return cost * (1.0 - JianLiaoGuTuning.DONOR_TAX_FRACTION);
  }

  /**
   * 给指定飞剑应用互补修复时的增量上限。
   */
  public static double repairCapPerTarget(FlyingSwordEntity target) {
    if (target == null) {
      return 0.0;
    }
    double max = Math.max(1.0, target.getSwordAttributes().maxDurability);
    double missing = Math.max(0.0, max - target.getDurability());
    double cap = JianLiaoGuTuning.TARGET_ONCE_CAP_RATIO * max;
    return Math.min(cap, missing);
  }

  /**
   * 激活时，根据道痕计算修复效率倍率（基础1.0）。
   */
  public static double activeEfficiency(double swordScar) {
    double bonus = Math.max(0.0, swordScar) / 100.0 * JianLiaoGuTuning.EFFICIENCY_PER_100_SCAR;
    return Math.max(1.0, 1.0 + bonus);
  }

  /**
   * 激活后的冷却时间（tick），随道痕缩短并受上下限约束。
   */
  public static int activeCooldownTicks(double swordScar) {
    double reduction = Math.max(0.0, swordScar) / 100.0 * JianLiaoGuTuning.COOLDOWN_REDUCTION_PER_100_SCAR;
    reduction = Math.min(JianLiaoGuTuning.COOLDOWN_REDUCTION_CAP, reduction);
    int base = Math.max(JianLiaoGuTuning.ACTIVE_MIN_COOLDOWN_T, JianLiaoGuTuning.ACTIVE_BASE_COOLDOWN_T);
    int reduced = (int) Math.round(base * (1.0 - reduction));
    return Math.max(JianLiaoGuTuning.ACTIVE_MIN_COOLDOWN_T, Math.min(base, reduced));
  }

  /**
   * 计算激活修复增量。
   */
  public static double activeRepairAmount(
      FlyingSwordEntity sword, float hpSpend, float maxHealth, double swordScar) {
    if (sword == null) {
      return 0.0;
    }
    FlyingSwordAttributes attrs = sword.getSwordAttributes();
    double max = Math.max(1.0, attrs.maxDurability);
    double fraction = Math.min(1.0, hpSpend / Math.max(1.0f, maxHealth));
    double efficiency = activeEfficiency(swordScar);
    return max * fraction * efficiency;
  }
}
