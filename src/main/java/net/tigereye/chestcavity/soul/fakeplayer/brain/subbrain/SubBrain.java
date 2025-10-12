package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain;

/**
 * A sub-brain encapsulates a slice of behaviour that can be orchestrated by a
 * higher level {@link net.tigereye.chestcavity.soul.fakeplayer.brain.Brain}.
 * Implementations should keep their own internal invariants using the provided
 * {@link SubBrainMemory} store and only concern themselves with issuing
 * high-level actions (e.g. start/stop action pipelines).
 */
public interface SubBrain {

    /** A stable identifier used for diagnostics and memory scoping. */
    String id();

    /**
     * Called when the sub-brain becomes active for the current soul. This is
     * triggered on the transition where {@link #shouldTick(SubBrainContext)}
     * returns {@code true} after being {@code false}.
     */
    default void onEnter(SubBrainContext ctx) {}

    /** Called when the sub-brain stops being active. */
    default void onExit(SubBrainContext ctx) {}

    /**
     * Whether this sub-brain should be allowed to tick for the current cycle.
     * Implementations can gate expensive behaviours based on context.
     */
    default boolean shouldTick(SubBrainContext ctx) { return true; }

    /** Called every tick while {@link #shouldTick(SubBrainContext)} is true. */
    void tick(SubBrainContext ctx);
}
