package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul;

import java.util.Arrays;
import java.util.Comparator;

/** Utility methods for converting hun po totals into soul level tiers. */
public final class HunDaoSoulLevelHelper {

  /** Conversion ratio between person units and hun po. */
  public static final double HUNPO_PER_PERSON = 100.0D;

  private static final HunDaoSoulLevelTier[] SORTED_TIERS =
      Arrays.stream(HunDaoSoulLevelTier.values())
          .sorted(Comparator.comparingLong(HunDaoSoulLevelTier::getMinPersonUnits))
          .toArray(HunDaoSoulLevelTier[]::new);

  private HunDaoSoulLevelHelper() {}

  /**
   * Converts hun po maximum into "person" units (100 hun po each) and clamps to at least one.
   */
  public static long computePersonUnits(double hunPoMax) {
    double units = Math.floor(Math.max(0.0D, hunPoMax) / HUNPO_PER_PERSON);
    if (units < 1.0D) {
      units = 1.0D;
    }
    if (units > Long.MAX_VALUE) {
      units = Long.MAX_VALUE;
    }
    return (long) units;
  }

  /**
   * Resolves the tier for a given hun po maximum.
   */
  public static HunDaoSoulLevelTier resolveTier(double hunPoMax) {
    long units = computePersonUnits(hunPoMax);
    HunDaoSoulLevelTier result = SORTED_TIERS[0];
    for (HunDaoSoulLevelTier tier : SORTED_TIERS) {
      if (units >= tier.getMinPersonUnits()) {
        result = tier;
      } else {
        break;
      }
    }
    return result;
  }
}
