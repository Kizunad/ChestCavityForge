package net.tigereye.chestcavity.util.reaction.tag;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 反应 Tag 的附着与判定工具：
 * - 维护“实体 -> (tagId -> expireTick)”映射；
 * - 提供 add/has/clear API；
 * - 对外暴露 {@link #purge(long)} 以便在服务端 tick 清理。
 *
 * 说明：
 * - 此处的 Tag 为轻量运行时标记（如“油涂层”、“火衣免疫窗口”），不是数据包 Tag；
 * - 选择用 ResourceLocation 直表述，便于与现有代码平滑过渡。
 */
public final class ReactionTagOps {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReactionTagOps() {}

    private static final Map<UUID, Map<ResourceLocation, Long>> TAGS = new HashMap<>();

    public static void add(LivingEntity entity, ResourceLocation tagId, int durationTicks) {
        if (entity == null || tagId == null || durationTicks <= 0) return;
        long expire = entity.level().getGameTime() + durationTicks;
        TAGS.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).put(tagId, expire);
    }

    public static boolean has(LivingEntity entity, ResourceLocation tagId) {
        if (entity == null || tagId == null) return false;
        Map<ResourceLocation, Long> map = TAGS.get(entity.getUUID());
        if (map == null) return false;
        Long exp = map.get(tagId);
        return exp != null && exp > entity.level().getGameTime();
    }

    public static void clear(LivingEntity entity, ResourceLocation tagId) {
        if (entity == null || tagId == null) return;
        Map<ResourceLocation, Long> map = TAGS.get(entity.getUUID());
        if (map != null) {
            map.remove(tagId);
            if (map.isEmpty()) TAGS.remove(entity.getUUID());
        }
    }

    public static void purge(long nowServerTick) {
        if (TAGS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Map<ResourceLocation, Long>>> it = TAGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Map<ResourceLocation, Long>> e = it.next();
            Map<ResourceLocation, Long> inner = e.getValue();
            if (inner == null || inner.isEmpty()) { it.remove(); continue; }
            inner.entrySet().removeIf(en -> en.getValue() <= nowServerTick);
            if (inner.isEmpty()) it.remove();
        }
    }
}

