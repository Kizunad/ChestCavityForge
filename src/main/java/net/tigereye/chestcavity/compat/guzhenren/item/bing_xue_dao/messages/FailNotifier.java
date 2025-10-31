package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages;

import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** 失败原因提示的低噪音节流器。 */
public final class FailNotifier {
  private FailNotifier() {}

  private static final WeakHashMap<ServerPlayer, Long> NEXT = new WeakHashMap<>();
  private static final long COOLDOWN_TICKS = 80L;

  public static void notifyThrottled(ServerPlayer player, Component message) {
    if (player == null || player.level() == null || player.level().isClientSide()) return;
    long now = player.level().getGameTime();
    long next = NEXT.getOrDefault(player, 0L);
    if (now < next) return;
    player.sendSystemMessage(message);
    NEXT.put(player, now + COOLDOWN_TICKS);
  }
}

