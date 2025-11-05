package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 出击行为 - 主动搜索并攻击敌对实体
 */
public class HuntBehavior {

  /**
   * 执行出击行为
   */
  public static void tick(
      FlyingSwordEntity sword, LivingEntity owner, @Nullable LivingEntity nearestHostile) {
    LivingEntity currentTarget = sword.getTargetEntity();

    // 检查当前目标是否有效
    if (currentTarget != null
        && currentTarget.isAlive()
        && sword.distanceTo(currentTarget) < FlyingSwordAITuning.HUNT_TARGET_VALID_RANGE) {
      // 调试：显示追击
      if (sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[FlyingSword] HUNT mode: Chasing target {}, distance: {}",
            currentTarget.getName().getString(),
            String.format("%.2f", sword.distanceTo(currentTarget)));
      }

      // 环绕斩杀轨迹 - 替代直线追击
      Vec3 orbitPoint = calculateOrbitSlashPoint(sword, currentTarget);
      Vec3 direction = orbitPoint.subtract(sword.position()).normalize();
      Vec3 desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedMax
                  * FlyingSwordAITuning.HUNT_CHASE_MAX_FACTOR);

      BehaviorSteering.commit(sword, desiredVelocity, true);
    } else if (nearestHostile != null) {
      // 找到新目标
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[FlyingSword] HUNT mode: Found NEW target {}, distance: {}",
          nearestHostile.getName().getString(),
          String.format("%.2f", sword.distanceTo(nearestHostile)));

      sword.setTargetEntity(nearestHostile);

      // 环绕斩杀轨迹 - 替代直线追击
      Vec3 orbitPoint = calculateOrbitSlashPoint(sword, nearestHostile);
      Vec3 direction = orbitPoint.subtract(sword.position()).normalize();
      Vec3 desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedMax
                  * FlyingSwordAITuning.HUNT_CHASE_MAX_FACTOR);

      // 应用分离力，避免飞剑重叠
      desiredVelocity = SeparationBehavior.applySeparation(sword, desiredVelocity);

      BehaviorSteering.commit(sword, desiredVelocity, true);
    } else {
      // 没有目标，回到主人身边
      sword.setTargetEntity(null);
      Vec3 ownerPos = owner.getEyePosition();
      Vec3 toOwner = ownerPos.subtract(sword.position());
      double distance = toOwner.length();

      if (distance > FlyingSwordAITuning.HUNT_RETURN_DISTANCE) {
        Vec3 desiredVelocity =
            toOwner.normalize()
                .scale(
                    sword.getSwordAttributes().speedBase
                        * FlyingSwordAITuning.HUNT_RETURN_APPROACH_FACTOR);

        BehaviorSteering.commit(sword, desiredVelocity, true);
      } else {
        // 在主人附近缓慢巡逻
        Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
        Vec3 desiredVelocity =
            tangent.scale(
                sword.getSwordAttributes().speedBase
                    * FlyingSwordAITuning.HUNT_IDLE_TANGENT_FACTOR);

        BehaviorSteering.commit(sword, desiredVelocity, true);
      }
    }
  }

  /**
   * 获取搜索范围
   */
  public static double getSearchRange() {
    return FlyingSwordAITuning.HUNT_SEARCH_RANGE;
  }

  /**
   * 计算环绕斩杀轨迹点
   *
   * <p>飞剑将围绕目标做螺旋环绕运动，并周期性穿过目标中心进行斩杀。
   * 轨迹特点：
   * <ul>
   *   <li>半径周期性变化：从外圈收缩到穿过中心</li>
   *   <li>垂直位置振荡：形成立体螺旋效果</li>
   *   <li>根据目标hitbox大小自适应调整</li>
   * </ul>
   *
   * @param sword 飞剑实体
   * @param target 攻击目标
   * @return 下一个轨迹点
   */
  private static Vec3 calculateOrbitSlashPoint(FlyingSwordEntity sword, LivingEntity target) {
    // 获取目标中心位置（腰部高度）
    Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);

    // 计算基础半径 - 基于目标碰撞箱大小
    double targetWidth = target.getBbWidth();
    double targetHeight = target.getBbHeight();
    double targetSize = Math.max(targetWidth, targetHeight);

    // 外圈半径：目标大小的1.8倍，确保有足够的环绕空间
    double maxRadius = targetSize * 1.8;

    // 半径振荡周期 - 控制穿刺频率
    // 每60 ticks（3秒）完成一次从外圈到中心的穿刺
    double radiusCycle = 60.0;
    double radiusPhase = (sword.tickCount % radiusCycle) / radiusCycle * Math.PI * 2;

    // 半径在 [0.2 * maxRadius, maxRadius] 之间振荡
    // 使用 (1 - cos) / 2 让收缩更平滑，在中心停留更短
    double radiusFactor = 0.2 + 0.8 * (1 - Math.cos(radiusPhase)) / 2;
    double currentRadius = maxRadius * radiusFactor;

    // 角度递增 - 控制环绕速度
    // 速度与半径成反比，靠近中心时旋转更快（类似开普勒定律）
    double baseAngularSpeed = 0.12; // 基础角速度（弧度/tick）
    double angularSpeed = baseAngularSpeed * (maxRadius / Math.max(currentRadius, 0.1));
    double angle = (sword.tickCount * angularSpeed) % (Math.PI * 2);

    // 水平面上的环绕偏移
    double offsetX = Math.cos(angle) * currentRadius;
    double offsetZ = Math.sin(angle) * currentRadius;

    // 垂直振荡 - 形成螺旋效果
    // 振荡周期与半径周期不同，避免轨迹重复
    double verticalCycle = 50.0;
    double verticalPhase = (sword.tickCount % verticalCycle) / verticalCycle * Math.PI * 2;
    double verticalAmplitude = targetHeight * 0.6; // 垂直振幅
    double offsetY = Math.sin(verticalPhase) * verticalAmplitude;

    // 计算最终轨迹点
    Vec3 orbitPoint = targetCenter.add(offsetX, offsetY, offsetZ);

    return orbitPoint;
  }
}
