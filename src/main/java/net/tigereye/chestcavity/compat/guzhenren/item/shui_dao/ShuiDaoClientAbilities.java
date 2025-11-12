package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 水道客户端能力注册。
 *
 * <p>当水道器官有主动技能时,在此处注册按键绑定。
 */
public final class ShuiDaoClientAbilities {

  private ShuiDaoClientAbilities() {}

  /**
   * 客户端初始化时调用。
   *
   * @param event 客户端初始化事件
   */
  public static void onClientSetup(FMLClientSetupEvent event) {
    // 水道暂无主动技能需要注册
    // 未来如果添加主动技能,在此处注册到 CCKeybindings.ATTACK_ABILITY_LIST
  }
}
