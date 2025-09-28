package net.tigereye.chestcavity.guscript.runtime.flow;

import java.util.List;

/**
 * Transition from one state to another, optionally gated by guards and performing actions.
 */
public final class FlowTransition {

    private final FlowTrigger trigger;
    private final FlowState target;
    private final List<FlowGuard> guards;
    private final List<FlowEdgeAction> actions;
    private final int minTicksInState;

    public FlowTransition(FlowTrigger trigger, FlowState target, List<FlowGuard> guards, List<FlowEdgeAction> actions, int minTicksInState) {
        this.trigger = trigger;
        this.target = target;
        this.guards = guards == null ? List.of() : List.copyOf(guards);
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.minTicksInState = Math.max(0, minTicksInState);
    }

    public FlowTrigger trigger() {
        return trigger;
    }

    public FlowState target() {
        return target;
    }

    public List<FlowGuard> guards() {
        return guards;
    }

    public List<FlowEdgeAction> actions() {
        return actions;
    }

    public int minTicksInState() {
        return minTicksInState;
    }
}
