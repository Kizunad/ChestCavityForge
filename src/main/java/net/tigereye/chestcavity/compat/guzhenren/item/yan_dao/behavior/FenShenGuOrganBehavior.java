package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.OrganPresenceUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Behaviour for 焚身蛊（炎道·肾脏）。
 */
public final class FenShenGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final FenShenGuOrganBehavior INSTANCE = new FenShenGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "[compat/guzhenren][yan_dao][fen_shen_gu]";

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fen_shen_gu");
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final ResourceLocation HUORENGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huorengu");
    private static final ResourceLocation YAN_DAO_INCREASE_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yan_dao_increase_effect");
    private static final ResourceLocation FLAME_AURA_FX = ResourceLocation.parse("chestcavity:fire_huo_yi");

    private static final String STATE_ROOT = "FenShenGu";
    private static final String STACKS_KEY = "LinghuoStacks";
    private static final String SYNERGY_KEY = "SynergyActive";
    private static final String INCREASE_KEY = "YanDaoIncrease";

    private static final double JINGLI_RESTORE_PER_SECOND = 3.0;
    private static final double DETOXIFICATION_CHANCE = 0.20;
    private static final float DAMAGE_REDUCTION_PER_STACK = BehaviorConfigAccess.getFloat(FenShenGuOrganBehavior.class, "DAMAGE_REDUCTION_PER_STACK", 12.0f);
    private static final int MAX_STACKS = BehaviorConfigAccess.getInt(FenShenGuOrganBehavior.class, "MAX_STACKS", 2);
    private static final int PERMANENT_FIRE_TICKS = BehaviorConfigAccess.getInt(FenShenGuOrganBehavior.class, "PERMANENT_FIRE_TICKS", 60); // 3 seconds of burn upkeep
    private static final int FIRE_RESIST_DURATION_TICKS = BehaviorConfigAccess.getInt(FenShenGuOrganBehavior.class, "FIRE_RESIST_DURATION_TICKS", 220); // 11 seconds, refreshed every slow tick
    private static final double SYNERGY_INCREASE = 0.2;
    private static final double EPSILON = 1.0E-6;
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

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
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        boolean synergyActive = hasFlameCoreSynergy(cc);
        boolean previousSynergy = state.getBoolean(SYNERGY_KEY, false);
        if (synergyActive != previousSynergy) {
            var change = collector.record(state.setBoolean(SYNERGY_KEY, synergyActive, false));
            logStateChange(LOGGER, LOG_PREFIX, organ, SYNERGY_KEY, change);
        }

        if (synergyActive) {
            collector.record(applySynergyIncrease(cc, organ, state, SYNERGY_INCREASE));
            maintainPermanentFlames(entity);
            grantFireResistance(entity);
            if (entity.level() instanceof ServerLevel serverLevel) {
                playFlameAura(serverLevel, entity);
            }
        } else {
            collector.record(applySynergyIncrease(cc, organ, state, 0.0));
        }

        if (entity.isOnFire() || synergyActive) {
            collector.record(applyBurningBenefits(entity, organ, state));
        } else {
            collector.record(resetStacks(state, organ));
        }

        collector.commit();
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

        // 近战
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
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

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        double stored = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(stored) > EPSILON) {
            applyIncreaseDelta(cc, organ, -stored);
            var change = state.setDouble(INCREASE_KEY, 0.0, value -> Math.max(0.0, value), 0.0);
            logStateChange(LOGGER, LOG_PREFIX, organ, INCREASE_KEY, change);
        }
        LedgerOps.remove(cc, organ, YAN_DAO_INCREASE_CHANNEL, NON_NEGATIVE, true);
    }

    @Override
    public void rebuildIncreaseEffects(ChestCavityInstance cc, ActiveLinkageContext context,
                                       ItemStack organ, IncreaseEffectLedger.Registrar registrar) {
        if (organ == null || organ.isEmpty() || registrar == null) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        double effect = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(effect) <= EPSILON) {
            return;
        }
        registrar.record(YAN_DAO_INCREASE_CHANNEL, Math.max(1, organ.getCount()), effect);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        LedgerOps.ensureChannel(cc, YAN_DAO_INCREASE_CHANNEL, NON_NEGATIVE);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        LedgerOps.registerContributor(cc, organ, this, YAN_DAO_INCREASE_CHANNEL);
    }

    private boolean applySynergyIncrease(ChestCavityInstance cc, ItemStack organ, OrganState state, double target) {
        double stored = state.getDouble(INCREASE_KEY, 0.0);
        if (Math.abs(stored - target) <= EPSILON) {
            return false;
        }
        applyIncreaseDelta(cc, organ, target - stored);
        var change = state.setDouble(INCREASE_KEY, target, value -> Math.max(0.0, value), 0.0);
        logStateChange(LOGGER, LOG_PREFIX, organ, INCREASE_KEY, change);
        return change.changed();
    }

    private void applyIncreaseDelta(ChestCavityInstance cc, ItemStack organ, double delta) {
        if (cc == null || Math.abs(delta) <= EPSILON) {
            return;
        }
        LedgerOps.adjust(cc, organ, YAN_DAO_INCREASE_CHANNEL, delta, NON_NEGATIVE, true);
    }

    private boolean applyBurningBenefits(LivingEntity entity, ItemStack organ, OrganState state) {
        boolean dirty = false;
        if (entity instanceof Player player) {
            OptionalDouble result = ResourceOps.tryAdjustJingli(player, JINGLI_RESTORE_PER_SECOND, true);
            if (result.isEmpty()) {
                LOGGER.debug("{} Failed to restore jingli for {}", LOG_PREFIX, player.getScoreboardName());
            }
        } else {
            ResourceOps.tryAdjustJingli(entity, JINGLI_RESTORE_PER_SECOND, true);
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

    private void playFlameAura(ServerLevel level, LivingEntity entity) {
        if (level == null || entity == null || FLAME_AURA_FX == null) {
            return;
        }
        Vec3 origin = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 look = entity.getLookAngle();
        Vec3 fallback = look.lengthSqr() > 1.0E-4D ? look : new Vec3(0.0D, 1.0D, 0.0D);
        ServerPlayer performer = entity instanceof ServerPlayer player ? player : null;
        AbilityFxDispatcher.play(level, FLAME_AURA_FX, origin, fallback, look, performer, entity, 1.0F);
    }

    // 检测存在火心蛊和火人蛊
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
