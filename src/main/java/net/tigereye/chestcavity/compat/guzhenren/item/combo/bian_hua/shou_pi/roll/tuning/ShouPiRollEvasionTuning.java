package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning;

/** 皮走滚袭的调参常量（完全脱离 item 调参）。 */
public final class ShouPiRollEvasionTuning {
  private ShouPiRollEvasionTuning() {}

  // 位移与缓冲
  public static final double BASE_DISTANCE = 3.0D;
  public static final double SYNERGY_DISTANCE_BONUS = 0.5D;
  public static final double DUAL_DISTANCE_BONUS = 0.25D;

  // 免伤与窗口
  public static final int RESISTANCE_DURATION_TICKS = 12; // 与减伤窗一致
  public static final int MITIGATION_WINDOW_TICKS = 12;   // 减伤判定窗
  public static final int BASE_RESISTANCE_AMPLIFIER = 0;
  public static final int DUAL_RESISTANCE_AMPLIFIER = 1;

  // 范围减速
  public static final int BASE_SLOW_TICKS = 20;
  public static final int DUAL_SLOW_EXTRA_TICKS = 10;
  public static final int BASE_SLOW_AMPLIFIER = 0;
  public static final int DUAL_SLOW_AMPLIFIER = 1;
  public static final double TARGET_SEARCH_RADIUS = 3.0D;

  // 资源与冷却
  public static final double ZHENYUAN_COST = 25.0D;
  public static final long COOLDOWN_TICKS = 14L * 20L;
}
