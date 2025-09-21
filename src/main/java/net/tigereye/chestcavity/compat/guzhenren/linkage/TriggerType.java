package net.tigereye.chestcavity.compat.guzhenren.linkage;

/**
 * Enumerates the trigger categories supported by the linkage system.
 */
public enum TriggerType {
    /** Fired automatically once per slow tick cadence. */
    SLOW_TICK,
    /** Represents passive reactions to incoming damage. */
    DAMAGE,
    /** Custom or imperative triggers initiated by active skills/actions. */
    CUSTOM
}
