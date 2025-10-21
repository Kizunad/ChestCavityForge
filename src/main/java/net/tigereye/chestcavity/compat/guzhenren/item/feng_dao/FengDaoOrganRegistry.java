package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior.QingFengLunOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * 清风轮蛊（风道）注册表。
 */
public final class FengDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation QING_FENG_LUN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu");

    private FengDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        List<OrganIntegrationSpec> list = new ArrayList<>();
        try {
            list.add(OrganIntegrationSpec.builder(QING_FENG_LUN_ID)
                    .addSlowTickListener(QingFengLunOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(QingFengLunOrganBehavior.INSTANCE)
                    .addOnHitListener(QingFengLunOrganBehavior.INSTANCE)
                    .addRemovalListener(QingFengLunOrganBehavior.INSTANCE)
                    .ensureAttached(QingFengLunOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(QingFengLunOrganBehavior.INSTANCE::onEquip)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][feng_dao] skip qing_feng_lun registration due to init error", t);
        }
        return list;
    }
}
