package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.LingXianguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 水道（Shui Dao） organs.
 */
public final class ShuiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation LING_XIAN_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ling_xian_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(LING_XIAN_GU_ID)
                    .addSlowTickListener(LingXianguOrganBehavior.INSTANCE)
                    .ensureAttached(LingXianguOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private ShuiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
