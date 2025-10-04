package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZiLiGengShengGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registry for 力道（三转） organ behaviours.
 */
public final class LiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation ZI_LI_GENG_SHENG_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_li_geng_sheng_gu_3");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(ZI_LI_GENG_SHENG_GU_ID)
                    .addSlowTickListener(ZiLiGengShengGuOrganBehavior.INSTANCE)
                    .addRemovalListener(ZiLiGengShengGuOrganBehavior.INSTANCE)
                    .ensureAttached(ZiLiGengShengGuOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private LiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
