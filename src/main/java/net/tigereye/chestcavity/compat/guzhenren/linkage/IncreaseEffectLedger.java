package net.tigereye.chestcavity.compat.guzhenren.linkage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks 增效 (increase effect) contributions per linkage channel and organ stack so we can
 * validate that runtime totals match the values stored inside linkage channels.
 */
public final class IncreaseEffectLedger {

    private static final double EFFECT_TOLERANCE = 1.0E-6;

    private final ActiveLinkageContext context;
    private final Map<ResourceLocation, IdentityHashMap<ItemStack, Entry>> contributions = new HashMap<>();
    private final IdentityHashMap<ItemStack, IncreaseEffectContributor> stackContributors = new IdentityHashMap<>();
    private final Map<ResourceLocation, IncreaseEffectContributor> itemContributors = new HashMap<>();
    private final Set<ResourceLocation> trackedChannels = new LinkedHashSet<>();

    public IncreaseEffectLedger(ActiveLinkageContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public synchronized void registerContributor(
            ItemStack stack,
            IncreaseEffectContributor contributor,
            ResourceLocation... channelIds
    ) {
        if (stack == null || stack.isEmpty() || contributor == null) {
            return;
        }
        stackContributors.put(stack, contributor);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId != null) {
            itemContributors.putIfAbsent(itemId, contributor);
        }
        if (channelIds != null) {
            Collections.addAll(trackedChannels, channelIds);
        }
    }

    public synchronized void unregisterContributor(ItemStack stack) {
        if (stack == null) {
            return;
        }
        stackContributors.remove(stack);
    }

    public synchronized void set(ItemStack stack, ResourceLocation channelId, int stackCount, double effect) {
        if (stack == null || stack.isEmpty() || channelId == null) {
            return;
        }
        trackedChannels.add(channelId);
        IdentityHashMap<ItemStack, Entry> entries = contributions.computeIfAbsent(channelId, unused -> new IdentityHashMap<>());
        entries.put(stack, new Entry(stackCount, effect));
    }

    public synchronized double adjust(ItemStack stack, ResourceLocation channelId, double delta) {
        if (stack == null || stack.isEmpty() || channelId == null) {
            return 0.0;
        }
        trackedChannels.add(channelId);
        IdentityHashMap<ItemStack, Entry> entries = contributions.computeIfAbsent(channelId, unused -> new IdentityHashMap<>());
        Entry previous = entries.get(stack);
        int stackCount = Math.max(1, stack.getCount());
        if (delta == 0.0) {
            return previous == null ? 0.0 : previous.effect();
        }
        double updated = (previous == null ? 0.0 : previous.effect()) + delta;
        entries.put(stack, new Entry(stackCount, updated));
        return updated;
    }

    public synchronized double remove(ItemStack stack, ResourceLocation channelId) {
        IdentityHashMap<ItemStack, Entry> entries = contributions.get(channelId);
        if (entries == null) {
            return 0.0;
        }
        Entry removed = entries.remove(stack);
        if (entries.isEmpty()) {
            contributions.remove(channelId);
        }
        return removed == null ? 0.0 : removed.effect();
    }

