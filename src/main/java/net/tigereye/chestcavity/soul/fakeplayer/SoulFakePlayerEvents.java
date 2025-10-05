package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.util.SoulPersistence;

import java.util.UUID;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulFakePlayerEvents {

    private SoulFakePlayerEvents() {
    }

    /**
     * 玩家登出：
     * - 完整刷新 owner 与所有分魂的离线快照；
     * - 强制切回本体档；
     * - 清除在场分魂壳。
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID owner = player.getUUID();
            SoulLog.info("[soul] logout-switch begin owner={}", owner);

            SoulPersistence.saveAll(player);
            SoulFakePlayerSpawner.forceOwner(player);
            SoulFakePlayerSpawner.removeByOwner(owner);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulPersistence.loadAll(player);
        }
    }

    /**
     * 服务器停止：释放所有仍存活的灵魂假人与可视化实体引用，防止内存泄漏。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SoulFakePlayerSpawner.clearAll();
    }
}
