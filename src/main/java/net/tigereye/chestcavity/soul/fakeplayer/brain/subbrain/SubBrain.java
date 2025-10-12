package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.tigereye.chestcavity.ChestCavity;

/**
 * A sub-brain encapsulates a slice of behaviour that can be orchestrated by a
 * higher level {@link net.tigereye.chestcavity.soul.fakeplayer.brain.Brain}.
 * <p>
 * Implementations compose their behaviour by registering a sequence of
 * {@link BrainActionStep} instances. Each step can gate its execution via
 * {@link BrainActionStep#shouldExecute(SubBrainContext)} which makes the
 * orchestration similar to building blocks in a behaviour tree.
 */
public abstract class SubBrain {

    private final String id;
    private final List<BrainActionStep> steps = new ArrayList<>();

    protected SubBrain(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /** A stable identifier used for diagnostics and memory scoping. */
    public final String id() {
        return id;
    }

    /**
     * Called when the sub-brain becomes active for the current soul. This is
     * triggered on the transition where {@link #shouldTick(SubBrainContext)}
     * returns {@code true} after being {@code false}.
     */
    public void onEnter(SubBrainContext ctx) {}

    /** Called when the sub-brain stops being active. */
    public void onExit(SubBrainContext ctx) {}

    /**
     * Whether this sub-brain should be allowed to tick for the current cycle.
     * Implementations can gate expensive behaviours based on context.
     */
    public boolean shouldTick(SubBrainContext ctx) {
        return true;
    }

    /**
     * Registers a new behaviour step. Steps are iterated in priority order and
     * executed sequentially when {@link #tick(SubBrainContext)} is invoked.
     */
    protected final void addStep(BrainActionStep step) {
        Objects.requireNonNull(step, "step");
        steps.add(step);
        steps.sort(BrainActionStep.PRIORITY_COMPARATOR);
    }

    /** Registers multiple steps at once following {@link #addStep(BrainActionStep)} semantics. */
    protected final void addSteps(Collection<? extends BrainActionStep> newSteps) {
        Objects.requireNonNull(newSteps, "newSteps");
        newSteps.forEach(this::addStep);
    }

    /** Returns an immutable snapshot of the current steps for diagnostics/testing. */
    protected final List<BrainActionStep> steps() {
        return Collections.unmodifiableList(steps);
    }

    /** Clears all registered steps. Primarily useful for tests or dynamic pipelines. */
    protected final void clearSteps() {
        steps.clear();
    }

    /** Called every tick while {@link #shouldTick(SubBrainContext)} is true. */
    public final void tick(SubBrainContext ctx) {
        for (BrainActionStep step : steps) {
            boolean shouldExecute;
            try {
                shouldExecute = step.shouldExecute(ctx);
            } catch (RuntimeException ex) {
                ChestCavity.LOGGER.error("SubBrain {} step {} failed shouldExecute", id(), step, ex);
                continue;
            }
            if (!shouldExecute) {
                continue;
            }
            try {
                step.execute(ctx);
            } catch (RuntimeException ex) {
                ChestCavity.LOGGER.error("SubBrain {} step {} execution failed", id(), step, ex);
            }
        }
    }
}
