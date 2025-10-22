package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior.QingFengLunOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registry for风道器官。 */
public final class FengDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation QING_FENG_LUN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_feng_lun_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(QING_FENG_LUN_ID)
              .addSlowTickListener(QingFengLunOrganBehavior.INSTANCE)
              .addIncomingDamageListener(QingFengLunOrganBehavior.INSTANCE)
              .addOnHitListener(QingFengLunOrganBehavior.INSTANCE)
              .addRemovalListener(QingFengLunOrganBehavior.INSTANCE)
              .ensureAttached(QingFengLunOrganBehavior.INSTANCE::ensureAttached)
              .onEquip(QingFengLunOrganBehavior.INSTANCE::onEquip)
              .build());

  private FengDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
