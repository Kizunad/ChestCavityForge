package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Central access point for the {@link SoulBeastState} attachment. */
public final class SoulBeastStateManager {

  private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
  private static final Map<UUID, ClientSnapshot> CLIENT_CACHE = new ConcurrentHashMap<>();

  private SoulBeastStateManager() {}

  /**
   * Gets or creates the soul beast state for the given entity.
   *
   * @param entity The entity.
   * @return The soul beast state.
   */
  public static SoulBeastState getOrCreate(LivingEntity entity) {
    return CCAttachments.getSoulBeastState(entity);
  }

  /**
   * Gets the existing soul beast state for the given entity.
   *
   * @param entity The entity.
   * @return The soul beast state, or an empty optional if it does not exist.
   */
  public static Optional<SoulBeastState> getExisting(LivingEntity entity) {
    return CCAttachments.getExistingSoulBeastState(entity);
  }

  /**
   * Checks if the given entity is an active soul beast.
   *
   * @param entity The entity.
   * @return {@code true} if the entity is an active soul beast, {@code false} otherwise.
   */
  public static boolean isActive(LivingEntity entity) {
    return getExisting(entity)
        .map(s -> s.isPermanent() || s.isEnabled() || s.isActive())
        .orElse(false);
  }

  /**
   * Checks if the given entity is a permanent soul beast.
   *
   * @param entity The entity.
   * @return {@code true} if the entity is a permanent soul beast, {@code false} otherwise.
   */
  public static boolean isPermanent(LivingEntity entity) {
    return getExisting(entity).map(SoulBeastState::isPermanent).orElse(false);
  }

  /**
   * Sets the active state of the soul beast.
   *
   * @param entity The entity.
   * @param active The active state.
   */
  public static void setActive(LivingEntity entity, boolean active) {
    SoulBeastState state = getOrCreate(entity);
    SoulBeastStateChangedEvent.Snapshot previous = snapshotOf(state);
    if (state.setActive(active)) {
      touch(entity, state);
      SoulBeastStateChangedEvent.Snapshot current = snapshotOf(state);
      postStateChanged(entity, previous, current);
      if (entity instanceof ServerPlayer player) {
        syncToClient(player);
      }
    }
  }

  /**
   * Sets the permanent state of the soul beast.
   *
   * @param entity The entity.
   * @param permanent The permanent state.
   */
  public static void setPermanent(LivingEntity entity, boolean permanent) {
    SoulBeastState state = getOrCreate(entity);
    SoulBeastStateChangedEvent.Snapshot previous = snapshotOf(state);
    if (state.setPermanent(permanent)) {
      touch(entity, state);
      if (permanent && state.getStartedTick() == 0L) {
        state.setStartedTick(state.getLastTick());
      }
      SoulBeastStateChangedEvent.Snapshot current = snapshotOf(state);
      postStateChanged(entity, previous, current);
      if (entity instanceof ServerPlayer player) {
        syncToClient(player);
      }
    }
  }

  /**
   * Checks if the soul beast state is enabled for the given entity.
   *
   * @param entity The entity.
   * @return {@code true} if the soul beast state is enabled, {@code false} otherwise.
   */
  public static boolean isEnabled(LivingEntity entity) {
    return getExisting(entity).map(SoulBeastState::isEnabled).orElse(false);
  }

  /**
   * Sets the enabled state of the soul beast.
   *
   * @param entity The entity.
   * @param enabled The enabled state.
   */
  public static void setEnabled(LivingEntity entity, boolean enabled) {
    SoulBeastState state = getOrCreate(entity);
    SoulBeastStateChangedEvent.Snapshot previous = snapshotOf(state);
    if (state.setEnabled(enabled)) {
      touch(entity, state);
      if (enabled && state.getStartedTick() == 0L) {
        state.setStartedTick(state.getLastTick());
      }
      SoulBeastStateChangedEvent.Snapshot current = snapshotOf(state);
      postStateChanged(entity, previous, current);
      if (entity instanceof ServerPlayer player) {
        syncToClient(player);
      }
    }
  }

  /**
   * Sets the source of the soul beast state.
   *
   * @param entity The entity.
   * @param source The source.
   */
  public static void setSource(
      LivingEntity entity, @Nullable net.minecraft.resources.ResourceLocation source) {
    SoulBeastState state = getOrCreate(entity);
    if (state.setSource(source)) {
      touch(entity, state);
      if (entity instanceof ServerPlayer player) {
        syncToClient(player);
      }
    }
  }

  /**
   * Synchronizes the soul beast state to the client.
   *
   * @param player The player.
   */
  public static void syncToClient(ServerPlayer player) {
    SoulBeastState state = getOrCreate(player);
    var payload =
        new SoulBeastSyncPayload(
            player.getId(),
            state.isActive(),
            state.isEnabled(),
            state.isPermanent(),
            state.getLastTick(),
            state.getSource().orElse(null));
    player.connection.send(payload);
    LOGGER.debug(
        "[compat/guzhenren][hun_dao][state] synced {} -> active={} enabled={} permanent={} source={} tick={}",
        describe(player),
        state.isActive(),
        state.isEnabled(),
        state.isPermanent(),
        state.getSource().map(Object::toString).orElse("<none>"),
        state.getLastTick());
  }

