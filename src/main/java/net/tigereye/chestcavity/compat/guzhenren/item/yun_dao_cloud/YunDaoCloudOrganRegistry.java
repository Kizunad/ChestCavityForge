package net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.BaiYunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.YinYunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registry for 云道器官相关胸腔效果。 */
public final class YunDaoCloudOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation BAI_YUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_yun_gu");
  private static final ResourceLocation YIN_YUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yun_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(BAI_YUN_GU_ID)
              // TODO: 后续补充白云蛊的 OrganScore 映射（肌肉等）以保持与数据包一致
              .addSlowTickListener(BaiYunGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(BaiYunGuOrganBehavior.INSTANCE)
              .addRemovalListener(BaiYunGuOrganBehavior.INSTANCE)
              .ensureAttached(BaiYunGuOrganBehavior.INSTANCE::ensureAttached)
              .build(),
          OrganIntegrationSpec.builder(YIN_YUN_GU_ID)
              .addSlowTickListener(YinYunGuOrganBehavior.INSTANCE)
              .addOnHitListener(YinYunGuOrganBehavior.INSTANCE)
              .build());

  private YunDaoCloudOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
