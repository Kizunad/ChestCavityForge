package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import java.util.Random;

/** 概率与随机：可注入种子便于单测 */
public final class ChanceOps {
  private ChanceOps() {}

  public static boolean roll(Random rnd, double chance) {
    if (chance <= 0.0) return false;
    if (chance >= 1.0) return true;
    return rnd.nextDouble() < chance;
  }

  public static boolean roll(long seed, double chance) {
    return roll(new Random(seed), chance);
  }
}

