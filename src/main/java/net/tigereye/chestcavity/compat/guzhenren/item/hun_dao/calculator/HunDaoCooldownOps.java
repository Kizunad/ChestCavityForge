package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder cooldown helpers for Hun Dao skills.
 *
 * <p>The goal of Phase 9 is to provide a compile-time verified entry point that mirrors Jian Dao's
 * {@code JiandaoCooldownOps}. Real scaling will be delivered during Phase 9.x, so the methods
 * simply record invocations and return the supplied base cooldown.
 */
public final class HunDaoCooldownOps {

  private static final Logger LOGGER = LoggerFactory.getLogger(HunDaoCooldownOps.class);

  public static final HunDaoCooldownOps INSTANCE = new HunDaoCooldownOps();

  private HunDaoCooldownOps() {}

  /** Apply Hun Dao liupai scaling to a base cooldown while enforcing default minimum ticks. */
  public long withHunDaoExp(long baseTicks, int liupaiExp) {
    return withHunDaoExp(baseTicks, liupaiExp, HunDaoRuntimeTuning.Cooldown.DEFAULT_MIN_TICKS);
  }

  /**
   * Apply Hun Dao liupai scaling to a base cooldown while enforcing a caller-provided minimum.
   *
   * <p>Phase 9 does not change the value; it merely logs the invocation for tracing.
   */
  public long withHunDaoExp(long baseTicks, int liupaiExp, long minTicks) {
    long effectiveMin = Math.max(1L, minTicks);
    if (baseTicks <= 0L) {
      return effectiveMin;
    }
    // TODO: HunDaoPhase9.x derive reduction from liupaiExp instead of pass-through.
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[hun_dao][cooldown] placeholder invoked (baseTicks={}, liupaiExp={}, minTicks={}, maxReduction={})",
          baseTicks,
          liupaiExp,
          minTicks,
          HunDaoRuntimeTuning.Cooldown.MAX_REDUCTION);
    }
    return Math.max(effectiveMin, baseTicks);
  }
}
