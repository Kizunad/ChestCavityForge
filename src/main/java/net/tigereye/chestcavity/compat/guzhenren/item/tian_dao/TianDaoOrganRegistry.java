package net.tigereye.chestcavity.compat.guzhenren.item.tian_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.behavior.ShouGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Registry wiring for 天道·寿蛊系列。 */
public final class TianDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation SHOU_GU_I =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_gu");
  private static final ResourceLocation SHOU_GU_II =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shi_nian_shou_gu");
  private static final ResourceLocation SHOU_GU_III =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bainianshougu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(buildSpec(SHOU_GU_I), buildSpec(SHOU_GU_II), buildSpec(SHOU_GU_III));

  private TianDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }

  private static OrganIntegrationSpec buildSpec(ResourceLocation organId) {
    return OrganIntegrationSpec.builder(organId)
        .addSlowTickListener(ShouGuOrganBehavior.INSTANCE)
        .addIncomingDamageListener(ShouGuOrganBehavior.INSTANCE)
        .addOnHitListener(ShouGuOrganBehavior.INSTANCE)
        .addRemovalListener(ShouGuOrganBehavior.INSTANCE)
        .onEquip(ShouGuOrganBehavior.INSTANCE::onEquip)
        .build();
  }
}
