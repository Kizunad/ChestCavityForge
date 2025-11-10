package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.JianQiGuSlashProjectile;

/**
 * 剑气蛊特效工具类。
 *
 * <p>提供剑光粒子和断势提示等特效方法。
 */
public final class JianQiGuFx {

  private JianQiGuFx() {}

  /**
   * 为剑气斩击生成拖尾粒子。
   *
   * @param projectile 剑气斩击投射物
   * @param visualPower 视觉威能（0-1）
   * @param level 服务端世界
   */
  public static void spawnSlashTrailParticles(
      JianQiGuSlashProjectile projectile, float visualPower, ServerLevel level) {

    // 根据威能控制粒子数量（威能越高粒子越多）
    int particleCount = (int) (1 + visualPower * 3);

    Vec3 pos = projectile.position();
    Vec3 direction = projectile.getDirection();

    for (int i = 0; i < particleCount; i++) {
      // 在剑光中段或尾端随机生成粒子
      double t = projectile.getRandom().nextDouble();
      Vec3 offset = direction.scale(t);

      double px = pos.x + offset.x;
      double py = pos.y + offset.y;
      double pz = pos.z + offset.z;

      // 略向外扩散
      double vx = (projectile.getRandom().nextDouble() - 0.5) * 0.05;
      double vy = (projectile.getRandom().nextDouble() - 0.5) * 0.05;
      double vz = (projectile.getRandom().nextDouble() - 0.5) * 0.05;

      // 生成白色光点粒子
      level.sendParticles(
          ParticleTypes.GLOW,
          px,
          py,
          pz,
          1,
          vx,
          vy,
          vz,
          0.02);
    }
  }

  /**
   * 播放断势提示特效（达到触发阈值时）。
   *
   * @param entity 宿主实体
   * @param level 服务端世界
   */
  public static void playDuanshiTriggerEffect(LivingEntity entity, ServerLevel level) {
    Vec3 pos = entity.position();

    // 在宿主周围生成环形粒子
    for (int i = 0; i < 12; i++) {
      double angle = (Math.PI * 2.0 * i) / 12.0;
      double radius = 0.8;

      double px = pos.x + Math.cos(angle) * radius;
      double py = pos.y + 1.0;
      double pz = pos.z + Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.ENCHANT,
          px,
          py,
          pz,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }
  }

  /**
   * 播放「一斩开天」激活特效。
   *
   * @param entity 施法者
   * @param level 服务端世界
   */
  public static void playYiZhanKaiTianActivation(LivingEntity entity, ServerLevel level) {
    Vec3 eyePos = entity.getEyePosition(1.0f);
    Vec3 lookVec = entity.getLookAngle();

    // 在施法者前方生成粒子爆发
    for (int i = 0; i < 20; i++) {
      double angle = (Math.PI * 2.0 * i) / 20.0;
      double radius = 0.5;

      double vx = Math.cos(angle) * 0.2 + lookVec.x * 0.3;
      double vy = Math.sin(angle) * 0.2 + lookVec.y * 0.3;
      double vz = lookVec.z * 0.3;

      level.sendParticles(
          ParticleTypes.SWEEP_ATTACK,
          eyePos.x,
          eyePos.y,
          eyePos.z,
          1,
          vx,
          vy,
          vz,
          0.1);
    }
  }
}
