package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registration entry for 兽皮蛊。负责将核心行为挂载到胸腔事件流水线， 遵循其它道系器官的懒加载模式，避免在类初始化阶段触发崩溃。 */
public final class ShouPiGuOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation SHOU_PI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu");

  private ShouPiGuOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    List<OrganIntegrationSpec> list = new ArrayList<>();
    try {
      list.add(
          OrganIntegrationSpec.builder(SHOU_PI_GU_ID)
              .addIncomingDamageListener(ShouPiGuOrganBehavior.INSTANCE)
              .addSlowTickListener(ShouPiGuOrganBehavior.INSTANCE)
              .addOnHitListener(ShouPiGuOrganBehavior.INSTANCE)
              .addRemovalListener(ShouPiGuOrganBehavior.INSTANCE)
              .build());
    } catch (Throwable t) {
      ChestCavity.LOGGER.warn(
          "[compat/guzhenren][bian_hua_dao] skip ShouPiGu registration due to init error", t);
    }
    return list;
  }
}
