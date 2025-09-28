package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allocates and caches {@link FlowController} instances per player.
 */
public final class FlowControllerManager {

    private static final Map<UUID, FlowController> CONTROLLERS = new ConcurrentHashMap<>();

    private FlowControllerManager() {
    }

    public static FlowController get(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return CONTROLLERS.compute(player.getUUID(), (uuid, controller) -> {
            if (controller == null) {
                return new FlowController(player);
            }
            controller.updatePerformer(player);
            return controller;
        });
    }

    public static void remove(ServerPlayer player) {
        if (player != null) {
            CONTROLLERS.remove(player.getUUID());
        }
    }
}
