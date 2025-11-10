package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 寻路上下文 - 封装飞剑寻路所需的所有状态信息
 *
 * <p>用于在不同寻路算法之间传递状态数据,支持状态机驱动的运动系统。
 *
 * @param sword 飞剑实体
 * @param currentPos 当前位置
 * @param currentVel 当前速度向量(m/tick)
 * @param target 目标位置
 * @param owner 主人实体
 * @param world 世界对象
 * @param aMax 最大加速度(m/s²)
 * @param vMax 最大速度(m/s)
 */
public record PathfindingContext(
    Entity sword,
    Vec3 currentPos,
    Vec3 currentVel,
    Vec3 target,
    @Nullable Entity owner,
    Level world,
    double aMax,
    double vMax) {
  /** 获取当前速率标量(m/tick) */
  public double getCurrentSpeed() {
    return currentVel.length();
  }

  /** 获取当前速率(m/s) */
  public double getCurrentSpeedPerSecond() {
    return getCurrentSpeed() * 20.0;
  }

  /** 获取到目标的距离(m) */
  public double getDistanceToTarget() {
    return currentPos.distanceTo(target);
  }

  /** 获取到目标的方向向量(归一化) */
  public Vec3 getDirectionToTarget() {
    Vec3 toTarget = target.subtract(currentPos);
    double dist = toTarget.length();
    if (dist < 0.001) {
      return Vec3.ZERO;
    }
    return toTarget.normalize();
  }

  /**
   * 计算每tick的最大加速度变化(m/tick)
   *
   * <p>公式: Δv_max = aMax / 20
   */
  public double getMaxAccelPerTick() {
    return aMax / 20.0;
  }

  /**
   * 计算刹车距离(m)
   *
   * <p>公式: d = v² / (2a) 即以最大减速度刹停所需的距离
   */
  public double getBrakingDistance() {
    double v = getCurrentSpeed() * 20.0; // 转换为 m/s
    if (v < 0.01) return 0.0;
    return (v * v) / (2.0 * aMax);
  }

  /**
   * 检查是否已到达目标(距离 < 阈值)
   *
   * @param threshold 到达阈值(m)
   */
  public boolean hasReachedTarget(double threshold) {
    return getDistanceToTarget() < threshold;
  }

  /** 构建器 - 从飞剑实体创建寻路上下文 */
  public static PathfindingContext from(
      Entity sword, Vec3 target, @Nullable Entity owner, double aMax, double vMax) {
    return new PathfindingContext(
        sword,
        sword.position(),
        sword.getDeltaMovement(),
        target,
        owner,
        sword.level(),
        aMax,
        vMax);
  }
}
