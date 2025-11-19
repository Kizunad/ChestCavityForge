package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

import java.util.UUID;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Handlers for server-to-client synchronization of Hun Dao state.
 *
 * <p>Processes network packets from server and updates HunDaoClientState accordingly. Designed to
 * be called from custom network payload handlers.
 *
 * <p>Phase 5: Client sync infrastructure (payload implementation in Phase 6+).
 */
public final class HunDaoClientSyncHandlers {

  private static final Logger LOGGER = LogUtils.getLogger();

  private HunDaoClientSyncHandlers() {}

  /**
   * Handles soul flame sync packet.
   *
   * @param entityId the entity UUID
   * @param stacks soul flame stack count
   * @param durationTicks remaining duration in ticks
   */
  public static void handleSoulFlameSync(UUID entityId, int stacks, int durationTicks) {
    LOGGER.debug(
        "[hun_dao][client_sync] Soul flame: entity={} stacks={} duration={}t",
        entityId,
        stacks,
        durationTicks);

    HunDaoClientState.instance().setSoulFlameStacks(entityId, stacks);
    HunDaoClientState.instance().setSoulFlameDuration(entityId, durationTicks);
  }

  /**
   * Handles soul beast sync packet.
   *
   * @param playerId the player UUID
   * @param active whether soul beast is active
   * @param durationTicks remaining duration in ticks (if active)
   */
  public static void handleSoulBeastSync(UUID playerId, boolean active, int durationTicks) {
    LOGGER.debug(
        "[hun_dao][client_sync] Soul beast: player={} active={} duration={}t",
        playerId,
        active,
        durationTicks);

    HunDaoClientState.instance().setSoulBeastActive(playerId, active);
    if (active) {
      HunDaoClientState.instance().setSoulBeastDuration(playerId, durationTicks);
    } else {
      HunDaoClientState.instance().setSoulBeastDuration(playerId, 0);
    }
  }

  /**
   * Handles hun po sync packet.
   *
   * @param playerId the player UUID
   * @param current current hun po value
   * @param max maximum hun po value
   */
  public static void handleHunPoSync(UUID playerId, double current, double max) {
    LOGGER.debug(
        "[hun_dao][client_sync] Hun po: player={} current={} max={}",
        playerId,
        String.format("%.2f", current),
        String.format("%.2f", max));

    HunDaoClientState.instance().setHunPo(playerId, current, max);
  }

  /**
   * Handles gui wu sync packet.
   *
   * @param playerId the player UUID
   * @param active whether gui wu is active
   * @param durationTicks remaining duration in ticks (if active)
   */
  public static void handleGuiWuSync(UUID playerId, boolean active, int durationTicks) {
    LOGGER.debug(
        "[hun_dao][client_sync] Gui wu: player={} active={} duration={}t",
        playerId,
        active,
        durationTicks);

    HunDaoClientState.instance().setGuiWuActive(playerId, active);
    if (active) {
      HunDaoClientState.instance().setGuiWuDuration(playerId, durationTicks);
    } else {
      HunDaoClientState.instance().setGuiWuDuration(playerId, 0);
    }
  }

  /**
   * Handles entity state clear packet.
   *
   * @param entityId the entity UUID to clear
   */
  public static void handleClearEntity(UUID entityId) {
    LOGGER.debug("[hun_dao][client_sync] Clearing entity state: {}", entityId);
    HunDaoClientState.instance().clear(entityId);
  }

  /**
   * Handles full state clear packet.
   *
   * <p>Called when player disconnects or changes dimensions.
   */
  public static void handleClearAll() {
    LOGGER.debug("[hun_dao][client_sync] Clearing all client state");
    HunDaoClientState.instance().clearAll();
  }
}
