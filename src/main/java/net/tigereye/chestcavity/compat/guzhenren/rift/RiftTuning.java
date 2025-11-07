package net.tigereye.chestcavity.compat.guzhenren.rift;

/**
 * 裂隙系统参数（可调）。
 *
 * <p>集中管理裂隙实体与共鸣系统的所有数值与常量，便于平衡与调参。
 */
public final class RiftTuning {

  private RiftTuning() {}

  // ====== 视觉与尺寸 ======
  public static final float MAJOR_DISPLAY_WIDTH = 2.5f;
  public static final float MAJOR_DISPLAY_HEIGHT = 1.6f;
  public static final double MAJOR_DAMAGE_RADIUS = 10;

  public static final float MINOR_DISPLAY_WIDTH = 1.5f;
  public static final float MINOR_DISPLAY_HEIGHT = 1.0f;
  public static final double MINOR_DAMAGE_RADIUS = 3.5;

  // ====== 持续与衰减 ======
  public static final int MAJOR_BASE_DURATION_TICKS = 120 * 20;
  public static final int MAJOR_DECAY_INTERVAL_TICKS = 20 * 20;
  public static final double MAJOR_INITIAL_DAMAGE_MULTIPLIER = 1.0;
  public static final double MAJOR_DECAY_STEP = 0.2; // 每次衰减-20%
  public static final double MAJOR_MIN_DAMAGE_MULTIPLIER = 0.2;
  public static final boolean MAJOR_CAN_ABSORB = true;

  public static final int MINOR_BASE_DURATION_TICKS = 20 * 20;
  public static final double MINOR_INITIAL_DAMAGE_MULTIPLIER = 0.5;
  public static final double MINOR_DECAY_STEP = 0.0; // 不衰减
  public static final double MINOR_MIN_DAMAGE_MULTIPLIER = 0.5;
  public static final boolean MINOR_CAN_ABSORB = false;

  // ====== 伤害基数 ======
  public static final float MAJOR_PIERCE_BASE_DAMAGE = 20.0f;
  public static final float MINOR_PIERCE_BASE_DAMAGE = 10.0f;

  // ====== 道痕伤害加成（每10000道痕增加的伤害倍率） ======
  public static final double DAMAGE_PER_10K_MAJOR = 500;
  public static final double DAMAGE_PER_10K_MINOR = 100;

  // ====== 共鸣系统 ======
  public static final double RESONANCE_CHAIN_BONUS_PER_NODE = 0.10; // 每个裂隙+10%
  public static final int RESONANCE_CHAIN_DISTANCE = 6; // 欧式距离
  public static final double RESONANCE_WAVE_RADIUS = 4.0;
  public static final double RESONANCE_WAVE_BASE_DAMAGE = 4.0;
  public static final int RESONANCE_PROPAGATION_DELAY_TICKS = 5;

  // ====== 吸纳微型裂隙 ======
  public static final int ABSORB_ADD_SECONDS = 15;
  public static final double ABSORB_DAMAGE_BOOST = 0.20; // +20%，上限1.0

  // ====== 血量设置 ======
  public static final int BASE_HEALTH = 20;
  /** 每 10000 剑道道痕增加的最大生命值。*/
  public static final int HEALTH_PER_10K = 10;

  // ====== 特效与音效 ======
  /** 是否启用所有特效（包括粒子和音效） */
  public static final boolean FX_ENABLED = true;

  /** 是否启用粒子特效 */
  public static final boolean PARTICLE_ENABLED = true;

  /** 是否启用音效 */
  public static final boolean SOUND_ENABLED = true;

  // 粒子数量配置
  public static final int SPAWN_PARTICLE_COUNT = 20;
  public static final int DAMAGE_PARTICLE_COUNT = 8;
  public static final int ABSORB_PARTICLE_COUNT = 15;
  public static final int RESONANCE_PARTICLE_COUNT = 12;
  public static final int WAVE_PARTICLE_COUNT = 30;
  public static final int DESPAWN_PARTICLE_COUNT = 10;

  // 音效音量配置
  public static final float VOL_SPAWN = 0.6f;
  public static final float VOL_DAMAGE = 0.4f;
  public static final float VOL_ABSORB = 0.7f;
  public static final float VOL_RESONANCE = 0.8f;
  public static final float VOL_WAVE = 0.5f;
  public static final float VOL_DESPAWN = 0.3f;

  // 音效音调配置
  public static final float PITCH_BASE = 1.0f;
  public static final float PITCH_VAR = 0.1f;

  // 粒子颜色配置 (RGB 0-1)
  public static final float[] COLOR_PRIMARY = {0.6f, 0.2f, 0.8f}; // 深紫色
  public static final float[] COLOR_SECONDARY = {0.2f, 0.8f, 0.9f}; // 青色
  public static final float[] COLOR_WAVE = {0.4f, 0.7f, 1.0f}; // 亮蓝色
  public static final float[] COLOR_ABSORB = {1.0f, 0.9f, 0.3f}; // 金色

  // ====== 伤害限频配置 ======
  /**
   * 是否启用裂隙伤害限频机制。
   *
   * <p>启用后，同一目标在窗口期内只结算一次裂隙/共鸣伤害，
   * 减少受伤冷却刷新频度，改善近战手感。
   */
  public static final boolean RATE_LIMIT_ENABLED = true;

  /**
   * 伤害限频窗口（tick）。
   *
   * <p>同一目标在此窗口内只结算一次裂隙伤害。
   * 默认 10 tick (0.5秒)。
   */
  public static final int RATE_LIMIT_WINDOW_TICKS = 10;

  /**
   * 限频表条目的最大保留时间（tick）。
   *
   * <p>超过此时间未更新的条目会被惰性清理。
   * 默认 1200 tick (60秒)。
   */
  public static final int RATE_LIMIT_MAX_KEEP_TICKS = 20 * 60;

  /** 是否输出限频聚合日志（用于调试）。默认关闭。 */
  public static final boolean RATE_LIMIT_DEBUG_LOG = false;

  /** 限频聚合日志输出的间隔（按通过/拒绝总次数统计）。默认每 500 次输出一次汇总。 */
  public static final int RATE_LIMIT_LOG_INTERVAL = 500;

  // ====== 共鸣加成缓存 ======
  /** 是否启用“共鸣链加成”小型缓存。 */
  public static final boolean RESONANCE_BONUS_CACHE_ENABLED = true;
  /** 共鸣加成缓存 TTL（tick）。默认 20 tick（1 秒）。 */
  public static final int RESONANCE_BONUS_CACHE_TTL_TICKS = 20;
}
