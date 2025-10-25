package net.tigereye.chestcavity.compat.guzhenren.item.jin_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jin_dao.behavior.TiePiGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** 金道器官（暂仅铁皮蛊）监听注册。 */
public final class TieDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation TIE_PI_GU_ID =
    ResourceLocation.fromNamespaceAndPath(MOD_ID, "t_tie_pi_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(TIE_PI_GU_ID)
              .addSlowTickListener(TiePiGuBehavior.INSTANCE)
              .addOnHitListener(TiePiGuBehavior.INSTANCE)
              .addIncomingDamageListener(TiePiGuBehavior.INSTANCE)
              .addRemovalListener(TiePiGuBehavior.INSTANCE)
              .ensureAttached(TiePiGuBehavior.INSTANCE::ensureAttached)
              .build());

  private TieDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
