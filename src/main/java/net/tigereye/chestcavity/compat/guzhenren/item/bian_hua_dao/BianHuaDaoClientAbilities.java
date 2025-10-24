package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuQunSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuShiSummonSharkSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuYueSkill;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** 客户端注册鱼鳞蛊相关主动技能的热键提示。 */
public final class BianHuaDaoClientAbilities {

  private BianHuaDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    register(YuQunSkill.ABILITY_ID);
    register(YuYueSkill.ABILITY_ID);
    register(YuShiSummonSharkSkill.ABILITY_ID);
  }

  private static void register(net.minecraft.resources.ResourceLocation id) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(id)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(id);
    }
  }
}
