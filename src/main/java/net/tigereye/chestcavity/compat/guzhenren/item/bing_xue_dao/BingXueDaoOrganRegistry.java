package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingBuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.QingReGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ShuangXiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Registry wiring for 冰雪道（Bing Xue Dao） organs. */
public final class BingXueDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation BING_BU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_bu_gu");
  private static final ResourceLocation BING_JI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");
  private static final ResourceLocation SHUANG_XI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuang_xi_gu");
  private static final ResourceLocation QING_RE_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_re_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(BING_BU_GU_ID)
              .addSlowTickListener(BingBuGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(BING_JI_GU_ID)
              .addSlowTickListener(BingJiGuOrganBehavior.INSTANCE)
              .addOnHitListener(BingJiGuOrganBehavior.INSTANCE)
              .addRemovalListener(BingJiGuOrganBehavior.INSTANCE)
              .onEquip(BingJiGuOrganBehavior.INSTANCE::onEquip)
              .build(),
          OrganIntegrationSpec.builder(SHUANG_XI_GU_ID)
              .addSlowTickListener(ShuangXiGuOrganBehavior.INSTANCE)
              .addOnGroundListener(ShuangXiGuOrganBehavior.INSTANCE)
              .addRemovalListener(ShuangXiGuOrganBehavior.INSTANCE)
              .ensureAttached(ShuangXiGuOrganBehavior.INSTANCE::ensureAttached)
              .onEquip(ShuangXiGuOrganBehavior.INSTANCE::onEquip)
              .build(),
          OrganIntegrationSpec.builder(QING_RE_GU_ID)
              .addSlowTickListener(QingReGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(QingReGuOrganBehavior.INSTANCE)
              .build());

  private BingXueDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
