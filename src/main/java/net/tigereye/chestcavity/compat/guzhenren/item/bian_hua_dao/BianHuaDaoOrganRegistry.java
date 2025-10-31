package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import java.util.List;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive.YinYangZhuanShenGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive.YuLinGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

public final class BianHuaDaoOrganRegistry {

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(ShouPiGuTuning.ORGAN_ID)
              .addSlowTickListener(ShouPiGuOrganBehavior.INSTANCE)
              .addIncomingDamageListener(ShouPiGuOrganBehavior.INSTANCE)
              .addOnHitListener(ShouPiGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(YuLinGuTuning.ORGAN_ID).build(),
          OrganIntegrationSpec.builder(YinYangZhuanShenGuTuning.ORGAN_ID).build());

  private BianHuaDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
