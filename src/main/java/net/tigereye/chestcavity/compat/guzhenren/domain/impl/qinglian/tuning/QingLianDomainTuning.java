package net.tigereye.chestcavity.compat.guzhenren.domain.impl.qinglian.tuning;

/**
 * 青莲剑域调参配置
 *
 * <p>蕴剑青莲蛊展开的青莲剑域数值参数。
 */
public final class QingLianDomainTuning {

  private QingLianDomainTuning() {}

  // ========== 领域基础属性 ==========

  /** 基础半径（方块） */
  public static final double BASE_RADIUS = 20.0;

  /** 领域等级（五转级别） */
  public static final int DOMAIN_LEVEL = 5;

  // ========== 友方增益效果 ==========

  /** 友方防御增幅倍率（+15% → 药水等级1） */
  public static final double FRIENDLY_DEFENSE_MULT = 0.15;

  /** 友方跳跃增幅倍率（+30% → 药水等级3） */
  public static final double FRIENDLY_JUMP_MULT = 0.3;

  /** 友方呼吸恢复加成（精力/秒） */
  public static final double FRIENDLY_BREATHING_REGEN = 1.0;

  /** 效果持续时间（tick，每tick刷新） */
  public static final int EFFECT_DURATION = 20; // 1秒

  // ========== 粒子特效 ==========

  /** 领域边界粒子生成频率（tick） */
  public static final int BORDER_PARTICLE_INTERVAL = 8;

  /** 每次生成的边界粒子数量 */
  public static final int BORDER_PARTICLE_COUNT = 24;

  /** 中心粒子生成频率（tick） */
  public static final int CENTER_PARTICLE_INTERVAL = 4;

  /** 中心粒子数量 */
  public static final int CENTER_PARTICLE_COUNT = 3;

  // ========== PNG渲染配置 ==========

  /** PNG纹理路径（相对于assets/guzhenren/） */
  public static final String TEXTURE_PATH = "textures/domain/qinlianjianyu_transparent_forced.png";

  /** PNG高度偏移（领域中心上方格数） */
  public static final double PNG_HEIGHT_OFFSET = 20.0;

  /** PNG透明度（0.0-1.0） */
  public static final float PNG_ALPHA = 0.7f;

  /** PNG旋转速度（度/tick） */
  public static final float PNG_ROTATION_SPEED = 0.5f;
}
