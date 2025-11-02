package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 飞剑分离行为 - 避免重叠，保持队形
 *
 * <p>通过计算排斥力向量，让飞剑之间保持合适的距离，避免拥挤和重叠。
 *
 * <p>特点：
 * <ul>
 *   <li>距离越近，排斥力越强（反平方定律）</li>
 *   <li>只影响同一主人的飞剑</li>
 *   <li>平滑的推力，不会造成抖动</li>
 * </ul>
 */
public final class SeparationBehavior {

  private SeparationBehavior() {}

  /**
   * 最小分离距离（格） - 飞剑间距小于此值会施加强排斥力
   *
   * <p>推荐值：0.8-1.2格
   * <ul>
   *   <li>飞剑实体宽度约0.5格，需要留0.3-0.7格的视觉间距</li>
   *   <li>太小（<0.5）：飞剑会视觉重叠，看起来混乱</li>
   *   <li>太大（>1.5）：飞剑过于分散，失去紧密护卫感</li>
   * </ul>
   */
  private static final double MIN_SEPARATION_DISTANCE = 1.0;

  /**
   * 排斥力搜索半径（格） - 超过此距离不再计算排斥力
   *
   * <p>推荐值：2.5-3.5格（应为环绕半径的80-120%）
   * <ul>
   *   <li>8把剑时环绕半径约3.0格，搜索半径应为2.5-3.0格</li>
   *   <li>32把剑时环绕半径约1.5格，搜索半径应为2.0-2.5格</li>
   *   <li>太小（<2.0）：检测不到相邻飞剑，排斥力失效</li>
   *   <li>太大（>4.0）：性能浪费，且会受远处飞剑影响</li>
   * </ul>
   */
  private static final double SEPARATION_SEARCH_RADIUS = 2.8;

  /**
   * 排斥力强度系数
   *
   * <p>推荐值：0.2-0.5
   * <ul>
   *   <li>0.15-0.25：柔和推开，适合悠闲环绕</li>
   *   <li>0.3-0.4：标准强度，平衡响应和稳定性（当前）</li>
   *   <li>0.5-0.8：强力推开，适合密集战斗</li>
   *   <li>太小（<0.1）：推不开，仍会重叠</li>
   *   <li>太大（>1.0）：过度反应，飞剑抖动</li>
   * </ul>
   */
  private static final double SEPARATION_STRENGTH = 0.35;

