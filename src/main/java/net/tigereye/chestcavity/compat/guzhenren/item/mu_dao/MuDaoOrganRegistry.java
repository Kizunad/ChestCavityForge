package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.CaoQunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.JiuYeShengJiCaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.ShengJiYeOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.synergy.SheShengQuYiSynergyBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Registry wiring for 木道（Mu Dao） organs such as the 镰刀蛊. */
public final class MuDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation LIANDAO_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "liandaogu");
  private static final ResourceLocation SHENG_JI_YE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "sheng_ji_xie");
  private static final ResourceLocation JIU_YE_SHENG_JI_CAO_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_xie_sheng_ji_cao");
  private static final ResourceLocation CAO_QUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(LIANDAO_GU_ID)
              .addIncomingDamageListener(LiandaoGuOrganBehavior.INSTANCE)
              .ensureAttached(LiandaoGuOrganBehavior.INSTANCE::ensureAttached)
              .build(),
          OrganIntegrationSpec.builder(SHENG_JI_YE_ID)
              .addSlowTickListener(ShengJiYeOrganBehavior.INSTANCE)
              .addSlowTickListener(SheShengQuYiSynergyBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(JIU_YE_SHENG_JI_CAO_ID)
              .addSlowTickListener(JiuYeShengJiCaoOrganBehavior.INSTANCE)
              .addSlowTickListener(SheShengQuYiSynergyBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(CAO_QUN_GU_ID)
              .addSlowTickListener(CaoQunGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(CaoQunGuOrganBehavior.INSTANCE)
              .build());

  private MuDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
