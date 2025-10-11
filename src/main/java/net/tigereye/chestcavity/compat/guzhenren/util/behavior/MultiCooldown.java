package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Container for multiple organ cooldown slots backed by a shared {@link OrganState}.
 * Provides optional sync integration and per-entry change callbacks.
 */
public final class MultiCooldown {

    private static final LongUnaryOperator NON_NEGATIVE_LONG = value -> Math.max(0L, value);
    private static final IntUnaryOperator NON_NEGATIVE_INT = value -> Math.max(0, value);

    private final OrganState state;
    private final ChestCavityInstance chestCavity;
    private final ItemStack organ;
    private final LongUnaryOperator longClamp;
    private final long longDefault;
    private final IntUnaryOperator intClamp;
    private final int intDefault;

    private final Map<String, Entry> longEntries = new HashMap<>();
    private final Map<String, EntryInt> intEntries = new HashMap<>();

    private MultiCooldown(
            OrganState state,
            ChestCavityInstance chestCavity,
            ItemStack organ,
            LongUnaryOperator longClamp,
            long longDefault,
            IntUnaryOperator intClamp,
            int intDefault
    ) {
        this.state = Objects.requireNonNull(state, "state");
        this.chestCavity = chestCavity;
        this.organ = organ;
        this.longClamp = longClamp == null ? NON_NEGATIVE_LONG : longClamp;
        this.longDefault = longDefault;
        this.intClamp = intClamp == null ? NON_NEGATIVE_INT : intClamp;
        this.intDefault = intDefault;
    }

    /** Build from an existing {@link OrganState}. */
    public static Builder builder(OrganState state) {
        return new Builder(state);
    }

    /** Build while binding the organ stack/root key inline. */
    public static Builder builder(ItemStack organ, String rootKey) {
        return new Builder(OrganState.of(organ, rootKey)).withOrgan(organ);
    }

    /** Access the underlying organ state. */
    public OrganState state() {
        return state;
    }

    /** Retrieve (or lazily create) a long-based cooldown entry. */
    public Entry entry(String key) {
        return longEntries.computeIfAbsent(key, Entry::new);
    }

    /** Whether a long cooldown value is currently stored for the given key. */
    public boolean hasLong(String key) {
        return state.getLong(key, Long.MIN_VALUE) != Long.MIN_VALUE;
    }

    /** Retrieve (or lazily create) an int-based countdown entry. */
    public EntryInt entryInt(String key) {
        return intEntries.computeIfAbsent(key, EntryInt::new);
    }

    /** Whether an int countdown value is currently stored for the given key. */
    public boolean hasInt(String key) {
        return state.getInt(key, Integer.MIN_VALUE) != Integer.MIN_VALUE;
    }

    /** Clear all tracked cooldown entries. */
    public void clearAll() {
        longEntries.values().forEach(Entry::clear);
        intEntries.values().forEach(EntryInt::clear);
    }

    private OrganState.Change<Long> writeLong(String key, long value, LongUnaryOperator clamp, long defaultValue) {
        if (chestCavity != null && organ != null && !organ.isEmpty()) {
            return OrganStateOps.setLong(state, chestCavity, organ, key, value, clamp, defaultValue);
        }
        return state.setLong(key, value, clamp, defaultValue);
    }

    private OrganState.Change<Integer> writeInt(String key, int value, IntUnaryOperator clamp, int defaultValue) {
        if (chestCavity != null && organ != null && !organ.isEmpty()) {
            return OrganStateOps.setInt(state, chestCavity, organ, key, value, clamp, defaultValue);
        }
        return state.setInt(key, value, clamp, defaultValue);
    }

    /** Represents a timestamp-style cooldown entry (stores the next ready game tick). */
    public final class Entry {
        private final String key;
        private BiConsumer<Long, Long> onChange;
        private LongUnaryOperator clampOverride;
        private Long defaultOverride;

        private Entry(String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        /** Override the clamp function for this entry only. */
        public Entry withClamp(LongUnaryOperator clamp) {
            this.clampOverride = clamp;
            return this;
        }

        /** Override the default ready tick for this entry. */
        public Entry withDefault(long defaultValue) {
            this.defaultOverride = defaultValue;
            return this;
        }

        /** Install a change listener invoked when the stored value mutates. */
        public Entry withOnChange(BiConsumer<Long, Long> onChange) {
            this.onChange = onChange;
            return this;
        }

        /** Next allowed game tick; equals the configured default when unset. */
        public long getReadyTick() {
            return state.getLong(key, resolveDefault());
        }

        /** Set the next allowed game tick. */
        public void setReadyAt(long readyTick) {
            long defaultValue = resolveDefault();
            LongUnaryOperator clamp = resolveClamp();
            OrganState.Change<Long> change = writeLong(key, readyTick, clamp, defaultValue);
            if (onChange != null && change.changed() && change.previous() != change.current()) {
                onChange.accept(change.previous(), change.current());
            }
        }

        /** Clear the cooldown; the entry becomes ready immediately. */
        public void clear() {
            setReadyAt(resolveDefault());
        }

        /** Remaining ticks until the cooldown becomes ready. */
        public long remaining(long gameTime) {
            return Math.max(0L, getReadyTick() - gameTime);
        }

        /** Whether the cooldown has elapsed for the provided game time. */
        public boolean isReady(long gameTime) {
            return gameTime >= getReadyTick();
        }

        /**
         * Attempt to start the cooldown.
         *
         * @return true if the cooldown started, false otherwise.
         */
        public boolean tryStart(long gameTime, long durationTicks) {
            if (!isReady(gameTime)) {
                return false;
            }
            setReadyAt(gameTime + Math.max(0L, durationTicks));
            return true;
        }

        /** Register a one-shot runnable when this timestamp-style cooldown becomes ready (relative to {@code now}). */
        public Entry onReady(net.minecraft.server.level.ServerLevel level, long now, Runnable task) {
            if (level == null || task == null) return this;
            long remaining = Math.max(0L, getReadyTick() - now);
            int delay = remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
            net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps.schedule(level, task, delay);
            return this;
        }

        private long resolveDefault() {
            return defaultOverride == null ? longDefault : defaultOverride;
        }

        private LongUnaryOperator resolveClamp() {
            return clampOverride == null ? longClamp : clampOverride;
        }
    }

