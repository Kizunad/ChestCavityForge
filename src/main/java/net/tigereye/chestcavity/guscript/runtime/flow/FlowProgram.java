package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable description of a flow state machine loaded from data.
 */
public final class FlowProgram {

    private final ResourceLocation id;
    private final FlowState initialState;
    private final Map<FlowState, FlowStateDefinition> states;

    public FlowProgram(ResourceLocation id, FlowState initialState, Map<FlowState, FlowStateDefinition> states) {
        this.id = Objects.requireNonNull(id, "id");
        this.initialState = Objects.requireNonNull(initialState, "initialState");
        this.states = new EnumMap<>(FlowState.class);
        this.states.putAll(states);
    }

    public ResourceLocation id() {
        return id;
    }

    public FlowState initialState() {
        return initialState;
    }

    public Optional<FlowStateDefinition> definition(FlowState state) {
        return Optional.ofNullable(states.get(state));
    }

    public Map<FlowState, FlowStateDefinition> states() {
        return Map.copyOf(states);
    }
}
