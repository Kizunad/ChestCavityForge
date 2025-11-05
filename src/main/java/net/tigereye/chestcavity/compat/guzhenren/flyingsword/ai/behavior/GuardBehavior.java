package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 防守行为 - 跟随主人并攻击附近的敌对实体
 */
public class GuardBehavior {

  /**
   * 执行防守行为
   */
  public static void tick(
      FlyingSwordEntity sword, LivingEntity owner, @Nullable LivingEntity nearestHostile) {
    if (nearestHostile != null) {
      // 调试：显示找到目标
      if (sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[FlyingSword] GUARD mode: Found target {}, distance: {}",
            nearestHostile.getName().getString(),
            String.format("%.2f", sword.distanceTo(nearestHostile)));
      }

      sword.setTargetEntity(nearestHostile);
      // 追击目标
      Vec3 targetPos = nearestHostile.getEyePosition();
      Vec3 direction = targetPos.subtract(sword.position()).normalize();
      Vec3 desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedMax
                  * FlyingSwordAITuning.GUARD_CHASE_MAX_FACTOR);

      BehaviorSteering.commit(sword, desiredVelocity, true);
    } else {
      sword.setTargetEntity(null);
      // 跟随主人
      Vec3 ownerPos = owner.getEyePosition();
      Vec3 toOwner = ownerPos.subtract(sword.position());
      double distance = toOwner.length();
      double followDistance = FlyingSwordAITuning.GUARD_FOLLOW_DISTANCE;

      if (distance > followDistance) {
        // 向主人移动
        Vec3 desiredVelocity =
            toOwner.normalize()
                .scale(
                    sword.getSwordAttributes().speedBase
                        * FlyingSwordAITuning.GUARD_FOLLOW_APPROACH_FACTOR);

        BehaviorSteering.commit(sword, desiredVelocity, true);
      } else {
        // 在主人附近缓慢环绕
        Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
        Vec3 desiredVelocity =
            tangent.scale(
                sword.getSwordAttributes().speedBase
                    * FlyingSwordAITuning.GUARD_IDLE_TANGENT_FACTOR);

        BehaviorSteering.commit(sword, desiredVelocity, true);
      }
    }
  }

  /**
   * 获取搜索范围
   */
  public static double getSearchRange() {
    return FlyingSwordAITuning.GUARD_SEARCH_RANGE;
  }
}
