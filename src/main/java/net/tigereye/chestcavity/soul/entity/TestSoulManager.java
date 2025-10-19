package net.tigereye.chestcavity.soul.entity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负责 Test 生物的上限与存活计数。
 */
public final class TestSoulManager {

    private static final AtomicInteger MAX_COUNT = new AtomicInteger(1);
    private static final Set<UUID> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private TestSoulManager() {
    }

    public static boolean canSpawn() {
        return ACTIVE.size() < MAX_COUNT.get();
    }

    public static void register(TestSoulEntity entity) {
        if (entity == null) return;
        ACTIVE.add(entity.getUUID());
    }

    public static void unregister(TestSoulEntity entity) {
        if (entity == null) return;
        ACTIVE.remove(entity.getUUID());
    }

    public static int getMaxCount() {
        return MAX_COUNT.get();
    }

    public static void setMaxCount(int value) {
        MAX_COUNT.set(Math.max(1, value));
    }

    public static int getActiveCount() {
        return ACTIVE.size();
    }
}
