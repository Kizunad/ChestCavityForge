package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcContexts;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;

/**
 * 维持消耗操作（可测试的轻薄封装）
 *
 * <p>职责：
 * - 计算在给定时间窗口的维持消耗
 * - 尝试从主人处消耗真元
 */
public final class UpkeepOps {

  private UpkeepOps() {}

  /**
   * 计算在 intervalTicks 窗口内的维持消耗量。
   * 该方法纯逻辑，可用于单测。
   */
  public static double computeIntervalUpkeepCost(
      double baseRate,
      AIMode mode,
      boolean sprinting,
      boolean breaking,
      double speedPercent,
      int intervalTicks) {
    double perSecond =
        FlyingSwordCalculator.calculateUpkeep(baseRate, mode, sprinting, breaking, speedPercent);
    return perSecond * (intervalTicks / 20.0);
  }

  /**
   * 尝试在给定窗口内消耗维持成本。
   *
   * <p>玩家：消耗缩放真元
   * <p>非玩家：根据配置模式决定是否消耗血量或不消耗
   *
   * @return 成功消耗返回 true；无法打开资源或不足则返回 false。
   */
  public static boolean consumeIntervalUpkeep(FlyingSwordEntity sword, int intervalTicks) {
    Player owner = sword.getOwner();
    if (owner == null) {
      return false;
    }

    var attrs = sword.getSwordAttributes();
    double speed = sword.getDeltaMovement().length();
    CalcContext ctx = CalcContexts.from(sword);
    double effectiveMax =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .FlyingSwordCalculator.effectiveSpeedMax(attrs.speedMax, ctx);
    double speedPercent = effectiveMax > 0 ? speed / effectiveMax : 0.0;

    double perSecond =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
            .FlyingSwordCalculator.calculateUpkeepWithContext(
                attrs.upkeepRate, sword.getAIMode(), owner.isSprinting(), false, speedPercent, ctx);
    double cost = perSecond * (intervalTicks / 20.0);

    // 使用 ResourceOps 统一消耗逻辑（支持玩家和非玩家）
    return ResourceOps.consumeFlyingSwordUpkeep(
        owner, cost, FlyingSwordTuning.NON_PLAYER_UPKEEP_MODE);
  }
}
