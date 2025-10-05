package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.util.SoulProfileOps;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulFakePlayerEvents {

    private SoulFakePlayerEvents() {
    }

    /**
     * 玩家登出：
     * - 强制切换回本体（Owner），确保离线前玩家主体处于本体魂档；
     * - 回写一次容器，标记持久；
     * - 清理该玩家名下的所有灵魂假人与可视化实体（并登记 autospawn）。
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulContainer container = CCAttachments.getSoulContainer(player);
            java.util.UUID owner = player.getUUID();
            var activeId = container.getActiveProfileId().orElse(owner);
            SoulLog.info("[soul] logout-switch begin owner={} active={}", owner, activeId);

            // 1) 先落盘当前激活魂（无论是否为本体），以免 forceOwner 覆盖 activeProfileId
            SoulFakePlayerSpawner.flushActiveSoulsForOwner(player);

            // 2) 再切回本体，确保退出时主体归位且不额外生成实体
            SoulFakePlayerSpawner.forceOwner(player);
            SoulLog.info("[soul] logout-switch applied owner={} nowActive={}", owner, container.getActiveProfileId().orElse(owner));

            // 3) 最后移除仍存活的分魂实体并登记 autospawn，保证下次登录自动还原
            SoulFakePlayerSpawner.removeByOwner(owner);
        }
    }

    /**
     * 玩家登录：根据容器中的 autospawn 列表自动重生对应的 SoulPlayer。
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulContainer container = CCAttachments.getSoulContainer(player);
            // Apply any offline queued snapshots first
            var server = player.serverLevel().getServer();
            var store = net.tigereye.chestcavity.soul.storage.SoulOfflineStore.get(server);
            var pending = store.consume(player.getUUID());
            if (!pending.isEmpty()) {
                for (var entry : pending.entrySet()) {
                    java.util.UUID soulId = entry.getKey();
                    var profile = net.tigereye.chestcavity.soul.profile.SoulProfile.load(entry.getValue(), player.registryAccess());
                    container.putProfile(soulId, profile);
                    SoulLog.info("[soul] offline-apply owner={} soul={} action=mergeSnapshotOnLogin", player.getUUID(), soulId);
                }
                SoulProfileOps.markContainerDirty(player, container, "login-offlineApply");
            }
            SoulProfileOps.tryResumeActive(player);
            // Never autospawn owner profile as a SoulPlayer entity
            container.removeAutospawnSoul(player.getUUID());
            boolean removedAny = false;
            for (java.util.UUID id : container.getAutospawnSouls()) {
                if (id.equals(player.getUUID())) {
                    continue;
                }
                if (SoulFakePlayerSpawner.findSoulPlayer(id).isPresent()) {
                    SoulLog.info("[soul] autospawn skip owner={} soul={} reason=alreadySpawned", player.getUUID(), id);
                    continue;
                }
                SoulLog.info("[soul] autospawn trigger owner={} soul={} reason=playerLogin", player.getUUID(), id);
                SoulFakePlayerSpawner.respawnForOwner(player, id);
                container.removeAutospawnSoul(id);
                removedAny = true;
            }
            if (removedAny) {
                SoulProfileOps.markContainerDirty(player, container, "login-autospawn-consume");
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
