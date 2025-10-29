package net.tigereye.chestcavity.guscript.ability;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.network.packets.FxEventPayload;

/**
 * Utility helpers that mirror the GuScript FX bridge for bespoke ability logic. Abilities can
 * invoke these methods from server-side code to broadcast FX definitions that are then resolved
 * client-side via the standard dispatcher.
 */
public final class AbilityFxDispatcher {

  private static final double BROADCAST_RADIUS = 64.0D;

  private AbilityFxDispatcher() {}

  /**
   * Dispatches effects.
   */
  public static void play(ServerPlayer performer, ResourceLocation fxId) {
    play(performer, fxId, Vec3.ZERO, null, 1.0F);
  }

  /**
   * Dispatches effects.
   */
  public static void play(
      ServerPlayer performer, ResourceLocation fxId, Vec3 originOffset, float intensity) {
    play(performer, fxId, originOffset, null, intensity);
  }

  /**
   * Dispatches effects.
   */
  public static void play(
      ServerPlayer performer,
      ResourceLocation fxId,
      Vec3 originOffset,
      @Nullable Vec3 targetOffset,
      float intensity) {
    if (performer == null || fxId == null) {
      return;
    }
    Vec3 base = center(performer);
    Vec3 origin = base.add(originOffset == null ? Vec3.ZERO : originOffset);
    Vec3 manualTarget = targetOffset == null ? null : base.add(targetOffset);
    Vec3 look = performer.getLookAngle();
    dispatch(
        performer.serverLevel(),
        fxId,
        origin,
        look,
        look,
        performer,
        null,
        manualTarget,
        intensity,
        -1);
  }

  /**
   * Dispatches effects.
   */
  public static void play(
      ServerLevel level,
      ResourceLocation fxId,
      Vec3 origin,
      Vec3 fallbackDirection,
      Vec3 look,
      @Nullable ServerPlayer performer,
      @Nullable LivingEntity target,
      float intensity) {
    dispatch(
        level,
        fxId,
        origin,
        fallbackDirection,
        look,
        performer,
        target,
        null,
        intensity,
        target != null ? target.getId() : -1);
  }

  /**
   * Dispatches effects.
   */
  private static void dispatch(
      ServerLevel level,
      ResourceLocation fxId,
      Vec3 origin,
      Vec3 fallbackDirection,
      Vec3 look,
      @Nullable ServerPlayer performer,
      @Nullable LivingEntity target,
      @Nullable Vec3 manualTarget,
      float intensity,
      int targetId) {
    if (level == null || fxId == null || origin == null) {
      return;
    }
    Vec3 safeFallback = fallbackDirection == null ? Vec3.ZERO : fallbackDirection;
    Vec3 safeLook = look == null ? safeFallback : look;
    Vec3 targetPosition = null;
    if (manualTarget != null) {
      targetPosition = manualTarget;
    } else if (target != null) {
      targetPosition = center(target);
    }
    FxEventPayload payload =
        new FxEventPayload(
            fxId,
            origin.x,
            origin.y,
            origin.z,
            (float) safeFallback.x,
            (float) safeFallback.y,
            (float) safeFallback.z,
            (float) safeLook.x,
            (float) safeLook.y,
            (float) safeLook.z,
            intensity <= 0.0F ? 1.0F : intensity,
            targetPosition != null,
            targetPosition != null ? targetPosition.x : 0.0D,
            targetPosition != null ? targetPosition.y : 0.0D,
            targetPosition != null ? targetPosition.z : 0.0D,
            performer != null ? performer.getId() : -1,
            targetId);
    broadcast(level, origin, payload);
  }

  /**
   * Registers effects.
   */
  private static void broadcast(ServerLevel level, Vec3 origin, FxEventPayload payload) {
    if (level == null || payload == null) {
      return;
    }
    double radiusSq = BROADCAST_RADIUS * BROADCAST_RADIUS;
    for (ServerPlayer viewer : level.players()) {
      if (viewer == null) {
        continue;
      }
      if (viewer.getId() == payload.performerId() || viewer.distanceToSqr(origin) <= radiusSq) {
        viewer.connection.send(payload);
      }
    }
  }

  /**
   * Unregisters effects.
   */
  private static Vec3 center(LivingEntity entity) {
    return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
  }

  /**
   * Dispatches ability FX.
   */
  public static void dispatch(String abilityId, LivingEntity entity) {
    // Implementation for dispatching ability FX
  }

  /**
   * Registers an ability FX.
   */
  public static void register(String abilityId, Consumer<LivingEntity> fx) {
    // Implementation for registering an ability FX
  }

  /**
   * Gets the FX map.
   */
  public static Map<String, Consumer<LivingEntity>> getFxMap() {
    // Implementation for getting the FX map
    return null;
  }

  /**
   * Clears the FX map.
   */
  public static void clear() {
    // Implementation for clearing the FX map
  }
}
