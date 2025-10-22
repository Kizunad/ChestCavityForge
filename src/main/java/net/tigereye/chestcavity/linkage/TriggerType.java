package net.tigereye.chestcavity.linkage;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>Enumerates the trigger categories supported by the linkage system.
 */
public enum TriggerType {
  /** Fired automatically once per slow tick cadence. */
  SLOW_TICK,
  /** Represents passive reactions to incoming damage. */
  DAMAGE,
  /** Custom or imperative triggers initiated by active skills/actions. */
  CUSTOM
}
