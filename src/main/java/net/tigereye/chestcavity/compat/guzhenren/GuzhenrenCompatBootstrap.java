package net.tigereye.chestcavity.compat.guzhenren;

import net.tigereye.chestcavity.compat.common.passive.PassiveBus;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuQunSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuShiSummonSharkSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuYueSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive.YinYangZhuanShenGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive.YuLinGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingBuGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.QingReGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ShuangXiGuPassive;
import net.tigereye.chestcavity.registration.ActivationHookRegistry;

public final class GuzhenrenCompatBootstrap {
  private GuzhenrenCompatBootstrap() {}

  public static void registerBianHuaDaoPassives() {
    if (ActivationHookRegistry.isFamilyEnabled("liupai_bianhuadao")) {
      PassiveBus.register("liupai_bianhuadao", ShouPiGuPassive::new);

      PassiveBus.register("liupai_bianhuadao", () -> net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive.YinYangZhuanShenGuPassive.INSTANCE);
      PassiveBus.register("liupai_bianhuadao", YuLinGuPassive::new);
      // 触发 enum Active 类加载以完成静态注册
      net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active.YuYueActive.INSTANCE.name();
      net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active.YuQunActive.INSTANCE.name();
      net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active.YuShiSummonSharkActive.INSTANCE.name();
      net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active.YinYangZhuanShenGuActive.INSTANCE.name();
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
