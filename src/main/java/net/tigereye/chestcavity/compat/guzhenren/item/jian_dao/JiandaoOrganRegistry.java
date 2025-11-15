package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianDangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianFengGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianQiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianSuoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYinGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.YunJianQingLianGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.LieJianGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianLiaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianmaiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianmuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmuGuTuning;
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
          // 多重剑影蛊：主动技能触发分身召唤/召回（重构自物品模式 2025-11-14）
          OrganIntegrationSpec.builder(
                  ResourceLocation.fromNamespaceAndPath(MOD_ID, "duochongjianying"))
              .addOnHitListener(
                  net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ
                      .DuochongjianyingGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(
                  net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ
                      .DuochongjianyingGuOrganBehavior.INSTANCE)
              .ensureAttached(
                  net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ
                      .DuochongjianyingGuOrganBehavior.INSTANCE::ensureAttached)
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
              .build(),
          // 剑荡蛊：OnHit触发波源 + 慢tick维持态消耗
          OrganIntegrationSpec.builder(JianDangGuOrganBehavior.ORGAN_ID)
              .addOnHitListener(JianDangGuOrganBehavior.INSTANCE)
              .addSlowTickListener(JianDangGuOrganBehavior.INSTANCE)
              .build(),
          // 剑脉蛊：卸载监听器（被动事件通过@EventBusSubscriber自动注册）
          OrganIntegrationSpec.builder(JianmaiGuOrganBehavior.ORGAN_ID)
              .addRemovalListener(JianmaiGuOrganBehavior.INSTANCE)
              .build(),
          // 剑梭蛊（三转）：受击躲避被动
          OrganIntegrationSpec.builder(JianSuoGuOrganBehavior.ORGAN_ID_3)
              .addIncomingDamageListener(JianSuoGuOrganBehavior.INSTANCE)
              .build(),
          // 剑梭蛊（四转）：受击躲避被动
          OrganIntegrationSpec.builder(JianSuoGuOrganBehavior.ORGAN_ID_4)
              .addIncomingDamageListener(JianSuoGuOrganBehavior.INSTANCE)
              .build(),
          // 剑梭蛊（五转）：受击躲避被动
          OrganIntegrationSpec.builder(JianSuoGuOrganBehavior.ORGAN_ID_5)
              .addIncomingDamageListener(JianSuoGuOrganBehavior.INSTANCE)
              .build(),
          // 剑幕蛊：护幕飞剑拦截 + 被动盔甲
          OrganIntegrationSpec.builder(JianmuGuTuning.ORGAN_ID)
              .addSlowTickListener(JianmuGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(JianmuGuOrganBehavior.INSTANCE)
              .ensureAttached(JianmuGuOrganBehavior.INSTANCE::ensureAttached)
              .build(),
          // 剑锋蛊（四转）：锋芒化形主动技 + 高额一击协同 + 剑意共振
          OrganIntegrationSpec.builder(JianFengGuOrganBehavior.ORGAN_ID_FOUR)
              .addOnHitListener(JianFengGuOrganBehavior.INSTANCE)
              .addSlowTickListener(JianFengGuOrganBehavior.INSTANCE)
              .build(),
          // 剑锋蛊（五转）：锋芒化形主动技 + 高额一击协同 + 剑意共振
          OrganIntegrationSpec.builder(JianFengGuOrganBehavior.ORGAN_ID_FIVE)
              .addOnHitListener(JianFengGuOrganBehavior.INSTANCE)
              .addSlowTickListener(JianFengGuOrganBehavior.INSTANCE)
              .build(),
          // 剑气蛊（四转）：一斩开天主动技 + 气断山河被动 + 非玩家OnHit触发
          OrganIntegrationSpec.builder(JianQiGuOrganBehavior.ORGAN_ID)
              .addOnHitListener(JianQiGuOrganBehavior.INSTANCE)
              .addSlowTickListener(JianQiGuOrganBehavior.INSTANCE)
              .ensureAttached(JianQiGuOrganBehavior.INSTANCE::ensureAttached)
              .build());

  private JiandaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
