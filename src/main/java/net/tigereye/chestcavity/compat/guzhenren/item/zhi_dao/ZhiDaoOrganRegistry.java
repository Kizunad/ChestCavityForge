package net.tigereye.chestcavity.compat.guzhenren.item.zhi_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.zhi_dao.behavior.LingGuangYiShanGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registration hub for 智道（Zhi Dao） organs.
 */
public final class ZhiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    public static final ResourceLocation LING_GUANG_YI_SHAN_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ling_guang_yi_shan_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(LING_GUANG_YI_SHAN_GU_ID)
                    .addSlowTickListener(LingGuangYiShanGuOrganBehavior.INSTANCE)
                    .build()
    );

    private ZhiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
