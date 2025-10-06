package net.tigereye.chestcavity.soul.combat.handlers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tigereye.chestcavity.soul.combat.AttackContext;
import net.tigereye.chestcavity.soul.combat.SoulAttackHandler;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attempts to right-click use a Guzhenren buff/consumable item just before attacking.
 * Does not consume the attack; abilities/melee still run after this.
 */
public final class GuzhenrenBuffItemAttackHandler implements SoulAttackHandler {

    private static final double RANGE = 6.5; // modest pre-attack window
    private static final int ATTEMPT_COOLDOWN_TICKS = 40; // 2s per soul to avoid spam

    private static final Set<Item> BUFF_ITEMS = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final java.util.concurrent.atomic.AtomicBoolean LOADED = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();

    // Buff items list (right-click, beneficial), based on provided mappings
    private static final ResourceLocation MAN_LI_TIAN_NIU_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "man_li_tian_niu_gu");
    private static final ResourceLocation LONG_WAN_QU_QU_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "long_wan_qu_qu_gu");
    private static final ResourceLocation HUANG_LUO_TIAN_NIU_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "huang_luo_tian_niu_gu");
    private static final ResourceLocation XIONG_HAO_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "xiong_hao_gu");
    private static final ResourceLocation QING_FENG_LUN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "qing_feng_lun_gu");
    private static final ResourceLocation XUAN_FENG_LUN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "xuan_feng_lun_gu");
    private static final ResourceLocation YIN_YUN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yun_gu");
    private static final ResourceLocation XUE_ZHAN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "xuezhangu");
    private static final ResourceLocation BI_FENG_LUN_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "bifenglungu");
    private static final ResourceLocation ZHUI_FENG_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "zhui_feng_gu");
    private static final ResourceLocation FENG_BA_WANG_GU = ResourceLocation.fromNamespaceAndPath("guzhenren", "feng_ba_wang_gu");

    private static void ensureLoaded() {
        if (!LOADED.compareAndSet(false, true)) return;
        addIfPresent(MAN_LI_TIAN_NIU_GU);
        addIfPresent(LONG_WAN_QU_QU_GU);
        addIfPresent(HUANG_LUO_TIAN_NIU_GU);
        addIfPresent(XIONG_HAO_GU);
        addIfPresent(QING_FENG_LUN_GU);
        addIfPresent(XUAN_FENG_LUN_GU);
        addIfPresent(YIN_YUN_GU);
        addIfPresent(XUE_ZHAN_GU);
        addIfPresent(BI_FENG_LUN_GU);
        addIfPresent(ZHUI_FENG_GU);
        addIfPresent(FENG_BA_WANG_GU);
    }

    private static void addIfPresent(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != null && item != Items.AIR) BUFF_ITEMS.add(item);
    }

    @Override
    public double getRange(SoulPlayer self, LivingEntity target) {
        return RANGE + (target.getBbWidth() * 0.25);
    }

    @Override
    public boolean tryAttack(AttackContext ctx) {
        SoulPlayer self = ctx.self();
        if (self.level().isClientSide()) return false;
        if (!self.isAlive()) return false;
        ensureLoaded();
        if (BUFF_ITEMS.isEmpty()) return false;

        long now = self.level().getGameTime();
        Long last = LAST_ATTEMPT.get(self.getSoulId());
        if (last != null && now - last < ATTEMPT_COOLDOWN_TICKS) return false;

        boolean used = tryUseAnyBuff(self);
        if (used) {
            LAST_ATTEMPT.put(self.getSoulId(), now);
        }
        // do not consume; allow abilities/melee to follow
        return false;
    }

    private boolean tryUseAnyBuff(SoulPlayer player) {
        for (Item item : BUFF_ITEMS) {
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
            SoulLog.info("[soul][attack][guz] buff-use item={}", BuiltInRegistries.ITEM.getKey(item));
            return true;
        }
        return false;
    }

    private static Pair<InteractionHand, Integer> chooseHandAndSlot(Player p, Item item) {
        if (p.getOffhandItem().getItem() == item) return Pair.of(InteractionHand.OFF_HAND, null);
        if (p.getMainHandItem().getItem() == item) return Pair.of(InteractionHand.MAIN_HAND, p.getInventory().selected);
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == item) return Pair.of(InteractionHand.MAIN_HAND, i);
        }
        return null;
    }
}