    public synchronized double total(ResourceLocation channelId) {
        IdentityHashMap<ItemStack, Entry> entries = contributions.get(channelId);
        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Entry entry : entries.values()) {
            total += entry.effect();
        }
        return total;
    }

    public synchronized Map<ItemStack, Entry> snapshot(ResourceLocation channelId) {
        IdentityHashMap<ItemStack, Entry> entries = contributions.get(channelId);
        if (entries == null || entries.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new IdentityHashMap<>(entries));
    }

    public synchronized Map<ItemStack, CountMismatch> detectCountMismatches(ResourceLocation channelId) {
        IdentityHashMap<ItemStack, Entry> entries = contributions.get(channelId);
        if (entries == null || entries.isEmpty()) {
            return Map.of();
        }
        IdentityHashMap<ItemStack, CountMismatch> mismatches = new IdentityHashMap<>();
        for (Map.Entry<ItemStack, Entry> entry : entries.entrySet()) {
            ItemStack stack = entry.getKey();
            Entry recorded = entry.getValue();
            int actual = stack == null || stack.isEmpty() ? 0 : Math.max(1, stack.getCount());
            if (actual != recorded.stackCount()) {
                mismatches.put(stack, new CountMismatch(recorded.stackCount(), actual));
            }
        }
        if (mismatches.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(mismatches);
    }

    public synchronized void clearChannel(ResourceLocation channelId) {
        if (channelId == null) {
            return;
        }
        trackedChannels.add(channelId);
        contributions.remove(channelId);
        context.lookupChannel(channelId).ifPresent(channel -> channel.set(0.0));
    }

    public synchronized void reset() {
        contributions.clear();
        stackContributors.clear();
        itemContributors.clear();
        trackedChannels.clear();
    }

    public ActiveLinkageContext context() {
        return context;
    }

    public synchronized void verifyAndRebuildIfNeeded() {
        if (trackedChannels.isEmpty()) {
            return;
        }
        ChestCavityInstance cc = context.getChestCavity();
        if (cc == null) {
            return;
        }
        List<String> issues = new ArrayList<>();
        for (ResourceLocation channelId : trackedChannels) {
            double expected = total(channelId);
            double actual = context.lookupChannel(channelId).map(LinkageChannel::get).orElse(0.0);
            Map<ItemStack, CountMismatch> mismatches = detectCountMismatches(channelId);
            if (!mismatches.isEmpty() || Math.abs(expected - actual) > EFFECT_TOLERANCE) {
                issues.add(formatVerificationData(channelId, expected, actual, snapshot(channelId), mismatches));
            }
        }
        if (issues.isEmpty()) {
            if (ChestCavity.LOGGER.isTraceEnabled()) {
                ChestCavity.LOGGER.trace("[Guzhenren] Increase effects verified {}", describeOwner());
            }
            return;
        }
        ChestCavity.LOGGER.warn(
                "[Guzhenren] Detected increase effect mismatch for {} -> {}",
                describeOwner(),
                String.join(" | ", issues)
        );
        rebuildAll();
    }

    public synchronized void rebuildAll() {
        ChestCavityInstance cc = context.getChestCavity();
        contributions.clear();
        if (cc == null) {
            return;
        }
        RebuildCollector collector = new RebuildCollector();
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            IncreaseEffectContributor contributor = resolveContributor(stack);
            if (contributor == null) {
                continue;
            }
            collector.begin(stack);
            contributor.rebuildIncreaseEffects(cc, context, stack, collector);
        }
        collector.commit();
    }

    private IncreaseEffectContributor resolveContributor(ItemStack stack) {
        IncreaseEffectContributor contributor = stackContributors.get(stack);
        if (contributor != null) {
            return contributor;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        return itemContributors.get(itemId);
    }

    private String formatVerificationData(
            ResourceLocation channelId,
            double expected,
            double actual,
            Map<ItemStack, Entry> entries,
            Map<ItemStack, CountMismatch> mismatches
    ) {
        return String.format(
                Locale.ROOT,
                "%s expected=%.3f actual=%.3f ledger=%s mismatches=%s",
                channelId,
                expected,
                actual,
                describeLedgerEntries(entries),
                describeCountMismatches(mismatches)
        );
    }

    private String describeLedgerEntries(Map<ItemStack, Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<ItemStack, Entry> entry : entries.entrySet()) {
            Entry data = entry.getValue();
            parts.add(String.format(
                    Locale.ROOT,
                    "%s#%d=%.3f",
                    describeStack(entry.getKey()),
                    data.stackCount(),
                    data.effect()
            ));
        }
        return parts.toString();
    }

    private String describeCountMismatches(Map<ItemStack, CountMismatch> mismatches) {
        if (mismatches == null || mismatches.isEmpty()) {
            return "[]";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<ItemStack, CountMismatch> entry : mismatches.entrySet()) {
            CountMismatch mismatch = entry.getValue();
            parts.add(String.format(
                    Locale.ROOT,
                    "%s expected=%d actual=%d",
                    describeStack(entry.getKey()),
                    mismatch.expectedCount(),
                    mismatch.actualCount()
            ));
        }
        return parts.toString();
    }

    private String describeOwner() {
        ChestCavityInstance cc = context.getChestCavity();
        if (cc == null || cc.owner == null) {
            return "<unbound>";
        }
        return cc.owner.getScoreboardName();
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "<unknown>" : id.toString();
    }

    private static String formatChannelMap(Map<ResourceLocation, Double> totals) {
        if (totals == null || totals.isEmpty()) {
            return "[]";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Double> entry : totals.entrySet()) {
            parts.add(String.format(Locale.ROOT, "%s=%.3f", entry.getKey(), entry.getValue()));
        }
        return parts.toString();
    }

    public interface Registrar {
        void record(ResourceLocation channelId, int stackCount, double effect);
    }

    public record Entry(int stackCount, double effect) {
    }

    public record CountMismatch(int expectedCount, int actualCount) {
    }

    private final class RebuildCollector implements Registrar {

        private final Map<ResourceLocation, Double> channelTotals = new LinkedHashMap<>();
        private final List<String> stackDetails = new ArrayList<>();
        private ItemStack currentStack;
        private Map<ResourceLocation, Double> currentEffects = new LinkedHashMap<>();
        private int currentCount;
        private int trackedStacks;

        void begin(ItemStack stack) {
            finishCurrent();
            currentStack = stack;
            currentCount = Math.max(1, stack.getCount());
            currentEffects = new LinkedHashMap<>();
        }

        @Override
        public void record(ResourceLocation channelId, int stackCount, double effect) {
            if (channelId == null || currentStack == null) {
                return;
            }
            trackedChannels.add(channelId);
            if (effect == 0.0) {
                IdentityHashMap<ItemStack, Entry> entries = contributions.get(channelId);
                if (entries != null) {
                    entries.remove(currentStack);
                    if (entries.isEmpty()) {
                        contributions.remove(channelId);
                    }
                }
                return;
            }
            IdentityHashMap<ItemStack, Entry> entries = contributions.computeIfAbsent(channelId, unused -> new IdentityHashMap<>());
            entries.put(currentStack, new Entry(stackCount, effect));
            channelTotals.merge(channelId, effect, Double::sum);
            currentEffects.merge(channelId, effect, Double::sum);
        }

        void finishCurrent() {
            if (currentStack == null) {
                return;
            }
            if (!currentEffects.isEmpty()) {
                trackedStacks++;
                stackDetails.add(String.format(
                        Locale.ROOT,
                        "%s#%d -> %s",
                        describeStack(currentStack),
                        currentCount,
                        formatChannelMap(currentEffects)
                ));
            }
            currentStack = null;
            currentEffects = new LinkedHashMap<>();
            currentCount = 0;
        }

        void commit() {
            finishCurrent();
            Map<ResourceLocation, Double> totalsForLog = new LinkedHashMap<>();
            for (ResourceLocation channelId : trackedChannels) {
                double total = channelTotals.getOrDefault(channelId, 0.0);
                context.getOrCreateChannel(channelId).set(total);
                totalsForLog.put(channelId, total);
            }
            if (ChestCavity.LOGGER.isWarnEnabled()) {
                ChestCavity.LOGGER.warn(
                        "[Guzhenren] Rebuilt increase effects for {} stacks={} totals={} details={}",
                        describeOwner(),
                        trackedStacks,
                        formatChannelMap(totalsForLog),
                        stackDetails
                );
            }
        }
    }
}
