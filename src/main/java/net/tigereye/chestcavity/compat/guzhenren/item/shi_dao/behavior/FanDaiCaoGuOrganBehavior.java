package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenHungerHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * Behaviour for 饭袋草蛊：消耗真元以维持饱食度。
 */
public enum FanDaiCaoGuOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fan_dai_cao_gu");
    private static final double BASE_ZHENYUAN_COST = 200.0;

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ)) {
            return;
        }
        if (!GuzhenrenHungerHelper.needsFood(player)) {
            return;
        }
        ConsumptionResult payment = GuzhenrenResourceCostHelper.consumeStrict(player, BASE_ZHENYUAN_COST, 0.0);
        if (!payment.succeeded()) {
            return;
        }
        if (!GuzhenrenHungerHelper.topUpToFull(player)) {
            GuzhenrenResourceCostHelper.refund(player, payment);
        }
    }

    private static boolean matchesOrgan(ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(organ.getItem());
        return ORGAN_ID.equals(id);
    }
}
