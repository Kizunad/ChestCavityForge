package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.tuning.ShouPiGuTuning;

/** 皮走滚袭的调参常量。 */
public final class ShouPiRollEvasionTuning {
  private ShouPiRollEvasionTuning() {}

  public static final double BASE_DISTANCE = ShouPiGuTuning.ROLL_DISTANCE;
  public static final double SYNERGY_DISTANCE_BONUS = 0.5D;
  public static final double DUAL_DISTANCE_BONUS = 0.25D;

  public static final int RESISTANCE_DURATION_TICKS =
      (int) ShouPiGuTuning.ROLL_DAMAGE_WINDOW_TICKS;
  public static final int BASE_RESISTANCE_AMPLIFIER = 0;
  public static final int DUAL_RESISTANCE_AMPLIFIER = 1;

  public static final int BASE_SLOW_TICKS = 20;
  public static final int DUAL_SLOW_EXTRA_TICKS = 10;
  public static final int BASE_SLOW_AMPLIFIER = 0;
  public static final int DUAL_SLOW_AMPLIFIER = 1;
  public static final double TARGET_SEARCH_RADIUS = 3.0D;

  public static final double ZHENYUAN_COST = ShouPiGuTuning.ACTIVE_ROLL_BASE_COST;
  public static final long COOLDOWN_TICKS = ShouPiGuTuning.ACTIVE_ROLL_COOLDOWN_TICKS;
}

