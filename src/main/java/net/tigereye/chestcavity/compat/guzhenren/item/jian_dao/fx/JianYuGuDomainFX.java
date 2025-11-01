package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 剑域蛊领域特效系统
 *
 * <p>提供剑域蛊"一念开阖"的各种视觉效果：
 * <ul>
 *   <li>调域窗口（Tuning）- 脉冲闪烁，表示可调整状态</li>
 *   <li>主动期间（Active）- 稳定光环，表示开阖完成</li>
 *   <li>半径变化 - 扩张/收缩过渡动画</li>
 *   <li>正面锥格挡 - 扇形防御提示</li>
 *   <li>小域模式 - R≤2时的特殊聚焦效果</li>
 * </ul>
 *
 * <p><b>调参配置：</b>详见 {@link JianYuGuDomainFXTuning}
 */
public final class JianYuGuDomainFX {

  private JianYuGuDomainFX() {}

  // ========== 调域窗口特效 ==========

  /**
   * 生成调域窗口的边界脉冲效果
   *
   * <p>快速闪烁的圆环，表示正在调整中
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param radius 当前半径
   * @param tickCount 当前tick计数
   */
  public static void spawnTuningBorderPulse(
      ServerLevel level, Vec3 center, double radius, long tickCount) {
    // 脉冲频率：每5tick一次（比主动期快）
    if (tickCount % JianYuGuDomainFXTuning.TUNING_PULSE_INTERVAL != 0) {
      return;
    }

    int particleCount = (int) (radius * JianYuGuDomainFXTuning.PARTICLES_PER_RADIUS);

    for (int i = 0; i < particleCount; i++) {
      double angle = (i / (double) particleCount) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.2; // 略高于地面

      // 金色火焰（表示可调整）
      level.sendParticles(
          ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.05, 0.0, 0.0);
    }
  }

