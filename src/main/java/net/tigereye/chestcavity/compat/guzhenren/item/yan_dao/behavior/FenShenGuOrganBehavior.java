package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.OrganPresenceUtil;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.slf4j.Logger;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Behaviour for 焚身蛊（炎道·肾脏）。
 */
public final class FenShenGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener {

    public static final FenShenGuOrganBehavior INSTANCE = new FenShenGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "[compat/guzhenren][yan_dao][fen_shen_gu]";

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fen_shen_gu");
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation HUORENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huorengu");

    private static final String STATE_ROOT = "FenShenGu";
    private static final String STACKS_KEY = "LinghuoStacks";
    private static final String SYNERGY_KEY = "SynergyActive";

    private static final double JINGLI_RESTORE_PER_SECOND = 3.0;
    private static final double DETOXIFICATION_CHANCE = 0.20;
    private static final float DAMAGE_REDUCTION_PER_STACK = 12.0f;
    private static final int MAX_STACKS = 2;
    private static final int PERMANENT_FIRE_TICKS = 60; // 3 seconds of burn upkeep
    private static final int FIRE_RESIST_DURATION_TICKS = 220; // 11 seconds, refreshed every slow tick

    private FenShenGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }

        OrganState state = organState(organ, STATE_ROOT);
        boolean dirty = false;

        boolean synergyActive = hasFlameCoreSynergy(cc);
        boolean previousSynergy = state.getBoolean(SYNERGY_KEY, false);
        if (synergyActive != previousSynergy) {
            var change = state.setBoolean(SYNERGY_KEY, synergyActive, false);
            logStateChange(LOGGER, LOG_PREFIX, organ, SYNERGY_KEY, change);
            dirty |= change.changed();
        }

        if (synergyActive) {
            maintainPermanentFlames(entity);
            grantFireResistance(entity);
        }

        if (entity.isOnFire() || synergyActive) {
            dirty |= applyBurningBenefits(entity, organ, state);
        } else {
            dirty |= resetStacks(state, organ);
        }

        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
    }

    @Override
    public float onIncomingDamage(
            DamageSource source,
            LivingEntity victim,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (damage <= 0.0f || victim == null || cc == null) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return damage;
        }

        if (!source.is(DamageTypeTags.IS_FIRE)) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        int stacks = Math.max(0, Math.min(MAX_STACKS, state.getInt(STACKS_KEY, 0)));
        if (stacks <= 0) {
            return damage;
        }
        float reduction = DAMAGE_REDUCTION_PER_STACK * stacks;
        if (reduction <= 0.0f) {
            return damage;
        }
        return Math.max(0.0f, damage - reduction);
    }

    private boolean applyBurningBenefits(LivingEntity entity, ItemStack organ, OrganState state) {
        boolean dirty = false;
        if (entity instanceof Player player) {
            Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
            if (handleOpt.isPresent()) {
                OptionalDouble result = handleOpt.get().adjustJingli(JINGLI_RESTORE_PER_SECOND, true);
                if (result.isEmpty()) {
                    LOGGER.debug("{} Failed to restore jingli for {}", LOG_PREFIX, player.getScoreboardName());
                }
            }
        } else {
            GuzhenrenResourceBridge.open(entity).ifPresent(handle -> handle.adjustJingli(JINGLI_RESTORE_PER_SECOND, true));
        }

        if (entity.hasEffect(MobEffects.POISON) && entity.getRandom().nextDouble() < DETOXIFICATION_CHANCE) {
            entity.removeEffect(MobEffects.POISON);
        }

        int currentStacks = Math.max(0, state.getInt(STACKS_KEY, 0));
        int updatedStacks = Math.min(MAX_STACKS, currentStacks + 1);
        var change = state.setInt(STACKS_KEY, updatedStacks, value -> Math.max(0, Math.min(value, MAX_STACKS)), 0);
        logStateChange(LOGGER, LOG_PREFIX, organ, STACKS_KEY, change);
        dirty |= change.changed();

        return dirty;
    }

    private boolean resetStacks(OrganState state, ItemStack organ) {
        int stored = Math.max(0, state.getInt(STACKS_KEY, 0));
        if (stored <= 0) {
            return false;
        }
        var change = state.setInt(STACKS_KEY, 0, value -> Math.max(0, Math.min(value, MAX_STACKS)), 0);
        logStateChange(LOGGER, LOG_PREFIX, organ, STACKS_KEY, change);
        return change.changed();
    }

    private void maintainPermanentFlames(LivingEntity entity) {
        int ticks = Math.max(entity.getRemainingFireTicks(), PERMANENT_FIRE_TICKS);
        entity.setRemainingFireTicks(ticks);
    }

    private void grantFireResistance(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESIST_DURATION_TICKS, 0, false, false, true));
    }

    private boolean hasFlameCoreSynergy(ChestCavityInstance cc) {
        return OrganPresenceUtil.has(cc, HUOXINGU_ID) && OrganPresenceUtil.has(cc, HUORENGU_ID);
    }

    private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack == null || slotStack.isEmpty()) {
                continue;
            }
            if (slotStack == organ) {
                return true;
            }
            if (!matchesOrgan(slotStack, ORGAN_ID)) {
                continue;
            }
            if (slotStack.getItem() == organ.getItem()) {
                return false;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(slotStack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return false;
            }
        }
        return false;
    }
}
