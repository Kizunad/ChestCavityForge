package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

/** Utility for scaling剑道技能冷却时间 based on流派经验. */
public final class JiandaoCooldownOps {

  private static final double MAX_REDUCTION = 0.95;
  private static final double EXP_DIVISOR = 10001.0;
  private static final long DEFAULT_MIN_TICKS = 20L;

  private JiandaoCooldownOps() {}

  public static long withJiandaoExp(long baseTicks, int liupaiExp) {
    return withJiandaoExp(baseTicks, liupaiExp, DEFAULT_MIN_TICKS);
  }

  public static long withJiandaoExp(long baseTicks, int liupaiExp, long minTicks) {
    long effectiveMin = Math.max(1L, minTicks);
    if (baseTicks <= 0L) {
      return effectiveMin;
    }
    double reduction = Math.min(MAX_REDUCTION, Math.max(0.0, liupaiExp / EXP_DIVISOR));
    long adjusted = Math.round(baseTicks * (1.0 - reduction));
    return Math.max(effectiveMin, adjusted);
  }
}
