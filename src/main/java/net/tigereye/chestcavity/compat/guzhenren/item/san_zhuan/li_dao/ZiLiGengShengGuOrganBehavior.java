package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.li_dao;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Objects;
import java.util.Optional;

/**
 * Behaviour for 三转自力更生蛊. Handles the 10 second passive heal pulse and the
 * attack ability that converts stored muscles into an extended regeneration buff.
 */
public enum ZiLiGengShengGuOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_li_geng_sheng_gu_3");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final ResourceLocation LI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/li_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_ROOT = "ZiLiGengShengGu";
    private static final String PASSIVE_NEXT_TICK_KEY = "PassiveNextTick";
    private static final String ACTIVE_UNTIL_KEY = "ActiveUntil";
    private static final String ACTIVE_HEAL_RATE_KEY = "ActiveHealRate";
    private static final String ACTIVE_MULTIPLIER_KEY = "ActiveLiMultiplier";

    private static final double PASSIVE_ZHENYUAN_COST = 500.0;
    private static final double PASSIVE_HEAL_BASE = 30.0;
    private static final int PASSIVE_INTERVAL_TICKS = 200;

    private static final double ACTIVE_BASE_RATE = 1.0;
    private static final int ACTIVE_DURATION_TICKS = 30 * 20;

    private static final double MULTIPLIER_EPSILON = 1.0E-4;

    static {
        OrganActivationListeners.register(ABILITY_ID, ZiLiGengShengGuOrganBehavior::activateAbility);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        LinkageChannel channel = ensureChannel(context, LI_DAO_INCREASE_EFFECT);
        if (channel != null) {
            channel.addPolicy(NON_NEGATIVE);
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        Level level = entity.level();
        long gameTime = level.getGameTime();
        OrganState state = organState(organ, STATE_ROOT);
        boolean stateChanged = false;

        stateChanged |= tickPassive(player, cc, state, stackCount, gameTime);
        stateChanged |= tickActive(player, cc, state, gameTime);

        if (stateChanged) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private boolean tickPassive(Player player, ChestCavityInstance cc, OrganState state, int stackCount, long gameTime) {
        long nextTick = state.getLong(PASSIVE_NEXT_TICK_KEY, 0L);
        if (nextTick <= 0L || nextTick <= gameTime) {
            double multiplier = computeLiDaoMultiplier(cc);
            double cost = PASSIVE_ZHENYUAN_COST * stackCount;
            double healAmount = PASSIVE_HEAL_BASE * stackCount * multiplier;
            GuzhenrenResourceCostHelper.ConsumptionResult payment =
                    GuzhenrenResourceCostHelper.consumeStrict(player, cost, 0.0);
            if (payment.succeeded()) {
                if (healAmount > 0.0) {
                    player.heal((float) healAmount);
                }
            }
            long scheduled = gameTime + PASSIVE_INTERVAL_TICKS;
            return state.setLong(PASSIVE_NEXT_TICK_KEY, scheduled).changed();
        }
        return false;
    }

    private boolean tickActive(Player player, ChestCavityInstance cc, OrganState state, long gameTime) {
        long activeUntil = state.getLong(ACTIVE_UNTIL_KEY, 0L);
        if (activeUntil <= 0L) {
            return false;
        }

        double healRate = Math.max(0.0, state.getDouble(ACTIVE_HEAL_RATE_KEY, 0.0));
        double storedMultiplier = state.getDouble(ACTIVE_MULTIPLIER_KEY, 0.0);
        if (gameTime < activeUntil) {
            if (healRate > 0.0) {
                player.heal((float) healRate);
            }
            return false;
        }

        double multiplier = storedMultiplier > 0.0
                ? storedMultiplier
                : computeLiDaoMultiplier(cc);
        double safeMultiplier = Math.max(MULTIPLIER_EPSILON, multiplier);
        double weaknessSeconds = 30.0 / safeMultiplier;
        int weaknessDuration = (int) Math.round(Math.max(1.0, weaknessSeconds) * 20.0);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessDuration, 0, false, true, true));

        boolean changed = false;
        changed |= state.setLong(ACTIVE_UNTIL_KEY, 0L).changed();
        changed |= state.setDouble(ACTIVE_HEAL_RATE_KEY, 0.0).changed();
        changed |= state.setDouble(ACTIVE_MULTIPLIER_KEY, 0.0).changed();
        return changed;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        Level level = entity.level();
        long gameTime = level.getGameTime();
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        long activeUntil = state.getLong(ACTIVE_UNTIL_KEY, 0L);
        if (activeUntil > gameTime) {
            return;
        }

        int musclesConsumed = consumeMuscles(cc, player);
        if (musclesConsumed <= 0) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        double multiplier = computeLiDaoMultiplier(cc);
        double healRate = ACTIVE_BASE_RATE * stackCount * musclesConsumed * multiplier;
        long newActiveUntil = gameTime + ACTIVE_DURATION_TICKS;

        boolean changed = false;
        changed |= state.setLong(ACTIVE_UNTIL_KEY, newActiveUntil).changed();
        changed |= state.setDouble(ACTIVE_HEAL_RATE_KEY, healRate).changed();
        changed |= state.setDouble(ACTIVE_MULTIPLIER_KEY, multiplier).changed();
        if (changed) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int consumeMuscles(ChestCavityInstance cc, Player player) {
        if (cc == null || cc.inventory == null) {
            return 0;
        }
        int consumed = 0;
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!isMuscleId(id)) {
                continue;
            }
            int amount = Math.max(1, stack.getCount());
            cc.inventory.removeItem(i, amount);
            consumed += amount;
            playConsumeSound(player, amount);
        }
        return consumed;
    }

    private static void playConsumeSound(Player player, int times) {
        if (player == null || times <= 0) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < times; i++) {
            float pitch = 0.9f + player.getRandom().nextFloat() * 0.2f;
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, pitch);
        }
    }

    private static boolean isMuscleId(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (!"chestcavity".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return "muscle".equals(path) || path.endsWith("_muscle");
    }

    private static double computeLiDaoMultiplier(ChestCavityInstance cc) {
        if (cc == null) {
            return 1.0;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return 1.0;
        }
        Optional<LinkageChannel> channelOpt = context.lookupChannel(LI_DAO_INCREASE_EFFECT);
        LinkageChannel channel = channelOpt.orElseGet(() -> context.getOrCreateChannel(LI_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE));
        double increase = channel == null ? 0.0 : channel.get();
        if (!Double.isFinite(increase)) {
            increase = 0.0;
        }
        double multiplier = 1.0 + Math.max(0.0, increase);
        return multiplier;
    }
}
