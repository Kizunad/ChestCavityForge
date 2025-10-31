package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.tuning.ShouPiGuTuning;

/** 嵌甲冲撞调参常量。 */
public final class ShouPiQianJiaCrashTuning {
  private ShouPiQianJiaCrashTuning() {}

  public static final double BASE_REFLECT_RATIO = 0.35D;
  public static final double DUAL_REFLECT_BONUS = 0.1D;
  public static final double BASE_DAMAGE_CAP = 8.0D;
  public static final double DUAL_DAMAGE_CAP_BONUS = 2.0D;
  public static final double ATTACK_SCALE = 0.6D;
  public static final double DUAL_RADIUS_BONUS = 0.4D;

  public static final double ZHENYUAN_COST = ShouPiGuTuning.SYNERGY_CRASH_BASE_COST;
  public static final long COOLDOWN_TICKS = ShouPiGuTuning.SYNERGY_CRASH_COOLDOWN_TICKS;
}

