package net.tigereye.chestcavity.soul.fakeplayer.brain.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight TTL blackboard for per-soul memory. Keys are strings here for
 * simplicity; production code should prefer ResourceLocation.
 */
public final class Blackboard {
    private final Map<String, Entry> data = new HashMap<>();

    public void put(String key, Object value, int ttlTicks) {
        Objects.requireNonNull(key, "key");
        if (value == null || ttlTicks <= 0) {
            data.remove(key);
            return;
        }
        data.put(key, new Entry(value, ttlTicks));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Entry e = data.get(key);
        if (e == null || e.ttl <= 0) return null;
        Object v = e.value;
        return type.isInstance(v) ? (T) v : null;
    }

    public void tick() {
        Iterator<Map.Entry<String, Entry>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> en = it.next();
            Entry e = en.getValue();
            e.ttl -= 1;
            if (e.ttl <= 0) it.remove();
        }
    }

    public void clear() { data.clear(); }

    private static final class Entry {
        final Object value;
        int ttl;
        Entry(Object value, int ttl) { this.value = value; this.ttl = ttl; }
    }
}

