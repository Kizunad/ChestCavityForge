package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.List;
import java.util.Locale;

/**
 * Base behaviour for metallic bone platings that contribute to linkage channels and grant periodic absorption.
 */
abstract class AbstractMetalBoneSupportBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {

    private static final String ABSORPTION_KEY = "LastAbsorptionTick";

    private static final ResourceLocation GU_DAO_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/gu_dao_increase_effect");
    private static final ResourceLocation JIN_DAO_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/jin_dao_increase_effect");
    private static final ResourceLocation BONE_GROWTH_CHANNEL =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/bone_growth");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);
    private static final boolean DEBUG_ABSORPTION = Boolean.getBoolean("chestcavity.debugMetalBoneAbsorption");

    AbstractMetalBoneSupportBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        boolean debug = debugAbsorptionLogging();
        if (entity == null || cc == null || organ == null || organ.isEmpty() || entity.level().isClientSide()) {
            if (debug) {
                ChestCavity.LOGGER.info("[compat/guzhenren] [metal_bone_debug] Skip tick: invalid context or clientside for {}", describeStack(organ));
            }
            return;
        }
        refreshIncreaseContribution(cc, organ);
        SteelBoneComboHelper.ensureAbsorptionCapacity(entity, cc);

        long gameTime = entity.level().getGameTime();
        OrganState state = organState(organ, stateRootKey());
        long lastApplied = state.getLong(ABSORPTION_KEY, Long.MIN_VALUE);
        boolean firstApplication = lastApplied == Long.MIN_VALUE;
        long intervalTicks = absorptionIntervalTicks();
        if (!firstApplication) {
            long delta = gameTime - lastApplied;
            if (delta < intervalTicks) {
                if (debug) {
                    ChestCavity.LOGGER.info(
                            "[compat/guzhenren] [metal_bone_debug] Interval gate blocked ({} < {}) for {}",
                            delta, intervalTicks, describeStack(organ)
                    );
                }
                return;
            }
        } else if (debug) {
            ChestCavity.LOGGER.info(
                    "[compat/guzhenren] [metal_bone_debug] First absorption check for {} (interval {} ticks)",
                    describeStack(organ), intervalTicks
            );
        }

        int stackCount = Math.max(1, organ.getCount());
        double energyCost = Math.max(0.0, boneEnergyCostPerStack()) * stackCount;
        if (!SteelBoneComboHelper.tryConsumeBoneEnergy(cc, energyCost)) {
            if (debug) {
                ChestCavity.LOGGER.info(
                        "[compat/guzhenren] [metal_bone_debug] Insufficient bone energy: need {} for {}",
                        String.format(Locale.ROOT, "%.2f", energyCost), describeStack(organ)
                );
            }
            return;
        }

        float requiredAbsorption = Math.max(0.0f, absorptionPerStack() * stackCount);
        if (requiredAbsorption <= 0.0f) {
            if (debug) {
                ChestCavity.LOGGER.info(
                        "[compat/guzhenren] [metal_bone_debug] No absorption target ({}); skipping {}",
                        requiredAbsorption, describeStack(organ)
                );
            }
            OrganStateOps.setLong(state, cc, organ, ABSORPTION_KEY, gameTime, value -> value, Long.MIN_VALUE);
            return;
        }

        float beforeAbsorption = entity.getAbsorptionAmount();
        if (debug) {
            ChestCavity.LOGGER.debug(
                    "[compat/guzhenren] [metal_bone_debug] Candidate absorption: before={} target={} for {}",
                    String.format(Locale.ROOT, "%.1f", beforeAbsorption),
                    String.format(Locale.ROOT, "%.1f", requiredAbsorption),
                    describeStack(organ)
            );
        }

