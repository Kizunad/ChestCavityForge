package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;

import java.util.List;
import java.util.Optional;

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
    private boolean finished;

    FlowInstance(FlowProgram program, Player performer, LivingEntity target, FlowController controller, long gameTime) {
        this.program = program;
        this.performer = performer;
        this.target = target;
        this.controller = controller;
        enterState(program.initialState(), gameTime);
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
        if (finished) {
            return;
        }
        ticksInState++;
        attemptTransitions(FlowTrigger.AUTO, level.getGameTime());
        if (state == FlowState.IDLE && ticksInState > 0) {
            finished = true;
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
    void rebindPerformer(Player performer) {
        if (performer != null) {
            this.performer = performer;
        }
    }
}
