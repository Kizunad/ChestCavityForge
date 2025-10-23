package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuaShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuangLuoTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.LongWanQuQuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ManLiTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.XiongHaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZiLiGengShengGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Client-side ability registration for 力道（三转） organs. */
public final class LiDaoClientAbilities {

  private LiDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(LongWanQuQuGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(LongWanQuQuGuOrganBehavior.ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ZiLiGengShengGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(ZiLiGengShengGuOrganBehavior.ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(HuangLuoTianNiuGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(HuangLuoTianNiuGuOrganBehavior.ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(XiongHaoGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(XiongHaoGuOrganBehavior.ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ManLiTianNiuGuOrganBehavior.BOOST_ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(ManLiTianNiuGuOrganBehavior.BOOST_ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ManLiTianNiuGuOrganBehavior.RUSH_ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(ManLiTianNiuGuOrganBehavior.RUSH_ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(HuaShiGuOrganBehavior.CHARGE_ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(HuaShiGuOrganBehavior.CHARGE_ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(HuaShiGuOrganBehavior.HOOFQUAKE_ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(HuaShiGuOrganBehavior.HOOFQUAKE_ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(HuaShiGuOrganBehavior.OVERLOAD_ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(HuaShiGuOrganBehavior.OVERLOAD_ABILITY_ID);
    }
  }
}
