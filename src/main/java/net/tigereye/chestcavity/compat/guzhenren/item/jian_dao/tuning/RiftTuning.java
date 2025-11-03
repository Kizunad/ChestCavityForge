package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

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
}

