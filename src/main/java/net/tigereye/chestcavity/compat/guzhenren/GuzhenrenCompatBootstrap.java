package net.tigereye.chestcavity.compat.guzhenren;

import net.tigereye.chestcavity.compat.common.passive.PassiveBus;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YinYangZhuanShenGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuPassive;
import net.tigereye.chestcavity.registration.ActivationHookRegistry;

public final class GuzhenrenCompatBootstrap {
  private GuzhenrenCompatBootstrap() {}

  public static void registerBianHuaDaoPassives() {
    if (ActivationHookRegistry.isFamilyEnabled("liupai_bianhuadao")) {
      PassiveBus.register("liupai_bianhuadao", ShouPiGuPassive::new);
      PassiveBus.register("liupai_bianhuadao", YinYangZhuanShenGuPassive::new);
      PassiveBus.register("liupai_bianhuadao", YuLinGuPassive::new);
    }
  }
}
