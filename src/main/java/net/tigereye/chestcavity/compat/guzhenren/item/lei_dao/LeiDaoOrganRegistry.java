package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.behavior.DianLiuguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registry for 雷道（Lei Dao）organ behaviours.
 */
public final class LeiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation DIANLIUGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dianliugu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(DIANLIUGU_ID)
                    .addSlowTickListener(DianLiuguOrganBehavior.INSTANCE)
                    .addOnHitListener(DianLiuguOrganBehavior.INSTANCE)
                    .ensureAttached(DianLiuguOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private LeiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
