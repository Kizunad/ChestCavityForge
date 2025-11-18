package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.engine.fx.FxContext;
import net.tigereye.chestcavity.engine.fx.FxEngine;
import org.slf4j.Logger;

/**
 * Central FX router for Hun Dao effects.
 *
 * <p>Dispatches audio and visual effects (sounds, particles, animations) in a data-driven manner
 * by looking up FX templates from HunDaoFxRegistry and routing to appropriate subsystems (FxEngine
 * for particles, Minecraft SoundSystem for audio).
 *
 * <p>Provides both one-shot and continuous effect dispatch, with automatic fallback handling when
 * FxEngine is unavailable or FX templates are not registered.
 *
 * <p>Phase 5: Server-client FX decoupling - server calls router, router dispatches to client.
 */
public final class HunDaoFxRouter {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<ResourceLocation, Long2LongOpenHashMap> SOUND_COOLDOWNS =
      new HashMap<>();

  private HunDaoFxRouter() {}

  /**
   * Dispatches a one-shot FX effect at a target entity.
   *
   * @param level the server level
   * @param target the target entity (anchor point for FX)
   * @param fxId the FX resource location
   * @return true if FX was dispatched successfully, false otherwise
   */
  public static boolean dispatch(ServerLevel level, Entity target, ResourceLocation fxId) {
    Objects.requireNonNull(level, "level cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    Objects.requireNonNull(fxId, "fxId cannot be null");

    return dispatch(level, target.position(), fxId, target);
  }

  /**
   * Dispatches a one-shot FX effect at a specific position.
   *
   * @param level the server level
   * @param position the world position
   * @param fxId the FX resource location
   * @return true if FX was dispatched successfully, false otherwise
   */
  public static boolean dispatch(ServerLevel level, Vec3 position, ResourceLocation fxId) {
    return dispatch(level, position, fxId, null);
  }

  /**
   * Dispatches a continuous (ambient) FX effect.
   *
   * @param level the server level
   * @param target the target entity (anchor point for FX)
   * @param fxId the FX resource location
   * @param durationTicks duration in ticks
   * @return true if FX was dispatched successfully, false otherwise
   */
  public static boolean dispatchContinuous(
      ServerLevel level, Entity target, ResourceLocation fxId, int durationTicks) {
    Objects.requireNonNull(level, "level cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    Objects.requireNonNull(fxId, "fxId cannot be null");

    if (durationTicks <= 0) {
      LOGGER.warn("[hun_dao][fx_router] Invalid duration for continuous FX: {} ticks", durationTicks);
      return false;
    }

    HunDaoFxRegistry.FxTemplate template = HunDaoFxRegistry.get(fxId);
    if (template == null) {
      LOGGER.debug("[hun_dao][fx_router] FX template not registered: {}", fxId);
      return false;
    }

    // Play initial sound if configured
    SoundEvent soundEvent = template.resolveSound();
    if (soundEvent != null
        && shouldPlaySound(level, fxId, template, target, target.position())) {
      playSound(level, target.position(), soundEvent, template.soundVolume, template.soundPitch);
    }

    // Dispatch continuous FX via FxEngine
    return dispatchToFxEngine(level, target.position(), fxId, target, durationTicks);
  }

  /**
   * Core dispatch implementation.
   *
   * @param level the server level
   * @param position the world position
   * @param fxId the FX resource location
   * @param target optional target entity (for entity-anchored effects)
   * @return true if dispatched, false otherwise
   */
  private static boolean dispatch(
      ServerLevel level, Vec3 position, ResourceLocation fxId, Entity target) {
    HunDaoFxRegistry.FxTemplate template = HunDaoFxRegistry.get(fxId);

    if (template == null) {
      LOGGER.debug("[hun_dao][fx_router] FX template not registered: {}", fxId);
      return false;
    }

    // Play sound if configured
    SoundEvent soundEvent = template.resolveSound();
    if (soundEvent != null
        && shouldPlaySound(level, fxId, template, target, position)) {
      playSound(level, position, soundEvent, template.soundVolume, template.soundPitch);
    }

    // Dispatch particles via FxEngine if template includes particle data
    if (template.particleTemplate != null || template.continuous) {
      int duration = template.continuous ? template.durationTicks : 20; // One-shot = 1 second
      return dispatchToFxEngine(level, position, fxId, target, duration);
    }

    return true;
  }

  /**
   * Plays a sound at the specified position.
   *
   * @param level the server level
   * @param position the world position
   * @param sound the sound event
   * @param volume the sound volume
   * @param pitch the sound pitch
   */
  private static void playSound(
      ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {
    level.playSound(
        null, // null player = broadcast to all nearby
        position.x,
        position.y,
        position.z,
        sound,
        SoundSource.PLAYERS,
        volume,
        pitch);
  }

  private static boolean shouldPlaySound(
      ServerLevel level,
      ResourceLocation fxId,
      HunDaoFxRegistry.FxTemplate template,
      Entity target,
      Vec3 position) {
    int interval = template.minRepeatIntervalTicks();
    if (interval <= 0) {
      return true;
    }

    long now = level.getGameTime();
    long anchorKey = computeAnchorKey(target, position);
    Long2LongOpenHashMap cooldowns =
        SOUND_COOLDOWNS.computeIfAbsent(fxId, key -> new Long2LongOpenHashMap());
    long lastPlayed = cooldowns.getOrDefault(anchorKey, Long.MIN_VALUE);
    if (now - lastPlayed < interval) {
      return false;
    }

    cooldowns.put(anchorKey, now);
    return true;
  }

  private static long computeAnchorKey(Entity target, Vec3 position) {
    if (target != null) {
      return target.getUUID().getMostSignificantBits() ^ target.getUUID().getLeastSignificantBits();
    }
    BlockPos pos = BlockPos.containing(position);
    return pos.asLong();
  }

  /**
   * Dispatches FX to the FxEngine for particle rendering.
   *
   * @param level the server level
   * @param position the world position
   * @param fxId the FX resource location
   * @param target optional target entity
   * @param durationTicks duration in ticks
   * @return true if FxEngine accepted the FX, false otherwise
   */
  private static boolean dispatchToFxEngine(
      ServerLevel level, Vec3 position, ResourceLocation fxId, Entity target, int durationTicks) {
    if (!FxEngine.getConfig().enabled) {
      LOGGER.debug("[hun_dao][fx_router] FxEngine disabled, skipping particle dispatch for {}", fxId);
      return false;
    }

    String fxKey = fxId.toString();
    if (!FxEngine.registry().isRegistered(fxKey)) {
      LOGGER.debug("[hun_dao][fx_router] FX not registered in FxEngine: {}", fxKey);
      return false;
    }

    FxContext.Builder contextBuilder = FxContext.builder(level).position(position);

    if (target != null) {
      contextBuilder
          .owner(target.getUUID())
          .customParam("targetId", target.getId())
          .customParam("targetHeight", target.getBbHeight())
          .customParam("targetWidth", target.getBbWidth());
    }

    contextBuilder.customParam("durationTicks", durationTicks);

    FxContext context = contextBuilder.build();
    boolean dispatched = FxEngine.registry().play(fxKey, context) != null;

    LOGGER.debug(
        "[hun_dao][fx_router] Dispatched FX {} to FxEngine: {} (duration={}t)",
        fxId,
        dispatched ? "success" : "failed",
        durationTicks);

    return dispatched;
  }

  /**
   * Stops a continuous FX effect early.
   *
   * @param level the server level
   * @param target the target entity
   * @param fxId the FX resource location
   */
  public static void stop(ServerLevel level, Entity target, ResourceLocation fxId) {
    Objects.requireNonNull(level, "level cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    Objects.requireNonNull(fxId, "fxId cannot be null");

    if (!FxEngine.getConfig().enabled) {
      return;
    }

    String fxKey = fxId.toString();
    // FxEngine.registry().stop(fxKey, target.getUUID()); // TODO: Implement if FxEngine supports stop
    LOGGER.debug("[hun_dao][fx_router] Stop request for FX {} on entity {}", fxId, target.getId());
  }
}
