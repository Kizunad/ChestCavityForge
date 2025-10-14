package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import org.slf4j.Logger;

/**
 * Placeholder behaviour for火心蛊（Yan Dao）。
 * 
 */
public final class HuoxinguOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final HuoxinguOrganBehavior INSTANCE = new HuoxinguOrganBehavior();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");
    private static final double BURN_ZHENYUAN_COST = 100.0;
    private static final float BURN_HEAL_AMOUNT = BehaviorConfigAccess.getFloat(HuoxinguOrganBehavior.class, "BURN_HEAL_AMOUNT", 4.0f);
    private static final float BURN_JINGLI_REPLENISH = BehaviorConfigAccess.getFloat(HuoxinguOrganBehavior.class, "BURN_JINGLI_REPLENISH", 3.0f);

    private HuoxinguOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        if (!entity.isOnFire()) {
            return;
        }

        var payment = ResourceOps.consumeStrict(entity, BURN_ZHENYUAN_COST, 0.0);
        if (!payment.succeeded()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[compat/guzhenren][yan_dao][huoxingu] insufficient resources for {} (reason={})", entity.getName().getString(), payment.failureReason());
            }
            return;
        }

        if (BURN_HEAL_AMOUNT > 0.0f && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(BURN_HEAL_AMOUNT);
        }
        if (BURN_JINGLI_REPLENISH > 0.0f) {
            ResourceOps.tryAdjustJingli(entity, BURN_JINGLI_REPLENISH, true);
        }
    }
}
