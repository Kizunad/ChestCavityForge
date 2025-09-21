package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;

/**
 * 金肺蛊：站地行为模板，后续可在 {@link #onGroundTick} 中实现实际效果。
 */
public enum JinfeiguOrganBehavior implements OrganOnGroundListener {
    INSTANCE;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // TODO implement behaviour
    }
}
