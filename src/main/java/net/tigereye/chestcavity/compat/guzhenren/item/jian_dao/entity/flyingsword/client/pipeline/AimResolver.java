package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.client.pipeline;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.client.profile.SwordVisualProfile.AlignMode;

/** 统一朝向解析（纯函数，可单测）。 */
public final class AimResolver {
  private AimResolver() {}

  public static Vec3 resolve(FlyingSwordEntity sword, AlignMode mode) {
    if (sword == null) return new Vec3(0, 0, -1);
    return switch (mode) {
      case TARGET -> {
        LivingEntity tgt = sword.getTargetEntity();
        if (tgt != null && tgt.isAlive()) {
          Vec3 dir = tgt.position().add(0, tgt.getBbHeight() * 0.5, 0).subtract(sword.position());
          if (dir.lengthSqr() > 1.0E-6) yield dir.normalize();
        }
        yield fallbackVelocity(sword);
      }
      case OWNER -> {
        LivingEntity owner = sword.getOwner();
        if (owner != null && owner.isAlive()) {
          Vec3 dir = owner.getEyePosition().subtract(sword.position());
          if (dir.lengthSqr() > 1.0E-6) yield dir.normalize();
        }
        yield fallbackVelocity(sword);
      }
      case NONE -> new Vec3(0, 0, -1);
      default -> fallbackVelocity(sword);
    };
  }

  private static Vec3 fallbackVelocity(FlyingSwordEntity sword) {
    Vec3 look = sword.getSmoothedLookAngle();
    if (look.lengthSqr() > 1.0E-6) return look;
    return new Vec3(0, 0, -1);
  }
}

