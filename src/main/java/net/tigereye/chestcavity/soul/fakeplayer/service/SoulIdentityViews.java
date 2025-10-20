package net.tigereye.chestcavity.soul.fakeplayer.service;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;

import java.util.UUID;

/**
 * Identity 辅助视图：提供展示层常用的名称解析逻辑，统一复用身份服务与容器记录。
 */
public final class SoulIdentityViews {

    private SoulIdentityViews() {
    }

    public static String resolveDisplayName(ServerPlayer owner, UUID soulId) {
        if (soulId == null) {
            return "#????????";
        }
        String displayName = null;
        if (owner != null) {
            // Owner 自身：使用玩家名优先
            if (owner.getUUID().equals(soulId)) {
                String ownerName = owner.getGameProfile().getName();
                if (ownerName != null && !ownerName.isBlank()) {
                    return ownerName;
                }
            }
            try {
                SoulContainer container = CCAttachments.getSoulContainer(owner);
                if (container != null) {
                    displayName = container.getName(soulId);
                }
            } catch (Exception ignored) {
                // 读取容器失败时退回到 identity 名称
            }
        }
        if (displayName == null || displayName.isBlank()) {
            var identity = SoulFakePlayerServices.identity().getIdentity(soulId);
            if (identity != null && identity.getName() != null && !identity.getName().isBlank()) {
                displayName = identity.getName();
            }
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = "#" + shortId(soulId);
        }
        return displayName;
    }

    private static String shortId(UUID soulId) {
        String id = soulId.toString();
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