        if (beforeAbsorption + 1.0E-3f < requiredAbsorption) {
            float applied = Math.max(beforeAbsorption, requiredAbsorption);
            entity.setAbsorptionAmount(applied);
            ChestCavity.LOGGER.debug(
                    "[compat/guzhenren] Applied metal bone absorption {} -> {} for {}",
                    String.format(java.util.Locale.ROOT, "%.1f", entity.getAbsorptionAmount()),
                    String.format(java.util.Locale.ROOT, "%.1f", requiredAbsorption),
                    describeStack(organ)
            );
            if (debug) {
                ChestCavity.LOGGER.debug(
                        "[compat/guzhenren] [metal_bone_debug] Set absorption to {} (applied {} -> {}) for {}",
                        String.format(Locale.ROOT, "%.1f", entity.getAbsorptionAmount()),
                        String.format(Locale.ROOT, "%.1f", beforeAbsorption),
                        String.format(Locale.ROOT, "%.1f", applied),
                        describeStack(organ)
                );
            }
        } else if (debug) {
            ChestCavity.LOGGER.info(
                    "[compat/guzhenren] [metal_bone_debug] Absorption already meets target ({} >= {}) for {}",
                    String.format(Locale.ROOT, "%.1f", beforeAbsorption),
                    String.format(Locale.ROOT, "%.1f", requiredAbsorption),
                    describeStack(organ)
            );
        }
        float afterAbsorption = entity.getAbsorptionAmount();
        if (debug) {
            ChestCavity.LOGGER.debug(
                    "[compat/guzhenren] [metal_bone_debug] Absorption tick completed: target={} before={} after={} for {}",
                    String.format(Locale.ROOT, "%.1f", requiredAbsorption),
                    String.format(Locale.ROOT, "%.1f", beforeAbsorption),
                    String.format(Locale.ROOT, "%.1f", afterAbsorption),
                    describeStack(organ)
            );
        }
        OrganStateOps.setLong(state, cc, organ, ABSORPTION_KEY, gameTime, value -> value, Long.MIN_VALUE);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.registerContributor(organ, this, GU_DAO_CHANNEL, JIN_DAO_CHANNEL);
        refreshIncreaseContribution(cc, organ);
        SteelBoneComboHelper.ensureAbsorptionCapacity(cc.owner, cc);
        if (!registration.alreadyRegistered()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren] Registered metal bone contributor for {}", describeStack(organ));
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, GU_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
        ensureChannel(context, JIN_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
        ensureChannel(context, BONE_GROWTH_CHANNEL).addPolicy(NON_NEGATIVE);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        double guRemoved = ledger.remove(organ, GU_DAO_CHANNEL);
        double jinRemoved = ledger.remove(organ, JIN_DAO_CHANNEL);
        ledger.unregisterContributor(organ);
        if (guRemoved != 0.0) {
            ensureChannel(context, GU_DAO_CHANNEL).addPolicy(NON_NEGATIVE).adjust(-guRemoved);
        }
        if (jinRemoved != 0.0) {
            ensureChannel(context, JIN_DAO_CHANNEL).addPolicy(NON_NEGATIVE).adjust(-jinRemoved);
        }
        ledger.verifyAndRebuildIfNeeded();
        SteelBoneComboHelper.ensureAbsorptionCapacity(entity, cc);
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        int stackCount = Math.max(1, organ.getCount());
        double guEffect = stackCount * guDaoEffectPerStack();
        double jinEffect = stackCount * jinDaoEffectPerStack();
        registrar.record(GU_DAO_CHANNEL, stackCount, guEffect);
        registrar.record(JIN_DAO_CHANNEL, stackCount, jinEffect);
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ) {
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();

        LinkageChannel guDao = ensureChannel(context, GU_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
        LinkageChannel jinDao = ensureChannel(context, JIN_DAO_CHANNEL).addPolicy(NON_NEGATIVE);

        double guTarget = Math.max(1, organ.getCount()) * guDaoEffectPerStack();
        double jinTarget = Math.max(1, organ.getCount()) * jinDaoEffectPerStack();

        double guPrevious = ledger.adjust(organ, GU_DAO_CHANNEL, 0.0);
        double jinPrevious = ledger.adjust(organ, JIN_DAO_CHANNEL, 0.0);

        double guDelta = guTarget - guPrevious;
        double jinDelta = jinTarget - jinPrevious;

        if (guDelta != 0.0) {
            LedgerOps.adjust(cc, organ, GU_DAO_CHANNEL, guDelta, NON_NEGATIVE, false);
        }
        if (jinDelta != 0.0) {
            LedgerOps.adjust(cc, organ, JIN_DAO_CHANNEL, jinDelta, NON_NEGATIVE, true);
        }
    }

    protected abstract float absorptionPerStack();

    protected abstract double boneEnergyCostPerStack();

    protected abstract int absorptionIntervalTicks();

    protected abstract double guDaoEffectPerStack();

    protected abstract double jinDaoEffectPerStack();

    protected abstract String stateRootKey();

    /**
     * Override to enable verbose absorption debugging for specific organs.
     */
    protected boolean debugAbsorptionLogging() {
        return DEBUG_ABSORPTION && ChestCavity.LOGGER.isDebugEnabled();
    }
}
