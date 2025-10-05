package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulFakePlayerEvents {

    private SoulFakePlayerEvents() {
    }

    /**
     * 玩家登出：
     * - 将激活的灵魂存档回写一次，避免未保存的状态丢失；
     * - 清理该玩家名下的所有灵魂假人与可视化实体。
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulContainer container = CCAttachments.getSoulContainer(player);
            container.updateActiveProfile();
            container.getActiveProfileId().ifPresent(SoulFakePlayerSpawner::saveSoulPlayerState);
            SoulFakePlayerSpawner.removeByOwner(player.getUUID());
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
