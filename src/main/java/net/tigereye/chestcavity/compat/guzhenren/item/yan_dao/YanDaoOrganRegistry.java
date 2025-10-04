package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoxinguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registration for炎道器官（火心蛊等）。
 */
public final class YanDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(HUOXINGU_ID)
                    .addSlowTickListener(HuoxinguOrganBehavior.INSTANCE)
                    .build()
    );

    private YanDaoOrganRegistry() {}

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
