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
      FlyingSwordEntity sword, Player owner, @Nullable LivingEntity nearestHostile) {
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

      // 继续追击当前目标
      Vec3 targetPos = currentTarget.getEyePosition();
      Vec3 direction = targetPos.subtract(sword.position()).normalize();
      Vec3 desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedMax
                  * FlyingSwordAITuning.HUNT_CHASE_MAX_FACTOR);
      sword.applySteeringVelocity(desiredVelocity);
    } else if (nearestHostile != null) {
      // 找到新目标
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[FlyingSword] HUNT mode: Found NEW target {}, distance: {}",
          nearestHostile.getName().getString(),
          String.format("%.2f", sword.distanceTo(nearestHostile)));

      sword.setTargetEntity(nearestHostile);
      Vec3 targetPos = nearestHostile.getEyePosition();
      Vec3 direction = targetPos.subtract(sword.position()).normalize();
      Vec3 desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedMax
                  * FlyingSwordAITuning.HUNT_CHASE_MAX_FACTOR);
      sword.applySteeringVelocity(desiredVelocity);
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
        sword.applySteeringVelocity(desiredVelocity);
      } else {
        // 在主人附近缓慢巡逻
        Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
        Vec3 desiredVelocity =
            tangent.scale(
                sword.getSwordAttributes().speedBase
                    * FlyingSwordAITuning.HUNT_IDLE_TANGENT_FACTOR);
        sword.applySteeringVelocity(desiredVelocity);
      }
    }
  }

  /**
   * 获取搜索范围
   */
  public static double getSearchRange() {
    return FlyingSwordAITuning.HUNT_SEARCH_RANGE;
  }
}
