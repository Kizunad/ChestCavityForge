package net.tigereye.chestcavity.soulbeast.state;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Event hooks for synchronising soul beast state with clients.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulBeastStateEvents {

    private SoulBeastStateEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulBeastStateManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulBeastStateManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulBeastStateManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulBeastStateManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long current = event.getServer().getTickCount();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SoulBeastStateManager.getExisting(player).ifPresent(state -> {
                if (!state.isActive()) {
                    return;
                }
                long lastTick = state.getLastTick();
                if (current - lastTick >= 20L) {
                    state.setLastTick(current);
                    SoulBeastStateManager.syncToClient(player);
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren][hun_dao][state] clearing client cache on level unload");
            SoulBeastStateManager.clearClientCache();
        }
    }
}
