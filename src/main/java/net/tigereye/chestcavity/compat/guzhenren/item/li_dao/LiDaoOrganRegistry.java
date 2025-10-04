package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.QuanLiYiFuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 力道（三转）器官。
 */
public final class LiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation QUAN_LI_YI_FU_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_li_yi_fu_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(QUAN_LI_YI_FU_GU_ID)
                    .addSlowTickListener(QuanLiYiFuGuOrganBehavior.INSTANCE)
                    .build()
    );

    private LiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
