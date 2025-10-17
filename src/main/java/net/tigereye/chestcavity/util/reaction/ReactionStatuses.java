package net.tigereye.chestcavity.util.reaction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.util.DoTTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一管理“反应相关状态”（非伤害型的临时标记，例如油涂层）。
 * 简化实现：使用静态映射，按需可迁移到 Attachment/Capability。
 */
public final class ReactionStatuses {
    private ReactionStatuses() {}

    // 状态常量
    public static final ResourceLocation OIL_COATING = ChestCavity.id("status/oil_coating");
    // 将火衣触发 DotType 映射为“可反应的触发类型”
    public static final ResourceLocation OIL_COATING_TRIGGER_DOT = DoTTypes.YAN_DAO_HUO_YI_AURA;

    // 全局状态表：实体 -> (statusId -> expireTick)
    static final Map<UUID, Map<ResourceLocation, Long>> STATUSES = new HashMap<>();

    static void addStatus(LivingEntity entity, ResourceLocation id, int durationTicks) {
        if (entity == null || id == null || durationTicks <= 0) return;
        long expire = entity.level().getGameTime() + durationTicks;
        STATUSES.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).put(id, expire);
    }

    static boolean hasStatus(LivingEntity entity, ResourceLocation id) {
        if (entity == null || id == null) return false;
        Map<ResourceLocation, Long> map = STATUSES.get(entity.getUUID());
        if (map == null) return false;
        Long exp = map.get(id);
        return exp != null && exp > entity.level().getGameTime();
    }

    static void clearStatus(LivingEntity entity, ResourceLocation id) {
        if (entity == null || id == null) return;
        Map<ResourceLocation, Long> map = STATUSES.get(entity.getUUID());
        if (map != null) {
            map.remove(id);
            if (map.isEmpty()) {
                STATUSES.remove(entity.getUUID());
            }
        }
    }

    public static boolean isHuoYiDot(ResourceLocation dotTypeId) {
        return DoTTypes.YAN_DAO_HUO_YI_AURA.equals(dotTypeId);
    }
}

