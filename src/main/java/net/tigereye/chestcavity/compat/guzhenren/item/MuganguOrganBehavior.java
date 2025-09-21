package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;

import java.util.OptionalDouble;
import java.util.Set;

/**
 * 木肝蛊：每秒为玩家补充真元。
 * <p>
 * - 若胸腔内缺少火、金、水、土任一蛊，则先扣除精力再按八折恢复真元；
 * - 若集齐其余四蛊，则直接按全额恢复真元；
 * - 真元恢复量随阶段与转数使用与消耗相同的缩放公式。
 */
public enum MuganguOrganBehavior implements OrganOnGroundListener, OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final Set<ResourceLocation> COMPANION_GU_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu")
    );
    private static final double BASE_ZHENYUAN_RESTORE = 2400.0;
    private static final double BASE_JINGLI_COST = 1.0;
    private static final double DISCOUNT_FACTOR = 0.5;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // Work handled in onSlowTick to reduce per-tick overhead.
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || !player.onGround()) {
            return;
        }

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            int stackCount = Math.max(1, organ.getCount());
            double baseRegen = BASE_ZHENYUAN_RESTORE * stackCount;
            boolean hasCompanionOrgans = hasCompanionGu(cc);

            if (!hasCompanionOrgans) {
                double jingliCost = BASE_JINGLI_COST * stackCount;
                OptionalDouble jingliResult = handle.adjustJingli(-jingliCost, true);
                if (jingliResult.isEmpty()) {
                    return;
                }

                double discountedRegen = baseRegen * DISCOUNT_FACTOR;
                OptionalDouble replenished = handle.replenishScaledZhenyuan(discountedRegen, true);
                if (replenished.isEmpty()) {
                    handle.adjustJingli(jingliCost, true);
                }
            } else {
                handle.replenishScaledZhenyuan(baseRegen, true);
            }
        });
    }

    private static boolean hasCompanionGu(ChestCavityInstance cc) {
        int found = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(slotStack.getItem());
            if (itemId != null && COMPANION_GU_IDS.contains(itemId)) {
                found++;
            }
        }
        return found >= COMPANION_GU_IDS.size();
    }
}
