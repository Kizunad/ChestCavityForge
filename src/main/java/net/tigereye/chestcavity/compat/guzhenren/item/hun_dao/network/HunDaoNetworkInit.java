package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientSyncHandlers;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui.HunDaoNotificationRenderer;
import org.slf4j.Logger;

/**
 * Registers Hun Dao network payloads and handlers.
 *
 * <p>Called during RegisterPayloadHandlersEvent to register client-bound sync payloads.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public final class HunDaoNetworkInit {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static boolean initialized = false;

  private HunDaoNetworkInit() {}

  /**
   * Registers all Hun Dao network payloads and handlers.
   *
   * @param event the payload registration event
   */
  public static void register(RegisterPayloadHandlersEvent event) {
    if (initialized) {
      LOGGER.warn("[hun_dao][network] Already initialized, skipping");
      return;
    }

    LOGGER.info("[hun_dao][network] Registering payloads...");

    PayloadRegistrar registrar = event.registrar("1"); // Protocol version

    // Register client-bound payloads
    registrar.playToClient(
        SoulFlameSyncPayload.TYPE,
        SoulFlameSyncPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () ->
                    HunDaoClientSyncHandlers.handleSoulFlameSync(
                        payload.entityId(), payload.stacks(), payload.durationTicks())));

    registrar.playToClient(
        SoulBeastSyncPayload.TYPE,
        SoulBeastSyncPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () ->
                    HunDaoClientSyncHandlers.handleSoulBeastSync(
                        payload.playerId(), payload.active(), payload.durationTicks())));

    registrar.playToClient(
        HunPoSyncPayload.TYPE,
        HunPoSyncPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () ->
                    HunDaoClientSyncHandlers.handleHunPoSync(
                        payload.playerId(), payload.current(), payload.max())));

    registrar.playToClient(
        GuiWuSyncPayload.TYPE,
        GuiWuSyncPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () ->
                    HunDaoClientSyncHandlers.handleGuiWuSync(
                        payload.playerId(), payload.active(), payload.durationTicks())));

    registrar.playToClient(
        HunDaoClearEntityPayload.TYPE,
        HunDaoClearEntityPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () -> HunDaoClientSyncHandlers.handleClearEntity(payload.entityId())));

    registrar.playToClient(
        HunDaoNotificationPayload.TYPE,
        HunDaoNotificationPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () -> HunDaoNotificationRenderer.show(payload.message(), payload.category())));

    initialized = true;
    LOGGER.info("[hun_dao][network] Payload registration complete");
  }

  /**
   * Checks if network payloads have been registered.
   *
   * @return true if registered, false otherwise
   */
  public static boolean isInitialized() {
    return initialized;
  }
}
