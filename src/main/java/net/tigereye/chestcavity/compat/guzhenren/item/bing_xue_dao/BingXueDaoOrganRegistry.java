package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 冰雪道（Bing Xue Dao） organs.
 */
public final class BingXueDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation BING_JI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(BING_JI_GU_ID)
                    .addSlowTickListener(BingJiGuOrganBehavior.INSTANCE)
                    .addOnHitListener(BingJiGuOrganBehavior.INSTANCE)
                    .addRemovalListener(BingJiGuOrganBehavior.INSTANCE)
                    .ensureAttached(BingJiGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(BingJiGuOrganBehavior.INSTANCE::onEquip)
                    .build()
    );

    private BingXueDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
