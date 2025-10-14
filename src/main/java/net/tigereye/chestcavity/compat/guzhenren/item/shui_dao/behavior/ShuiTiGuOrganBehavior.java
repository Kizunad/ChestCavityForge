package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
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
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;

/**
 * Behaviour for 水体蛊 (Shui Ti Gu).
 */
public final class ShuiTiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final ShuiTiGuOrganBehavior INSTANCE = new ShuiTiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shui_ti_gu");
    private static final ResourceLocation SHUI_SHEN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu");
    private static final ResourceLocation SHUI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shui_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double INCREASE_PER_STACK = 0.08;
    private static final float HEAL_PER_SECOND = BehaviorConfigAccess.getFloat(ShuiTiGuOrganBehavior.class, "HEAL_PER_SECOND", 3.0f);
    private static final double ZHENYUAN_PER_SECOND = 200.0;
    private static final int SHIELD_CHARGE_PER_TICK = BehaviorConfigAccess.getInt(ShuiTiGuOrganBehavior.class, "SHIELD_CHARGE_PER_TICK", 2);

    private ShuiTiGuOrganBehavior() {
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, java.util.List<OrganRemovalContext> staleRemovalContexts) {
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
        refreshIncreaseContribution(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        refreshIncreaseContribution(cc, organ);

        if (cc == null || !entity.isAlive()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        double totalCost = ZHENYUAN_PER_SECOND * stackCount;
        ConsumptionResult payment = (entity instanceof Player player)
                ? ResourceOps.consumeStrict(player, totalCost, 0.0)
                : ResourceOps.consumeWithFallback(entity, totalCost, 0.0);
        if (!payment.succeeded()) {
            return;
        }

        if (HEAL_PER_SECOND > 0.0f && entity.getHealth() < entity.getMaxHealth()) {
            float heal = HEAL_PER_SECOND * stackCount;
            ChestCavityUtil.runWithOrganHeal(() -> entity.heal(heal));
        }

        rechargeShuishengu(cc, stackCount * SHIELD_CHARGE_PER_TICK);
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
        // Guard against stale stacked entries
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
        int stackCount = Math.max(1, organ.getCount());
        double effect = stackCount * INCREASE_PER_STACK;
        registrar.record(SHUI_DAO_INCREASE_EFFECT, stackCount, effect);
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.verifyAndRebuildIfNeeded();
        double previous = ledger.adjust(organ, SHUI_DAO_INCREASE_EFFECT, 0.0);
        double target = Math.max(1, organ.getCount()) * INCREASE_PER_STACK;
        double delta = target - previous;
        if (delta != 0.0) {
            LedgerOps.adjust(cc, organ, SHUI_DAO_INCREASE_EFFECT, delta, NON_NEGATIVE, true);
        }
    }

    private void rechargeShuishengu(ChestCavityInstance cc, int chargeAmount) {
        if (cc == null || chargeAmount <= 0) {
            return;
        }
        int containerSize = cc.inventory.getContainerSize();
        int totalCharge = 0;
        int totalMax = 0;
        boolean anyChanged = false;
        for (int i = 0; i < containerSize; i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (candidate.isEmpty()) {
                continue;
            }
            if (!matchesShuishengu(candidate)) {
                continue;
            }
            int stackCount = Math.max(1, candidate.getCount());
            int maxCharge = ShuishenguShield.getEffectiveMaxCharge(stackCount);
            int current = Math.min(NBTCharge.getCharge(candidate, ShuishenguShield.STATE_KEY), maxCharge);
            int updated = Math.min(maxCharge, current + chargeAmount);
            totalCharge += updated;
            totalMax += maxCharge;
            if (updated != current) {
                ShuishenguShield.setCharge(candidate, updated, stackCount);
                NetworkUtil.sendOrganSlotUpdate(cc, candidate);
                anyChanged = true;
            }
        }
        if (anyChanged && totalMax > 0) {
            ShuishenguShield.broadcastChargeRatio(cc, totalMax, totalCharge);
        }
    }

    private boolean matchesShuishengu(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return SHUI_SHEN_GU_ID.equals(id);
    }
}
