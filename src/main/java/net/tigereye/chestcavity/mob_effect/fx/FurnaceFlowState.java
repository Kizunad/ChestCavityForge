package net.tigereye.chestcavity.mob_effect.fx;

/**
 * Represents the coarse-grained lifecycle for the Furnace Power effect.
 * The states are intentionally lightweight so they can be used by gameplay
 * logic and tests without requiring a running Minecraft world.
 */
public enum FurnaceFlowState {
    /** No furnace activity is happening. */
    IDLE,
    /** The effect is charging and preparing to feed the player. */
    CHARGING,
    /** The effect has just fed the player. */
    FEEDING
}
