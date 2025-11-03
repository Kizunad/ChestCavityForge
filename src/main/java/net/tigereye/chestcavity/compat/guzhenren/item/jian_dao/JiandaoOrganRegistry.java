package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.YunJianQingLianGuOrganBehavior;

/** Declarative registry for sword-path organs. */
public final class JiandaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation JIAN_YING_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(JIAN_YING_GU_ID)
              .addOnHitListener(JianYingGuOrganBehavior.INSTANCE)
              .ensureAttached(JianYingGuOrganBehavior.INSTANCE::ensureAttached)
              .build(),
          // 蕴剑青莲蛊：慢tick + 受击护体（玩家路径生效，非玩家仅慢tick自动化）
          OrganIntegrationSpec.builder(YunJianQingLianGuOrganBehavior.ORGAN_ID)
              .addSlowTickListener(YunJianQingLianGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(YunJianQingLianGuOrganBehavior.INSTANCE)
              .build(),
          // 剑心蛊（体质）集成：慢tick + 受击打断
          OrganIntegrationSpec.builder(
                  net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_xin_gu"))
              .addSlowTickListener(
                  net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ
                      .JianXinGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(
                  net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ
                      .JianXinGuOrganBehavior.INSTANCE)
              .build());

  private JiandaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
