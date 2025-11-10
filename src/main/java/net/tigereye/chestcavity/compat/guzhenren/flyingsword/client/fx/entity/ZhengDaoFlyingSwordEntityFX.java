package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 正道飞剑粒子特效
 *
 * <p>使用青色、亮色、光芒类粒子效果。 体现正道飞剑的清澈、明亮、圣洁特征。
 */
public class ZhengDaoFlyingSwordEntityFX implements IFlyingSwordEntityFX {

  private static final ZhengDaoFlyingSwordEntityFX INSTANCE = new ZhengDaoFlyingSwordEntityFX();

  private ZhengDaoFlyingSwordEntityFX() {}

  public static ZhengDaoFlyingSwordEntityFX getInstance() {
    return INSTANCE;
  }

  @Override
  public void spawnFlightTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();
    double speed = velocity.length();

    int particleCount = (int) Math.max(1, speed * 10);

    // 青白色光芒粒子（末地烛光）
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.2;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

      level.sendParticles(
          ParticleTypes.END_ROD,
          pos.x + offsetX,
          pos.y + offsetY,
          pos.z + offsetZ,
          1,
          0.0,
          0.0,
          0.0,
          0.0);
    }
  }

  @Override
  public void spawnAttackImpact(
      ServerLevel level, FlyingSwordEntity sword, Vec3 pos, double damage) {
    int particleCount = (int) Math.min(20, 5 + damage);

    // 横扫粒子
    level.sendParticles(
        ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);

    // 发光粒子（明亮）
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

      level.sendParticles(
          ParticleTypes.GLOW,
          pos.x + offsetX,
          pos.y + 0.5 + offsetY,
          pos.z + offsetZ,
          1,
          offsetX * 0.1,
          offsetY * 0.1,
          offsetZ * 0.1,
          0.1);
    }

    // 额外的青色火花
    for (int i = 0; i < 5; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.4;
      double offsetY = level.random.nextDouble() * 0.5;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.4;

      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME,
          pos.x + offsetX,
          pos.y + 0.8 + offsetY,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.0);
    }
  }

  @Override
  public void spawnRecallEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 螺旋上升的发光鱿鱼墨水（青色）
    for (int i = 0; i < 30; i++) {
      double angle = (i / 30.0) * Math.PI * 4;
      double radius = 0.5;
      double height = (i / 30.0) * 2.0;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.GLOW_SQUID_INK,
          pos.x + offsetX,
          pos.y + height,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 中心青色火焰爆发
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.05);
  }

  @Override
  public void spawnLevelUpEffect(ServerLevel level, FlyingSwordEntity sword, int newLevel) {
    Vec3 pos = sword.position();

    // 发光粒子闪耀
    level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);

    // 青白闪光环形扩散
    for (int i = 0; i < 16; i++) {
      double angle = (i / 16.0) * Math.PI * 2;
      double radius = 1.0;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.WAX_OFF,
          pos.x + offsetX,
          pos.y + 0.5,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 额外的光芒四射效果
    for (int i = 0; i < 8; i++) {
      double angle = (i / 8.0) * Math.PI * 2;
      double radius = 0.5;

      double velocityX = Math.cos(angle) * 0.2;
      double velocityZ = Math.sin(angle) * 0.2;

      level.sendParticles(
          ParticleTypes.END_ROD, pos.x, pos.y + 0.5, pos.z, 1, velocityX, 0.1, velocityZ, 0.1);
    }
  }

  @Override
  public void spawnSpeedBoostEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();
    Vec3 reverseVelocity = velocity.normalize().scale(-0.2);

    // 音爆效果
    level.sendParticles(
        ParticleTypes.SONIC_BOOM,
        pos.x,
        pos.y,
        pos.z,
        1,
        reverseVelocity.x,
        reverseVelocity.y,
        reverseVelocity.z,
        0.0);

    // 额外的青色光芒尾迹
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME,
        pos.x,
        pos.y,
        pos.z,
        3,
        reverseVelocity.x * 0.5,
        reverseVelocity.y * 0.5,
        reverseVelocity.z * 0.5,
        0.05);
  }

  @Override
  public void spawnOrbitTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 青色火焰轨迹
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
  }

  @Override
  public void spawnHuntTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 电火花（青白色）
    level.sendParticles(
        ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
  }

  @Override
  public void spawnGuardTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 青色光点守护
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
  }

  @Override
  public void spawnRecallTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 青色光芒轨迹 - 正道飞剑返回
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.01);
    // 发光鱿鱼墨水（青色）
    if (level.random.nextFloat() < 0.3f) {
      level.sendParticles(
          ParticleTypes.GLOW_SQUID_INK, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
    }
    // 末地烛光（白色光芒）
    if (level.random.nextFloat() < 0.2f) {
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }
  }
}
