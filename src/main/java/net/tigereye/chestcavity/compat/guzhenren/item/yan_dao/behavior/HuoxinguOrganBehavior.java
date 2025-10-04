package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.slf4j.Logger;

/**
 * Placeholder behaviour for火心蛊（Yan Dao）。
 * TODO: implement recovery、飞行、火焰抗性等逻辑。
 */
public final class HuoxinguOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final HuoxinguOrganBehavior INSTANCE = new HuoxinguOrganBehavior();
    private static final Logger LOGGER = LogUtils.getLogger();

    private HuoxinguOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // TODO: 实现火心蛊被动：生命/精力恢复、火焰抗性、喷气式飞行。
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[compat/guzhenren][yan_dao][huoxingu] slow tick placeholder invoked for {}", entity.getName().getString());
        }
    }
}
