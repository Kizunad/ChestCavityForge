package net.tigereye.chestcavity.guzhenren.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.network.event.GuzhenrenPlayerVariablesSyncedEvent;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.apache.logging.log4j.Logger;

/** Client-side payload hook implementation. */
@OnlyIn(Dist.CLIENT)
final class GuzhenrenClientNetworkBridge {

  private static final Logger LOGGER = ChestCavity.LOGGER;
  private static final Set<GuzhenrenPayloadListener> LISTENERS = new CopyOnWriteArraySet<>();
  private static volatile boolean installed = false;

  private GuzhenrenClientNetworkBridge() {}

  static void bootstrap() {
    LOGGER.debug(
        "[compat/guzhenren][network/client] bootstrap (installed={}, bridgeAvailable={})",
        installed,
        GuzhenrenResourceBridge.isAvailable());
    if (installed) {
      LOGGER.debug("[compat/guzhenren][network/client] bootstrap aborted: already installed");
      return;
    }
    if (!GuzhenrenResourceBridge.isAvailable()) {
      LOGGER.debug(
          "[compat/guzhenren][network/client] bootstrap aborted: Guzhenren bridge unavailable");
      return;
    }
    synchronized (GuzhenrenClientNetworkBridge.class) {
      if (installed) {
        LOGGER.debug(
            "[compat/guzhenren][network/client] bootstrap abort inside sync: installed flag set");
        return;
      }
      try {
        installInterceptor();
        installed = true;
        LOGGER.info("[compat/guzhenren][network/client] hooked player variable payload");
      } catch (Throwable throwable) {
        LOGGER.warn(
            "[compat/guzhenren][network/client] Failed to hook Guzhenren player variable payload",
            throwable);
      }
    }
  }

  static boolean registerListener(GuzhenrenPayloadListener listener) {
    if (listener == null) {
      LOGGER.warn("[compat/guzhenren][network/client] Attempted to register null listener");
      return false;
    }
    boolean added = LISTENERS.add(listener);
    LOGGER.debug(
        "[compat/guzhenren][network/client] listener {} registered? {} (total={})",
        listener.getClass().getName(),
        added,
        LISTENERS.size());
    return added;
  }

  static boolean unregisterListener(GuzhenrenPayloadListener listener) {
    if (listener == null) {
      return false;
    }
    boolean removed = LISTENERS.remove(listener);
    LOGGER.debug(
        "[compat/guzhenren][network/client] listener {} removed? {} (total={})",
        listener.getClass().getName(),
        removed,
        LISTENERS.size());
    return removed;
  }

  private static void installInterceptor() throws Exception {
    Class<?> modClass = Class.forName("net.guzhenren.GuzhenrenMod");
    Field messagesField = modClass.getDeclaredField("MESSAGES");
    messagesField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Object, Object> messages = (Map<Object, Object>) messagesField.get(null);
    if (messages == null) {
      throw new IllegalStateException("Guzhenren message registry not initialised");
    }

    Class<?> payloadClass =
        Class.forName("net.guzhenren.network.GuzhenrenModVariables$PlayerVariablesSyncMessage");
    Field typeField = payloadClass.getDeclaredField("TYPE");
    typeField.setAccessible(true);
    Object type = typeField.get(null);
    if (type == null) {
      throw new IllegalStateException("Guzhenren PlayerVariablesSyncMessage TYPE missing");
    }

    Object existing = messages.get(type);
    if (existing == null) {
      LOGGER.warn(
          "[compat/guzhenren][network/client] PlayerVariablesSyncMessage not present in Guzhenren message map");
      return;
    }

    Class<?> networkMessageClass = existing.getClass();
    Method readerAccessor = networkMessageClass.getDeclaredMethod("reader");
    readerAccessor.setAccessible(true);
    @SuppressWarnings("unchecked")
    StreamCodec<?, ?> reader = (StreamCodec<?, ?>) readerAccessor.invoke(existing);

    Method handlerAccessor = networkMessageClass.getDeclaredMethod("handler");
    handlerAccessor.setAccessible(true);
    @SuppressWarnings("unchecked")
    IPayloadHandler<CustomPacketPayload> originalHandler =
        (IPayloadHandler<CustomPacketPayload>) handlerAccessor.invoke(existing);
    if (originalHandler == null) {
      LOGGER.warn(
          "[compat/guzhenren][network/client] PlayerVariablesSyncMessage handler unavailable");
      return;
    }

    Constructor<?> ctor;
    try {
      ctor = networkMessageClass.getDeclaredConstructor(StreamCodec.class, IPayloadHandler.class);
    } catch (NoSuchMethodException ex) {
      Constructor<?>[] constructors = networkMessageClass.getDeclaredConstructors();
      if (constructors.length == 0) {
        throw ex;
      }
      ctor = constructors[0];
    }
    ctor.setAccessible(true);

    IPayloadHandler<CustomPacketPayload> wrapper =
        new IPayloadHandler<>() {
          @Override
          public void handle(CustomPacketPayload payload, IPayloadContext context) {
            originalHandler.handle(payload, context);
            if (context.flow() != PacketFlow.CLIENTBOUND) {
              LOGGER.trace(
                  "[compat/guzhenren][network/client] payload ignored due to flow {}",
                  context.flow());
              return;
            }
            Player player = context.player();
            if (player == null || !player.level().isClientSide()) {
              LOGGER.trace(
                  "[compat/guzhenren][network/client] payload ignored (player null or not clientside)");
              return;
            }
            LOGGER.trace(
                "[compat/guzhenren][network/client] payload received for {}",
                player.getScoreboardName());
            Minecraft.getInstance().execute(() -> dispatch(player));
          }
        };

    Object replacement = ctor.newInstance(reader, wrapper);
    messages.put(type, replacement);
    LOGGER.debug(
        "[compat/guzhenren][network/client] installed wrapper handler {} for payload {}",
        wrapper.getClass().getName(),
        type);
  }

  private static void dispatch(Player player) {
    GuzhenrenResourceBridge.open(player)
        .ifPresent(
            handle -> {
              var snapshot = handle.snapshotAll();
              if (snapshot.isEmpty()) {
                LOGGER.trace(
                    "[compat/guzhenren][network/client] dispatch aborted: empty snapshot for {}",
                    player.getScoreboardName());
                return;
              }
              LOGGER.trace(
                  "[compat/guzhenren][network/client] dispatching snapshot with {} keys for {}",
                  snapshot.size(),
                  player.getScoreboardName());
              var event = new GuzhenrenPlayerVariablesSyncedEvent(player, handle, snapshot);
              NeoForge.EVENT_BUS.post(event);
              for (GuzhenrenPayloadListener listener : LISTENERS) {
                try {
                  listener.onPlayerVariablesSynced(player, handle, snapshot);
                } catch (Throwable throwable) {
                  LOGGER.warn(
                      "[compat/guzhenren][network/client] payload listener threw", throwable);
                }
              }
            });
  }
}
