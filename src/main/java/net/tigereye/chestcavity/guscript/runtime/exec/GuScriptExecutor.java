package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.GuScriptProgramCache;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;

/**
 * Executes compiled GuScript programs for a player.
 */
public final class GuScriptExecutor {

    private static final GuScriptRuntime RUNTIME = new GuScriptRuntime();

    private GuScriptExecutor() {
    }

    public static void trigger(ServerPlayer player, LivingEntity target, GuScriptAttachment attachment) {
        if (player == null || attachment == null) {
            return;
        }
        GuScriptProgramCache cache = GuScriptCompiler.compileIfNeeded(attachment, player.level().getGameTime());
        if (cache.roots().isEmpty()) {
            ChestCavity.LOGGER.debug("[GuScript] No compiled roots to execute for {}", player.getGameProfile().getName());
            return;
        }
        ChestCavity.LOGGER.info(
                "[GuScript] Trigger dispatch for {}: {} roots -> {}",
                player.getGameProfile().getName(),
                cache.roots().size(),
                cache.roots().stream()
                        .map(root -> root.kind() + ":" + root.name())
                        .toList()
        );
        RUNTIME.executeAll(cache.roots(), index -> {
            LivingEntity actualTarget = target == null ? player : target;
            DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(player, actualTarget, index);
            return new DefaultGuScriptContext(player, actualTarget, bridge);
        });
    }
}
