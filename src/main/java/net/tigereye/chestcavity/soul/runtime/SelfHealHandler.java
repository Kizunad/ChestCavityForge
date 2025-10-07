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
        // Do not interfere with normal attacks: skip when eating/drinking or during attack cooldown
        if (player.isUsingItem()) return;
        if (player.getAttackStrengthScale(0.0f) < 1.0f) return;

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
        return tryUseGuzhenrenItem(player);
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

            // 🩸 关键补丁：FakePlayer 无法自动吃/喝 → 直接执行 finishUsingItem()
            if (player instanceof SoulPlayer) {
                ItemStack resultStack = after.finishUsingItem(player.level(), player);
                player.setItemInHand(hand, resultStack);
                SoulLog.info("[soul][heal][guz] force-finish use for fake player, item={}", BuiltInRegistries.ITEM.getKey(item));
            } else {
                // 真实玩家（本体）仍使用正常流程
                if (after.getUseDuration(player) > 0 && !player.isUsingItem()) {
                    player.startUsingItem(hand);
                }
            }

            // 冷却由物品本身处理，这里仅日志输出
            SoulLog.info("[soul][heal][guz] used item={} (cooldown handled by item)", BuiltInRegistries.ITEM.getKey(item));

            if (switched) player.getInventory().selected = original;
            return true;
        }

        return false;
    }


    private static Pair<InteractionHand, Integer> chooseHandAndSlot(Player p, Item item) {
        // Restrict healing to offhand only to avoid interfering with main-hand attacks
        return (p.getOffhandItem().getItem() == item)
                ? Pair.of(InteractionHand.OFF_HAND, null)
                : null;
    }


}
