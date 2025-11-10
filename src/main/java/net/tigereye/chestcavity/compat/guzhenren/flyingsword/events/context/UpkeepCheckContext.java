package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 维持消耗检查事件上下文
 *
 * <p>Phase 3: 在每次维持消耗检查时触发（间隔由FlyingSwordTuning配置）。
 *
 * <p>用途：
 *
 * <ul>
 *   <li>修改维持消耗量（如道痕减免、buff加成）
 *   <li>记录消耗历史（统计、可视化）
 *   <li>在消耗前触发特殊逻辑（如低能量预警）
 *   <li>跳过本次消耗（如临时免疫）
 * </ul>
 */
public class UpkeepCheckContext {
  public final FlyingSwordEntity sword;

  /** 基础消耗量 */
  public final double baseCost;

  /** 速度倍率（影响消耗） */
  public final double speedMultiplier;

  /** 检查间隔（tick） */
  public final int tickInterval;

  /** 最终消耗量（可修改） */
  public double finalCost;

  /** 是否跳过本次消耗 */
  public boolean skipConsumption = false;

  public UpkeepCheckContext(
      FlyingSwordEntity sword, double baseCost, double speedMultiplier, int tickInterval) {
    this.sword = sword;
    this.baseCost = baseCost;
    this.speedMultiplier = speedMultiplier;
    this.tickInterval = tickInterval;
    this.finalCost = baseCost * speedMultiplier;
  }
}
