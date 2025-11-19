package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phase 9 placeholder implementation for Hun Dao dao-hen (scar) effective value hooks.
 *
 * <p>The class mirrors Jian Dao's {@code JiandaoDaohenOps} API so future phases can reuse the same
 * calling patterns. For now all methods simply proxy to the raw resource handle and log the
 * invocation so we know when real math is required.
 */
public final class HunDaoDaohenOps {

  private static final Logger LOGGER = LoggerFactory.getLogger(HunDaoDaohenOps.class);

  public static final HunDaoDaohenOps INSTANCE = new HunDaoDaohenOps();

  private HunDaoDaohenOps() {}

  /**
   * Read the effective dao-hen for a given owner without caching.
   *
   * <p>Phase 9 returns the raw {@code daohen_hun_dao} field and only logs that the future multiplier
   * hook is still pending.
   */
  public double effectiveUncached(LivingEntity owner, long now) {
    if (!(owner instanceof ServerPlayer player)) {
      return ResourceOps.openHandle(owner)
          .map(handle -> handle.read("daohen_hun_dao").orElse(0.0))
          .orElse(0.0);
    }

    GuzhenrenResourceBridge.ResourceHandle handle = ResourceOps.openHandle(player).orElse(null);
    if (handle == null) {
      return 0.0;
    }

    double daohen = handle.read("daohen_hun_dao").orElse(0.0);
    double liupaiExp = handle.read("liupai_hun_dao").orElse(0.0);
    double expMultiplier = resolveExpMultiplier(liupaiExp);
    double effective = daohen * expMultiplier;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[hun_dao][scar] owner={}, daohen={}, liupai={}, expMultiplier={}, effective={}",
          player.getGameProfile().getName(),
          daohen,
          liupaiExp,
          expMultiplier,
          effective);
    }
    return effective;
  }

  /**
   * Read the effective dao-hen using a 20 tick cache to reduce bridge lookups.
   *
   * @see HunDaoDaohenCache
   */
  public double effectiveCached(LivingEntity owner, long now) {
    if (!(owner instanceof ServerPlayer player)) {
      return effectiveUncached(owner, now);
    }
    return HunDaoDaohenCache.getEffective(player, now, () -> effectiveUncached(player, now));
  }

  /** Manually invalidate the cached value for an owner (if any). */
  public void invalidate(LivingEntity owner) {
    if (owner instanceof ServerPlayer player) {
      invalidate(player);
    }
  }

  /** Manually invalidate the cached value for a player (if any). */
  public void invalidate(ServerPlayer player) {
    HunDaoDaohenCache.invalidate(player);
  }

  private double resolveExpMultiplier(double liupaiExp) {
    // TODO: HunDaoPhase9.x replace pass-through multiplier with finalized liupai curve.
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(
          "[hun_dao][scar] resolveExpMultiplier placeholder invoked (liupai={})", liupaiExp);
    }
    // Phase 9 intentionally returns pass-through for future balance updates.
    return 1.0D;
  }
}
