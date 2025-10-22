package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior.ShuishenguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registration for the Wu Hang (五行蛊) organ behaviours. */
public final class WuHangOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation TUPIGU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu");
  private static final ResourceLocation MUGANGU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "mugangu");
  private static final ResourceLocation JINFEIGU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu");
  private static final ResourceLocation SHUISHENGU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(TUPIGU_ID)
              .addOnGroundListener(TupiguOrganBehavior.INSTANCE)
              .addSlowTickListener(TupiguOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(MUGANGU_ID)
              .addSlowTickListener(MuganguOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(JINFEIGU_ID)
              .addSlowTickListener(JinfeiguOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(SHUISHENGU_ID)
              .addSlowTickListener(ShuishenguOrganBehavior.INSTANCE)
              .addIncomingDamageListener(ShuishenguOrganBehavior.INSTANCE)
              .build());

  private WuHangOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
