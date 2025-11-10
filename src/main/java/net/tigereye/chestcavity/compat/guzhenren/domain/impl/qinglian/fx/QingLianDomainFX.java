package net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.QingLianDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.tuning.QingLianDomainTuning;

/**
 * 青莲剑域粒子特效
 *
 * <p>青莲主题：青色魂焰+末影烛光，呈现莲花盛开的视觉效果。
 */
public final class QingLianDomainFX {

  private QingLianDomainFX() {}

  /**
   * 生成领域边界粒子（莲瓣圆环）
   *
   * @param level 服务端世界
   * @param domain 青莲剑域
   */
  public static void spawnBorderParticles(ServerLevel level, QingLianDomain domain) {
    Vec3 center = domain.getCenter();
    double radius = domain.getRadius();
    int particleCount = QingLianDomainTuning.BORDER_PARTICLE_COUNT;

    // 青色魂焰环形粒子（水平面）
    for (int i = 0; i < particleCount; i++) {
      double angle = (i / (double) particleCount) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.2; // 略高于地面

      // 青色魂焰（主色调）
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

      // 每隔3个粒子添加一个发光粒子（莲瓣点缀）
      if (i % 3 == 0) {
        level.sendParticles(ParticleTypes.GLOW, x, y + 0.3, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }

    // 莲瓣花纹（8瓣）
    for (int petal = 0; petal < 8; petal++) {
      double petalAngle = (petal / 8.0) * Math.PI * 2.0;
      double petalX = center.x + Math.cos(petalAngle) * radius * 0.7;
      double petalZ = center.z + Math.sin(petalAngle) * radius * 0.7;

      // 莲瓣上的末影烛光
      level.sendParticles(
          ParticleTypes.END_ROD, petalX, center.y + 0.5, petalZ, 1, 0.0, 0.1, 0.0, 0.01);
    }
  }

  /**
   * 生成中心能量涌动粒子
   *
   * @param level 服务端世界
   * @param domain 青莲剑域
   */
  public static void spawnCenterParticles(ServerLevel level, QingLianDomain domain) {
    Vec3 center = domain.getCenter();
    int particleCount = QingLianDomainTuning.CENTER_PARTICLE_COUNT;

    // 螺旋上升的青色能量（莲心）
    long time = level.getGameTime();
    for (int i = 0; i < particleCount; i++) {
      double angle = (time * 0.1 + i * Math.PI * 2 / particleCount) % (Math.PI * 2);
      double spiralRadius = 0.5;
      double height = (time % 20) / 20.0 * 2.0; // 螺旋上升

      double x = center.x + Math.cos(angle) * spiralRadius;
      double z = center.z + Math.sin(angle) * spiralRadius;
      double y = center.y + height;

      // 末影烛光（向上螺旋）
      level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.1, 0.0, 0.0);
    }

    // 中心轻微的魂焰涌动
    if (level.random.nextFloat() < 0.4f) {
      double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
      double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;

      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME,
          center.x + offsetX,
          center.y + 0.5,
          center.z + offsetZ,
          1,
          0.0,
          0.05,
          0.0,
          0.0);
    }
  }

  /**
   * 生成领域创建特效
   *
   * @param level 服务端世界
   * @param center 中心位置
   * @param radius 半径
   */
  public static void spawnCreationEffect(ServerLevel level, Vec3 center, double radius) {
    // 圆环扩散效果（莲花绽放）
    for (int ring = 0; ring < 3; ring++) {
      double r = radius * (ring + 1) / 3.0;
      int particles = 32 * (ring + 1);

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * r;
        double z = center.z + Math.sin(angle) * r;
        double y = center.y + 0.2;

        // 青色魂焰扩散
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
      }
    }

    // 中心爆发（莲心绽放）
    level.sendParticles(
        ParticleTypes.GLOW, center.x, center.y + 0.5, center.z, 30, 0.4, 0.4, 0.4, 0.1);

    // 末影烛光向上爆发
    level.sendParticles(
        ParticleTypes.END_ROD, center.x, center.y, center.z, 25, 0.4, 0.0, 0.4, 0.2);

    // 附魔符文环绕
    level.sendParticles(
        ParticleTypes.ENCHANT, center.x, center.y + 0.5, center.z, 40, 0.6, 0.6, 0.6, 0.1);
  }

  /**
   * 生成领域销毁特效
   *
   * @param level 服务端世界
   * @param center 中心位置
   * @param radius 半径
   */
  public static void spawnDestructionEffect(ServerLevel level, Vec3 center, double radius) {
    // 向内收缩的环（莲花收拢）
    for (int ring = 0; ring < 3; ring++) {
      double r = radius * (3 - ring) / 3.0;
      int particles = 20 * (3 - ring);

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * r;
        double z = center.z + Math.sin(angle) * r;
        double y = center.y + 0.2;

        // 向中心移动的粒子
        double vx = -Math.cos(angle) * 0.1;
        double vz = -Math.sin(angle) * 0.1;

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, vx, 0.0, vz, 0.05);
      }
    }

    // 中心消散（莲心凋零）
    level.sendParticles(
        ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z, 15, 0.3, 0.3, 0.3, 0.05);
  }

  /**
   * 每tick的领域特效更新
   *
   * @param level 服务端世界
   * @param domain 青莲剑域
   * @param tickCount 当前tick计数
   */
  public static void tickDomainEffects(ServerLevel level, QingLianDomain domain, long tickCount) {
    // 边界粒子
    if (tickCount % QingLianDomainTuning.BORDER_PARTICLE_INTERVAL == 0) {
      spawnBorderParticles(level, domain);
    }

    // 中心粒子
    if (tickCount % QingLianDomainTuning.CENTER_PARTICLE_INTERVAL == 0) {
      spawnCenterParticles(level, domain);
    }
  }
}
