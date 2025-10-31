package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import net.tigereye.chestcavity.registration.CCItems;

public class BingBuGuPassive implements PassiveHook {

    @Override
    public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        BingBuGuOrganBehavior.INSTANCE.onSlowTick(owner, cc, organ);
    }

    private ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.is(CCItems.GUZHENREN_BING_BU_GU)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
