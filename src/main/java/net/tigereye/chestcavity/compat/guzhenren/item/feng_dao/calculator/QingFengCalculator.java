package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.tuning.FengTuning;

/** 清风轮 纯计算器：不读写世界/实体，便于单测与推导。 */
public final class QingFengCalculator {
  private QingFengCalculator() {}

  // 闪避几率
  public static double dodgeChance(int stage, boolean moving, int stacks) {
    double chance = 0.0;
    if (stage >= 3 && moving) {
      chance += FengTuning.BASE_DODGE_STAGE3;
    }
    if (stage >= 5) {
      int s = Math.max(0, Math.min(FengTuning.WIND_STACK_MAX, stacks));
      chance += s * FengTuning.WIND_STACK_DODGE_PER_STACK;
    }
    return clamp01(chance);
  }

  public static boolean roll(double chance, double random01) {
    return random01 < clamp01(chance);
  }

  // 叠层更新（移动+1；停 40t 清零）
  public static WindStackUpdate updateStacks(int stacks, boolean moving, long now, long lastMove) {
    stacks = clampStack(stacks);
    boolean dirty = false;
    if (moving) {
      int ns = Math.min(FengTuning.WIND_STACK_MAX, stacks + 1);
      dirty |= (ns != stacks);
      stacks = ns;
      return new WindStackUpdate(stacks, now, dirty, false);
    }
    boolean reset = lastMove > 0 && now - lastMove >= FengTuning.WIND_STACK_IDLE_RESET_TICKS;
    if (reset && stacks > 0) {
      stacks = 0;
      dirty = true;
    }
    return new WindStackUpdate(stacks, lastMove, dirty, reset);
  }

  // 冲刺里程累计（厘米）：低于阈值不计
  public static long deltaCentimeters(double horizontalDistance) {
    if (horizontalDistance < Math.max(0.0, FengTuning.RUN_SAMPLE_THRESHOLD)) {
      return 0L;
    }
    long cm = Math.round(horizontalDistance * 100.0);
    return cm > 0 ? cm : 0L;
  }

  public static boolean hitRunFxMilestone(long beforeUnits, long afterUnits) {
    long interval = Math.max(1L, FengTuning.RUN_FX_INTERVAL_UNITS);
    return beforeUnits / interval < afterUnits / interval;
  }

  public static double clampFallSpeed(double vy) {
    double maxDown = Math.abs(FengTuning.GLIDE_MAX_FALL_SPEED);
    return vy < -maxDown ? -maxDown : vy;
  }

  private static int clampStack(int s) {
    if (s < 0) return 0;
    return Math.min(FengTuning.WIND_STACK_MAX, s);
  }

  private static double clamp01(double v) {
    if (v <= 0.0) return 0.0;
    if (v >= 1.0) return 1.0;
    return v;
  }

  // 输出结构
  public record WindStackUpdate(int stacks, long lastMoveTick, boolean dirty, boolean reset) {}
}

