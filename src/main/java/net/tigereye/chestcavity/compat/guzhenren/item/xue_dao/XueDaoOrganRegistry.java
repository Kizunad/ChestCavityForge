package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.TiexueguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieFeiguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XiediguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieyanguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 血道（Xue Dao） organs.
 */
public final class XueDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation TIE_XUE_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiexuegu");

    private static final ResourceLocation XUE_FEI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_fei_gu");

    private static final ResourceLocation XIE_DI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_di_gu");


    private static final ResourceLocation XIE_YAN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_yan_gu");



    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(TIE_XUE_GU_ID)
                    .addSlowTickListener(TiexueguOrganBehavior.INSTANCE)
                    .addRemovalListener(TiexueguOrganBehavior.INSTANCE)
                    .ensureAttached(TiexueguOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(TiexueguOrganBehavior.INSTANCE::onEquip)
                    .build(),
            OrganIntegrationSpec.builder(XUE_FEI_GU_ID)
                    .addSlowTickListener(XieFeiguOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(XieFeiguOrganBehavior.INSTANCE)
                    .addRemovalListener(XieFeiguOrganBehavior.INSTANCE)
                    .ensureAttached(XieFeiguOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(XieFeiguOrganBehavior.INSTANCE::onEquip)
                    .build(),
            OrganIntegrationSpec.builder(XIE_YAN_GU_ID)
                    .addSlowTickListener(XieyanguOrganBehavior.INSTANCE)
                    .addOnHitListener(XieyanguOrganBehavior.INSTANCE)
                    .addRemovalListener(XieyanguOrganBehavior.INSTANCE)
                    .ensureAttached(XieyanguOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(XieyanguOrganBehavior.INSTANCE::onEquip)
                    .build(),
            OrganIntegrationSpec.builder(XIE_DI_GU_ID)
                    .addSlowTickListener(XiediguOrganBehavior.INSTANCE)
                    .addIncomingDamageListener(XiediguOrganBehavior.INSTANCE)
                    .addRemovalListener(XiediguOrganBehavior.INSTANCE)
                    .ensureAttached(XiediguOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(XiediguOrganBehavior.INSTANCE::onEquip)
                    .build()
    );

    private XueDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
