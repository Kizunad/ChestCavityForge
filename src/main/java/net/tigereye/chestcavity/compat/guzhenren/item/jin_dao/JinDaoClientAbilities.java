package net.tigereye.chestcavity.compat.guzhenren.item.jin_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** 客户端注册金道（铁皮蛊）相关主动技，避免提前加载行为类。 */
public final class JinDaoClientAbilities {

  private JinDaoClientAbilities() {}

  private static final List<ResourceLocation> ABILITIES =
      List.of(
          ResourceLocation.parse("guzhenren:tiepi/hardening"),
          ResourceLocation.parse("guzhenren:tiepi/ironwall"),
          ResourceLocation.parse("guzhenren:tiepi/heavy_blow"),
          ResourceLocation.parse("guzhenren:tiepi/slam_fist"));

  public static void onClientSetup(FMLClientSetupEvent event) {
    for (ResourceLocation ability : ABILITIES) {
      if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ability)) {
        CCKeybindings.ATTACK_ABILITY_LIST.add(ability);
      }
    }
  }
}
