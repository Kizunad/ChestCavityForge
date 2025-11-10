package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 人兽葬生飞剑粒子特效（魔道）
 *
 * <p>使用深红色、暗色、血滴类粒子效果。 体现魔道飞剑的邪恶、血腥、诡异特征。
 */
public class RenShouZangShengFlyingSwordEntityFX implements IFlyingSwordEntityFX {

  private static final RenShouZangShengFlyingSwordEntityFX INSTANCE =
      new RenShouZangShengFlyingSwordEntityFX();

  private RenShouZangShengFlyingSwordEntityFX() {}

  public static RenShouZangShengFlyingSwordEntityFX getInstance() {
    return INSTANCE;
  }

  @Override
  public void spawnFlightTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();
    double speed = velocity.length();

    int particleCount = (int) Math.max(1, speed * 10);

    // 深红孢子粒子
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.2;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

      level.sendParticles(
          ParticleTypes.CRIMSON_SPORE,
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

    // 暗色灵魂粒子
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

      level.sendParticles(
          ParticleTypes.SOUL,
          pos.x + offsetX,
          pos.y + 0.5 + offsetY,
          pos.z + offsetZ,
          1,
          offsetX * 0.1,
          offsetY * 0.1,
          offsetZ * 0.1,
          0.1);
    }

    // 血滴效果（从上方滴落）
    for (int i = 0; i < 3; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

      level.sendParticles(
          ParticleTypes.DRIPPING_DRIPSTONE_LAVA,
          pos.x + offsetX,
          pos.y + 1.0,
          pos.z + offsetZ,
          1,
          0.0,
          -0.1,
          0.0,
          0.0);
    }

    // 深红孢子爆发
    for (int i = 0; i < 8; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.6;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;

      level.sendParticles(
          ParticleTypes.CRIMSON_SPORE,
          pos.x + offsetX,
          pos.y + 0.5 + offsetY,
          pos.z + offsetZ,
          1,
          offsetX * 0.15,
          offsetY * 0.15,
          offsetZ * 0.15,
          0.05);
    }
  }

  @Override
  public void spawnRecallEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 螺旋上升的深色鱿鱼墨水
    for (int i = 0; i < 30; i++) {
      double angle = (i / 30.0) * Math.PI * 4;
      double radius = 0.5;
      double height = (i / 30.0) * 2.0;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.SQUID_INK,
          pos.x + offsetX,
          pos.y + height,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 中心大烟雾爆发
    level.sendParticles(
        ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.05);

    // 额外的灵魂粒子环绕
    for (int i = 0; i < 12; i++) {
      double angle = (i / 12.0) * Math.PI * 2;
      double radius = 0.8;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.SOUL,
          pos.x + offsetX,
          pos.y + 0.5,
          pos.z + offsetZ,
          1,
          -offsetX * 0.1,
          0.0,
          -offsetZ * 0.1,
          0.0);
    }
  }

  @Override
  public void spawnLevelUpEffect(ServerLevel level, FlyingSwordEntity sword, int newLevel) {
    Vec3 pos = sword.position();

    // 熔岩粒子闪耀
    level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);

    // 熔岩粒子环形扩散
    for (int i = 0; i < 16; i++) {
      double angle = (i / 16.0) * Math.PI * 2;
      double radius = 1.0;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.LAVA,
          pos.x + offsetX,
          pos.y + 0.5,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 深红孢子云爆发
    level.sendParticles(
        ParticleTypes.CRIMSON_SPORE, pos.x, pos.y + 0.5, pos.z, 30, 0.4, 0.4, 0.4, 0.1);

    // 暗色灵魂粒子向上升腾
    for (int i = 0; i < 10; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

      level.sendParticles(
          ParticleTypes.SOUL, pos.x + offsetX, pos.y, pos.z + offsetZ, 1, 0.0, 0.2, 0.0, 0.05);
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

    // 额外的深红孢子尾迹
    level.sendParticles(
        ParticleTypes.CRIMSON_SPORE,
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
    // 诡异孢子轨迹（深色）
    level.sendParticles(ParticleTypes.WARPED_SPORE, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
  }

  @Override
  public void spawnHuntTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 红色火焰轨迹
    level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);

    // 额外的灵魂粒子
    if (level.random.nextFloat() < 0.3f) {
      level.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
    }
  }

  @Override
  public void spawnGuardTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 暗色灵魂守护
    level.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
  }

  @Override
  public void spawnRecallTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 暗色灵魂轨迹 - 魔道飞剑返回
    level.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.01);
    // 深红孢子
    if (level.random.nextFloat() < 0.4f) {
      level.sendParticles(
          ParticleTypes.CRIMSON_SPORE, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
    }
    // 诡异孢子（暗色）
    if (level.random.nextFloat() < 0.2f) {
      level.sendParticles(
          ParticleTypes.WARPED_SPORE, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
    }
  }
}
