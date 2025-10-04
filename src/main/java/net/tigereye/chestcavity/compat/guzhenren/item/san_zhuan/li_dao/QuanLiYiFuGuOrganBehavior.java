package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.li_dao;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCOrganScores;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 三转全力以赴蛊（力道）被动行为实现。
 */
public enum QuanLiYiFuGuOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_li_yi_fu_gu");
    private static final ResourceLocation LI_DAO_INCREASE_EFFECT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/li_dao_increase_effect");

    private static final String STATE_ROOT = "quan_li_yi_fu";
    private static final String NEXT_TRIGGER_TICK_KEY = "next_trigger_tick";

    private static final int INTERVAL_TICKS = 20 * 15; // 15 seconds
    private static final double BASE_ZHENYUAN_COST = 500.0;
    private static final double BASE_JINGLI_RESTORE = 20.0;
    private static final double MUSCLE_BONUS_PER_STACK = 0.5;
    private static final double MUSCLE_BONUS_CAP_BASE = 15.0;

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }

        long gameTime = entity.level().getGameTime();
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long nextTrigger = state.getLong(NEXT_TRIGGER_TICK_KEY, 0L);
        if (nextTrigger > gameTime) {
            return;
        }

        ConsumptionResult payment = GuzhenrenResourceCostHelper.consumeStrict(player, BASE_ZHENYUAN_COST, 0.0);
        long scheduleNext = gameTime + INTERVAL_TICKS;
        state.setLong(NEXT_TRIGGER_TICK_KEY, scheduleNext);

        if (!payment.succeeded()) {
            return;
        }

        double liDaoIncrease = readLiDaoIncrease(cc);
        double multiplier = 1.0 + Math.max(0.0, liDaoIncrease);
        int muscleStacks = countMuscleStacks(cc, organ);
        double extra = Math.min(muscleStacks * MUSCLE_BONUS_PER_STACK * multiplier, MUSCLE_BONUS_CAP_BASE + multiplier);
        double totalRestore = (BASE_JINGLI_RESTORE * multiplier) + extra;

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> handle.adjustJingli(totalRestore, true));
    }

    private static boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!Objects.equals(id, ORGAN_ID)) {
                continue;
            }
            return stack == organ;
        }
        return false;
    }

    private static int countMuscleStacks(ChestCavityInstance cc, ItemStack self) {
        if (cc == null || cc.inventory == null) {
            return 0;
        }
        int total = 0;
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty() || stack == self) {
                continue;
            }
            if (!isMuscleOrgan(stack)) {
                continue;
            }
            total += Math.max(1, stack.getCount());
        }
        return total;
    }

    private static boolean isMuscleOrgan(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || Objects.equals(id, ORGAN_ID)) {
            return false;
        }
        if (!id.getPath().toLowerCase(Locale.ROOT).contains("muscle")) {
            return false;
        }
        OrganData data = OrganManager.getEntry(stack.getItem());
        if (data == null) {
            return false;
        }
        Float strength = data.organScores.get(CCOrganScores.STRENGTH);
        return strength != null && strength > 0.0f;
    }

    private static double readLiDaoIncrease(ChestCavityInstance cc) {
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return 0.0;
        }
        Optional<LinkageChannel> channel = context.lookupChannel(LI_DAO_INCREASE_EFFECT);
        return channel.map(LinkageChannel::get).orElse(0.0);
    }
}
