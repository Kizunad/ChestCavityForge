package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a single flow state.
 */
public final class FlowStateDefinition {

    private final List<FlowEdgeAction> enterActions;
    private final List<ResourceLocation> enterFx;
    private final List<FlowTransition> transitions;

    public FlowStateDefinition(List<FlowEdgeAction> enterActions, List<ResourceLocation> enterFx, List<FlowTransition> transitions) {
        this.enterActions = enterActions == null ? List.of() : List.copyOf(enterActions);
        this.enterFx = enterFx == null ? List.of() : List.copyOf(enterFx);
        this.transitions = transitions == null ? List.of() : List.copyOf(transitions);
    }

    public List<FlowEdgeAction> enterActions() {
        return enterActions;
    }

    public List<ResourceLocation> enterFx() {
        return enterFx;
    }

    public List<FlowTransition> transitions() {
        return transitions;
    }

    public List<FlowTransition> transitionsFor(FlowTrigger trigger) {
        if (transitions.isEmpty()) {
            return List.of();
        }
        List<FlowTransition> matches = new ArrayList<>();
        for (FlowTransition transition : transitions) {
            if (transition.trigger() == trigger) {
                matches.add(transition);
            }
        }
        return matches;
    }
}
