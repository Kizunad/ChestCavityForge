package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui.HunDaoNotificationRenderer.NotificationCategory;

/**
 * Helper class for sending Hun Dao network packets from server to clients.
 *
 * <p>Provides convenient methods to sync HUD/FX state without directly depending on packet
 * classes.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public final class HunDaoNetworkHelper {

  private HunDaoNetworkHelper() {}

  /**
   * Syncs soul flame DoT state to all nearby players.
   *
   * @param entity the entity with soul flame
   * @param stacks soul flame stack count
   * @param durationTicks remaining duration in ticks
   */
  public static void syncSoulFlame(LivingEntity entity, int stacks, int durationTicks) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }

    SoulFlameSyncPayload payload =
        new SoulFlameSyncPayload(entity.getUUID(), stacks, durationTicks);

    // Send to all tracking players (within render distance)
    entity
        .level()
        .players()
        .stream()
        .filter(p -> p instanceof ServerPlayer)
        .map(p -> (ServerPlayer) p)
        .filter(p -> p.distanceToSqr(entity) < 64 * 64) // 64 block radius
        .forEach(p -> p.connection.send(payload));
  }

  /**
   * Syncs soul beast transformation state to a specific player.
   *
   * @param player the player
   * @param active whether soul beast is active
   * @param durationTicks remaining duration in ticks
   */
  public static void syncSoulBeast(Player player, boolean active, int durationTicks) {
    if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer)) {
      return;
    }

    SoulBeastSyncPayload payload =
        new SoulBeastSyncPayload(player.getUUID(), active, durationTicks);
    ((ServerPlayer) player).connection.send(payload);
  }

  /**
   * Syncs hun po resource to a specific player.
   *
   * @param player the player
   * @param current current hun po value
   * @param max maximum hun po value
   */
  public static void syncHunPo(Player player, double current, double max) {
    if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer)) {
      return;
    }

    HunPoSyncPayload payload = new HunPoSyncPayload(player.getUUID(), current, max);
    ((ServerPlayer) player).connection.send(payload);
  }

  /**
   * Syncs gui wu (ghost mist) state to a specific player.
   *
   * @param player the player
   * @param active whether gui wu is active
   * @param durationTicks remaining duration in ticks
   */
  public static void syncGuiWu(Player player, boolean active, int durationTicks) {
    if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer)) {
      return;
    }

    GuiWuSyncPayload payload = new GuiWuSyncPayload(player.getUUID(), active, durationTicks);
    ((ServerPlayer) player).connection.send(payload);
  }

  /**
   * Clears Hun Dao state for a specific entity on all clients.
   *
   * @param entity the entity to clear
   */
  public static void clearEntity(LivingEntity entity) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }

    HunDaoClearEntityPayload payload = new HunDaoClearEntityPayload(entity.getUUID());

    // Send to all nearby players
    entity
        .level()
        .players()
        .stream()
        .filter(p -> p instanceof ServerPlayer)
        .map(p -> (ServerPlayer) p)
        .filter(p -> p.distanceToSqr(entity) < 128 * 128)
        .forEach(p -> p.connection.send(payload));
  }

  /**
   * Sends a notification to a specific player.
   *
   * @param player the player
   * @param message the notification message
   * @param category the notification category
   */
  public static void sendNotification(
      Player player, Component message, NotificationCategory category) {
    if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer)) {
      return;
    }

    HunDaoNotificationPayload payload = new HunDaoNotificationPayload(message, category);
    ((ServerPlayer) player).connection.send(payload);
  }

  /**
   * Sends a notification to a specific player (INFO category).
   *
   * @param player the player
   * @param message the notification message
   */
  public static void sendNotification(Player player, Component message) {
    sendNotification(player, message, NotificationCategory.INFO);
  }

  /**
   * Broadcasts soul flame sync to all nearby players (optimized for frequent updates).
   *
   * <p>Use this sparingly to avoid bandwidth issues. Consider rate-limiting to ~1/sec per entity.
   *
   * @param entityId the entity UUID
   * @param stacks soul flame stack count
   * @param durationTicks remaining duration
   * @param origin origin position for radius check
   */
  public static void broadcastSoulFlameSync(
      UUID entityId, int stacks, int durationTicks, LivingEntity origin) {
    if (origin == null || origin.level().isClientSide()) {
      return;
    }

    SoulFlameSyncPayload payload = new SoulFlameSyncPayload(entityId, stacks, durationTicks);

    origin
        .level()
        .players()
        .stream()
        .filter(p -> p instanceof ServerPlayer)
        .map(p -> (ServerPlayer) p)
        .filter(p -> p.distanceToSqr(origin) < 64 * 64)
        .forEach(p -> p.connection.send(payload));
  }
}
