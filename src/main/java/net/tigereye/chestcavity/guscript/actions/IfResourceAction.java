package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import java.util.List;

/**
 * Runs nested actions only if the performer has at least the given amount of the specified resource.
 * Resource identifier follows GuzhenrenResourceBridge string aliases (e.g., "zhenyuan", "jingli").
 */
public record IfResourceAction(String identifier, double minimum, List<Action> actions) implements Action {

    public static final String ID = "if.resource";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "条件: 资源>=" + minimum + " -> " + identifier;
    }

    @Override
    public void execute(GuScriptContext context) {
        Player performer = context.performer();
        if (performer == null || identifier == null || identifier.isBlank() || actions == null || actions.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.open(performer).ifPresent(handle -> {
            double current = handle.read(identifier).orElse(0.0);
            if (Double.isFinite(current) && current >= minimum) {
                for (Action a : actions) {
                    a.execute(context);
                }
            }
        });
    }
}

