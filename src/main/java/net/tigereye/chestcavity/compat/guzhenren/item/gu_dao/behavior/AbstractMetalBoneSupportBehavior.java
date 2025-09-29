package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.List;

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

    AbstractMetalBoneSupportBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || cc == null || organ == null || organ.isEmpty() || entity.level().isClientSide()) {
            return;
        }
        refreshIncreaseContribution(cc, organ);

        long gameTime = entity.level().getGameTime();
        OrganState state = organState(organ, stateRootKey());
        long lastApplied = state.getLong(ABSORPTION_KEY, Long.MIN_VALUE);
        if (gameTime - lastApplied < absorptionIntervalTicks()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        double energyCost = Math.max(0.0, boneEnergyCostPerStack()) * stackCount;
        if (!net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper.tryConsumeBoneEnergy(cc, energyCost)) {
            return;
        }

        float requiredAbsorption = Math.max(0.0f, absorptionPerStack() * stackCount);
        if (requiredAbsorption <= 0.0f) {
            state.setLong(ABSORPTION_KEY, gameTime);
            return;
        }

        if (entity.getAbsorptionAmount() + 1.0E-3f < requiredAbsorption) {
            entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), requiredAbsorption));
            ChestCavity.LOGGER.debug(
                    "[compat/guzhenren] Applied metal bone absorption {} -> {} for {}",
                    String.format(java.util.Locale.ROOT, "%.1f", entity.getAbsorptionAmount()),
                    String.format(java.util.Locale.ROOT, "%.1f", requiredAbsorption),
                    describeStack(organ)
            );
        }
        state.setLong(ABSORPTION_KEY, gameTime);
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
            guDao.adjust(guDelta);
            ledger.adjust(organ, GU_DAO_CHANNEL, guDelta);
        }
        if (jinDelta != 0.0) {
            jinDao.adjust(jinDelta);
            ledger.adjust(organ, JIN_DAO_CHANNEL, jinDelta);
        }
    }

    protected abstract float absorptionPerStack();

    protected abstract double boneEnergyCostPerStack();

    protected abstract int absorptionIntervalTicks();

    protected abstract double guDaoEffectPerStack();

    protected abstract double jinDaoEffectPerStack();

    protected abstract String stateRootKey();
}

