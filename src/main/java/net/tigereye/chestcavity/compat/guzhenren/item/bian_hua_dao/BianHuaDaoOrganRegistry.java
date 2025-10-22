package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registration for 变化道器官：兽皮蛊。 */
public final class BianHuaDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation SHOU_PI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(SHOU_PI_GU_ID)
              .addIncomingDamageListener(ShouPiGuOrganBehavior.INSTANCE)
              .addSlowTickListener(ShouPiGuOrganBehavior.INSTANCE)
              .addOnHitListener(ShouPiGuOrganBehavior.INSTANCE)
              .build());

  private BianHuaDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
