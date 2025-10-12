package net.tigereye.chestcavity.soul.ai;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 灵魂指令状态表。
 *
 * <p>默认指令为 {@code IDLE}，优先通过 {@link SoulContainer} 持久化存储；在无宿主上下文时，则退化为进程内的
 * 临时缓存。</p>
 */
public final class SoulAIOrders {

    public enum Order { IDLE, FOLLOW, GUARD, FORCE_FIGHT }

    private static final Map<UUID, Order> EPHEMERAL = new ConcurrentHashMap<>();

    private SoulAIOrders() {}

    // Persistent path
    /**
     * 从玩家容器读取指定灵魂的指令。
     */
    public static Order get(ServerPlayer owner, UUID soulId) {
        SoulContainer c = CCAttachments.getSoulContainer(owner);
        return c.getOrder(soulId);
    }

    /**
     * 将指令写入容器并同步持久化。
     */
    public static void set(ServerPlayer owner, UUID soulId, Order order, String reason) {
        if (order == null) order = Order.IDLE;
        SoulContainer c = CCAttachments.getSoulContainer(owner);
        c.setOrder(owner, soulId, order, reason);
    }

    // Fallback (no owner context)
    /**
     * 读取临时缓存中的指令（无宿主）。
     */
    public static Order get(UUID soulId) {
        return EPHEMERAL.getOrDefault(soulId, Order.IDLE);
    }

    /**
     * 写入临时缓存中的指令。
     */
    public static void set(UUID soulId, Order order) {
        if (order == null) order = Order.IDLE;
        EPHEMERAL.put(soulId, order);
    }

    /**
     * 清除单个灵魂的临时指令缓存。
     */
    public static void clear(UUID soulId) {
        EPHEMERAL.remove(soulId);
    }

    /**
     * 清除全部临时指令缓存。
     */
    public static void clearAll() {
        EPHEMERAL.clear();
    }
}
