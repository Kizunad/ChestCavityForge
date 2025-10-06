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

    private static final int ATTEMPT_COOLDOWN_TICKS = 20; // once per second
    private static final float HEALTH_THRESHOLD_FRACTION = 0.60f;
    private static final float HEALTH_MISSING_MIN = 4.0f;

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

    private boolean tryUseHealingItem(SoulPlayer player) {
        // 0) Try Guzhenren-specific healing items first (e.g., 灵涎蛊)
        if (tryUseGuzhenrenItem(player)) return true;

        // 1) Prioritize golden apples (instant regen/absorption)
        int slot = findHotbarSlot(player, Items.ENCHANTED_GOLDEN_APPLE);
        if (slot == -1) slot = findHotbarSlot(player, Items.GOLDEN_APPLE);
        if (slot != -1 && tryUseFromHotbar(player, slot)) return true;

        // 2) Healing/Regeneration potions (drinkable)
        int potionSlot = findFirstDrinkableHealingPotion(player);
        if (potionSlot != -1 && tryUseFromHotbar(player, potionSlot)) return true;

        // 3) Generic edible if can eat (to trigger natural regen)
        if (canBenefitFromFood(player)) {
            int foodSlot = findAnyEdibleHotbar(player);
            if (foodSlot != -1 && tryUseFromHotbar(player, foodSlot)) return true;
        }

        // 4) Try offhand if it already holds a good item
        ItemStack off = player.getOffhandItem();
        if (isGoldenApple(off) || isDrinkableHealingPotion(off) || (off.getFoodProperties(player) != null && canBenefitFromFood(player))) {
            return useInHand(player, InteractionHand.OFF_HAND);
        }
        return false;
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

    private static void addIfPresent(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != null && item != Items.AIR) {
            GUZ_HEAL_ITEMS.add(item);
        }
    }

    private boolean tryUseGuzhenrenItem(SoulPlayer player) {
        ensureGuzhenrenItems();
        if (GUZ_HEAL_ITEMS.isEmpty()) return false;
        for (Item item : GUZ_HEAL_ITEMS) {
            if (player.getCooldowns().isOnCooldown(item)) continue;
            Pair<InteractionHand, Integer> choice = chooseHandAndSlot(player, item);
            if (choice == null) continue;
            InteractionHand hand = choice.getFirst();
            Integer slot = choice.getSecond();
            int original = player.getInventory().selected;
            boolean switched = false;
            if (slot != null && slot >= 0 && slot <= 8 && original != slot) {
                player.getInventory().selected = slot;
                switched = true;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || stack.getItem() != item) {
                if (switched) player.getInventory().selected = original;
                continue;
            }
            InteractionResultHolder<ItemStack> result = stack.use(player.level(), player, hand);
            InteractionResult type = result.getResult();
            boolean consumed = type.consumesAction() || type == InteractionResult.SUCCESS;
            if (!consumed) {
                if (switched) player.getInventory().selected = original;
                continue;
            }
            ItemStack after = result.getObject();
            if (after != stack) player.setItemInHand(hand, after);
            if (after.getUseDuration(player) > 0 && !player.isUsingItem()) player.startUsingItem(hand);
            // Cooldown is applied by the item/mod itself; we only respect it via isOnCooldown above
            net.tigereye.chestcavity.soul.util.SoulLog.info("[soul][heal][guz] used item={} (cooldown handled by item)", BuiltInRegistries.ITEM.getKey(item));
            return true;
        }
        return false;
    }

    private static Pair<InteractionHand, Integer> chooseHandAndSlot(Player p, Item item) {
        // Prefer offhand if already holding the item
        if (p.getOffhandItem().getItem() == item) return Pair.of(InteractionHand.OFF_HAND, null);
        if (p.getMainHandItem().getItem() == item) return Pair.of(InteractionHand.MAIN_HAND, p.getInventory().selected);
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == item) return Pair.of(InteractionHand.MAIN_HAND, i);
        }
        return null;
    }

    private static boolean tryUseFromHotbar(Player player, int slot) {
        if (slot < 0 || slot > 8) return false;
        if (player.getInventory().selected != slot) {
            player.getInventory().selected = slot;
        }
        return useInHand(player, InteractionHand.MAIN_HAND);
    }

    private static boolean useInHand(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return false;
        InteractionResultHolder<ItemStack> result = stack.use(player.level(), player, hand);
        InteractionResult type = result.getResult();
        boolean consumed = type.consumesAction() || type == InteractionResult.SUCCESS;
        if (consumed) {
            SoulLog.info("[soul][heal] use item hand={} item={} x{}", hand, stack.getItem().toString(), stack.getCount());
            // If the use returns a different stack, update hand (mirrors vanilla client flow)
            ItemStack after = result.getObject();
            if (after != stack) {
                player.setItemInHand(hand, after);
            }
            // For items that require holding (e.g., potions/food), startUsingItem keeps ticking until finished
            if (player.getItemInHand(hand) == after && after.getUseDuration(player) > 0 && !player.isUsingItem()) {
                player.startUsingItem(hand);
            }
        }
        return consumed;
    }

    private static boolean canBenefitFromFood(Player player) {
        FoodData food = player.getFoodData();
        boolean canEat = player.canEat(false);
        boolean lowHp = player.getHealth() < player.getMaxHealth();
        return canEat || (lowHp && player.hasEffect(MobEffects.REGENERATION));
    }

    private static boolean isGoldenApple(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private static int findHotbarSlot(Player player, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == item) return i;
        }
        return -1;
    }

    private static int findAnyEdibleHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getFoodProperties(player) != null) return i;
        }
        return -1;
    }

    private static int findFirstDrinkableHealingPotion(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && isDrinkableHealingPotion(s)) return i;
        }
        return -1;
    }

    private static boolean isDrinkableHealingPotion(ItemStack stack) {
        if (stack.getItem() != Items.POTION) return false;
        PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        var potion = contents.potion().orElse(null);
        if (potion == null) return false;
        if (potion.equals(Potions.HEALING) || potion.equals(Potions.STRONG_HEALING) ||
            potion.equals(Potions.REGENERATION) || potion.equals(Potions.STRONG_REGENERATION)) {
            return true;
        }
        // Also accept custom potions that grant REGEN or INSTANT_HEALTH effects
        for (MobEffectInstance eff : contents.customEffects()) {
            if (eff.getEffect() == MobEffects.REGENERATION || eff.getEffect() == MobEffects.HEAL) {
                return true;
            }
        }
        return false;
    }
}
