package net.tigereye.chestcavity.compat.guzhenren;

import net.tigereye.chestcavity.compat.common.passive.PassiveBus;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.behavior.YuLinGuPassive;
import net.tigereye.chestcavity.registration.ActivationHookRegistry;

public final class GuzhenrenCompatBootstrap {
  private GuzhenrenCompatBootstrap() {}

  public static void registerBianHuaDaoPassives() {
    if (ActivationHookRegistry.isFamilyEnabled("liupai_bianhuadao")) {
      PassiveBus.register("liupai_bianhuadao", ShouPiGuPassive::new);

      PassiveBus.register("liupai_bianhuadao", () -> YinYangZhuanShenGuPassive.INSTANCE);
      PassiveBus.register("liupai_bianhuadao", YuLinGuPassive::new);
    }
  }

  public static void registerBingXueDaoPassives() {
    if (ActivationHookRegistry.isFamilyEnabled("liupai_bingxuedao")) {
      PassiveBus.register("liupai_bingxuedao", BingJiGuPassive::new);
      PassiveBus.register("liupai_bingxuedao", ShuangXiGuPassive::new);
      PassiveBus.register("liupai_bingxuedao", QingReGuPassive::new);
      PassiveBus.register("liupai_bingxuedao", BingBuGuPassive::new);
    }
  }
}
