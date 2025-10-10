package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.ShiPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.TuQiangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registration for 土道蛊 (Tu Dao) organ behaviours.
 */
public final class TuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation SHI_PI_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shi_pi_gu");
    private static final ResourceLocation TU_QIANG_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "tu_qiang_gu");

    static {
        TuDaoOrganEvents.register();
    }

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(SHI_PI_GU_ID)
                    .addSlowTickListener(ShiPiGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(ShiPiGuOrganBehavior.INSTANCE)
                    .ensureAttached(ShiPiGuOrganBehavior.INSTANCE::ensureAttached)
                    .build(),
            OrganIntegrationSpec.builder(TU_QIANG_GU_ID)
                    .addSlowTickListener(TuQiangGuOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(TuQiangGuOrganBehavior.INSTANCE)
                    .addRemovalListener(TuQiangGuOrganBehavior.INSTANCE)
                    .ensureAttached(TuQiangGuOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private TuDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