  /**
   * 计算飞剑的分离向量（排斥力）
   *
   * <p>遍历附近的其他飞剑，计算累积的排斥力向量。
   *
   * @param sword 当前飞剑
   * @return 分离向量（单位化后乘以强度系数）
   */
  public static Vec3 calculateSeparationVector(FlyingSwordEntity sword) {
    Vec3 separationForce = Vec3.ZERO;
    Vec3 swordPos = sword.position();

    // 计算期望高度层（基于UUID哈希，让不同飞剑自然分层）
    double targetHeightOffset = calculateTargetHeightOffset(sword);
    Vec3 ownerPos = sword.getOwner() != null ? sword.getOwner().position() : swordPos;
    double targetHeight = ownerPos.y + 1.5 + targetHeightOffset; // 主人腰部高度 + 偏移

    // 添加垂直归位力：引导飞剑到目标高度
    double currentHeight = swordPos.y;
    double heightDiff = targetHeight - currentHeight;
    if (Math.abs(heightDiff) > 0.3) {
      // 轻微向上/向下的归位力
      separationForce = separationForce.add(new Vec3(0, heightDiff * 0.15, 0));
    }

    // 搜索附近的飞剑
    AABB searchBox = sword.getBoundingBox().inflate(SEPARATION_SEARCH_RADIUS);
    List<FlyingSwordEntity> nearbySwords =
        sword
            .level()
            .getEntitiesOfClass(
                FlyingSwordEntity.class, searchBox, other -> other != sword && !other.isRemoved());

    int count = 0;
    for (FlyingSwordEntity other : nearbySwords) {
      // 只与同一主人的飞剑分离（避免影响敌对飞剑战斗）
      if (sword.getOwner() == null || !sword.isOwnedBy(sword.getOwner())) {
        continue;
      }
      if (other.getOwner() == null || !other.isOwnedBy(sword.getOwner())) {
        continue;
      }

      Vec3 otherPos = other.position();
      Vec3 diff = swordPos.subtract(otherPos);
      double distance = diff.length();

      // 忽略过远的飞剑
      if (distance > SEPARATION_SEARCH_RADIUS || distance < 0.01) {
        continue;
      }

      // 分解为水平和垂直距离
      Vec3 horizontalDiff = new Vec3(diff.x, 0, diff.z);
      double horizontalDistance = horizontalDiff.length();
      double verticalDistance = Math.abs(diff.y);

      // 计算排斥力：距离越近，排斥力越强
      // 使用反平方定律的变体：force = strength / (distance^2 + epsilon)
      double epsilon = 0.1; // 防止除零
      double forceMagnitude =
          SEPARATION_STRENGTH / (distance * distance + epsilon);

      // 距离小于最小分离距离时，施加额外强排斥力
      if (distance < MIN_SEPARATION_DISTANCE) {
        forceMagnitude *= 2.0;
      }

      // 方向：远离其他飞剑 + 向上抬升
      Vec3 forceDirection;

      // 如果水平距离很近但垂直距离小，说明飞剑在同一层，需要向上推
      if (horizontalDistance < MIN_SEPARATION_DISTANCE * 0.8 && verticalDistance < 1.0) {
        // 增加向上分量，让飞剑错层飞行
        Vec3 horizontalForce = horizontalDiff.length() > 0.01
            ? horizontalDiff.normalize()
            : new Vec3(1, 0, 0); // 水平推力
        Vec3 upwardForce = new Vec3(0, 1, 0); // 向上推力

        // 混合：70% 水平 + 30% 向上
        forceDirection = horizontalForce.scale(0.7).add(upwardForce.scale(0.3)).normalize();

        // 同层挤压时加倍推力
        forceMagnitude *= 1.5;
      } else {
        // 正常情况：沿着diff方向推开
        forceDirection = diff.normalize();
      }

      separationForce = separationForce.add(forceDirection.scale(forceMagnitude));
      count++;
    }

    // 如果有多个飞剑，取平均值
    if (count > 0) {
      separationForce = separationForce.scale(1.0 / count);
    }

    return separationForce;
  }

  /**
   * 计算飞剑的目标高度偏移
   *
   * <p>基于飞剑UUID的哈希值，让不同飞剑自然分布在不同高度层。
   * 形成莲花的立体层次感：底层、中层、顶层。
   *
   * @param sword 飞剑实体
   * @return 高度偏移（-1.0 到 +1.0 格）
   */
  private static double calculateTargetHeightOffset(FlyingSwordEntity sword) {
    // 使用UUID的哈希值作为随机种子，但每把剑的值是稳定的
    long hash = sword.getUUID().getMostSignificantBits();
    hash = hash ^ (hash >>> 32); // 混合高低位

    // 归一化到 [-1.0, 1.0] 范围
    // 这样飞剑会分散在主人上下各1格的范围内
    double normalized = ((hash & 0xFFFF) / 65535.0) * 2.0 - 1.0;

    // 稍微偏向上方，形成上升的莲花形态
    return normalized * 0.8 + 0.2; // 范围: [-0.6, 1.0]
  }

  /**
   * 应用分离力到飞剑速度
   *
   * <p>在原有的目标速度基础上，叠加分离向量。
   *
   * @param sword 飞剑
   * @param baseVelocity 基础目标速度（如追击、环绕等）
   * @return 应用分离力后的最终速度
   */
  public static Vec3 applySeparation(FlyingSwordEntity sword, Vec3 baseVelocity) {
    Vec3 separationVector = calculateSeparationVector(sword);

    // 叠加分离向量（排斥力）
    Vec3 finalVelocity = baseVelocity.add(separationVector);

    // 限制速度上限，避免过度加速
    double maxSpeed = sword.getSwordAttributes().speedMax * 1.2; // 允许超出最大速度20%
    double currentLength = finalVelocity.length();
    if (currentLength > maxSpeed) {
      finalVelocity = finalVelocity.normalize().scale(maxSpeed);
    }

    return finalVelocity;
  }
}
