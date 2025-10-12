package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a single behavioural block that can be chained together inside a {@link SubBrain}.
 */
public interface BrainActionStep {

    /** Sorts steps by {@link #priority()} in descending order. */
    Comparator<BrainActionStep> PRIORITY_COMPARATOR = Comparator
        .comparingInt(BrainActionStep::priority)
        .reversed();

    /**
     * Whether this step should execute for the provided context. Returning {@code false} skips
     * {@link #execute(SubBrainContext)} for the current tick.
     */
    boolean shouldExecute(SubBrainContext ctx);

    /** Performs the behavioural work for the step. */
    void execute(SubBrainContext ctx);

    /**
     * Steps with a higher priority are scheduled earlier in the tick. The default priority is zero.
     */
    default int priority() {
        return 0;
    }

    /**
     * Convenience factory that creates a step from predicates and consumers.
     */
    static BrainActionStep of(Predicate<SubBrainContext> predicate, Consumer<SubBrainContext> executor) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(executor, "executor");
        return new BrainActionStep() {
            @Override
            public boolean shouldExecute(SubBrainContext ctx) {
                return predicate.test(ctx);
            }

            @Override
            public void execute(SubBrainContext ctx) {
                executor.accept(ctx);
            }

            @Override
            public String toString() {
                return "BrainActionStep[of]";
            }
        };
    }

    /** Creates a step that always runs when ticked. */
    static BrainActionStep always(Consumer<SubBrainContext> executor) {
        return of(ctx -> true, executor);
    }
}
