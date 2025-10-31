package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.YuLinGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.YinYangZhuanShenGuOrganBehavior;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.common.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.common.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** 变化道器官注册（与冰雪道同模式）：声明器官与对应监听。 */
public final class BianHuaDaoOrganRegistry {

  private static final ResourceLocation YU_LIN_GU_ID = YuLinGuTuning.ORGAN_ID;
  private static final ResourceLocation YIN_YANG_ID = YinYangZhuanShenGuTuning.ORGAN_ID;
  private static final ResourceLocation SHOU_PI_ID = ShouPiGuTuning.ORGAN_ID;

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          // 鱼鳞蛊：以 slowTick 维护召唤周期
          OrganIntegrationSpec.builder(YU_LIN_GU_ID)
              .addSlowTickListener(YuLinGuOrganBehavior.INSTANCE)
              .build(),
          // 阴阳转身蛊：以 slowTick 承接被动（模式属性与锚点维护）
          OrganIntegrationSpec.builder(YIN_YANG_ID)
              .addSlowTickListener(YinYangZhuanShenGuOrganBehavior.INSTANCE)
              .build()
          // 兽皮蛊：组合技/被动迁移中，暂不注册 organ 监听（保留占位）
          );

  private BianHuaDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
