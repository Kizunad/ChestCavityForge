package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 护幕飞剑调试辅助工具
 * <p>
 * 用于诊断为什么拦截没有触发
 */
public final class WardSwordDebugHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(WardSwordDebugHelper.class);

    private WardSwordDebugHelper() {}

    /**
     * 调试玩家的护幕状态
     *
     * @param player 玩家
     */
    public static void debugPlayerWardState(Player player) {
        LOGGER.info("=== Ward Sword Debug for Player: {} ===", player.getName().getString());

        // 1. 检查是否有ChestCavity
        ChestCavityEntity.of(player).ifPresentOrElse(
            ccEntity -> {
                LOGGER.info("✓ Player has ChestCavityEntity");
                var cc = ccEntity.getChestCavityInstance();
                if (cc == null) {
                    LOGGER.warn("✗ ChestCavity instance is NULL");
                    return;
                }
                if (cc.inventory == null) {
                    LOGGER.warn("✗ ChestCavity inventory is NULL");
                    return;
                }

                LOGGER.info("✓ ChestCavity inventory exists, size: {}", cc.inventory.getContainerSize());

                // 列出所有器官
                boolean foundBlockshield = false;
                for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                    var stack = cc.inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                        LOGGER.info("  Slot {}: {} (count: {})", i, itemId, stack.getCount());

                        // 检查是否是blockshield
                        if (itemId != null && itemId.getPath().contains("blockshield")) {
                            LOGGER.info("  ★ Found blockshield-like organ!");
                            foundBlockshield = true;
                        }
                    }
                }

                if (!foundBlockshield) {
                    LOGGER.warn("✗ No blockshield organ found in inventory");
                }
            },
            () -> LOGGER.warn("✗ Player does NOT have ChestCavityEntity")
        );

        // 2. 检查护幕服务
        DefaultWardSwordService service = DefaultWardSwordService.getInstance();
        int swordCount = service.getWardCount(player);
        LOGGER.info("Ward sword count: {}", swordCount);

        if (swordCount > 0) {
            LOGGER.info("✓ Player has {} ward sword(s)", swordCount);
            var swords = service.getWardSwords(player);
            for (int i = 0; i < swords.size(); i++) {
                var sword = swords.get(i);
                LOGGER.info("  Sword {}: state={}, durability={}, pos={}",
                    i,
                    sword.getWardState(),
                    sword.getWardDurability(),
                    sword.position());
            }
        } else {
            LOGGER.warn("✗ Player has NO ward swords");
        }

        LOGGER.info("=== End Debug ===");
    }

    /**
     * 调试威胁检测
     *
     * @param player 玩家
     * @param eventDescription 事件描述
     */
    public static void debugThreatDetection(Player player, String eventDescription) {
        LOGGER.info("=== Threat Detection Debug ===");
        LOGGER.info("Event: {}", eventDescription);
        LOGGER.info("Player: {}", player.getName().getString());

        // 检查事件处理器是否被调用
        LOGGER.info("WardSwordDamageEvents.onLivingIncomingDamage was called");

        // 检查关键条件
        DefaultWardSwordService service = DefaultWardSwordService.getInstance();
        boolean hasOrgan = hasBlockShieldOrganDebug(player);
        boolean hasSwords = service.hasWardSwords(player);

        LOGGER.info("Has blockshield organ: {}", hasOrgan);
        LOGGER.info("Has ward swords: {}", hasSwords);

        if (!hasOrgan) {
            LOGGER.warn("✗ Missing blockshield organ - event will be skipped");
        }
        if (!hasSwords) {
            LOGGER.warn("✗ No ward swords - event will be skipped");
        }

        LOGGER.info("=== End Threat Debug ===");
    }

    private static boolean hasBlockShieldOrganDebug(Player player) {
        ResourceLocation BLOCKSHIELD_ORGAN_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "blockshield");

        return ChestCavityEntity.of(player)
            .map(ChestCavityEntity::getChestCavityInstance)
            .filter(cc -> cc.inventory != null)
            .map(cc -> {
                for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                    var stack = cc.inventory.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                    LOGGER.debug("Checking slot {}: {} vs {}", i, itemId, BLOCKSHIELD_ORGAN_ID);
                    if (itemId != null && itemId.equals(BLOCKSHIELD_ORGAN_ID)) {
                        LOGGER.info("✓ Found matching blockshield organ at slot {}", i);
                        return true;
                    }
                }
                return false;
            })
            .orElse(false);
    }
}
