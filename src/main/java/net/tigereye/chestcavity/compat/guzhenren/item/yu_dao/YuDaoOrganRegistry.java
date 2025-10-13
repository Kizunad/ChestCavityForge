package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior.YuanLaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior.YuanLaoGuSecondTierBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registration for 宇道 (Yu Dao) organ behaviours.
 */
public final class YuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation YUAN_LAO_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "yuan_lao_gu_1");
    private static final ResourceLocation E_YUAN_LAO_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "e_yuanlaogurzhuan");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(YUAN_LAO_GU_ID)
                    .addSlowTickListener(YuanLaoGuOrganBehavior.INSTANCE)
                    .build(),
            OrganIntegrationSpec.builder(E_YUAN_LAO_GU_ID)
                    .addSlowTickListener(YuanLaoGuSecondTierBehavior.INSTANCE)
                    .build()
    );

    private YuDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
