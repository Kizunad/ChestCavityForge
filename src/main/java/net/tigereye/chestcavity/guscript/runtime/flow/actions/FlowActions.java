package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import java.util.List;

/**
 * Built-in flow actions used by the MVP implementation.
 */
public final class FlowActions {

    private FlowActions() {
    }

    public static FlowEdgeAction consumeResource(String identifier, double amount) {
        if (amount <= 0) {
            return describe(() -> "consume_resource(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || identifier == null) {
                    return;
                }
                GuzhenrenResourceBridge.open(performer).ifPresent(handle -> {
                    double delta = -Math.abs(amount);
                    handle.adjustDouble(identifier, delta, true);
                });
            }

            @Override
            public String describe() {
                return "consume_resource(" + identifier + ", " + amount + ")";
            }
        };
    }

    public static FlowEdgeAction setCooldown(String key, long durationTicks) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || key == null) {
                    return;
                }
                controller.setCooldown(key, gameTime + Math.max(0L, durationTicks));
            }

            @Override
            public String describe() {
                return "set_cooldown(" + key + ", " + durationTicks + ")";
            }
        };
    }

    public static FlowEdgeAction runActions(List<Action> actions) {
        List<Action> immutable = actions == null ? List.of() : List.copyOf(actions);
        if (immutable.isEmpty()) {
            return describe(() -> "run_actions(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(performer, target);
                DefaultGuScriptContext context = new DefaultGuScriptContext(performer, target, bridge);
                for (Action action : immutable) {
                    try {
                        action.execute(context);
                    } catch (Exception ex) {
                        ChestCavity.LOGGER.error("[Flow] Failed to run embedded action {}", action.id(), ex);
                    }
                }
            }

            @Override
            public String describe() {
                return "run_actions(" + immutable.size() + ")";
            }
        };
    }

    private static FlowEdgeAction describe(java.util.function.Supplier<String> description) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
            }

            @Override
            public String describe() {
                return description.get();
            }
        };
    }
}
