package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning;

/** 嵌甲冲撞调参常量（完全脱离 item 调参）。 */
public final class ShouPiQianJiaCrashTuning {
  private ShouPiQianJiaCrashTuning() {}

  // 伤害与半径
  public static final double BASE_REFLECT_RATIO = 0.35D;
  public static final double DUAL_REFLECT_BONUS = 0.1D;
  public static final double BASE_DAMAGE_CAP = 8.0D;
  public static final double DUAL_DAMAGE_CAP_BONUS = 2.0D;
  public static final double ATTACK_SCALE = 0.6D;
  public static final double BASE_SPLASH_RADIUS = 1.5D;
  public static final double DUAL_RADIUS_BONUS = 0.4D;

  // 位移与免疫
  public static final double CRASH_DISTANCE = 4.0D;
  public static final long IMMUNE_TICKS = 10L;

  // 资源与冷却
  public static final double ZHENYUAN_COST = 60.0D;
  public static final long COOLDOWN_TICKS = 18L * 20L;
}
