package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYinGuOrganBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * 剑引蛊被动桥接占位：监听玩家 Tick，用于后续的引剑逻辑。
 *
 * <p>当前仅在服务端 Phase.END 时触发，占位调用行为层。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class JianYinGuPassiveEvents {

  private JianYinGuPassiveEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (event == null) {
      return;
    }
    Player player = event.getEntity();
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }
    if (player.level().isClientSide()) {
      return;
    }

    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc ->
                JianYinGuOrganBehavior.INSTANCE.handlePassiveBridge(
                    serverPlayer, cc, player.level().getGameTime()));
  }
}
