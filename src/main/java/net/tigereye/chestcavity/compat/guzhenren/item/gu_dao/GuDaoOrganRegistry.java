package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuzhuguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.HuGuguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.RouBaiguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.YuGuguOrganBehavior; // 你需要自己写对应行为
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;

/**
 * Declarative registry for 骨道蛊 items. Each behaviour is registered through the
 * {@link GuzhenrenLinkageEffectRegistry} so that listener wiring stays consistent.
 */
public final class GuDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation BONE_BAMBOO_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_zhu_gu");
    private static final ResourceLocation BONE_SPEAR_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_qiang_gu");
    private static final ResourceLocation SPIRAL_BONE_SPEAR_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "luo_xuan_gu_qiang_gu");
    private static final ResourceLocation TIGER_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hugugu");
    private static final ResourceLocation JADE_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu"); // 新增玉骨蛊
    private static final ResourceLocation ROU_BAI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "rou_bai_gu");
    private static final List<OrganIntegrationSpec> SPECS;

    static {
        GuDaoOrganEvents.register();
        SPECS = List.of(
                OrganIntegrationSpec.builder(BONE_BAMBOO_ID)
                        .addSlowTickListener(GuzhuguOrganBehavior.INSTANCE)
                        .ensureAttached(GuzhuguOrganBehavior.INSTANCE::ensureAttached)
                        .build(),
                OrganIntegrationSpec.builder(BONE_SPEAR_ID)
                        .addSlowTickListener(GuQiangguOrganBehavior.INSTANCE)
                        .addOnHitListener(GuQiangguOrganBehavior.INSTANCE)
                        .ensureAttached(GuQiangguOrganBehavior.INSTANCE::ensureAttached)
                        .build(),
                OrganIntegrationSpec.builder(SPIRAL_BONE_SPEAR_ID)
                        .addSlowTickListener(LuoXuanGuQiangguOrganBehavior.INSTANCE)
                        .addOnHitListener(LuoXuanGuQiangguOrganBehavior.INSTANCE)
                        .addIncomingDamageListener(LuoXuanGuQiangguOrganBehavior.INSTANCE)
                        .ensureAttached(LuoXuanGuQiangguOrganBehavior.INSTANCE::ensureAttached)
                        .build(),
                OrganIntegrationSpec.builder(TIGER_BONE_ID)
                        .addSlowTickListener(HuGuguOrganBehavior.INSTANCE)
                        .addIncomingDamageListener(HuGuguOrganBehavior.INSTANCE)
                        .ensureAttached(HuGuguOrganBehavior.INSTANCE::ensureAttached)
                        .build(),
                OrganIntegrationSpec.builder(JADE_BONE_ID)
                        .addSlowTickListener(YuGuguOrganBehavior.INSTANCE)
                        .ensureAttached(YuGuguOrganBehavior.INSTANCE::ensureAttached)
                        .onEquip(YuGuguOrganBehavior.INSTANCE::onEquip)
                        .build(),
                OrganIntegrationSpec.builder(ROU_BAI_GU_ID)
                        .addSlowTickListener(RouBaiguOrganBehavior.INSTANCE)
                        .addOnHitListener(RouBaiguOrganBehavior.INSTANCE)
                        .ensureAttached(RouBaiguOrganBehavior.INSTANCE::ensureAttached)
                        .build()
        );
    }

    private GuDaoOrganRegistry() {
    }

    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }
}
