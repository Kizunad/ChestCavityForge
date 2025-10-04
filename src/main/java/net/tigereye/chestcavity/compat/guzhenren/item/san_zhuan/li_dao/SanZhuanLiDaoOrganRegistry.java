package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.li_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registry for third-turn Li Dao organs.
 */
public final class SanZhuanLiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation ZI_LI_GENG_SHENG_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_li_geng_sheng_gu_3");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(ZI_LI_GENG_SHENG_GU_ID)
                    .addSlowTickListener(ZiLiGengShengGuOrganBehavior.INSTANCE)
                    .ensureAttached(ZiLiGengShengGuOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private SanZhuanLiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
