package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;

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
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.soulbeast.state.SoulBeastStateManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Behaviour for 体魄蛊 (Ti Po Gu).
 */
public final class TiPoGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {

    public static final TiPoGuOrganBehavior INSTANCE = new TiPoGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tipogu");
    private static final ResourceLocation HUN_DAO_INCREASE_EFFECT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_dao_increase_effect");

    private static final double PASSIVE_HUNPO_PER_SECOND = 3.0D;
    private static final double PASSIVE_JINGLI_PER_SECOND = 1.0D;
    private static final double SOUL_BEAST_DAMAGE_PERCENT = 0.01D;
    private static final double SOUL_BEAST_HUNPO_COST_PERCENT = 0.001D;
    private static final double ZI_HUN_INCREASE_BONUS = 0.10D;
    private static final double SHIELD_PERCENT = 0.005D;
    private static final int SHIELD_REFRESH_INTERVAL_TICKS = 200;
    private static final double EPSILON = 1.0E-4D;

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
        updateIncreaseContribution(cc, organ, false);
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        updateIncreaseContribution(cc, organ, false);
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
            handle.adjustDouble("hunpo", hunpoGain, true, "zuida_hunpo");
        }
        if (jingliGain != 0.0D) {
            handle.adjustDouble("jingli", jingliGain, true, "zuida_jingli");
        }

        boolean soulBeast = SoulBeastStateManager.isActive(player);
        OrganState state = organState(organ, STATE_ROOT_KEY);
        long currentTick = entity.level().getGameTime();
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_TICK, state.setLong(KEY_LAST_TICK, currentTick));
        boolean wasSoulBeast = state.getBoolean(KEY_SOUL_BEAST, false);
        logStateChange(LOGGER, prefix(), organ, KEY_SOUL_BEAST, state.setBoolean(KEY_SOUL_BEAST, soulBeast));

        boolean increaseActive = updateIncreaseContribution(cc, organ, !soulBeast);
        logStateChange(LOGGER, prefix(), organ, KEY_INCREASE_ACTIVE, state.setBoolean(KEY_INCREASE_ACTIVE, increaseActive));

        boolean forceShieldRefresh = !soulBeast && wasSoulBeast;
        maybeRefreshShield(player, cc, handle, organ, state, soulBeast, currentTick, forceShieldRefresh);
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
        if (!SoulBeastStateManager.isActive(player)) {
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
        handle.adjustDouble("hunpo", -hunpoCost, true, "zuida_hunpo");
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
        if (cc == null || organ == null || organ.isEmpty()) {
            return false;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return false;
        }
        LinkageChannel channel = context.getOrCreateChannel(HUN_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.verifyAndRebuildIfNeeded();
        double previous = ledger.adjust(organ, HUN_DAO_INCREASE_EFFECT, 0.0D);
        boolean activate = requestActive && !hasOtherActiveTiPoGu(cc, organ);
        double target = activate ? ZI_HUN_INCREASE_BONUS : 0.0D;
        double delta = target - previous;
        if (Math.abs(delta) > EPSILON) {
            channel.adjust(delta);
        }
        if (target > 0.0D) {
            ledger.set(organ, HUN_DAO_INCREASE_EFFECT, Math.max(1, organ.getCount()), target);
        } else {
            ledger.remove(organ, HUN_DAO_INCREASE_EFFECT);
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
            boolean soulBeast,
            long currentTick,
            boolean forceRefresh
    ) {
        if (soulBeast) {
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, state.setLong(KEY_LAST_SHIELD_TICK, currentTick));
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_AMOUNT, state.setDouble(KEY_LAST_SHIELD_AMOUNT, 0.0D));
            return;
        }
        long lastRefresh = state.getLong(KEY_LAST_SHIELD_TICK, Long.MIN_VALUE);
        if (forceRefresh) {
            lastRefresh = Long.MIN_VALUE;
        }
        boolean shouldRefresh = lastRefresh == Long.MIN_VALUE || currentTick - lastRefresh >= SHIELD_REFRESH_INTERVAL_TICKS;
        if (!shouldRefresh) {
            return;
        }
        double maxHunpo = handle.read("zuida_hunpo").orElse(0.0D);
        if (!(maxHunpo > 0.0D)) {
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, state.setLong(KEY_LAST_SHIELD_TICK, currentTick));
            logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_AMOUNT, state.setDouble(KEY_LAST_SHIELD_AMOUNT, 0.0D));
            return;
        }
        double increase = Math.max(0.0D, readHunDaoIncrease(cc));
        double shieldValue = maxHunpo * SHIELD_PERCENT * (1.0D + increase);
        float desired = (float) Math.max(0.0D, shieldValue);
        float currentAbsorption = player.getAbsorptionAmount();
        float updated = Math.max(currentAbsorption, desired);
        if (updated - currentAbsorption > EPSILON) {
            player.setAbsorptionAmount(updated);
        }
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_TICK, state.setLong(KEY_LAST_SHIELD_TICK, currentTick));
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_SHIELD_AMOUNT, state.setDouble(KEY_LAST_SHIELD_AMOUNT, shieldValue));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "{} refreshed shield: player={} shield={} increase={} hunpo_max={}",
                    prefix(),
                    player.getScoreboardName(),
                    format(shieldValue),
                    format(increase),
                    format(maxHunpo)
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

}
