package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.JiezeguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.LingXianguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.QuanYongMingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.ShuiTiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Registry wiring for 水道（Shui Dao） organs.
 */
public final class ShuiDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation LING_XIAN_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ling_xian_gu");
    private static final ResourceLocation SHUI_TI_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shui_ti_gu");
    private static final ResourceLocation JIEZE_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiezegu");
    private static final ResourceLocation QUAN_YONG_MING_GU_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_yong_ming_gu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(LING_XIAN_GU_ID)
                    .addSlowTickListener(LingXianguOrganBehavior.INSTANCE)
                    .ensureAttached(LingXianguOrganBehavior.INSTANCE::ensureAttached)
                    .build(),
            OrganIntegrationSpec.builder(SHUI_TI_GU_ID)
                    .addSlowTickListener(ShuiTiGuOrganBehavior.INSTANCE)
                    .addRemovalListener(ShuiTiGuOrganBehavior.INSTANCE)
                    .ensureAttached(ShuiTiGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(ShuiTiGuOrganBehavior.INSTANCE::onEquip)
                    .build(),
            OrganIntegrationSpec.builder(JIEZE_GU_ID)
                    .addSlowTickListener(JiezeguOrganBehavior.INSTANCE)
                    .addOnHitListener(JiezeguOrganBehavior.INSTANCE)
                    .ensureAttached(JiezeguOrganBehavior.INSTANCE::ensureAttached)
            OrganIntegrationSpec.builder(QUAN_YONG_MING_GU_ID)
                    .addSlowTickListener(QuanYongMingGuOrganBehavior.INSTANCE)
                    .addRemovalListener(QuanYongMingGuOrganBehavior.INSTANCE)
                    .ensureAttached(QuanYongMingGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(QuanYongMingGuOrganBehavior.INSTANCE::onEquip)
                    .build()
    );

    private ShuiDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
