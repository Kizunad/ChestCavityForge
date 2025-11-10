package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 默认飞剑粒子特效
 *
 * <p>使用标准白色/混合粒子效果。
 */
public class DefaultFlyingSwordEntityFX implements IFlyingSwordEntityFX {

  private static final DefaultFlyingSwordEntityFX INSTANCE = new DefaultFlyingSwordEntityFX();

  private DefaultFlyingSwordEntityFX() {}

  public static DefaultFlyingSwordEntityFX getInstance() {
    return INSTANCE;
  }

  @Override
  public void spawnFlightTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();
    double speed = velocity.length();

    int particleCount = (int) Math.max(1, speed * 10);

    // 白色暴击星星
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.2;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

      level.sendParticles(
          ParticleTypes.CRIT,
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

    // 伤害指示器粒子
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

      level.sendParticles(
          ParticleTypes.DAMAGE_INDICATOR,
          pos.x + offsetX,
          pos.y + 0.5 + offsetY,
          pos.z + offsetZ,
          1,
          offsetX * 0.1,
          offsetY * 0.1,
          offsetZ * 0.1,
          0.1);
    }
  }

  @Override
  public void spawnRecallEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 螺旋上升的附魔符文
    for (int i = 0; i < 30; i++) {
      double angle = (i / 30.0) * Math.PI * 4;
      double radius = 0.5;
      double height = (i / 30.0) * 2.0;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.ENCHANT,
          pos.x + offsetX,
          pos.y + height,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 中心云雾爆发
    level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.05);
  }

  @Override
  public void spawnLevelUpEffect(ServerLevel level, FlyingSwordEntity sword, int newLevel) {
    Vec3 pos = sword.position();

    // 末地烛光闪耀
    level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);

    // 烟花环形扩散
    for (int i = 0; i < 16; i++) {
      double angle = (i / 16.0) * Math.PI * 2;
      double radius = 1.0;

      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;

      level.sendParticles(
          ParticleTypes.FIREWORK,
          pos.x + offsetX,
          pos.y + 0.5,
          pos.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }
  }

  @Override
  public void spawnSpeedBoostEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();
    Vec3 reverseVelocity = velocity.normalize().scale(-0.2);

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
  }

  @Override
  public void spawnOrbitTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
  }

  @Override
  public void spawnHuntTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
  }

  @Override
  public void spawnGuardTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    level.sendParticles(
        ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
  }

  @Override
  public void spawnRecallTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    // 附魔符文粒子 - 象征飞剑返回主人
    level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.02);
    // 白色暴击星星
    if (level.random.nextFloat() < 0.5f) {
      level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
    }
  }
}
