package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Lightweight per-soul storage dedicated to a single sub-brain instance. */
public final class SubBrainMemory {

    private final Map<String, Object> backing = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> fallback) {
        return (T) backing.computeIfAbsent(key, k -> fallback.get());
    }

    @SuppressWarnings("unchecked")
    public <T> T getIfPresent(String key) {
        return (T) backing.get(key);
    }

    public void put(String key, Object value) {
        if (value == null) {
            backing.remove(key);
        } else {
            backing.put(key, value);
        }
    }

    public void clear() {
        backing.clear();
    }
}
