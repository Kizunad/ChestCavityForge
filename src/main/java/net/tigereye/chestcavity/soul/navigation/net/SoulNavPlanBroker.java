package net.tigereye.chestcavity.soul.navigation.net;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SoulNavPlanBroker {
    private SoulNavPlanBroker() {}

    private static final Map<Long, CompletableFuture<List<Vec3>>> WAITING = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong IDS = new java.util.concurrent.atomic.AtomicLong(1L);

    // Simple owner-side limits to avoid flooding
    private static final Map<java.util.UUID, java.util.concurrent.atomic.AtomicInteger> INFLIGHT = new ConcurrentHashMap<>();
    private static final Map<java.util.UUID, Long> LAST_REQ_AT = new ConcurrentHashMap<>();
    private static final int MAX_INFLIGHT = Math.max(1, Integer.getInteger("chestcavity.soul.baritone.maxInflight", 2));
    private static final long MIN_INTERVAL_MS = Math.max(50L, Long.getLong("chestcavity.soul.baritone.minPlanIntervalMs", 150L));

    // Stats (debug only)
    private static final java.util.concurrent.atomic.AtomicInteger STAT_TIMEOUTS = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger STAT_SEND_FAILS = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger STAT_RATE_LIMITED = new java.util.concurrent.atomic.AtomicInteger();

    public static Optional<List<Vec3>> requestPlan(ServerPlayer owner, java.util.UUID soulId, Vec3 from, Vec3 target, long timeoutMs) {
        long id = IDS.getAndIncrement();
        CompletableFuture<List<Vec3>> fut = new CompletableFuture<>();
        // simple rate limits per owner
        java.util.UUID ownerId = owner.getUUID();
        long now = System.currentTimeMillis();
        long last = LAST_REQ_AT.getOrDefault(ownerId, 0L);
        if ((now - last) < MIN_INTERVAL_MS) {
            STAT_RATE_LIMITED.incrementAndGet();
            return Optional.empty();
        }
        int inflight = INFLIGHT.computeIfAbsent(ownerId, k -> new java.util.concurrent.atomic.AtomicInteger(0)).get();
        if (inflight >= MAX_INFLIGHT) {
            STAT_RATE_LIMITED.incrementAndGet();
            return Optional.empty();
        }
        LAST_REQ_AT.put(ownerId, now);
        INFLIGHT.get(ownerId).incrementAndGet();
        WAITING.put(id, fut);
        try {
            owner.connection.send(new SoulNavPlanRequestPayload(id, soulId, from, target, timeoutMs));
        } catch (Throwable t) {
            WAITING.remove(id);
            INFLIGHT.get(ownerId).decrementAndGet();
            if (SoulLog.DEBUG_LOGS) SoulLog.info("[soul][nav][baritone] failed to send plan req: {}", t.toString());
            STAT_SEND_FAILS.incrementAndGet();
            return Optional.empty();
        }
        try {
            List<Vec3> result = fut.get(Math.max(250L, timeoutMs), TimeUnit.MILLISECONDS);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            if (SoulLog.DEBUG_LOGS) SoulLog.info("[soul][nav][baritone] plan timeout or error: {}", e.toString());
            STAT_TIMEOUTS.incrementAndGet();
            return Optional.empty();
        } finally {
            WAITING.remove(id);
            INFLIGHT.get(ownerId).decrementAndGet();
        }
    }

    static void onResponse(SoulNavPlanResponsePayload payload) {
        CompletableFuture<List<Vec3>> fut = WAITING.get(payload.requestId());
        if (fut != null) {
            fut.complete(payload.waypoints());
        }
    }

    public static String debugSummary() {
        return "timeouts=" + STAT_TIMEOUTS.get() + ", sendFails=" + STAT_SEND_FAILS.get() + ", rateLimited=" + STAT_RATE_LIMITED.get();
    }
}
