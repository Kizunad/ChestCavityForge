package net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.resource.UpkeepOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;

/**
 * Phase 2: 维持系统 (Upkeep System)
 *
 * <p>职责:
 * <ul>
 *   <li>检查维持消耗间隔</li>
 *   <li>调用 ResourceOps 消耗真元</li>
 *   <li>触发 OnUpkeepCheck 事件</li>
 *   <li>处理维持不足的回调 (召回/消散)</li>
 * </ul>
 *
 * <p>设计原则:
 * <ul>
 *   <li>无状态: 所有方法为静态方法，不持有实例变量</li>
 *   <li>事件驱动: 关键操作触发事件钩子</li>
 *   <li>可测试: 输入输出明确，便于单元测试</li>
 * </ul>
 *
 * <p>实现说明:
 * <ul>
 *   <li>Phase 2 保持现有维持逻辑不变，仅重构调用方式</li>
 *   <li>委托给 UpkeepOps 处理资源消耗</li>
 *   <li>Phase 3 将扩展 OnUpkeepCheck 事件，允许外部模块修改消耗量</li>
 * </ul>
 */
public final class UpkeepSystem {

  private UpkeepSystem() {
    // 禁止实例化
  }

  /**
   * Phase 2: 维持系统 Tick 入口
   *
   * <p>执行维持消耗检查，若维持不足则召回飞剑
   *
   * @param sword 飞剑实体
   * @param upkeepTicks 当前维持 tick 计数
   * @return 新的维持 tick 计数
   */
  public static int tick(FlyingSwordEntity sword, int upkeepTicks) {
    if (sword == null || sword.level() == null) {
      return upkeepTicks;
    }

    // Phase 2: 服务端检查
    if (!(sword.level() instanceof ServerLevel)) {
      return upkeepTicks;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null || !owner.isAlive()) {
      // Phase 2: 无主人，直接消散
      sword.discard();
      return upkeepTicks;
    }

    // Phase 2: 增加 tick 计数
    upkeepTicks++;

    // Phase 2: 检查是否到达维持间隔
    if (upkeepTicks >= FlyingSwordTuning.UPKEEP_CHECK_INTERVAL) {
      upkeepTicks = 0; // 重置计数器

      // Phase 3: 触发 UpkeepCheck 事件
      var attrs = sword.getSwordAttributes();
      double speed = sword.getDeltaMovement().length();
      var ctx = net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContexts.from(sword);
      double effectiveMax = net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.FlyingSwordCalculator
          .effectiveSpeedMax(attrs.speedMax, ctx);
      double speedPercent = effectiveMax > 0 ? speed / effectiveMax : 0.0;

      boolean sprinting = (owner instanceof net.minecraft.world.entity.player.Player player)
          && player.isSprinting();

      double baseCost = UpkeepOps.computeIntervalUpkeepCost(
          attrs.upkeepRate,
          sword.getAIMode(),
          sprinting,
          false,
          speedPercent,
          FlyingSwordTuning.UPKEEP_CHECK_INTERVAL);

      // Phase 3：事件上下文（finalCost 默认等于 baseCost，为简化不额外乘速度倍率）
      var upkeepCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.UpkeepCheckContext(
          sword, baseCost, /*speedMultiplier*/ 1.0, FlyingSwordTuning.UPKEEP_CHECK_INTERVAL);

      net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventRegistry
          .fireUpkeepCheck(upkeepCtx);

      // Phase 3: 检查是否跳过本次消耗
      if (upkeepCtx.skipConsumption) {
        return upkeepTicks; // 跳过消耗，继续运行
      }

      // Phase 2/3: 尝试消耗维持资源（优先使用事件调整后的 finalCost）
      boolean success = UpkeepOps.consumeFixedUpkeep(sword, upkeepCtx.finalCost);

      if (!success) {
        // Phase 2: 维持不足，播放音效
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
            .playOutOfEnergy(sword);

        // Phase 2: 召回飞剑
        FlyingSwordController.recall(sword);
      }
    }

    return upkeepTicks;
  }

  /**
   * Phase 2: 计算单次维持消耗 (不执行消耗)
   *
   * <p>用于 UI 显示和调试
   *
   * @param sword 飞剑实体
   * @param ticks 维持 tick 数
   * @return 计算的消耗量
   */
  public static double calculateUpkeepCost(FlyingSwordEntity sword, int ticks) {
    if (sword == null) {
      return 0.0;
    }

    // Phase 2: 委托给 UpkeepOps 计算
    // 使用现有的 computeIntervalUpkeepCost 方法
    var attrs = sword.getSwordAttributes();
    double speed = sword.getDeltaMovement().length();
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContext ctx =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContexts.from(sword);
    double effectiveMax =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.FlyingSwordCalculator
            .effectiveSpeedMax(attrs.speedMax, ctx);
    double speedPercent = effectiveMax > 0 ? speed / effectiveMax : 0.0;

    LivingEntity owner = sword.getOwner();
    boolean sprinting = (owner instanceof net.minecraft.world.entity.player.Player player)
        && player.isSprinting();

    return UpkeepOps.computeIntervalUpkeepCost(
        attrs.upkeepRate,
        sword.getAIMode(),
        sprinting,
        false, // breaking (暂不支持)
        speedPercent,
        ticks);
  }

  // ========== Phase 3 扩展预留接口 ==========

  /**
   * Phase 3 预留: OnUpkeepCheck 事件钩子
   *
   * <p>在维持消耗前触发，允许外部模块修改消耗量或跳过消耗
   *
   * <p>当前 Phase 2 暂不实现，保持与原逻辑一致
   */
  // public static void fireUpkeepCheckEvent(FlyingSwordEntity sword, int intervalTicks) {
  //   // Phase 3: 触发 OnUpkeepCheck 事件
  //   // 允许扩展模块：
  //   // - 修改消耗量 (如减免、加成)
  //   // - 跳过消耗 (如领域内免费维持)
  //   // - 提前召回 (如特殊状态)
  // }

  /**
   * Phase 3 预留: 维持失败策略可配置
   *
   * <p>当前 Phase 2 硬编码为"召回"，Phase 4 将支持多种策略：
   * <ul>
   *   <li>RECALL: 召回到物品栏 (当前默认)</li>
   *   <li>DISCARD: 直接消散</li>
   *   <li>HOVER: 切换到悬停模式</li>
   *   <li>CUSTOM: 由扩展模块处理</li>
   * </ul>
   *
   * <p>Phase 4 实现时将从配置读取策略
   */
  // public enum UpkeepFailureStrategy {
  //   RECALL,
  //   DISCARD,
  //   HOVER,
  //   CUSTOM
  // }
}
