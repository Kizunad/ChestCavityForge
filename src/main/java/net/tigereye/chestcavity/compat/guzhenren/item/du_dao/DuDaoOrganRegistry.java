package net.tigereye.chestcavity.compat.guzhenren.item.du_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.behavior.ChouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registration for 毒道蛊 (Du Dao) organ behaviours.
 */
public final class DuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation CHOU_PI_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "chou_pi_gu");

    private static final List<OrganIntegrationSpec> SPECS;

    static {
        DuDaoOrganEvents.register();
        SPECS = List.of(
                OrganIntegrationSpec.builder(CHOU_PI_GU_ID)
                        .addSlowTickListener(ChouPiGuOrganBehavior.INSTANCE)
                        .addIncomingDamageListener(ChouPiGuOrganBehavior.INSTANCE)
                        .ensureAttached(ChouPiGuOrganBehavior.INSTANCE::ensureAttached)
                        .build()
        );
    }

    private DuDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
