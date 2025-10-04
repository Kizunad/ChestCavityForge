package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.li_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registration for 三转力道类蛊虫。
 */
public final class LiDaoSanZhuanOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation QUAN_LI_YI_FU_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_li_yi_fu_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(QUAN_LI_YI_FU_GU_ID)
                    .addSlowTickListener(QuanLiYiFuGuOrganBehavior.INSTANCE)
                    .build()
    );

    private LiDaoSanZhuanOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
