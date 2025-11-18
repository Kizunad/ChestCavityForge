package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common;

import javax.annotation.Nullable;

/**
 * 魂道战斗计算上下文（不可变值对象）。
 *
 * <p>用于在各 Calculator 之间传递只读的环境参数，如：
 *
 * <ul>
 *   <li>当前魂魄值 / 最大魂魄值
 *   <li>联动通道增益（efficiency）
 *   <li>堆叠数（stackCount）
 * </ul>
 *
 * <p>设计原则：Calculator 不可变更 Minecraft 对象，所有计算仅返回数值结果。
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunDaoCalcContext {

  private final double currentHunpo;
  private final double maxHunpo;
  private final double efficiency;
  private final int stackCount;

  private HunDaoCalcContext(
      double currentHunpo, double maxHunpo, double efficiency, int stackCount) {
    this.currentHunpo = Math.max(0.0, currentHunpo);
    this.maxHunpo = Math.max(0.0, maxHunpo);
    this.efficiency = Math.max(0.0, efficiency);
    this.stackCount = Math.max(1, stackCount);
  }

  /**
   * 创建计算上下文。
   *
   * @param currentHunpo 当前魂魄值
   * @param maxHunpo 最大魂魄值
   * @param efficiency 联动通道增益（1.0 表示无增益）
   * @param stackCount 堆叠数
   * @return 新的上下文实例
   */
  public static HunDaoCalcContext create(
      double currentHunpo, double maxHunpo, double efficiency, int stackCount) {
    return new HunDaoCalcContext(currentHunpo, maxHunpo, efficiency, stackCount);
  }

  /**
   * 创建默认上下文（无增益、单堆叠）。
   *
   * @param currentHunpo 当前魂魄值
   * @param maxHunpo 最大魂魄值
   * @return 新的上下文实例
   */
  public static HunDaoCalcContext createDefault(double currentHunpo, double maxHunpo) {
    return new HunDaoCalcContext(currentHunpo, maxHunpo, 1.0, 1);
  }

  public double getCurrentHunpo() {
    return currentHunpo;
  }

  public double getMaxHunpo() {
    return maxHunpo;
  }

  public double getEfficiency() {
    return efficiency;
  }

  public int getStackCount() {
    return stackCount;
  }

  /**
   * 返回带有新的 efficiency 值的副本。
   *
   * @param newEfficiency 新的增益系数
   * @return 新的上下文实例
   */
  public HunDaoCalcContext withEfficiency(double newEfficiency) {
    return new HunDaoCalcContext(currentHunpo, maxHunpo, newEfficiency, stackCount);
  }

  /**
   * 返回带有新的 stackCount 值的副本。
   *
   * @param newStackCount 新的堆叠数
   * @return 新的上下文实例
   */
  public HunDaoCalcContext withStackCount(int newStackCount) {
    return new HunDaoCalcContext(currentHunpo, maxHunpo, efficiency, newStackCount);
  }

  @Override
  public String toString() {
    return String.format(
        "HunDaoCalcContext{hunpo=%.2f/%.2f, efficiency=%.2f, stack=%d}",
        currentHunpo, maxHunpo, efficiency, stackCount);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof HunDaoCalcContext)) {
      return false;
    }
    HunDaoCalcContext other = (HunDaoCalcContext) obj;
    return Double.compare(currentHunpo, other.currentHunpo) == 0
        && Double.compare(maxHunpo, other.maxHunpo) == 0
        && Double.compare(efficiency, other.efficiency) == 0
        && stackCount == other.stackCount;
  }

  @Override
  public int hashCode() {
    int result = 17;
    long temp;
    temp = Double.doubleToLongBits(currentHunpo);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(maxHunpo);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(efficiency);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + stackCount;
    return result;
  }
}
