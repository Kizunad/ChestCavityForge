package net.tigereye.chestcavity.compat.guzhenren.item.yue_dao;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.yue_dao.behavior.MoonlightGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative注册：月道器官（月光蛊）。 */
public final class YueDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation MOONLIGHT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yue_guang_gu");

  private YueDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    List<OrganIntegrationSpec> list = new ArrayList<>();
    try {
      list.add(
          OrganIntegrationSpec.builder(MOONLIGHT_ID)
              .addSlowTickListener(MoonlightGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(MoonlightGuOrganBehavior.INSTANCE)
              .addOnHitListener(MoonlightGuOrganBehavior.INSTANCE)
              .addRemovalListener(MoonlightGuOrganBehavior.INSTANCE)
              .ensureAttached(MoonlightGuOrganBehavior.INSTANCE::ensureAttached)
              .onEquip(MoonlightGuOrganBehavior.INSTANCE::onEquip)
              .build());
    } catch (Throwable t) {
      ChestCavity.LOGGER.warn(
          "[compat/guzhenren][yue_dao] skip MoonlightGu registration due to init error", t);
    }
    return list;
  }
}
