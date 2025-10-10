package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;

import java.util.List;
import java.util.Optional;

/**
 * Behaviour for 泉涌命蛊 (Quan Yong Ming Gu).
 */
public final class QuanYongMingGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final QuanYongMingGuOrganBehavior INSTANCE = new QuanYongMingGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_yong_ming_gu");
    private static final ResourceLocation SHUI_TI_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shui_ti_gu");
    private static final ResourceLocation SHUI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shui_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double INCREASE_EFFECT_BONUS = 0.25;
    private static final double ZHENYUAN_COST_PER_SECOND = 800.0;
    private static final double JINGLI_GAIN_PER_SECOND = 5.0;
    private static final double HEALTH_PERCENT_PER_SECOND = 0.0075;
    private static final float PURE_WATER_ABSORPTION = 10.0f;

    private QuanYongMingGuOrganBehavior() {
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        context.getOrCreateChannel(SHUI_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.registerContributor(organ, this, SHUI_DAO_INCREASE_EFFECT);

        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        refreshIncreaseContribution(cc, organ, isPrimaryOrgan(cc, organ));
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        boolean primary = isPrimaryOrgan(cc, organ);
        refreshIncreaseContribution(cc, organ, primary);
        if (!primary) {
            return;
        }
        if (cc == null || !entity.isAlive()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());

        if (entity instanceof Player player) {
            double zhenyuanCost = ZHENYUAN_COST_PER_SECOND * stackCount;
            ConsumptionResult result = ResourceOps.consumeStrict(player, zhenyuanCost, 0.0);
            if (!result.succeeded()) {
                return;
            }

            applyHealing(entity, stackCount);
            ResourceOps.adjustJingli(player, JINGLI_GAIN_PER_SECOND * stackCount);

            if (hasShuiTiGu(cc, organ)) {
                ensurePureWaterAbsorption(entity, stackCount);
            }
            return;
        }

        applyHealing(entity, stackCount);


        if (hasShuiTiGu(cc, organ)) {
            ensurePureWaterAbsorption(entity, stackCount);
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        double removed = ledger.remove(organ, SHUI_DAO_INCREASE_EFFECT);
        ledger.unregisterContributor(organ);
        context.lookupChannel(SHUI_DAO_INCREASE_EFFECT)
                .ifPresent(channel -> channel.adjust(-removed));
        // Ensure ledger integrity to avoid stale stacked contributions
        ledger.verifyAndRebuildIfNeeded();
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        LinkageManager.getContext(cc)
                .getOrCreateChannel(SHUI_DAO_INCREASE_EFFECT)
                .addPolicy(NON_NEGATIVE);
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        boolean primary = isPrimaryOrgan(cc, organ);
        double effect = primary ? INCREASE_EFFECT_BONUS : 0.0;
        registrar.record(SHUI_DAO_INCREASE_EFFECT, Math.max(1, organ.getCount()), effect);
    }

    private void applyHealing(LivingEntity entity, int stackCount) {
        if (HEALTH_PERCENT_PER_SECOND <= 0.0) {
            return;
        }
        float maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0.0f || entity.getHealth() >= maxHealth) {
            return;
        }
        float healAmount = (float) (maxHealth * HEALTH_PERCENT_PER_SECOND * stackCount);
        if (healAmount <= 0.0f) {
            return;
        }
        ChestCavityUtil.runWithOrganHeal(() -> entity.heal(healAmount));
    }

    private void grantJingli(Player player, int stackCount) {
        if (JINGLI_GAIN_PER_SECOND <= 0.0) {
            return;
        }
        double delta = JINGLI_GAIN_PER_SECOND * stackCount;
        ResourceOps.adjustJingli(player, delta);
    }

    private void ensurePureWaterAbsorption(LivingEntity entity, int stackCount) {
        // First ensure the capacity (MAX_ABSORPTION) is high enough like SteelBone helper does
        net.minecraft.world.entity.ai.attributes.AttributeInstance attr =
                entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION);
        double desiredCap = (double) PURE_WATER_ABSORPTION * Math.max(1, stackCount);
        if (attr != null && desiredCap > 0.0 && Math.abs(attr.getBaseValue() - desiredCap) > 1.0E-3) {
            attr.setBaseValue(desiredCap);
        }
        // Then softly raise current absorption up to the target, without clobbering higher values
        float targetCurrent = (float) desiredCap;
        if (targetCurrent <= 0.0f) {
            return;
        }
        float current = entity.getAbsorptionAmount();
        if (current + 0.01f >= targetCurrent) {
            return;
        }
        entity.setAbsorptionAmount(targetCurrent);
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ, boolean primary) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.verifyAndRebuildIfNeeded();
        double previous = ledger.adjust(organ, SHUI_DAO_INCREASE_EFFECT, 0.0);
        double target = primary ? INCREASE_EFFECT_BONUS : 0.0;
        double delta = target - previous;
        if (delta != 0.0) {
            LedgerOps.adjust(cc, organ, SHUI_DAO_INCREASE_EFFECT, delta, NON_NEGATIVE, true);
        }
    }

    private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);
        if (slotIndex < 0) {
            return true;
        }
        for (int i = 0; i < slotIndex; i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (matchesOrgan(candidate, ORGAN_ID)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasShuiTiGu(ChestCavityInstance cc, ItemStack self) {
        if (cc == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (candidate == self || candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (matchesOrgan(candidate, SHUI_TI_GU_ID)) {
                return true;
            }
        }
        return false;
    }
}
