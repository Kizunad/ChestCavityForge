package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑心蛊（体质）行为侧调参项。
 *
 * <p>仅承载可调数值/上限/冷却等，不包含行为逻辑。
 */
public final class JianXinGuTuning {

  private JianXinGuTuning() {}

  /** 冥想冷却（tick）。*/
  public static final int MEDITATION_COOLDOWN_T = 20 * 20; // 20s

  /** 被打断时给予的“失心冻结”时长（tick）。*/
  public static final int FREEZE_ON_BREAK_T = 40; // 2s

  /** 冥想期间每秒精力恢复量。*/
  public static final double REGEN_JINGLI_PER_SEC = 1.0; // 每秒 +1 精力

  /** 剑势最大层数。*/
  public static final int MAX_MOMENTUM = 5;

  /** 冥想时移动速度衰减比例（0~1]，例如0.9表示-90%速度。*/
  public static final double JIAN_XIN_DOMAIN_VELOCITY_DECREASEMENT = 0.9;
}
