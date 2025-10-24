package net.tigereye.chestcavity.compat.guzhenren.item.gu_cai;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.behavior.JianjitengOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registry for 蛊材（Gu Cai）organ behaviours. */
public final class GuCaiOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation JIANJITENG_BLOCK_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianjiteng");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(JIANJITENG_BLOCK_ID)
              .addSlowTickListener(JianjitengOrganBehavior.INSTANCE)
              .ensureAttached(JianjitengOrganBehavior.INSTANCE::ensureAttached)
              .build());

  private GuCaiOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
