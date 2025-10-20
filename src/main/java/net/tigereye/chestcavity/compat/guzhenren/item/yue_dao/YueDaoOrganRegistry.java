package net.tigereye.chestcavity.compat.guzhenren.item.yue_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.yue_dao.behavior.YueGuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative registration for 月道器官（如月光蛊）。
 */
public final class YueDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation YUE_GUANG_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yue_guang_gu");

    private YueDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        List<OrganIntegrationSpec> list = new ArrayList<>();
        try {
            list.add(OrganIntegrationSpec.builder(YUE_GUANG_GU_ID)
                    .addSlowTickListener(YueGuangGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(YueGuangGuOrganBehavior.INSTANCE)
                    .addOnHitListener(YueGuangGuOrganBehavior.INSTANCE)
                    .addRemovalListener(YueGuangGuOrganBehavior.INSTANCE)
                    .ensureAttached(YueGuangGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(YueGuangGuOrganBehavior.INSTANCE::onEquip)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][yue_dao] skip YueGuangGu registration due to init error", t);
        }
        return list;
    }
}
