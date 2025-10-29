package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload;

/**
 * Small helpers around cooldown/countdown completions.
 *
 * <p>Note: This is a thin wrapper that currently leverages TickOps.schedule under the hood to
 * invoke completion exactly at the requested tick offset. For EntryInt-based countdowns, prefer
 * attaching onChange(prev>0 && curr==0) to avoid extra scheduling.
 */
public final class CountdownOps {
  private CountdownOps() {}

  /** Schedule a ready-toast after {@code ticks} for the given player. */
  public static void scheduleToast(
      ServerLevel level,
      ServerPlayer player,
      int ticks,
      ItemStack icon,
      String title,
      String subtitle) {
    if (level == null || player == null || ticks <= 0) {
      return;
    }
    TickOps.schedule(level, () -> sendToast(player, icon, title, subtitle), ticks);
  }

  /** Schedule a ready-toast after {@code ticks} using a specific Item as icon. */
  public static void scheduleToast(
      ServerLevel level, ServerPlayer player, int ticks, Item icon, String title, String subtitle) {
    if (icon == null) {
      scheduleToast(level, player, ticks, ItemStack.EMPTY, title, subtitle);
    } else {
      scheduleToast(level, player, ticks, new ItemStack(icon), title, subtitle);
    }
  }

  /** Schedule a ready-toast at {@code readyAtTick}, given current {@code now}. */
  public static void scheduleToastAt(
      ServerLevel level,
      ServerPlayer player,
      long readyAtTick,
      long now,
      ItemStack icon,
      String title,
      String subtitle) {
    if (level == null || player == null) return;
    long delta = Math.max(0L, readyAtTick - now);
    int ticks = delta > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) delta;
    scheduleToast(level, player, ticks, icon, title, subtitle);
  }

  private static void sendToast(
      ServerPlayer player, ItemStack icon, String title, String subtitle) {
    var id =
        icon.isEmpty()
            ? ResourceLocation.parse("minecraft:air")
            : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(icon.getItem());
    CooldownReadyToastPayload payload = new CooldownReadyToastPayload(true, id, title, subtitle);
    NetworkHandler.sendCooldownToast(player, payload);
  }
}
