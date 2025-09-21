package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;

/**
 * 木肝蛊：站地时的行为模板。后续可在 {@link #onGroundTick} 中填充实际逻辑。
 */
public enum MuganguOrganBehavior implements OrganOnGroundListener {
    INSTANCE;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // TODO: implement wood liver behaviour
    }
}
