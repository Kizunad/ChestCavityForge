package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.state;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.fml.common.Mod.EventBusSubscriber;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Event hooks for synchronising soul beast state with clients.
 */
@EventBusSubscriber(modid = ChestCavity.MODID, bus = EventBusSubscriber.Bus.FORGE)
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
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level().isClientSide() || event.phase() != TickEvent.Phase.END) {
            return;
        }
        event.level().players().forEach(player -> SoulBeastStateManager.getExisting(player).ifPresent(state -> {
            long current = event.level().getGameTime();
            if (!state.isActive()) {
                return;
            }
            long lastTick = state.getLastTick();
            if (current - lastTick >= 20L) {
                state.setLastTick(current);
                SoulBeastStateManager.syncToClient((ServerPlayer) player);
            }
        }));
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
