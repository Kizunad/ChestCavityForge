package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Client-side ability registration for 力道（三转） organs. */
public final class LiDaoClientAbilities {

  private LiDaoClientAbilities() {}

  /** 按字面 ResourceLocation 注册所有力道相关主动技，避免提前加载行为类导致客户端初始化问题。 */
  private static final List<ResourceLocation> REGISTERED_ABILITIES =
      List.of(
          ResourceLocation.parse("guzhenren:long_wan_qu_qu_gu"),
          ResourceLocation.parse("guzhenren:zi_li_geng_sheng_gu_3"),
          ResourceLocation.parse("guzhenren:huang_luo_tian_niu_gu"),
          ResourceLocation.parse("guzhenren:xiong_hao_burst"),
          ResourceLocation.parse("guzhenren:xiong_hao_slam"),
          ResourceLocation.parse("guzhenren:xiong_hao_roar"),
          ResourceLocation.parse("guzhenren:hua_shi_gu/charge"),
          ResourceLocation.parse("guzhenren:hua_shi_gu/hoofquake"),
          ResourceLocation.parse("guzhenren:hua_shi_gu/overload_burst"),
          ResourceLocation.parse("guzhenren:man_li_tian_niu_gu/boost"),
          ResourceLocation.parse("guzhenren:man_li_tian_niu_gu/rush"));

  public static void onClientSetup(FMLClientSetupEvent event) {
    for (ResourceLocation abilityId : REGISTERED_ABILITIES) {
      if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(abilityId)) {
        CCKeybindings.ATTACK_ABILITY_LIST.add(abilityId);
      }
    }
  }
}
