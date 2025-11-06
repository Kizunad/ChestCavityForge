package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYinGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.YunJianQingLianGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.LieJianGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianLiaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

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
          // 剑引蛊：占位行为（慢tick + 命中桥接）
          OrganIntegrationSpec.builder(JianYinGuTuning.ORGAN_ID)
              .addSlowTickListener(JianYinGuOrganBehavior.INSTANCE)
              .addOnHitListener(JianYinGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(JianYinGuOrganBehavior.INSTANCE)
              .ensureAttached(JianYinGuOrganBehavior.INSTANCE::ensureAttached)
              .build(),
          // 裂剑蛊：被动近战触发微型裂隙
          OrganIntegrationSpec.builder(LieJianGuOrganBehavior.ORGAN_ID)
              .addOnHitListener(LieJianGuOrganBehavior.INSTANCE)
              .addSlowTickListener(LieJianGuOrganBehavior.INSTANCE)
              .build(),
          // 蕴剑青莲蛊：慢tick + 受击护体（玩家路径生效，非玩家仅慢tick自动化）
          OrganIntegrationSpec.builder(YunJianQingLianGuOrganBehavior.ORGAN_ID)
              .addSlowTickListener(YunJianQingLianGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(YunJianQingLianGuOrganBehavior.INSTANCE)
              .build(),
          // 剑疗蛊：慢tick心跳治疗 + 飞剑互补修复；主动技：剑血互济
          OrganIntegrationSpec.builder(JianLiaoGuOrganBehavior.ORGAN_ID)
              .addSlowTickListener(JianLiaoGuOrganBehavior.INSTANCE)
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
