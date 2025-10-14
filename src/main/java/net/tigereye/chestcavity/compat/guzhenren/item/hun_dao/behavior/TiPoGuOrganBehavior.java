package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.combat.HunDaoDamageUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.soulbeast.state.SoulBeastStateManager;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Behaviour for 体魄蛊 (Ti Po Gu).
 */
public final class TiPoGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final TiPoGuOrganBehavior INSTANCE = new TiPoGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tipogu");
    private static final ResourceLocation HUN_DAO_INCREASE_EFFECT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_dao_increase_effect");

    private static final double PASSIVE_HUNPO_PER_SECOND = 3.0D;
    private static final double PASSIVE_JINGLI_PER_SECOND = 1.0D;
    private static final double SOUL_BEAST_DAMAGE_PERCENT = 0.03D;
    private static final double SOUL_BEAST_HUNPO_COST_PERCENT = 0.001D;
    private static final double ZI_HUN_INCREASE_BONUS = 0.10D;
    private static final double SHIELD_PERCENT = 0.005D;
    private static final int SHIELD_REFRESH_INTERVAL_TICKS = BehaviorConfigAccess.getInt(TiPoGuOrganBehavior.class, "SHIELD_REFRESH_INTERVAL_TICKS", 200);
    private static final double EPSILON = 1.0E-4D;

    private static final ResourceLocation SHIELD_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/ti_po_gu_shield");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0D, Double.MAX_VALUE);

    private static final String STATE_ROOT_KEY = "HunDaoTiPoGu";
    private static final String KEY_LAST_TICK = "LastTick";
    private static final String KEY_SOUL_BEAST = "SoulBeast";
    private static final String KEY_INCREASE_ACTIVE = "IncreaseActive";
    private static final String KEY_LAST_SHIELD_TICK = "LastShieldTick";
    private static final String KEY_LAST_SHIELD_AMOUNT = "LastShieldAmount";

    private static final ThreadLocal<Boolean> REENTRY_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TiPoGuOrganBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, HUN_DAO_INCREASE_EFFECT);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
        LedgerOps.registerContributor(cc, organ, this, HUN_DAO_INCREASE_EFFECT);
        updateIncreaseContribution(cc, organ, true);
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        updateIncreaseContribution(cc, organ, false);
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context != null) {
            context.increaseEffects().unregisterContributor(organ);
        }
        AbsorptionHelper.clearAbsorptionCapacity(entity, SHIELD_MODIFIER_ID);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        ResourceHandle handle = handleOpt.get();
        int stackCount = Math.max(1, organ.getCount());
        double hunpoGain = PASSIVE_HUNPO_PER_SECOND * stackCount;
        double jingliGain = PASSIVE_JINGLI_PER_SECOND * stackCount;
        if (hunpoGain != 0.0D) {
            ResourceOps.tryAdjustDouble(handle, "hunpo", hunpoGain, true, "zuida_hunpo");
        }
        if (jingliGain != 0.0D) {
            ResourceOps.tryAdjustDouble(handle, "jingli", jingliGain, true, "zuida_jingli");
        }

        OrganState state = organState(organ, STATE_ROOT_KEY);
        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry lastTickEntry = cooldown.entry(KEY_LAST_TICK);
        boolean soulBeast = SoulBeastStateManager.isActive(player);
        long currentTick = entity.level().getGameTime();
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_TICK, updateEntry(lastTickEntry, currentTick));
        boolean wasSoulBeast = state.getBoolean(KEY_SOUL_BEAST, false);
        logStateChange(LOGGER, prefix(), organ, KEY_SOUL_BEAST, OrganStateOps.setBoolean(state, cc, organ, KEY_SOUL_BEAST, soulBeast, false));

        boolean increaseActive = updateIncreaseContribution(cc, organ, !soulBeast);
        logStateChange(LOGGER, prefix(), organ, KEY_INCREASE_ACTIVE, OrganStateOps.setBoolean(state, cc, organ, KEY_INCREASE_ACTIVE, increaseActive, false));

        boolean forceShieldRefresh = !soulBeast && wasSoulBeast;
        maybeRefreshShield(player, cc, handle, organ, state, cooldown, soulBeast, currentTick, forceShieldRefresh);
    }

    @Override
    public float onHit(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (Boolean.TRUE.equals(REENTRY_GUARD.get())) {
            return damage;
        }
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (target == null || !target.isAlive()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return damage;
        }

        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return damage;
        }
        ResourceHandle handle = handleOpt.get();
        double maxHunpo = handle.read("zuida_hunpo").orElse(0.0D);
        if (!(maxHunpo > 0.0D)) {
            return damage;
        }
        double increase = Math.max(0.0D, readHunDaoIncrease(cc));
        double extraDamage = maxHunpo * SOUL_BEAST_DAMAGE_PERCENT * (1.0D + increase);
        if (!(extraDamage > EPSILON)) {
            return damage;
        }
        double hunpoCost = maxHunpo * SOUL_BEAST_HUNPO_COST_PERCENT;
        double currentHunpo = handle.read("hunpo").orElse(0.0D);
        if (currentHunpo + EPSILON < hunpoCost) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "{} blocked extra damage: insufficient hunpo (needed={} current={} increase={})",
                        prefix(),
                        format(hunpoCost),
                        format(currentHunpo),
                        format(increase)
                );
            }
            return damage;
        }
        ResourceOps.tryAdjustDouble(handle, "hunpo", -hunpoCost, true, "zuida_hunpo");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "{} applied soul beast strike: extra={} cost={} increase={} target={}",
                    prefix(),
                    format(extraDamage),
                    format(hunpoCost),
                    format(increase),
                    target.getName().getString()
            );
        }
        REENTRY_GUARD.set(Boolean.TRUE);
        try {
            DamageSource trueSource = player.damageSources().magic();
            HunDaoDamageUtil.markHunDaoAttack(trueSource);
            target.hurt(trueSource, (float) extraDamage);
        } finally {
            REENTRY_GUARD.set(Boolean.FALSE);
        }
        return damage;
    }

    private boolean updateIncreaseContribution(ChestCavityInstance cc, ItemStack organ, boolean requestActive) {
        if (cc == null || organ == null) {
            return false;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return false;
        }
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.verifyAndRebuildIfNeeded();
        double previous = ledger.adjust(organ, HUN_DAO_INCREASE_EFFECT, 0.0D);
        boolean activate = requestActive && !hasOtherActiveTiPoGu(cc, organ);
        double target = activate ? ZI_HUN_INCREASE_BONUS : 0.0D;
        if (target > 0.0D) {
            LedgerOps.set(cc, organ, HUN_DAO_INCREASE_EFFECT, Math.max(1, organ.getCount()), target, NON_NEGATIVE, true);
        } else {
            LedgerOps.remove(cc, organ, HUN_DAO_INCREASE_EFFECT, NON_NEGATIVE, true);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "{} increase contribution updated: active={} request={} previous={}",
                    prefix(),
                    activate,
                    requestActive,
                    format(previous)
            );
        }
        return activate;
    }

    private boolean hasOtherActiveTiPoGu(ChestCavityInstance cc, ItemStack currentOrgan) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (stack == currentOrgan) {
                continue;
            }
            if (!matchesOrgan(stack, ORGAN_ID)) {
                continue;
            }
            OrganState otherState = organState(stack, STATE_ROOT_KEY);
            if (otherState.getBoolean(KEY_INCREASE_ACTIVE, false)) {
                return true;
            }
        }
        return false;
    }

    private void maybeRefreshShield(
            Player player,
            ChestCavityInstance cc,
            ResourceHandle handle,
            ItemStack organ,
            OrganState state,
            MultiCooldown cooldown,
            boolean soulBeast,
            long currentTick,
            boolean forceRefresh
    ) {
        if (soulBeast) {
            MultiCooldown.Entry shieldTickEntry = cooldown.entry(KEY_LAST_SHIELD_TICK);
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, updateEntry(shieldTickEntry, currentTick));
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_AMOUNT, OrganStateOps.setDouble(state, cc, organ, KEY_LAST_SHIELD_AMOUNT, 0.0D, value -> value, 0.0D));
            AbsorptionHelper.clearAbsorptionCapacity(player, SHIELD_MODIFIER_ID);
            return;
        }
        MultiCooldown.Entry shieldTickEntry = cooldown.entry(KEY_LAST_SHIELD_TICK);
        long lastRefresh = shieldTickEntry.getReadyTick();
        if (forceRefresh) {
            lastRefresh = Long.MIN_VALUE;
        }
        if (lastRefresh != Long.MIN_VALUE) {
            long minAllowed = Math.max(0L, currentTick - SHIELD_REFRESH_INTERVAL_TICKS);
            long clampedTick = Math.max(minAllowed, Math.min(lastRefresh, currentTick));
            if (clampedTick != lastRefresh) {
                logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, updateEntry(shieldTickEntry, clampedTick));
                lastRefresh = clampedTick;
            }
        }
        boolean shouldRefresh = lastRefresh == Long.MIN_VALUE || currentTick - lastRefresh >= SHIELD_REFRESH_INTERVAL_TICKS;
        if (!shouldRefresh) {
            return;
        }
        double maxHunpo = handle.read("zuida_hunpo").orElse(0.0D);
        if (!(maxHunpo > 0.0D)) {
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, updateEntry(shieldTickEntry, currentTick));
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_AMOUNT, OrganStateOps.setDouble(state, cc, organ, KEY_LAST_SHIELD_AMOUNT, 0.0D, value -> value, 0.0D));
            AbsorptionHelper.clearAbsorptionCapacity(player, SHIELD_MODIFIER_ID);
            return;
        }
        double increase = Math.max(0.0D, readHunDaoIncrease(cc));
        double shieldValue = maxHunpo * SHIELD_PERCENT * (1.0D + increase);
        if (shieldValue <= EPSILON) {
            AbsorptionHelper.clearAbsorptionCapacity(player, SHIELD_MODIFIER_ID);
            return;
        }
        float updated = AbsorptionHelper.applyAbsorption(player, shieldValue, SHIELD_MODIFIER_ID, true);
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, updateEntry(shieldTickEntry, currentTick));
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_AMOUNT, OrganStateOps.setDouble(state, cc, organ, KEY_LAST_SHIELD_AMOUNT, shieldValue, value -> value, 0.0D));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "{} refreshed shield: player={} shield={} increase={} hunpo_max={} updated_abs={}",
                    prefix(),
                    player.getScoreboardName(),
                    format(shieldValue),
                    format(increase),
                    format(maxHunpo),
                    format(updated)
            );
        }
    }

    private double readHunDaoIncrease(ChestCavityInstance cc) {
        if (cc == null) {
            return 0.0D;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return 0.0D;
        }
        return context.lookupChannel(HUN_DAO_INCREASE_EFFECT)
                .map(channel -> Math.max(0.0D, channel.get()))
                .orElse(0.0D);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String prefix() {
        return "[compat/guzhenren][hun_dao][ti_po_gu]";
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT_KEY))
                .withLongClamp(value -> value, Long.MIN_VALUE);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static OrganState.Change<Long> updateEntry(MultiCooldown.Entry entry, long value) {
        long previous = entry.getReadyTick();
        entry.setReadyAt(value);
        long current = entry.getReadyTick();
        return new OrganState.Change<>(previous, current);
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (cc == null || organ == null || organ.isEmpty() || registrar == null) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT_KEY);
        boolean active = state.getBoolean(KEY_INCREASE_ACTIVE, false);
        double effect = active ? ZI_HUN_INCREASE_BONUS : 0.0D;
        registrar.record(HUN_DAO_INCREASE_EFFECT, Math.max(1, organ.getCount()), effect);
    }

}
