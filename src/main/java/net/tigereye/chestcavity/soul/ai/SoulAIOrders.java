package net.tigereye.chestcavity.soul.ai;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI orders with persistent storage. Default is IDLE.
 * - If a ServerPlayer owner is provided, read/write through SoulContainer attachment (persistent).
 * - Otherwise falls back to an ephemeral map for temporary use.
 */
public final class SoulAIOrders {

    public enum Order { IDLE, FOLLOW, GUARD, FORCE_FIGHT }

    private static final Map<UUID, Order> EPHEMERAL = new ConcurrentHashMap<>();

    private SoulAIOrders() {}

    // Persistent path
    public static Order get(ServerPlayer owner, UUID soulId) {
        SoulContainer c = CCAttachments.getSoulContainer(owner);
        return c.getOrder(soulId);
    }

    public static void set(ServerPlayer owner, UUID soulId, Order order, String reason) {
        if (order == null) order = Order.IDLE;
        SoulContainer c = CCAttachments.getSoulContainer(owner);
        c.setOrder(owner, soulId, order, reason);
    }

    // Fallback (no owner context)
    public static Order get(UUID soulId) {
        return EPHEMERAL.getOrDefault(soulId, Order.IDLE);
    }

    public static void set(UUID soulId, Order order) {
        if (order == null) order = Order.IDLE;
        EPHEMERAL.put(soulId, order);
    }

    public static void clear(UUID soulId) {
        EPHEMERAL.remove(soulId);
    }

    public static void clearAll() {
        EPHEMERAL.clear();
    }
}
