package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.HunDaoSoulBeastBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 魂道（Hun Dao） organs.
 */
public final class HunDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation XIAO_HUN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiao_hun_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(XIAO_HUN_GU_ID)
                    .addSlowTickListener(HunDaoSoulBeastBehavior.INSTANCE)
                    .addOnHitListener(HunDaoSoulBeastBehavior.INSTANCE)
                    .addRemovalListener(HunDaoSoulBeastBehavior.INSTANCE)
                    .ensureAttached(HunDaoSoulBeastBehavior.INSTANCE::ensureAttached)
                    .onEquip(HunDaoSoulBeastBehavior.INSTANCE::onEquip)
                    .build()
    );

    private HunDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
