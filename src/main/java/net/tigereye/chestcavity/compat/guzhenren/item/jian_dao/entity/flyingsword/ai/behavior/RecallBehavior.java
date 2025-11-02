package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 召回行为 - 弧形轨迹返回主人
 *
 * <p>特点：
 * <ul>
 *   <li>弧形轨迹：通过二次贝塞尔曲线计算，形成优雅的弧线</li>
 *   <li>速度变化：从快速接近逐渐减速，最后平稳到达</li>
 *   <li>自动消散：到达主人附近后自动消失</li>
 * </ul>
 */
public class RecallBehavior {

  // 召回完成距离（小于此距离视为到达）
  private static final double ARRIVAL_DISTANCE = 1.0;

  // 初始速度倍数（快速启动）
  private static final double INITIAL_SPEED_FACTOR = 2.5;

  // 最终速度倍数（慢速到达）
  private static final double FINAL_SPEED_FACTOR = 0.3;

  /**
   * 执行召回行为
   *
   * @param sword 飞剑实体
   * @param owner 主人
   */
  public static void tick(FlyingSwordEntity sword, LivingEntity owner) {
    Vec3 ownerPos = owner.getEyePosition();
    Vec3 currentPos = sword.position();
    double distanceToOwner = currentPos.distanceTo(ownerPos);

    // 检查是否到达主人
    if (distanceToOwner < ARRIVAL_DISTANCE) {
      // 召回完成，触发实际召回逻辑
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword
          .FlyingSwordController.recall(sword);
      return;
    }

    // 计算弧形轨迹点
    Vec3 arcTarget = calculateArcPoint(sword, currentPos, ownerPos, distanceToOwner);

    // 计算朝向目标点的方向
    Vec3 direction = arcTarget.subtract(currentPos);
    double distanceToArc = direction.length();

    if (distanceToArc < 0.01) {
      // 已到达弧线点，直接指向主人
      direction = ownerPos.subtract(currentPos);
    }

    direction = direction.normalize();

    // 根据距离计算速度因子（越接近越慢）
    // 使用平滑的减速曲线
    double progress = 1.0 - Math.min(1.0, distanceToOwner / 20.0); // 20格开始减速
    double speedFactor =
        INITIAL_SPEED_FACTOR - (INITIAL_SPEED_FACTOR - FINAL_SPEED_FACTOR) * smoothProgress(progress);

    // 应用速度
    Vec3 desiredVelocity =
        direction.scale(sword.getSwordAttributes().speedMax * speedFactor);

    sword.applySteeringVelocity(desiredVelocity);
  }

  /**
   * 计算弧形轨迹上的下一个目标点
   *
   * <p>使用二次贝塞尔曲线创建弧形效果：
   * <ul>
   *   <li>起点：飞剑当前位置</li>
   *   <li>控制点：在起点和终点之间的上方和侧方</li>
   *   <li>终点：主人位置</li>
   * </ul>
   *
   * @param sword 飞剑实体
   * @param start 起点（当前位置）
   * @param end 终点（主人位置）
   * @param totalDistance 总距离
   * @return 弧形轨迹上的下一个目标点
   */
  private static Vec3 calculateArcPoint(
      FlyingSwordEntity sword, Vec3 start, Vec3 end, double totalDistance) {

    // 计算中点
    Vec3 midpoint = start.add(end).scale(0.5);

    // 计算起点到终点的向量
    Vec3 toEnd = end.subtract(start);

    // 计算垂直于水平面的上方偏移
    // 弧线高度基于距离，远距离时弧线更高
    double arcHeight = Math.min(totalDistance * 0.4, 8.0); // 最高8格

    // 计算侧向偏移（基于飞剑当前速度方向）
    Vec3 currentVelocity = sword.getDeltaMovement();
    Vec3 lateralOffset = Vec3.ZERO;

    if (currentVelocity.length() > 0.1) {
      // 使用当前速度的侧向分量创建弧形偏移
      Vec3 perpendicular = new Vec3(-toEnd.z, 0, toEnd.x).normalize();
      // 根据速度与返回方向的夹角决定侧向偏移
      double lateralScale = currentVelocity.normalize().dot(toEnd.normalize());
      if (Math.abs(lateralScale) < 0.9) { // 不是直线返回
        lateralOffset = perpendicular.scale(totalDistance * 0.2 * (1 - Math.abs(lateralScale)));
      }
    }

    // 控制点：中点 + 上方偏移 + 侧向偏移
    Vec3 controlPoint = midpoint.add(0, arcHeight, 0).add(lateralOffset);

    // 计算贝塞尔曲线参数 t
    // t 基于飞剑到起点的距离比例
    // 使用 tickCount 创建平滑的前进效果
    double baseProgress = 1.0 - (totalDistance / Math.max(20.0, totalDistance + 5.0));
    double t = Math.min(0.95, baseProgress + 0.3); // 始终瞄准曲线上稍前方的点

    // 二次贝塞尔曲线公式：B(t) = (1-t)²P₀ + 2(1-t)tP₁ + t²P₂
    double oneMinusT = 1.0 - t;
    Vec3 arcPoint =
        start
            .scale(oneMinusT * oneMinusT)
            .add(controlPoint.scale(2 * oneMinusT * t))
            .add(end.scale(t * t));

    return arcPoint;
  }

  /**
   * 平滑的进度曲线函数
   *
   * <p>使用 smoothstep 函数创建缓入缓出效果
   *
   * @param t 原始进度 [0, 1]
   * @return 平滑后的进度 [0, 1]
   */
  private static double smoothProgress(double t) {
    // Smoothstep: 3t² - 2t³
    return t * t * (3.0 - 2.0 * t);
  }
}
