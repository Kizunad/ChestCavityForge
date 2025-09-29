package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Mutable runtime for a {@link FlowProgram} bound to a performer and optional target.
 */
public final class FlowInstance {

    private final FlowProgram program;
    private Player performer;

    private final LivingEntity target;
    private final FlowController controller;
    private FlowState state;
    private long stateEnteredGameTime;
    private int ticksInState;
    private double tickAccumulator;
    private double timeScale;
    private boolean finished;

    private final java.util.Map<String, String> flowParams;

    FlowInstance(FlowProgram program, Player performer, LivingEntity target, FlowController controller, double timeScale, java.util.Map<String, String> flowParams, long gameTime) {
        this.program = program;
        this.performer = performer;
        this.target = target;
        this.controller = controller;
        this.timeScale = timeScale <= 0.0 ? 1.0 : timeScale;
        this.tickAccumulator = 0.0;
        this.flowParams = flowParams == null ? java.util.Map.of() : java.util.Map.copyOf(flowParams);
        enterState(program.initialState(), gameTime);
    }

    // Backwards-compatible ctor used by existing tests
    FlowInstance(FlowProgram program, Player performer, LivingEntity target, FlowController controller, long gameTime) {
        this(program, performer, target, controller, 1.0, java.util.Map.of(), gameTime);
    }

    public FlowProgram program() {
        return program;
    }

    public FlowState state() {
        return state;
    }

    public long stateEnteredGameTime() {
        return stateEnteredGameTime;
    }

    public int ticksInState() {
        return ticksInState;
    }

    public boolean isFinished() {
        return finished;
    }

    void tick(ServerLevel level) {
        if (level == null) {
            return;
        }
        tickInternal(level::getGameTime);
    }

    void tick(long gameTime) {
        tickInternal(() -> gameTime);
    }

    private void tickInternal(LongSupplier gameTimeSupplier) {
        if (finished) {
            return;
        }
        // Accumulate scaled time and advance logical ticks accordingly
        tickAccumulator += timeScale;
        int steps = (int) Math.floor(tickAccumulator);
        if (steps <= 0) {
            return;
        }
        tickAccumulator -= steps;
        for (int i = 0; i < steps && !finished; i++) {
            final long gameTime = gameTimeSupplier.getAsLong();
            // per-tick update actions
            definition().ifPresent(def -> {
                int period = def.updatePeriodTicks();
                boolean shouldRun = period <= 0 || (ticksInState % period == 0);
                if (shouldRun) {
                    for (FlowEdgeAction action : def.updateActions()) {
                        try {
                            action.apply(performer, target, controller, gameTime);
                        } catch (Exception ex) {
                            ChestCavity.LOGGER.error("[Flow] Update action {} failed for program {}", action.describe(), program.id(), ex);
                        }
                    }
                }
            });
            ticksInState++;
            attemptTransitions(FlowTrigger.AUTO, gameTime);
            if (state == FlowState.IDLE && ticksInState > 0) {
                finished = true;
                break;
            }
        }
    }

    void handleInput(FlowInput input, long gameTime) {
        if (finished) {
            return;
        }
        attemptTransitions(input.toTrigger(), gameTime);
    }

    boolean attemptStart(long gameTime) {
        if (finished) {
            return false;
        }
        List<FlowTransition> transitions = definition().map(def -> def.transitionsFor(FlowTrigger.START)).orElse(List.of());
        return fireFirstTransition(transitions, gameTime);
    }

    private void attemptTransitions(FlowTrigger trigger, long gameTime) {
        List<FlowTransition> transitions = definition().map(def -> def.transitionsFor(trigger)).orElse(List.of());
        fireFirstTransition(transitions, gameTime);
    }

    private boolean fireFirstTransition(List<FlowTransition> transitions, long gameTime) {
        if (transitions.isEmpty()) {
            return false;
        }
        for (FlowTransition transition : transitions) {
            if (ticksInState < transition.minTicksInState()) {
                continue;
            }
            if (!guardsPass(transition, gameTime)) {
                continue;
            }
            executeActions(transition, gameTime);
            enterState(transition.target(), gameTime);
            return true;
        }
        return false;
    }

    private boolean guardsPass(FlowTransition transition, long gameTime) {
        for (FlowGuard guard : transition.guards()) {
            try {
                if (!guard.test(performer, target, controller, gameTime)) {
                    return false;
                }
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[Flow] Guard {} threw for program {}", guard.describe(), program.id(), ex);
                return false;
            }
        }
        return true;
    }

    private void executeActions(FlowTransition transition, long gameTime) {
        for (FlowEdgeAction action : transition.actions()) {
            try {
                action.apply(performer, target, controller, gameTime);
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[Flow] Action {} failed for program {}", action.describe(), program.id(), ex);
            }
        }
    }

    private void enterState(FlowState nextState, long gameTime) {
        this.state = nextState;
        this.stateEnteredGameTime = gameTime;
        this.ticksInState = 0;
        // log state enter for diagnostics
        ChestCavity.LOGGER.info(
                "[Flow] {} entered {} (timeScale={}, params={})",
                program.id(),
                nextState,
                String.format("%.3f", timeScale),
                flowParams.isEmpty() ? "{}" : flowParams
        );
        controller.handleStateChanged(this);
        Optional<FlowStateDefinition> definition = definition();
        if (definition.isPresent()) {
            for (FlowEdgeAction action : definition.get().enterActions()) {
                try {
                    action.apply(performer, target, controller, gameTime);
                } catch (Exception ex) {
                    ChestCavity.LOGGER.error("[Flow] Enter action {} failed for program {}", action.describe(), program.id(), ex);
                }
            }
        }
    }

    private Optional<FlowStateDefinition> definition() {
        return program.definition(state);
    }

    public List<ResourceLocation> enterFx() {
        return definition().map(FlowStateDefinition::enterFx).orElse(List.of());
    }
    void rebindPerformer(Player performer) {
        if (performer != null) {
            this.performer = performer;
        }
    }

    java.util.Optional<String> resolveParam(String key) {
        if (key == null || key.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(flowParams.get(key));
    }

    double resolveParamAsDouble(String key, double defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        String raw = flowParams.get(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return defaultValue;
            }
            return parsed;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
