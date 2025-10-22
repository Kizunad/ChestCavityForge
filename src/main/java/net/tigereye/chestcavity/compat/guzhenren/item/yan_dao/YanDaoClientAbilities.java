package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Ensures Yan Dao attack abilities are exposed to the shared attack hotkey on the client. */
public final class YanDaoClientAbilities {

  private YanDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    // Avoid class-loading behaviour classes on client setup; use literal id
    ResourceLocation huoYiGuId = ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_gu");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(huoYiGuId)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(huoYiGuId);
    }
  }
}
