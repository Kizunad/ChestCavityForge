package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 护幕飞剑玩家 Tick 事件处理器
 * <p>
 * 监听玩家 Tick 事件，驱动护幕飞剑服务的状态机。
 * <ul>
 *   <li>检查玩家是否装备了"剑幕·反击之幕"器官</li>
 *   <li>调用 {@link DefaultWardSwordService#tick(net.minecraft.world.entity.player.Player)} 驱动护幕飞剑状态机</li>
 *   <li>自动调用 {@link WardSwordService#ensureWardSwords} 确保飞剑数量正确</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class WardSwordPlayerTickEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(WardSwordPlayerTickEvents.class);

    private static final String MOD_ID = "guzhenren";

    /**
     * 剑幕·反击之幕器官ID
     * TODO: 确保这个ID与实际的器官注册ID一致
     */
    private static final ResourceLocation BLOCKSHIELD_ORGAN_ID =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "blockshield");

    private WardSwordPlayerTickEvents() {}

    /**
     * 玩家 Tick 事件处理器（服务端）
     *
     * @param event 玩家 Tick 事件
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        // 检查玩家是否装备了剑幕器官
        if (!hasBlockShieldOrgan(player)) {
            // 如果之前有护幕飞剑，现在没有器官了，需要清理
            DefaultWardSwordService service = DefaultWardSwordService.getInstance();
            if (service.hasWardSwords(player)) {
                LOGGER.debug("Player {} unequipped BlockShield organ, disposing ward swords",
                    player.getName().getString());
                service.disposeWardSwords(player);
            }
            return;
        }

        // 获取护幕服务实例
        DefaultWardSwordService service = DefaultWardSwordService.getInstance();

        try {
            // 确保护幕飞剑数量正确（首次装备时会创建，之后会维持）
            service.ensureWardSwords(player);

            // 驱动护幕飞剑状态机
            service.tick(player);

        } catch (Exception e) {
            LOGGER.error("Error ticking ward swords for player {}", player.getName().getString(), e);
        }
    }

    /**
     * 检查玩家是否装备了剑幕·反击之幕器官
     *
     * @param player 玩家
     * @return 是否装备了剑幕器官
     */
    private static boolean hasBlockShieldOrgan(ServerPlayer player) {
        return ChestCavityEntity.of(player)
            .map(ChestCavityEntity::getChestCavityInstance)
            .filter(cc -> cc.inventory != null)
            .map(cc -> {
                for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                    var stack = cc.inventory.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    ResourceLocation itemId =
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null && itemId.equals(BLOCKSHIELD_ORGAN_ID)) {
                        return true;
                    }
                }
                return false;
            })
            .orElse(false);
    }
}
