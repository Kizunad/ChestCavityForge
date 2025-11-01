package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/** 悬浮（靠近并在主人身边静止）。 */
public final class HoverBehavior {
  private HoverBehavior() {}

  public static void tick(FlyingSwordEntity sword, Player owner) {
    if (sword == null || owner == null) return;
    Vec3 toOwner = owner.position().subtract(sword.position());
    double dist = toOwner.length();

    double keep = FlyingSwordAITuning.HOVER_FOLLOW_DISTANCE;
    if (dist > keep) {
      // 逼近
      Vec3 dir = toOwner.normalize();
      double speed = sword.getSwordAttributes().speedBase * FlyingSwordAITuning.HOVER_APPROACH_FACTOR;
      Vec3 desired = dir.scale(speed);
      sword.applySteeringVelocity(desired);
    } else {
      // 静止（微幅阻尼）
      Vec3 damped = sword.getDeltaMovement().scale(0.6);
      sword.setDeltaMovement(damped);
      if (damped.lengthSqr() < 1.0e-6) {
        sword.setDeltaMovement(Vec3.ZERO);
      }
      sword.setTargetEntity(null);
    }
  }
}

