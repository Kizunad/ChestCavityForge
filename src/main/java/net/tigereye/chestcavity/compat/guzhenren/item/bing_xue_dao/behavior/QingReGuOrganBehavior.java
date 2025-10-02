package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Behaviour implementation for 清热蛊 (Qing Re Gu).
 */
public final class QingReGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener {

    public static final QingReGuOrganBehavior INSTANCE = new QingReGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_re_gu");
    private static final ResourceLocation JADE_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu");
    private static final ResourceLocation BING_XUE_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bing_xue_dao_increase_effect");
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double BASE_ZHENYUAN_COST = 100.0;
    private static final double JINGLI_PER_TICK = 1.0;
    private static final float HEAL_PER_TICK = 3.0f;
    private static final double POISON_CLEAR_CHANCE = 0.10;
    private static final double FIRE_DAMAGE_REDUCTION = 0.03;

    private QingReGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_QING_RE_GU, ORGAN_ID)) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        int stackCount = Math.max(1, organ.getCount());
        double zhenyuanCost = BASE_ZHENYUAN_COST * stackCount;
        OptionalDouble consumed = handle.consumeScaledZhenyuan(zhenyuanCost);
        if (consumed.isEmpty()) {
            return;
        }

        handle.adjustJingli(JINGLI_PER_TICK * stackCount, true);
        float healAmount = HEAL_PER_TICK * stackCount;
        if (healAmount > 0.0f) {
            ChestCavityUtil.runWithOrganHeal(() -> player.heal(healAmount));
        }

        if (hasJadeBone(cc)) {
            maybeClearPoison(player);
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
        if (source == null || victim == null || cc == null || damage <= 0.0f) {
            return damage;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_QING_RE_GU, ORGAN_ID)) {
            return damage;
        }
        if (!hasJadeBone(cc) || !source.is(DamageTypeTags.IS_FIRE)) {
            return damage;
        }

        double increase = Math.max(0.0, lookupIncreaseEffect(cc));
        double reductionFraction = FIRE_DAMAGE_REDUCTION * (1.0 + increase);
        double multiplier = Math.max(0.0, 1.0 - reductionFraction);
        return (float) (damage * multiplier);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        context.getOrCreateChannel(BING_XUE_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    }

    private static void maybeClearPoison(Player player) {
        if (player == null || !player.hasEffect(MobEffects.POISON)) {
            return;
        }
        if (player.getRandom().nextDouble() < POISON_CLEAR_CHANCE) {
            player.removeEffect(MobEffects.POISON);
        }
    }

    private static double lookupIncreaseEffect(ChestCavityInstance cc) {
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return 0.0;
        }
        return context.lookupChannel(BING_XUE_INCREASE_EFFECT)
                .map(LinkageChannel::get)
                .orElse(0.0);
    }

    private static boolean hasJadeBone(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(stackId, JADE_BONE_ID)) {
                return true;
            }
        }
        return false;
    }
}
