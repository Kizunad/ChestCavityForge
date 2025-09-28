package net.tigereye.chestcavity.guscript.runtime.flow.sync;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.guscript.network.packets.FlowSyncPayload;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowInstance;
import net.tigereye.chestcavity.network.NetworkHandler;

/**
 * Sends flow state updates to clients.
 */
public final class FlowSyncDispatcher {

    private FlowSyncDispatcher() {
    }

    public static void syncState(ServerPlayer player, FlowInstance instance) {
        if (player == null || instance == null) {
            return;
        }
        NetworkHandler.sendFlowSync(player, new FlowSyncPayload(
                player.getId(),
                instance.program().id(),
                instance.state(),
                instance.stateEnteredGameTime(),
                instance.ticksInState(),
                instance.enterFx()
        ));
    }

    public static void syncStopped(ServerPlayer player, FlowInstance instance) {
        if (player == null || instance == null) {
            return;
        }
        NetworkHandler.sendFlowSync(player, new FlowSyncPayload(
                player.getId(),
                instance.program().id(),
                instance.state(),
                instance.stateEnteredGameTime(),
                instance.ticksInState(),
                instance.enterFx()
        ));
    }
}
