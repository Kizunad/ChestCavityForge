package net.tigereye.chestcavity.linkage;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>A trigger endpoint represents either a passive or active reaction to a linkage event. It can
 * enforce a cooldown/minimum interval between firings to avoid over-processing.
 */
public final class TriggerEndpoint {

  @FunctionalInterface
  public interface TriHandler {
    void handle(LivingEntity entity, ChestCavityInstance chestCavity, ActiveLinkageContext context);
  }

  public enum Activation {
    PASSIVE,
    ACTIVE
  }

  private final TriggerType type;
  private final Activation activation;
  private final long minIntervalTicks;
  private final TriHandler handler;
  private long lastFireGameTime = Long.MIN_VALUE;

  public TriggerEndpoint(
      TriggerType type, Activation activation, long minIntervalTicks, TriHandler handler) {
    this.type = type;
    this.activation = activation;
    this.minIntervalTicks = Math.max(0L, minIntervalTicks);
    this.handler = handler;
  }

  public TriggerType type() {
    return type;
  }

  public Activation activation() {
    return activation;
  }

  /** Attempts to fire the trigger given the current world time. */
  boolean tryFire(
      long gameTime,
      LivingEntity entity,
      ChestCavityInstance chestCavity,
      ActiveLinkageContext context) {
    if (handler == null) {
      return false;
    }
    if (minIntervalTicks > 0 && lastFireGameTime != Long.MIN_VALUE) {
      long elapsed = gameTime - lastFireGameTime;
      if (elapsed < minIntervalTicks) {
        return false;
      }
    }
    handler.handle(entity, chestCavity, context);
    lastFireGameTime = gameTime;
    return true;
  }
}