  /**
   * 生成调域窗口的中心聚焦效果
   *
   * <p>旋转的末地烛光，表示"意念聚焦"
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param tickCount 当前tick计数
   */
  public static void spawnTuningCenterFocus(ServerLevel level, Vec3 center, long tickCount) {
    // 每2tick生成一次，更密集
    if (tickCount % 2 != 0) {
      return;
    }

    // 三条螺旋臂旋转
    for (int arm = 0; arm < 3; arm++) {
      double angle = (tickCount * 0.15 + arm * Math.PI * 2.0 / 3.0) % (Math.PI * 2);
      double radius = 0.6;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.5;

      level.sendParticles(
          ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
  }

  /**
   * 生成半径标记线（可视化当前半径）
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param radius 当前半径
   * @param tickCount 当前tick计数
   */
  public static void spawnRadiusMarkers(
      ServerLevel level, Vec3 center, double radius, long tickCount) {
    // 每10tick更新一次标记
    if (tickCount % 10 != 0) {
      return;
    }

    // 四条基准线（东南西北）
    for (int dir = 0; dir < 4; dir++) {
      double angle = dir * Math.PI / 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.3;

      // 末影粒子标记端点
      level.sendParticles(
          ParticleTypes.PORTAL, x, y, z, 2, 0.05, 0.1, 0.05, 0.0);
    }
  }

  // ========== 主动期间特效 ==========

  /**
   * 生成主动期间的稳定边界光环
   *
   * <p>较慢的脉冲，表示"开阖完成，稳定运行"
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param radius 当前半径
   * @param tickCount 当前tick计数
   * @param isSmallDomain 是否为小域模式（R≤2）
   */
  public static void spawnActiveBorderAura(
      ServerLevel level, Vec3 center, double radius, long tickCount, boolean isSmallDomain) {
    // 脉冲频率：每10tick一次（比调域慢）
    if (tickCount % JianYuGuDomainFXTuning.ACTIVE_PULSE_INTERVAL != 0) {
      return;
    }

    int particleCount = (int) (radius * JianYuGuDomainFXTuning.PARTICLES_PER_RADIUS);
    if (isSmallDomain) {
      particleCount *= 2; // 小域模式粒子密度翻倍
    }

    for (int i = 0; i < particleCount; i++) {
      double angle = (i / (double) particleCount) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.2;

      // 青色魂火（稳定状态）
      // 小域模式：白色发光粒子（更强的视觉效果）
      var particleType = isSmallDomain ? ParticleTypes.GLOW : ParticleTypes.SOUL_FIRE_FLAME;

      level.sendParticles(particleType, x, y, z, 1, 0.0, 0.02, 0.0, 0.0);
    }
  }

  /**
   * 生成小域模式的能量聚焦效果
   *
   * <p>R≤2时，中心有密集的能量涌动
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param tickCount 当前tick计数
   */
  public static void spawnSmallDomainFocus(ServerLevel level, Vec3 center, long tickCount) {
    // 每tick生成，密集特效
    // 向内聚焦的光芒
    for (int i = 0; i < 2; i++) {
      double angle = level.random.nextDouble() * Math.PI * 2.0;
      double radius = 1.5 + level.random.nextDouble() * 0.5;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.5;

      // 向中心移动的速度
      double vx = -Math.cos(angle) * 0.05;
      double vz = -Math.sin(angle) * 0.05;

      level.sendParticles(
          ParticleTypes.END_ROD, x, y, z, 1, vx, 0.0, vz, 0.0);
    }

    // 中心爆发
    if (tickCount % 5 == 0) {
      level.sendParticles(
          ParticleTypes.GLOW, center.x, center.y + 0.5, center.z, 3, 0.1, 0.1, 0.1, 0.05);
    }
  }

  // ========== 半径变化特效 ==========

  /**
   * 生成半径扩张/收缩的过渡动画
   *
   * <p>从旧半径到新半径的波纹效果
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param oldRadius 旧半径
   * @param newRadius 新半径
   */
  public static void spawnRadiusChangeEffect(
      ServerLevel level, Vec3 center, double oldRadius, double newRadius) {
    boolean expanding = newRadius > oldRadius;

    // 三圈波纹
    for (int ring = 0; ring < 3; ring++) {
      double progress = (ring + 1) / 3.0;
      double radius = expanding
          ? oldRadius + (newRadius - oldRadius) * progress
          : newRadius + (oldRadius - newRadius) * (1.0 - progress);

      int particles = Math.max(16, (int) (radius * 8));

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;
        double y = center.y + 0.2;

        // 扩张：向外速度；收缩：向内速度
        double vx = expanding ? Math.cos(angle) * 0.1 : -Math.cos(angle) * 0.1;
        double vz = expanding ? Math.sin(angle) * 0.1 : -Math.sin(angle) * 0.1;

        level.sendParticles(
            ParticleTypes.ENCHANT, x, y, z, 1, vx, 0.0, vz, 0.05);
      }
    }

    // 中心冲击
    level.sendParticles(
        expanding ? ParticleTypes.EXPLOSION : ParticleTypes.POOF,
        center.x,
        center.y + 0.5,
        center.z,
        expanding ? 5 : 3,
        0.2,
        0.2,
        0.2,
        0.0);
  }

  // ========== 正面锥格挡特效 ==========

  /**
   * 生成正面锥格挡的扇形提示
   *
   * <p>显示格挡方向的扇形粒子
   *
   * @param level 服务端世界
   * @param player 玩家
   * @param tickCount 当前tick计数
   */
  public static void spawnFrontConeIndicator(
      ServerLevel level, ServerPlayer player, long tickCount) {
    // 每5tick显示一次
    if (tickCount % 5 != 0) {
      return;
    }

    Vec3 center = player.position();
    Vec3 lookDir = player.getLookAngle().normalize();
    double yaw = Math.atan2(lookDir.z, lookDir.x);

    // 扇形范围：±60度（cos=0.5）
    double coneAngle = Math.acos(0.5); // 60度

    // 三条弧线显示扇形
    for (int arc = 0; arc < 3; arc++) {
      double distance = 1.5 + arc * 0.5; // 1.5, 2.0, 2.5格

      int particles = 8 + arc * 4; // 密度递减
      for (int i = 0; i < particles; i++) {
        double angleOffset = -coneAngle + (i / (double) (particles - 1)) * 2.0 * coneAngle;
        double angle = yaw + angleOffset;

        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        double y = center.y + 0.3;

        // 青色云雾（半透明提示）
        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }
  }

  /**
   * 生成格挡成功的冲击波效果
   *
   * @param level 服务端世界
   * @param player 玩家
   */
  public static void spawnBlockSuccessEffect(ServerLevel level, ServerPlayer player) {
    Vec3 pos = player.position().add(0, 1.0, 0);
    Vec3 lookDir = player.getLookAngle().normalize();

    // 扇形冲击波
    double yaw = Math.atan2(lookDir.z, lookDir.x);
    double coneAngle = Math.acos(0.5);

    for (int i = 0; i < 16; i++) {
      double angleOffset = -coneAngle + (i / 15.0) * 2.0 * coneAngle;
      double angle = yaw + angleOffset;

      double distance = 1.5;
      double x = pos.x + Math.cos(angle) * distance;
      double z = pos.z + Math.sin(angle) * distance;
      double y = pos.y;

      // 白色发光粒子
      level.sendParticles(
          ParticleTypes.GLOW, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
    }

    // 音爆效果
    level.sendParticles(
        ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
  }

  // ========== 开启/结束特效 ==========

  /**
   * 生成调域窗口开启特效
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param radius 初始半径
   */
  public static void spawnTuningStartEffect(ServerLevel level, Vec3 center, double radius) {
    // 圆环扩散
    for (int ring = 0; ring < 2; ring++) {
      double r = radius * (ring + 1) / 2.0;
      int particles = Math.max(16, (int) (r * 10));

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * r;
        double z = center.z + Math.sin(angle) * r;
        double y = center.y + 0.2;

        level.sendParticles(
            ParticleTypes.FLAME, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
      }
    }

    // 中心符文
    level.sendParticles(
        ParticleTypes.ENCHANT, center.x, center.y + 0.5, center.z, 20, 0.3, 0.3, 0.3, 0.1);
  }

  /**
   * 生成主动期间开始特效（调域窗口 → 主动期间）
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param radius 最终半径
   */
  public static void spawnActiveStartEffect(ServerLevel level, Vec3 center, double radius) {
    // 冲击波环
    for (int i = 0; i < 24; i++) {
      double angle = (i / 24.0) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.3;

      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 3, 0.1, 0.1, 0.1, 0.08);
    }

    // 中心爆发
    level.sendParticles(
        ParticleTypes.GLOW, center.x, center.y + 0.5, center.z, 15, 0.3, 0.3, 0.3, 0.1);
  }

  /**
   * 生成主动期间结束特效
   *
   * @param level 服务端世界
   * @param center 领域中心
   * @param radius 半径
   */
  public static void spawnActiveEndEffect(ServerLevel level, Vec3 center, double radius) {
    // 向内收缩的环
    for (int ring = 0; ring < 2; ring++) {
      double r = radius * (2 - ring) / 2.0;
      int particles = Math.max(12, (int) (r * 8));

      for (int i = 0; i < particles; i++) {
        double angle = (i / (double) particles) * Math.PI * 2.0;
        double x = center.x + Math.cos(angle) * r;
        double z = center.z + Math.sin(angle) * r;
        double y = center.y + 0.2;

        // 向中心移动
        double vx = -Math.cos(angle) * 0.08;
        double vz = -Math.sin(angle) * 0.08;

        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, vx, 0.0, vz, 0.04);
      }
    }

    // 中心消散
    level.sendParticles(
        ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z, 8, 0.2, 0.2, 0.2, 0.05);
  }
}
