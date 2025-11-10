package net.tigereye.chestcavity.compat.guzhenren.flyingsword;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui.TUITheme;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianQiaoGuOrganBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * 飞剑事件处理器
 *
 * <p>处理飞剑相关的游戏事件：
 *
 * <ul>
 *   <li>玩家登录时提示恢复召回的飞剑
 *   <li>玩家维度切换时的飞剑管理
 * </ul>
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class FlyingSwordEventHandler {

  private FlyingSwordEventHandler() {}

  // 注册默认计算钩子（类加载时一次）
  static {
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.hooks.DefaultHooks
        .registerDefaults();
  }

  /** 玩家登录时检查并提示恢复飞剑 */
  @SubscribeEvent
  public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }

    ServerLevel level = player.serverLevel();

    // 延迟检查，确保玩家完全加载
    level
        .getServer()
        .execute(
            () -> {
              checkAndNotifyRecalledSwords(player);
            });
  }

  /** 玩家退出时：将在场且可召回的飞剑保存到存储，防止“退出前正在返回途中”导致丢失。 */
  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }

    ServerLevel level = player.serverLevel();

    var swords = FlyingSwordController.getPlayerSwords(level, player);
    if (swords.isEmpty()) {
      return;
    }

    var storage = CCAttachments.getFlyingSwordStorage(player);
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    int capacity = JianQiaoGuOrganBehavior.computeStorageCapacity(player, cc);
    int stored = 0;

    for (var sword : swords) {
      // 仅处理允许召回的飞剑；主动技能生成的临时飞剑跳过（与在线时一致的逻辑）
      if (!sword.isRecallable()) {
        // 仍通知钩子“主人离线”并移除
        if (level != null) {
          var ctx =
              new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                  .DespawnContext(
                  sword,
                  level,
                  player,
                  net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                      .DespawnContext.Reason.OWNER_GONE,
                  null);
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventRegistry
              .fireDespawnOrRecall(ctx);
        }
        sword.discard();
        continue;
      }

      // 写入存储并触发钩子（OWNER_GONE）。这里不走动画，直接落盘保证可靠性。
      JianQiaoGuOrganBehavior.handleRecallRepair(player, cc, sword);
      boolean ok = storage.recallSword(sword, capacity);
      if (ok) {
        stored++;
      }

      if (level != null) {
        var ctx =
            new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.DespawnContext(
                sword,
                level,
                player,
                net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.DespawnContext
                    .Reason.OWNER_GONE,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventRegistry
            .fireDespawnOrRecall(ctx);
      }

      sword.discard();
    }

    if (stored > 0) {
      // 显式触发写回，确保在随后的玩家保存流程中带上最新数据
      player.setData(CCAttachments.FLYING_SWORD_STORAGE.get(), storage);
      ChestCavity.LOGGER.info(
          "[FlyingSword] Stored {} sword(s) for player {} on logout",
          stored,
          player.getGameProfile().getName());
    }

    SwordCommandCenter.clear(player);
  }

  /** 检查并通知玩家有召回的飞剑 */
  private static void checkAndNotifyRecalledSwords(ServerPlayer player) {
    FlyingSwordStorage storage = CCAttachments.getFlyingSwordStorage(player);
    int count = storage.getCount();

    if (count == 0) {
      return;
    }

    // 创建可点击的恢复消息
    Component message =
        Component.literal("[飞剑系统] ")
            .withStyle(Style.EMPTY.withColor(TUITheme.ACCENT.getColor()))
            .append(
                Component.literal(
                        String.format("你有 %d 个召回的飞剑。点击此处恢复或使用 /flyingsword restore", count))
                    .withStyle(
                        Style.EMPTY
                            .withColor(TUITheme.VALUE.getColor())
                            .withClickEvent(
                                new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND, "/flyingsword restore"))
                            .withHoverEvent(
                                new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("点击恢复所有召回的飞剑")))));

    player.sendSystemMessage(message);

    ChestCavity.LOGGER.info(
        "[FlyingSword] Player {} logged in with {} recalled swords",
        player.getGameProfile().getName(),
        count);
  }

  /** 玩家维度切换时的处理（可选功能） */
  @SubscribeEvent
  public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }

    ServerLevel level = player.serverLevel();

    // 检查玩家的飞剑是否跟随到新维度
    // 如果飞剑在旧维度，给出提示
    int swordCount = FlyingSwordController.getSwordCount(level, player);
    if (swordCount > 0) {
      ChestCavity.LOGGER.debug(
          "[FlyingSword] Player {} changed dimension with {} active swords",
          player.getGameProfile().getName(),
          swordCount);
    }
  }
}