    /** Represents a countdown-style cooldown entry (stores remaining ticks). */
    public final class EntryInt {
        private final String key;
        private BiConsumer<Integer, Integer> onChange;
        private IntUnaryOperator clampOverride;
        private Integer defaultOverride;

        private EntryInt(String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        public EntryInt withClamp(IntUnaryOperator clamp) {
            this.clampOverride = clamp;
            return this;
        }

        public EntryInt withDefault(int defaultValue) {
            this.defaultOverride = defaultValue;
            return this;
        }

        public EntryInt withOnChange(BiConsumer<Integer, Integer> onChange) {
            this.onChange = onChange;
            return this;
        }

        /** Remaining ticks on this countdown. */
        public int getTicks() {
            int value = state.getInt(key, resolveDefault());
            return Math.max(0, value);
        }

        /** Set the remaining ticks on this countdown. */
        public void setTicks(int ticks) {
            int defaultValue = resolveDefault();
            IntUnaryOperator clamp = resolveClamp();
            OrganState.Change<Integer> change = writeInt(key, ticks, clamp, defaultValue);
            if (onChange != null && change.changed() && !change.previous().equals(change.current())) {
                onChange.accept(change.previous(), change.current());
            }
        }

        /** Clear the countdown (set to default). */
        public void clear() {
            setTicks(resolveDefault());
        }

        /** Whether the countdown has expired (ready to act). */
        public boolean isReady() {
            return getTicks() <= 0;
        }

        /** Start (or restart) the countdown to the provided duration. */
        public void start(int durationTicks) {
            setTicks(Math.max(0, durationTicks));
        }

        /**
         * Decrement the countdown by one tick.
         *
         * @return true if a decrement occurred, false if already ready.
         */
        public boolean tickDown() {
            int current = getTicks();
            if (current <= 0) {
                return false;
            }
            setTicks(current - 1);
            return true;
        }

        /** Attach a callback invoked when the countdown crosses from >0 to 0 (no polling). */
        public EntryInt onReady(Runnable task) {
            if (task == null) return this;
            BiConsumer<Integer, Integer> prevHook = this.onChange;
            this.onChange = (prev, curr) -> {
                if (prevHook != null) prevHook.accept(prev, curr);
                if (prev != null && prev > 0 && curr != null && curr == 0) {
                    task.run();
                }
            };
            return this;
        }

        private int resolveDefault() {
            return defaultOverride == null ? intDefault : defaultOverride;
        }

        private IntUnaryOperator resolveClamp() {
            return clampOverride == null ? intClamp : clampOverride;
        }
    }

    /** Builder for {@link MultiCooldown} instances. */
    public static final class Builder {
        private final OrganState state;
        private ChestCavityInstance chestCavity;
        private ItemStack organ;
        private LongUnaryOperator longClamp = NON_NEGATIVE_LONG;
        private long longDefault = 0L;
        private IntUnaryOperator intClamp = NON_NEGATIVE_INT;
        private int intDefault = 0;

        private Builder(OrganState state) {
            this.state = Objects.requireNonNull(state, "state");
        }

        /** Attach the owning chest cavity + organ stack for automatic slot sync. */
        public Builder withSync(ChestCavityInstance chestCavity, ItemStack organ) {
            this.chestCavity = chestCavity;
            this.organ = organ;
            return this;
        }

        /** Provide the organ stack without enabling sync (useful when cc is not available). */
        public Builder withOrgan(ItemStack organ) {
            this.organ = organ;
            return this;
        }

        public Builder withLongClamp(LongUnaryOperator clamp, long defaultValue) {
            if (clamp != null) {
                this.longClamp = clamp;
            }
            this.longDefault = defaultValue;
            return this;
        }

        public Builder withIntClamp(IntUnaryOperator clamp, int defaultValue) {
            if (clamp != null) {
                this.intClamp = clamp;
            }
            this.intDefault = defaultValue;
            return this;
        }

        public MultiCooldown build() {
            return new MultiCooldown(state, chestCavity, organ, longClamp, longDefault, intClamp, intDefault);
        }
    }
}
