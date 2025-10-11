package net.tigereye.chestcavity.soul.runtime;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.component.DataComponents;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attempts to self-heal by right-click using suitable items from inventory (food/potions/apples).
 * Runs on tick; avoids spamming via a short cooldown per soul.
 */
public final class SelfHealHandler implements SoulRuntimeHandler {

    private static final int ATTEMPT_COOLDOWN_TICKS = getIntProp("chestcavity.soul.selfHealCooldown", 20, 0, 200); // once per second
    private static final float HEALTH_THRESHOLD_FRACTION = getFloatProp("chestcavity.soul.selfHealHealthFrac", 0.60f, 0.0f, 1.0f);
    private static final float HEALTH_MISSING_MIN = getFloatProp("chestcavity.soul.selfHealMinMissing", 4.0f, 0.0f, 1000.0f);

    private static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();

    // Guzhenren-specific quick-use healing items (right-click use). Cooldown is handled by the item itself.
    private static final java.util.Set<Item> GUZ_HEAL_ITEMS = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final java.util.concurrent.atomic.AtomicBoolean GUZ_LOADED = new java.util.concurrent.atomic.AtomicBoolean(false);
    // Example list from en_us.json keys (item.guzhenren.*)
    private static final ResourceLocation LING_XIAN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "ling_xian_gu");
    private static final ResourceLocation SHENG_JI_XIE = ResourceLocation.fromNamespaceAndPath("guzhenren", "sheng_ji_xie");
    private static final ResourceLocation ZOU_XUE_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "zou_xue_gu");
    private static final ResourceLocation ROU_BAI_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "rou_bai_gu");
    private static final ResourceLocation CHUN_YU_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "chun_yu_gu");
    private static final ResourceLocation JIUXINGGU = ResourceLocation.fromNamespaceAndPath("guzhenren", "jiuxinggu");
    private static final ResourceLocation LIAO_GUANG_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "liao_guang_gu");
    private static final ResourceLocation LVYAO_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "lvyaogu");
    private static final ResourceLocation JINFENG_SONG_SHUANG_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfengsongshuanggu");
    private static final ResourceLocation CHUN_GUANG_WU_XIAN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "chun_guang_wu_xian_gu");

    @Override
    public void onTickEnd(SoulPlayer player) {
        if (!player.isAlive() || player.level().isClientSide()) return;
        // Do not interfere with normal attacks: skip when eating/drinking or during attack cooldown
        if (player.isUsingItem()) return;

        float hp = player.getHealth();
        float max = player.getMaxHealth();
        if (max <= 0) return;
        if (hp / max > HEALTH_THRESHOLD_FRACTION && (max - hp) < HEALTH_MISSING_MIN) return;

        long now = player.level().getGameTime();
        Long last = LAST_ATTEMPT.get(player.getSoulId());
        if (last != null && now - last < ATTEMPT_COOLDOWN_TICKS) return;

        if (tryUseHealingItem(player)) {
            LAST_ATTEMPT.put(player.getSoulId(), now);
        }
    }

    private boolean tryUseHealingItem(SoulPlayer player) { return tryUseAnyHealingItem(player); }

    /**
     * Exposed for Actions: attempt a single healing-item use.
     * Keeps the same Guzhenren item set and offhand-first policy.
     */
    public static boolean tryUseAnyHealingItem(SoulPlayer player) {
        float before = player.getHealth();
        boolean used = false;
        // 1) Guzhenren quick-use items
        used = tryUseGuzhenrenItemStatic(player);
        // 2) Enchanted Golden Apple
        if (!used) used = useSimpleItem(player, Items.ENCHANTED_GOLDEN_APPLE);
        // 3) Golden Apple
        if (!used) used = useSimpleItem(player, Items.GOLDEN_APPLE);
        // 4) Potions: Instant Health (any strength)
        if (!used) used = usePotionMatching(player, p -> p == Potions.HEALING || p == Potions.STRONG_HEALING);
        // 5) Potions: Regeneration (any strength)
        if (!used) used = usePotionMatching(player, p -> p == Potions.REGENERATION || p == Potions.STRONG_REGENERATION || p == Potions.LONG_REGENERATION);

        float after = player.getHealth();
        try {
            SoulLog.info("[soul][heal][attempt] soul={} used={} hp:{}->{}", player.getSoulId(), used,
                    String.format("%.3f", before), String.format("%.3f", after));
        } catch (Throwable ignored) {}
        return used;
    }

    private static boolean useSimpleItem(SoulPlayer player, Item item) {
        return net.tigereye.chestcavity.soul.util.SoulPlayerInput.useOffhandIfReady(player, item, true)
                || net.tigereye.chestcavity.soul.util.SoulPlayerInput.useWithOffhandSwapIfReady(player, item, true)
                || net.tigereye.chestcavity.soul.util.SoulPlayerInput.useMainhandIfReady(player, item, true)
                || net.tigereye.chestcavity.soul.util.SoulPlayerInput.useWithMainhandSwapIfReady(player, item, true);
    }

    private static boolean usePotionMatching(SoulPlayer player, java.util.function.Predicate<net.minecraft.world.item.alchemy.Potion> test) {
        java.util.function.Predicate<ItemStack> matcher = (stack) -> {
            if (stack.getItem() != Items.POTION) return false;
            PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
            if (pc == null) return false;
            var opt = pc.potion();
            if (opt == null || opt.isEmpty()) return false;
            net.minecraft.world.item.alchemy.Potion pot = opt.get().value();
            return test.test(pot);
        };
        return net.tigereye.chestcavity.soul.util.SoulPlayerInput.useAnyMatchingWithOffhandFirst(player, matcher, true);
    }

    private static void ensureGuzhenrenItems() {
        if (!GUZ_LOADED.compareAndSet(false, true)) return;
        try {
            addIfPresent(LING_XIAN_GU);
            addIfPresent(SHENG_JI_XIE);
            addIfPresent(ZOU_XUE_GU);
            addIfPresent(ROU_BAI_GU);
            addIfPresent(CHUN_YU_GU);
            addIfPresent(JIUXINGGU);
            addIfPresent(LIAO_GUANG_GU);
            addIfPresent(LVYAO_GU);
            addIfPresent(JINFENG_SONG_SHUANG_GU);
            addIfPresent(CHUN_GUANG_WU_XIAN_GU);
        } catch (Throwable ignored) {
            // Optional compat; silently skip if registry or item is missing
        }
    }

    private static boolean tryUseGuzhenrenItemStatic(SoulPlayer player) {
        ensureGuzhenrenItems();
        if (GUZ_HEAL_ITEMS.isEmpty()) return false;
        for (Item item : GUZ_HEAL_ITEMS) {
            if (player.getCooldowns().isOnCooldown(item)) continue;
            // Prefer offhand, then fallback to mainhand (some mods only handle mainhand use)
            if (net.tigereye.chestcavity.soul.util.SoulPlayerInput.useOffhandIfReady(player, item, true)
                    || net.tigereye.chestcavity.soul.util.SoulPlayerInput.useWithOffhandSwapIfReady(player, item, true)
                    || net.tigereye.chestcavity.soul.util.SoulPlayerInput.useMainhandIfReady(player, item, true)
                    || net.tigereye.chestcavity.soul.util.SoulPlayerInput.useWithMainhandSwapIfReady(player, item, true)) {
                SoulLog.info("[soul][heal][guz] used item={} hand=auto (cooldown by item)", BuiltInRegistries.ITEM.getKey(item));
                return true;
            }
        }
        return false;
    }

    private static void addIfPresent(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != null && item != Items.AIR) {
            GUZ_HEAL_ITEMS.add(item);
        }
    }


    private static Pair<InteractionHand, Integer> chooseHandAndSlot(Player p, Item item) {
        // Restrict healing to offhand only to avoid interfering with main-hand attacks
        return (p.getOffhandItem().getItem() == item)
                ? Pair.of(InteractionHand.OFF_HAND, null)
                : null;
    }

    private static int getIntProp(String key, int def, int lo, int hi) {
        try {
            String v = System.getProperty(key);
            if (v == null) return def;
            int x = Integer.parseInt(v);
            if (x < lo) return lo;
            if (x > hi) return hi;
            return x;
        } catch (Throwable ignored) { return def; }
    }

    private static float getFloatProp(String key, float def, float lo, float hi) {
        try {
            String v = System.getProperty(key);
            if (v == null) return def;
            float x = Float.parseFloat(v);
            if (x < lo) return lo;
            if (x > hi) return hi;
            return x;
        } catch (Throwable ignored) { return def; }
    }
}
