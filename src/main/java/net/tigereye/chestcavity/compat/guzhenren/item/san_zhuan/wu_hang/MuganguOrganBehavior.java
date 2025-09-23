package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.DecayPolicy;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.OptionalDouble;
import java.util.Set;

/**
 * Mugangu regenerates zhenyuan each slow tick, with optional jingli cost when the five-element set is incomplete.
 */
public enum MuganguOrganBehavior implements OrganOnGroundListener, OrganSlowTickListener, IncreaseEffectContributor {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final Set<ResourceLocation> COMPANION_GU_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu")
    );
    private static final ResourceLocation COMPLETION_CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/wuhang_completion");
    private static final ResourceLocation REGEN_RATE_CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/mugangu_regen_rate");
    private static final ClampPolicy UNIT_CLAMP = new ClampPolicy(0.0, 1.0);
    private static final DecayPolicy REGEN_DECAY = new DecayPolicy(0.05);

    private static final double BASE_ZHENYUAN_RESTORE = 12000;
    private static final double BASE_JINGLI_COST = 1.0;
    private static final double DISCOUNT_FACTOR = 0.8;
    private static final double REGEN_ALPHA = 3.0;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // Work handled in onSlowTick to reduce per-tick overhead.
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // 修正： 木肝蛊无需触地即可使用被动
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            int stackCount = Math.max(1, organ.getCount());
            ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);

            LinkageChannel completionChannel = context.lookupChannel(COMPLETION_CHANNEL_ID)
                    .orElseGet(() -> context.getOrCreateChannel(COMPLETION_CHANNEL_ID).addPolicy(UNIT_CLAMP));
            LinkageChannel regenChannel = context.lookupChannel(REGEN_RATE_CHANNEL_ID)
                    .orElseGet(() -> context.getOrCreateChannel(REGEN_RATE_CHANNEL_ID)
                            .addPolicy(UNIT_CLAMP)
                            .addPolicy(REGEN_DECAY));

            int companionCount = countCompanionGu(cc);
            int required = COMPANION_GU_IDS.size();
            boolean hasCompanionOrgans = companionCount >= required;
            double completionRatio = required == 0 ? 1.0 : Math.min(1.0, companionCount / (double) required);
            completionChannel.set(completionRatio);

            double baseRegen = BASE_ZHENYUAN_RESTORE * stackCount;
            double multiplier = hasCompanionOrgans ? 1.0 : DISCOUNT_FACTOR;
            double regenBudget = computeZhenyuanGain(handle, baseRegen * multiplier);

            if (regenBudget <= 0.0) {
                regenChannel.set(0.0);
                return;
            }

            boolean success;
            if (!hasCompanionOrgans) {
                double jingliCost = BASE_JINGLI_COST * stackCount;
                OptionalDouble jingliResult = handle.adjustJingli(-jingliCost, true);
                if (jingliResult.isEmpty()) {
                    regenChannel.set(0.0);
                    return;
                }
                success = handle.replenishScaledZhenyuan(regenBudget, true).isPresent();
                if (!success) {
                    handle.adjustJingli(jingliCost, true);
                }
            } else {
                success = handle.replenishScaledZhenyuan(regenBudget, true).isPresent();
            }

            double regenRatio = baseRegen <= 0.0 ? 0.0 : Math.min(1.0, regenBudget / baseRegen);
            regenChannel.set(success ? regenRatio : 0.0);
        });
    }

    private static int countCompanionGu(ChestCavityInstance cc) {
        if (COMPANION_GU_IDS.isEmpty()) {
            return 0;
        }
        java.util.Set<ResourceLocation> matched = new java.util.HashSet<>();
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(slotStack.getItem());
            if (itemId != null && COMPANION_GU_IDS.contains(itemId)) {
                matched.add(itemId);
                if (matched.size() == COMPANION_GU_IDS.size()) {
                    break;
                }
            }
        }
        return matched.size();
    }

    private static double computeZhenyuanGain(GuzhenrenResourceBridge.ResourceHandle handle, double baseAmount) {
        if (baseAmount <= 0.0) {
            return 0.0;
        }
        OptionalDouble maxOpt = handle.getMaxZhenyuan();
        OptionalDouble currentOpt = handle.getZhenyuan();
        if (maxOpt.isEmpty() || currentOpt.isEmpty()) {
            return baseAmount;
        }
        double max = Math.max(1.0, maxOpt.getAsDouble());
        double current = Math.max(0.0, currentOpt.getAsDouble());
        double missing = Math.max(0.0, max - current);
        if (missing <= 0.0) {
            return 0.0;
        }
        double fraction = Math.min(1.0, missing / max);
        double scaled = baseAmount * (1.0 - Math.exp(-REGEN_ALPHA * fraction));
        return Double.isFinite(scaled) ? Math.max(0.0, scaled) : 0.0;
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        // Mugangu does not contribute to INCREASE effects.
    }
}
