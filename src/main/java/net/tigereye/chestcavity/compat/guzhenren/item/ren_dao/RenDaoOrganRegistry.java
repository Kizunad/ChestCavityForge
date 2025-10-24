package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.BaiYinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ChiTieSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.HuangJinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.QingTongSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ZaijinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** 人道（Ren Dao）器官注册表。 */
public final class RenDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation QING_TONG_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_tong_she_li_gu");
  private static final ResourceLocation CHI_TIE_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "chi_tie_she_li_gu");
  private static final ResourceLocation BAI_YIN_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_yin_she_li_gu");
  private static final ResourceLocation HUANG_JIN_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "huang_jin_she_li_gu");
  private static final ResourceLocation ZI_JIN_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_jin_she_li_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(QING_TONG_SHE_LI_GU_ID)
              .addSlowTickListener(QingTongSheLiGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(CHI_TIE_SHE_LI_GU_ID)
              .addSlowTickListener(ChiTieSheLiGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(BAI_YIN_SHE_LI_GU_ID)
              .addSlowTickListener(BaiYinSheLiGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(BaiYinSheLiGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(HUANG_JIN_SHE_LI_GU_ID)
              .addSlowTickListener(HuangJinSheLiGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(HuangJinSheLiGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(ZI_JIN_SHE_LI_GU_ID)
              .addSlowTickListener(ZaijinSheLiGuOrganBehavior.INSTANCE)
              .build());

  private RenDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
