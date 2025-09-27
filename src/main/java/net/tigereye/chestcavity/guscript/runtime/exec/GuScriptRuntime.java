package net.tigereye.chestcavity.guscript.runtime.exec;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.ast.GuNode;

import java.util.List;
import java.util.Objects;

/**
 * Simple interpreter executing GuScript AST nodes depth-first.
 */
public final class GuScriptRuntime {

    public void execute(GuNode root, GuScriptContext context) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(context, "context");
        executeNode(root, context, 0);
    }

    public void executeAll(List<GuNode> roots, GuScriptContext context) {
        if (roots == null || roots.isEmpty() || context == null) {
            return;
        }
        for (GuNode root : roots) {
            execute(root, context);
        }
    }

    private void executeNode(GuNode node, GuScriptContext context, int depth) {
        for (GuNode child : node.children()) {
            executeNode(child, context, depth + 1);
        }
        for (Action action : node.actions()) {
            try {
                action.execute(context);
            } catch (Exception ex) {
                ChestCavity.LOGGER.error("[GuScript] Action {} failed at node {}", action.id(), node.name(), ex);
            }
        }
    }
}
