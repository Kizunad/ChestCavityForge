package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 飞剑粒子特效系统
 *
 * <p>提供飞剑的各种视觉特效：
 * <ul>
 *   <li>飞行轨迹粒子</li>
 *   <li>攻击碰撞粒子</li>
 *   <li>召回特效</li>
 *   <li>升级特效</li>
 * </ul>
 */
public final class FlyingSwordFX {

  private FlyingSwordFX() {}

  /**
   * 飞行轨迹粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnFlightTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();
    double speed = velocity.length();

    // 根据速度调整粒子数量和密度
    int particleCount = (int) Math.max(1, speed * 10);

    // 使用剑气粒子效果（白色火花）
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

  /**
   * 攻击碰撞粒子
   *
   * @param level 服务端世界
   * @param pos 碰撞位置
   * @param damage 伤害值（影响粒子数量）
   */
  public static void spawnAttackImpact(ServerLevel level, Vec3 pos, double damage) {
    int particleCount = (int) Math.min(20, 5 + damage);

    // 横扫粒子
    level.sendParticles(
        ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);

    // 伤害粒子（红色）
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

  /**
   * 召回特效
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnRecallEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 螺旋上升的粒子
    for (int i = 0; i < 30; i++) {
      double angle = (i / 30.0) * Math.PI * 4; // 两圈螺旋
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

    // 中心爆发
    level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 10, 0.2, 0.2, 0.2, 0.05);
  }

  /**
   * 升级特效
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   * @param newLevel 新等级
   */
  public static void spawnLevelUpEffect(ServerLevel level, FlyingSwordEntity sword, int newLevel) {
    Vec3 pos = sword.position();

    // 金色闪光
    level.sendParticles(
        ParticleTypes.END_ROD, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);

    // 环形扩散
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

  /**
   * 速度冲刺粒子（高速飞行时）
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnSpeedBoostEffect(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();
    Vec3 velocity = sword.getDeltaMovement();

    // 沿着速度反方向的流线粒子
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

  /**
   * 环绕模式的轨道粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnOrbitTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 淡蓝色螺旋轨迹
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
  }

  /**
   * 出击模式的狩猎粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnHuntTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 红色火焰轨迹
    level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
  }

  /**
   * 防守模式的守护粒子
   *
   * @param level 服务端世界
   * @param sword 飞剑实体
   */
  public static void spawnGuardTrail(ServerLevel level, FlyingSwordEntity sword) {
    Vec3 pos = sword.position();

    // 绿色光点
    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
  }
}
