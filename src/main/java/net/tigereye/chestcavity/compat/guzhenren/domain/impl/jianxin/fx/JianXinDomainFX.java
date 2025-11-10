package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.JianXinDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;

/**
 * 剑心域粒子特效
 *
 * <p>提供剑心域的各种视觉效果：
 *
 * <ul>
 *   <li>领域边界光环（粒子）
 *   <li>中心能量涌动（粒子）
 *   <li>强化状态特效（粒子）
 *   <li>创建和销毁特效（粒子）
 *   <li>领域PNG纹理渲染（在玩家上方20格显示，由 {@link
 *       net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.client.JianXinDomainRenderer}
 *       负责）
 * </ul>
 *
 * <p><b>注意：</b>PNG纹理渲染是通过网络同步实现的，服务端会定期同步领域数据到客户端。
 */
public final class JianXinDomainFX {

  private JianXinDomainFX() {}

  // ========== 剑域呼吸系统（Domain Breathing） ==========

  /**
   * 计算当前呼吸相位值（0.0-1.0）
   *
   * <p>基于正弦波，频率根据资源状态动态调整：
   *
   * <ul>
   *   <li>正常状态：慢呼吸（频率低）
   *   <li>资源告急：快呼吸（频率高，类似心跳加速）
   * </ul>
   *
   * @param level 服务端世界
   * @param owner 领域主人
   * @return 呼吸相位（0.0 = 呼气末，0.5 = 吸气末，1.0 = 呼气末）
   */
  private static double calculateBreathingPhase(ServerLevel level, LivingEntity owner) {
    long time = level.getGameTime();

    // 获取资源状态比例（0.0-1.0）
    double minRatio = getMinResourceRatio(owner);

    // 频率调整：正常0.05（慢，~60秒周期），告急0.15（快，~20秒周期）
    // 模拟"续航告急"时的急促呼吸
    double baseFrequency = JianXinDomainTuning.BREATHING_FREQUENCY_NORMAL;
    double panicFrequency = JianXinDomainTuning.BREATHING_FREQUENCY_PANIC;
    double frequency = baseFrequency + (1.0 - minRatio) * (panicFrequency - baseFrequency);

    // 正弦波：sin(t * freq) ∈ [-1, 1] → [0, 1]
    double phase = (Math.sin(time * frequency) + 1.0) / 2.0;

    return phase;
  }

  /**
   * 计算呼吸亮度系数（0.8-1.0）
   *
   * <p>通过调整粒子数量实现视觉上的明暗起伏。
   *
   * @param phase 呼吸相位（0.0-1.0）
   * @param resourceRatio 最低资源比例（0.0-1.0）
   * @return 亮度系数（用于乘以粒子数量）
   */
  private static double calculateBreathingIntensity(double phase, double resourceRatio) {
    // 正常状态：0.9-1.0（小起伏）
    // 告急状态：0.8-1.0（大起伏，更明显的脉动）
    double minIntensity =
        JianXinDomainTuning.BREATHING_INTENSITY_MIN
            + resourceRatio * JianXinDomainTuning.BREATHING_INTENSITY_RANGE_NORMAL;
    double maxIntensity = JianXinDomainTuning.BREATHING_INTENSITY_MAX;

    return minIntensity + (maxIntensity - minIntensity) * phase;
  }

  /**
   * 计算呼吸半径缩放（0.97-1.03，即±3%）
   *
   * <p>实现域的"呼吸式"收缩/扩张。
   *
   * @param phase 呼吸相位（0.0-1.0）
   * @param resourceRatio 最低资源比例（0.0-1.0）
   * @return 半径缩放系数
   */
  private static double calculateBreathingScale(double phase, double resourceRatio) {
    // 正常状态：0.99-1.01（±1%，微妙）
    // 告急状态：0.97-1.03（±3%，明显）
    double minAmplitude = JianXinDomainTuning.BREATHING_SCALE_AMPLITUDE_NORMAL;
    double maxAmplitude = JianXinDomainTuning.BREATHING_SCALE_AMPLITUDE_PANIC;
    double amplitude = minAmplitude + (1.0 - resourceRatio) * (maxAmplitude - minAmplitude);

    // phase ∈ [0, 1]，转换为 [-amplitude, +amplitude]
    return 1.0 + (phase - 0.5) * 2.0 * amplitude;
  }

  /**
   * 获取实体的最低资源比例（真元、精力中的最小值）
   *
   * @param entity 实体
   * @return 最低资源比例（0.0-1.0），无资源时返回1.0（视为正常）
   */
  private static double getMinResourceRatio(LivingEntity entity) {
    if (!(entity instanceof ServerPlayer player)) {
      return 1.0; // 非玩家，视为资源充足
    }

    double zhenyuanRatio = getResourceRatio(player, "zhenyuan", "zhenyuan_max");
    double jingliRatio = getResourceRatio(player, "jingli", "jingli_max");

    // 取最低值（最告急的资源）
    return Math.min(zhenyuanRatio, jingliRatio);
  }

  /**
   * 获取单个资源的比例
   *
   * @param player 玩家
   * @param currentField 当前值字段名
   * @param maxField 最大值字段名
   * @return 资源比例（0.0-1.0），无资源时返回1.0
   */
  private static double getResourceRatio(
      ServerPlayer player, String currentField, String maxField) {
    return ResourceOps.openHandle(player)
        .map(
            h -> {
              double current = h.read(currentField).orElse(0.0);
              double max = h.read(maxField).orElse(1.0);
              if (max <= 0.0) return 1.0;
              return Math.max(0.0, Math.min(1.0, current / max));
            })
        .orElse(1.0);
  }

