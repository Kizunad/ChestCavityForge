package net.tigereye.chestcavity.compat.guzhenren.item.nu_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.nudao.behavior.YuJunGuHandlers;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** 奴道器官的集成登记入口，目前仅包含御军蛊。 */
public final class NuDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  public static final ResourceLocation YU_JUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_jun_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(OrganIntegrationSpec.builder(YU_JUN_GU_ID).build());

  static {
    // 确保事件处理器类被加载以注册 Forge 事件总线。
    try {
      Class.forName(YuJunGuHandlers.class.getName());
    } catch (ClassNotFoundException ignored) {
      // ignore, Forge event bus订阅器会在其他路径初始化
    }
  }

  private NuDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
