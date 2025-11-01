package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.JianXinDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;

/**
 * 剑心域粒子特效
 *
 * <p>提供剑心域的各种视觉效果：
 * <ul>
 *   <li>领域边界光环（粒子）</li>
 *   <li>中心能量涌动（粒子）</li>
 *   <li>强化状态特效（粒子）</li>
 *   <li>创建和销毁特效（粒子）</li>
 *   <li>领域PNG纹理渲染（在玩家上方20格显示，由 {@link net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.client.JianXinDomainRenderer} 负责）</li>
 * </ul>
 *
 * <p><b>注意：</b>PNG纹理渲染是通过网络同步实现的，服务端会定期同步领域数据到客户端。
 */
public final class JianXinDomainFX {

  private JianXinDomainFX() {}

  /**
   * 生成领域边界粒子（圆环）
   *
   * @param level 服务端世界
   * @param domain 剑心域
   */
  public static void spawnBorderParticles(ServerLevel level, JianXinDomain domain) {
    Vec3 center = domain.getCenter();
    double radius = domain.getRadius();
    int particleCount = JianXinDomainTuning.BORDER_PARTICLE_COUNT;

    // 强化状态下增加粒子密度
    if (domain.isEnhanced()) {
      particleCount *= JianXinDomainTuning.ENHANCED_PARTICLE_MULT;
    }

    // 环形粒子（水平面）
    for (int i = 0; i < particleCount; i++) {
      double angle = (i / (double) particleCount) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.1; // 略高于地面

      // 正常状态：青色灵魂火焰
      // 强化状态：白色发光粒子
      var particleType = domain.isEnhanced() ? ParticleTypes.GLOW : ParticleTypes.SOUL_FIRE_FLAME;

      level.sendParticles(particleType, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
  }

  /**
   * 生成中心能量涌动粒子
   *
   * @param level 服务端世界
   * @param domain 剑心域
   */
  public static void spawnCenterParticles(ServerLevel level, JianXinDomain domain) {
    Vec3 center = domain.getCenter();

    if (domain.isEnhanced()) {
      // 强化状态：螺旋上升的白色光芒
      for (int i = 0; i < 3; i++) {
        double angle = (level.getGameTime() * 0.1 + i * Math.PI * 2 / 3) % (Math.PI * 2);
        double radius = 0.5;
        double height = (level.getGameTime() % 20) / 20.0 * 2.0; // 螺旋上升

        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;
        double y = center.y + height;

        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.1, 0.0, 0.0);
      }
    } else {
      // 正常状态：轻微的青色火花
      if (level.random.nextFloat() < 0.3f) {
        double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

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
  }

  /**
   * 生成领域创建特效
   *
   * @param level 服务端世界
   * @param center 中心位置
   * @param radius 半径
   */
  public static void spawnCreationEffect(ServerLevel level, Vec3 center, double radius) {
    // 圆环扩散效果
    for (int ring = 0; ring < 3; ring++) {
      double r = radius * (ring + 1) / 3.0;
      int particles = 24 * (ring + 1);

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * r;
        double z = center.z + Math.sin(angle) * r;
        double y = center.y + 0.1;

        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
      }
    }

    // 中心爆发
    level.sendParticles(
        ParticleTypes.GLOW, center.x, center.y + 0.5, center.z, 20, 0.3, 0.3, 0.3, 0.1);

    // 附魔符文
    level.sendParticles(
        ParticleTypes.ENCHANT, center.x, center.y + 0.5, center.z, 30, 0.5, 0.5, 0.5, 0.1);
  }

  /**
   * 生成领域销毁特效
   *
   * @param level 服务端世界
   * @param center 中心位置
   * @param radius 半径
   */
  public static void spawnDestructionEffect(ServerLevel level, Vec3 center, double radius) {
    // 向内收缩的环
    for (int ring = 0; ring < 3; ring++) {
      double r = radius * (3 - ring) / 3.0;
      int particles = 16 * (3 - ring);

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * r;
        double z = center.z + Math.sin(angle) * r;
        double y = center.y + 0.1;

        // 向中心移动的粒子
        double vx = -Math.cos(angle) * 0.1;
        double vz = -Math.sin(angle) * 0.1;

        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, vx, 0.0, vz, 0.05);
      }
    }

    // 中心烟雾
    level.sendParticles(
        ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z, 10, 0.2, 0.2, 0.2, 0.05);
  }

  /**
   * 生成强化状态触发特效
   *
   * @param level 服务端世界
   * @param center 中心位置
   * @param radius 半径
   */
  public static void spawnEnhancementEffect(ServerLevel level, Vec3 center, double radius) {
    // 冲击波环
    for (int i = 0; i < 32; i++) {
      double angle = (i / 32.0) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.5;

      level.sendParticles(ParticleTypes.GLOW, x, y, z, 3, 0.1, 0.1, 0.1, 0.1);
    }

    // 音爆效果
    level.sendParticles(
        ParticleTypes.SONIC_BOOM, center.x, center.y + 0.5, center.z, 1, 0.0, 0.0, 0.0, 0.0);

    // 末地烛光向上爆发
    level.sendParticles(
        ParticleTypes.END_ROD, center.x, center.y, center.z, 20, 0.3, 0.0, 0.3, 0.2);
  }

  /**
   * 生成剑气反噬特效
   *
   * @param level 服务端世界
   * @param pos 目标位置
   */
  public static void spawnCounterAttackEffect(ServerLevel level, Vec3 pos) {
    // 暴击星星
    level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 10, 0.3, 0.3, 0.3, 0.1);

    // 伤害指示器
    level.sendParticles(
        ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y + 1.5, pos.z, 5, 0.2, 0.2, 0.2, 0.0);

    // 青色火焰爆发
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.0, pos.z, 8, 0.2, 0.2, 0.2, 0.05);
  }

  /**
   * 每tick的领域特效更新
   *
   * @param level 服务端世界
   * @param domain 剑心域
   * @param tickCount 当前tick计数
   */
  public static void tickDomainEffects(ServerLevel level, JianXinDomain domain, long tickCount) {
    // 边界粒子
    if (tickCount % JianXinDomainTuning.BORDER_PARTICLE_INTERVAL == 0) {
      spawnBorderParticles(level, domain);
    }

    // 中心粒子
    if (tickCount % JianXinDomainTuning.CENTER_PARTICLE_INTERVAL == 0) {
      spawnCenterParticles(level, domain);
    }
  }
}
