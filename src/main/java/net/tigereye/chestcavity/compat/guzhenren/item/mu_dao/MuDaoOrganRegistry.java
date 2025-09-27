package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 木道（Mu Dao） organs such as the 镰刀蛊.
 */
public final class MuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation LIANDAO_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "liandaogu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(LIANDAO_GU_ID)
                    .addIncomingDamageListener(LiandaoGuOrganBehavior.INSTANCE)
                    .ensureAttached(LiandaoGuOrganBehavior.INSTANCE::ensureAttached)
                    .build()
    );

    private MuDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
