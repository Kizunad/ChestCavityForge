package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering;

import net.minecraft.world.phys.Vec3;

/**
 * 针对旧逻辑返回“原始速度向量”的适配器，将其转换为 {@link SteeringCommand}。
 */
public final class LegacySteeringAdapter {

  private static final double EPS = 1.0e-6;

  private LegacySteeringAdapter() {}

  public static SteeringCommand fromDesiredVelocity(
      Vec3 desiredVelocity, KinematicsSnapshot snapshot) {
    if (desiredVelocity.lengthSqr() < EPS) {
      return SteeringCommand.of(Vec3.ZERO, 0.0);
    }
    Vec3 dir = KinematicsOps.normaliseSafe(desiredVelocity);
    double targetSpeed = desiredVelocity.length();
    double base = Math.max(EPS, snapshot.scaledBaseSpeed());
    double scale = targetSpeed / base;
    return SteeringCommand.of(dir, scale);
  }
}
