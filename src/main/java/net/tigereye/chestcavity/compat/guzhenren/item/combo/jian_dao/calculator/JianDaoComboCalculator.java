package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;

/**
 * 剑道组合杀招（第一个）纯计算模块（占位）。
 *
 * <p>仅包含与窗口/计数/倍率相关的可测试纯函数，不触及 Minecraft 运行时。
 */
public final class JianDaoComboCalculator {

  private JianDaoComboCalculator() {}

  /**
   * 组合状态（窗口 + 命中计数）。
   */
  public record State(long windowEndTick, int hits) {
    public State {
      if (hits < 0) hits = 0;
    }
  }

  /** 开启窗口。 */
  public static State startWindow(long now) {
    return new State(now + JianDaoComboTuning.COMBO_WINDOW_TICKS, 0);
  }

  /** 当前是否处于窗口内。 */
  public static boolean isInWindow(long now, State state) {
    if (state == null) return false;
    return now <= state.windowEndTick();
  }

  /**
   * 记录一次命中并返回新状态（窗口内方可计数）。
   * 命中数上限为 {@link JianDaoComboTuning#MAX_HITS}。
   */
  public static State registerHit(long now, State state) {
    if (state == null || !isInWindow(now, state)) {
      return state;
    }
    int nextHits = Math.min(JianDaoComboTuning.MAX_HITS, state.hits() + 1);
    return new State(state.windowEndTick(), nextHits);
  }

  /**
   * 基于命中数计算伤害倍率（占位实现：base * (1 + growth * (hits-1))）。
   */
  public static double computeMultiplier(int hits) {
    if (hits <= 0) {
      return JianDaoComboTuning.BASE_MULTIPLIER;
    }
    int effective = Math.min(hits, JianDaoComboTuning.MAX_HITS);
    return JianDaoComboTuning.BASE_MULTIPLIER
        * (1.0D + JianDaoComboTuning.GROWTH_PER_HIT * Math.max(0, effective - 1));
  }
}

