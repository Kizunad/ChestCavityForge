package net.tigereye.chestcavity.guscript.runtime.flow.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.network.packets.FlowSyncPayload;

import java.util.Map;

/**
 * Lightweight client mirror keeping track of server flow state.
 */
public final class GuScriptClientFlows {

    private static final Map<Integer, FlowMirror> MIRRORS = new Int2ObjectOpenHashMap<>();

    private GuScriptClientFlows() {
    }

    public static void handleSync(FlowSyncPayload payload) {
        FlowMirror mirror = MIRRORS.computeIfAbsent(payload.entityId(), FlowMirror::new);
        mirror.update(payload);
    }

    public static final class FlowMirror {
        private final int entityId;
        private FlowSyncPayload lastPayload;

        FlowMirror(int entityId) {
            this.entityId = entityId;
        }

        void update(FlowSyncPayload payload) {
            this.lastPayload = payload;
            ChestCavity.LOGGER.debug("[Flow][Client] Entity {} program {} -> {} (t={} start={})", entityId, payload.programId(), payload.state(), payload.ticksInState(), payload.stateGameTime());
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == entityId) {
                // Future: drive client-side FX. For now only log.
            }
        }
    }
}
