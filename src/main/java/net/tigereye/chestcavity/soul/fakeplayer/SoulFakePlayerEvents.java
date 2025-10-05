package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.minecraft.nbt.CompoundTag;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.Map;
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

            var server = player.serverLevel().getServer();
            var store = net.tigereye.chestcavity.soul.storage.SoulOfflineStore.get(server);
            SoulContainer container = CCAttachments.getSoulContainer(player);
            Map<UUID, CompoundTag> serialized = container.snapshotAll(player);
            store.putAll(owner, serialized);
            SoulLog.info("[soul] logout-switch stored owner={} profiles={} ", owner, serialized.size());

            SoulFakePlayerSpawner.forceOwner(player);
            SoulFakePlayerSpawner.removeByOwner(owner);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var server = player.serverLevel().getServer();
            var store = net.tigereye.chestcavity.soul.storage.SoulOfflineStore.get(server);
            SoulContainer container = CCAttachments.getSoulContainer(player);
            var pending = store.consume(player.getUUID());
            if (!pending.isEmpty()) {
                container.restoreAll(player, pending);
            }
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