  /**
   * Handles the soul beast sync payload.
   *
   * @param payload The payload.
   * @param context The context.
   */
  public static void handleSyncPayload(SoulBeastSyncPayload payload, IPayloadContext context) {
    if (context.flow() != PacketFlow.CLIENTBOUND) {
      LOGGER.warn(
          "[compat/guzhenren][hun_dao][state] rejected unexpected {} payload from {}",
          payload.type(),
          context.flow());
      return;
    }
    context.enqueueWork(() -> applyClientSnapshot(payload, context.player()));
  }

  /**
   * Gets the client snapshot for the given UUID.
   *
   * @param uuid The UUID.
   * @return The client snapshot, or an empty optional if it does not exist.
   */
  public static Optional<ClientSnapshot> getClientSnapshot(UUID uuid) {
    return Optional.ofNullable(CLIENT_CACHE.get(uuid));
  }

  /**
   * Gets the client snapshot for the given entity.
   *
   * @param entity The entity.
   * @return The client snapshot, or an empty optional if it does not exist.
   */
  public static Optional<ClientSnapshot> getClientSnapshot(Entity entity) {
    return entity == null ? Optional.empty() : getClientSnapshot(entity.getUUID());
  }

  /**
   * Applies the client snapshot.
   *
   * @param payload The payload.
   * @param contextPlayer The player.
   */
  public static void applyClientSnapshot(
      SoulBeastSyncPayload payload, @Nullable Player contextPlayer) {
    Player resolvedPlayer = contextPlayer;
    if (resolvedPlayer == null && FMLEnvironment.dist.isClient()) {
      resolvedPlayer = resolveClientPlayer();
    }
    if (resolvedPlayer == null) {
      return;
    }
    Entity target = resolvedPlayer.level().getEntity(payload.entityId());
    UUID uuid = target != null ? target.getUUID() : resolvedPlayer.getUUID();
    ClientSnapshot previous = CLIENT_CACHE.get(uuid);
    ClientSnapshot snapshot =
        new ClientSnapshot(
            payload.active(),
            payload.enabled(),
            payload.permanent(),
            payload.lastTick(),
            payload.source());
    CLIENT_CACHE.put(uuid, snapshot);
    LivingEntity living = target instanceof LivingEntity le ? le : resolvedPlayer;
    if (living != null) {
      SoulBeastStateChangedEvent.Snapshot before =
          previous != null ? previous.toEventSnapshot() : SoulBeastStateChangedEvent.Snapshot.EMPTY;
      SoulBeastStateChangedEvent.Snapshot after = snapshot.toEventSnapshot();
      postStateChanged(living, before, after);
    }
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug(
          "[compat/guzhenren][hun_dao][state] client cached {} -> {}", uuid, snapshot);
    }
  }

  @OnlyIn(Dist.CLIENT)
  private static Player resolveClientPlayer() {
    return net.minecraft.client.Minecraft.getInstance().player;
  }

  /** Clears the client-side soul beast state cache. */
  public static void clearClientCache() {
    CLIENT_CACHE.clear();
  }

  /**
   * Handle a serverbound request to (re)sync the player's SoulBeastState to the client.
   *
   * @param payload The payload.
   * @param context The context.
   */
  public static void handleRequestSyncPayload(
      SoulBeastRequestSyncPayload payload, IPayloadContext context) {
    if (context.flow() != PacketFlow.SERVERBOUND) {
      LOGGER.warn(
          "[compat/guzhenren][hun_dao][state] rejected unexpected {} payload from {}",
          payload.type(),
          context.flow());
      return;
    }
    context.enqueueWork(
        () -> {
          if (context.player() instanceof ServerPlayer sp) {
            syncToClient(sp);
          }
        });
  }

  private static SoulBeastStateChangedEvent.Snapshot snapshotOf(SoulBeastState state) {
    return new SoulBeastStateChangedEvent.Snapshot(
        state.isActive(), state.isEnabled(), state.isPermanent());
  }

  private static void postStateChanged(
      LivingEntity entity,
      SoulBeastStateChangedEvent.Snapshot previous,
      SoulBeastStateChangedEvent.Snapshot current) {
    if (entity == null || previous.equals(current)) {
      return;
    }
    // 旧包路径 (net.tigereye.chestcavity.soulbeast) 已被完全迁移至 compat.hun_dao 命名空间，
    // 因此仅发布新的事件，避免重复触发或加载已删除的过渡事件类型。
    NeoForge.EVENT_BUS.post(new SoulBeastStateChangedEvent(entity, previous, current));
  }

  private static void touch(LivingEntity entity, SoulBeastState state) {
    long tick = entity.level() != null ? entity.level().getGameTime() : 0L;
    state.setLastTick(tick);
  }

  private static String describe(Player player) {
    return String.format(Locale.ROOT, "%s(%s)", player.getScoreboardName(), player.getUUID());
  }

  public record ClientSnapshot(
      boolean active,
      boolean enabled,
      boolean permanent,
      long lastTick,
      @Nullable net.minecraft.resources.ResourceLocation source) {

    public SoulBeastStateChangedEvent.Snapshot toEventSnapshot() {
      return new SoulBeastStateChangedEvent.Snapshot(active, enabled, permanent);
    }
  }
}
