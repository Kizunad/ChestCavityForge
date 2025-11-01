package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 环绕行为 - 绕着主人旋转
 */
public class OrbitBehavior {

  /**
   * 执行环绕行为
   */
  public static void tick(FlyingSwordEntity sword, Player owner) {
    Vec3 ownerPos = owner.getEyePosition();
    Vec3 currentPos = sword.position();
    Vec3 toOwner = ownerPos.subtract(currentPos);
    double distance = toOwner.length();

    Vec3 desiredVelocity;

    double targetDistance = FlyingSwordAITuning.ORBIT_TARGET_DISTANCE;
    double distanceTolerance = FlyingSwordAITuning.ORBIT_DISTANCE_TOLERANCE;

    if (distance > targetDistance + distanceTolerance) {
      // 太远，向主人移动
      Vec3 direction = toOwner.normalize();
      desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedBase
                  * FlyingSwordAITuning.ORBIT_APPROACH_SPEED_FACTOR);
    } else if (distance < targetDistance - distanceTolerance) {
      // 太近，远离主人
      Vec3 direction = toOwner.normalize().scale(-1);
      desiredVelocity =
          direction.scale(
              sword.getSwordAttributes().speedBase
                  * FlyingSwordAITuning.ORBIT_RETREAT_SPEED_FACTOR);
    } else {
      // 距离合适，绕圈飞行
      // 使用垂直于toOwner的切线方向
      Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
      // 添加轻微的向内偏移以维持轨道
      Vec3 radial = toOwner.normalize().scale(-FlyingSwordAITuning.ORBIT_RADIAL_PULL_IN);
      desiredVelocity =
          tangent
              .add(radial)
              .normalize()
              .scale(
                  sword.getSwordAttributes().speedBase
                      * FlyingSwordAITuning.ORBIT_TANGENT_SPEED_FACTOR);
    }

    // 应用转向
    sword.applySteeringVelocity(desiredVelocity);
  }
}
