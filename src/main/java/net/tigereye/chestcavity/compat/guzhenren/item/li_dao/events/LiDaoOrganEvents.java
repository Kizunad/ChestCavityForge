package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.events;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuaShiGuOrganBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * Registers 力道系列的玩家交互事件，目前仅用于花豕蛊 × 铁手擒拿蛊协同的「副手短按定身」判定。
 *
 * <p>监听 {@link PlayerInteractEvent.RightClickItem} 与 {@link PlayerInteractEvent.RightClickEmpty}
 * 以捕捉 0.8 秒抓取窗口内的副手点击。事件只在服务端真正尝试定身，避免客户端重复调用。
 */
public final class LiDaoOrganEvents {

  private static boolean registered;

  private LiDaoOrganEvents() {}

  /** 注册一次性事件监听，避免重复挂载导致的性能问题或判定多次触发。 */
  public static void register() {
    if (registered) {
      return;
    }
    registered = true;
    NeoForge.EVENT_BUS.addListener(LiDaoOrganEvents::onRightClickItem);
    NeoForge.EVENT_BUS.addListener(LiDaoOrganEvents::onRightClickEmpty);
  }

  private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
    if (event.isCanceled()) {
      return;
    }
    handleOffhandTap(event.getEntity(), event.getHand());
  }

  private static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
    handleOffhandTap(event.getEntity(), event.getHand());
  }

  /** 当玩家副手短按时，尝试触发花豕蛊的铁手擒拿协同。若玩家不满足条件则静默返回。 */
  private static void handleOffhandTap(Player player, InteractionHand hand) {
    if (player == null || hand != InteractionHand.OFF_HAND) {
      return;
    }
    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(cc -> HuaShiGuOrganBehavior.INSTANCE.tryTriggerGrabFollowUp(player, cc, hand));
  }
}