  /**
   * 生成领域边界粒子（圆环）- 带呼吸效果
   *
   * @param level 服务端世界
   * @param domain 剑心域
   */
  public static void spawnBorderParticles(ServerLevel level, JianXinDomain domain) {
    LivingEntity owner = domain.getOwner();
    if (owner == null) return;

    Vec3 center = domain.getCenter();
    double baseRadius = domain.getRadius();
    int baseParticleCount = JianXinDomainTuning.BORDER_PARTICLE_COUNT;

    // 强化状态下增加粒子密度
    if (domain.isEnhanced()) {
      baseParticleCount = (int) (baseParticleCount * JianXinDomainTuning.ENHANCED_PARTICLE_MULT);
    }

    // === 呼吸效果应用 ===
    double phase = calculateBreathingPhase(level, owner);
    double resourceRatio = getMinResourceRatio(owner);
    double breathingIntensity = calculateBreathingIntensity(phase, resourceRatio);
    double breathingScale = calculateBreathingScale(phase, resourceRatio);

    // 应用呼吸：半径缩放 + 粒子数量调整（模拟明暗）
    double radius = baseRadius * breathingScale;
    int particleCount = (int) (baseParticleCount * breathingIntensity);

    // 呼吸方向：phase < 0.5 吸气（向内），phase >= 0.5 呼气（向外）
    boolean inhaling = phase < 0.5;
    double breathingSpeed = JianXinDomainTuning.BREATHING_PARTICLE_SPEED;
    // 资源告急时速度加快（更明显的呼吸感）
    breathingSpeed *= (1.0 + (1.0 - resourceRatio) * 0.5);

    // 环形粒子（水平面）
    for (int i = 0; i < particleCount; i++) {
      double angle = (i / (double) particleCount) * Math.PI * 2.0;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = center.y + 0.1; // 略高于地面

      // 粒子速度：径向呼吸（吸气向内，呼气向外）
      double vx = Math.cos(angle) * breathingSpeed * (inhaling ? -1 : 1);
      double vz = Math.sin(angle) * breathingSpeed * (inhaling ? -1 : 1);

      // 正常状态：青色灵魂火焰
      // 强化状态：白色发光粒子
      var particleType = domain.isEnhanced() ? ParticleTypes.GLOW : ParticleTypes.SOUL_FIRE_FLAME;

      level.sendParticles(particleType, x, y, z, 1, vx, 0.0, vz, 0.0);
    }
  }

  /**
   * 生成中心能量涌动粒子 - 带呼吸效果
   *
   * @param level 服务端世界
   * @param domain 剑心域
   */
  public static void spawnCenterParticles(ServerLevel level, JianXinDomain domain) {
    LivingEntity owner = domain.getOwner();
    if (owner == null) return;

    Vec3 center = domain.getCenter();

    // === 呼吸效果参数 ===
    double phase = calculateBreathingPhase(level, owner);
    double resourceRatio = getMinResourceRatio(owner);
    double breathingIntensity = calculateBreathingIntensity(phase, resourceRatio);

    if (domain.isEnhanced()) {
      // 强化状态：螺旋上升的白色光芒（呼吸影响螺旋半径和速度）
      double baseRadius = 0.5;
      double spiralRadius = baseRadius * (0.8 + 0.4 * phase); // 呼吸式脉动

      for (int i = 0; i < 3; i++) {
        double angle = (level.getGameTime() * 0.1 + i * Math.PI * 2 / 3) % (Math.PI * 2);
        double height = (level.getGameTime() % 20) / 20.0 * 2.0; // 螺旋上升

        double x = center.x + Math.cos(angle) * spiralRadius;
        double z = center.z + Math.sin(angle) * spiralRadius;
        double y = center.y + height;

        // 上升速度随呼吸变化
        double upSpeed = 0.1 * breathingIntensity;

        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, upSpeed, 0.0, 0.0);
      }
    } else {
      // 正常状态：轻微的青色火花（呼吸影响生成概率）
      // 呼吸峰值时更多粒子，呼吸低谷时更少
      float spawnChance = (float) (0.2 + 0.2 * breathingIntensity);

      if (level.random.nextFloat() < spawnChance) {
        double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

        // 呼吸影响向上速度（呼气时更强）
        double upSpeed = 0.05 * breathingIntensity;

        level.sendParticles(
            ParticleTypes.SOUL_FIRE_FLAME,
            center.x + offsetX,
            center.y + 0.5,
            center.z + offsetZ,
            1,
            0.0,
            upSpeed,
            0.0,
            0.0);
      }
    }

    // === 资源告急特效：心跳般的核心脉冲 ===
    if (resourceRatio < JianXinDomainTuning.BREATHING_PANIC_THRESHOLD) {
      // 仅在呼吸峰值时触发（模拟心跳）
      if (phase > 0.9 && level.random.nextFloat() < 0.3f) {
        // 红色粒子爆发，提示续航告急
        level.sendParticles(
            ParticleTypes.LAVA, center.x, center.y + 0.5, center.z, 3, 0.2, 0.2, 0.2, 0.02);
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

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.05, 0.05, 0.05, 0.02);
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

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, vx, 0.0, vz, 0.05);
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
