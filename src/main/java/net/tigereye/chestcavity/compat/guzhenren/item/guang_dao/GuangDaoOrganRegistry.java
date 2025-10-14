package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.behavior.ShanGuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Registration broker for 光道器官。
 */
public final class GuangDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation SHAN_GUANG_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "shan_guang_gu");

    private GuangDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        List<OrganIntegrationSpec> list = new ArrayList<>();
        try {
            list.add(OrganIntegrationSpec.builder(SHAN_GUANG_GU_ID)
                    .addSlowTickListener(ShanGuangGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(ShanGuangGuOrganBehavior.INSTANCE)
                    .build());
        } catch (Throwable t) {
            ChestCavity.LOGGER.warn("[compat/guzhenren][guang_dao] skip ShanGuangGu registration due to init error", t);
        }
        return list;
    }
}
