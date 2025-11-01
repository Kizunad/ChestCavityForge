package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

/**
 * 剑域蛊领域特效调参配置
 *
 * <p>集中管理所有FX相关的数值参数，便于平衡性调整。
 */
public final class JianYuGuDomainFXTuning {

  private JianYuGuDomainFXTuning() {}

  // ========== 粒子密度 ==========

  /** 每单位半径的粒子数量（用于圆环） */
  public static final double PARTICLES_PER_RADIUS = 8.0;

  // ========== 调域窗口（Tuning） ==========

  /** 调域边界脉冲频率（tick） - 较快，表示可调整 */
  public static final int TUNING_PULSE_INTERVAL = 5;

  /** 调域中心聚焦更新频率（tick） */
  public static final int TUNING_CENTER_INTERVAL = 2;

  /** 半径标记更新频率（tick） */
  public static final int RADIUS_MARKER_INTERVAL = 10;

  // ========== 主动期间（Active） ==========

  /** 主动边界脉冲频率（tick） - 较慢，表示稳定运行 */
  public static final int ACTIVE_PULSE_INTERVAL = 10;

  /** 小域聚焦效果更新频率（tick） - 密集 */
  public static final int SMALL_DOMAIN_FOCUS_INTERVAL = 1;

  /** 正面锥指示器更新频率（tick） */
  public static final int FRONT_CONE_INDICATOR_INTERVAL = 5;

  // ========== 小域模式阈值 ==========

  /** 小域模式触发的半径阈值 */
  public static final double SMALL_DOMAIN_RADIUS_THRESHOLD = 2.0;
}
