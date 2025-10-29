package net.tigereye.chestcavity.soul.util;

/**
 * Central knobs for soul combat decisions. Backed by system properties so developers can tune
 * without rebuilding: -Dchestcavity.soul.guardHpRatio=1.5
 */
public final class SoulCombatTuning {
  private SoulCombatTuning() {}

  private static final String KEY_GUARD_HP_RATIO = "chestcavity.soul.guardHpRatio";
  private static final float DEFAULT_GUARD_HP_RATIO = 2.0f;

  public static float guardHpRatio() {
    String v = System.getProperty(KEY_GUARD_HP_RATIO);
    if (v == null) {
      return DEFAULT_GUARD_HP_RATIO;
    }
    try {
      return Math.max(0.5f, Float.parseFloat(v));
    } catch (NumberFormatException e) {
      return DEFAULT_GUARD_HP_RATIO;
    }
  }
}
