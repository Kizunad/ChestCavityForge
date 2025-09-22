package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.NBTCharge;

import java.util.Optional;

final class GuQiangguRenderUtil {

    static final ResourceLocation GU_QIANG_GU_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang_gu");
    private static final ResourceLocation GU_QIANG_ITEM_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang");
    static final String CHARGE_TAG = "GuQiangCharge";
    private static final ItemStack RENDER_STACK = resolveRenderStack();

    private GuQiangguRenderUtil() {
    }

    static ItemStack getRenderStack() {
        return RENDER_STACK;
    }

    private static ItemStack resolveRenderStack() {
        return BuiltInRegistries.ITEM.getOptional(GU_QIANG_ITEM_ID)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    static boolean hasActiveCharge(AbstractClientPlayer player) {
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return false;
        }
        return hasActiveCharge(optional.get().getChestCavityInstance());
    }

    static boolean hasActiveCharge(ChestCavityInstance cc) {
        return findChargedOrgan(cc) != null;
    }

    static ItemStack findChargedOrgan(ChestCavityInstance cc) {
        if (cc == null) {
            return null;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(GU_QIANG_GU_ID)) {
                if (NBTCharge.getCharge(stack, CHARGE_TAG) > 0) {
                    return stack;
                }
            }
        }
        return null;
    }
}
