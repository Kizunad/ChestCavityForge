package net.tigereye.chestcavity.guscript.runtime.flow.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.FxRegistry;
import net.tigereye.chestcavity.guscript.fx.client.FxClientDispatcher;
import net.tigereye.chestcavity.guscript.network.packets.FlowSyncPayload;

import java.util.List;
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
            FlowSyncPayload previous = this.lastPayload;
            this.lastPayload = payload;
            ChestCavity.LOGGER.debug("[Flow][Client] Entity {} program {} -> {} (t={} start={})", entityId, payload.programId(), payload.state(), payload.ticksInState(), payload.stateGameTime());
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == entityId) {
                if (previous == null || previous.state() != payload.state()) {
                    playEnterFx(payload);
                }
            }
        }

        private void playEnterFx(FlowSyncPayload payload) {
            if (payload.enterFx().isEmpty()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            Entity entity = minecraft.level.getEntity(entityId);
            if (!(entity instanceof LivingEntity living)) {
                return;
            }
            Vec3 origin = new Vec3(living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ());
            Vec3 look = living.getLookAngle();
            FxRegistry.FxContext context = new FxRegistry.FxContext(origin, look, look, null, 1.0F, living.getId(), -1);
            List<ResourceLocation> fxIds = payload.enterFx();
            for (ResourceLocation fxId : fxIds) {
                FxClientDispatcher.play(fxId, context);
            }
        }
    }
}
