package net.tigereye.chestcavity.guscript.fx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;

/** Registry storing FX definitions for client playback. */
public final class FxRegistry {

  private static final Map<ResourceLocation, FxDefinition> DEFINITIONS = new HashMap<>();

  private FxRegistry() {}

  public static synchronized void updateDefinitions(
      Map<ResourceLocation, FxDefinition> definitions) {
    DEFINITIONS.clear();
    DEFINITIONS.putAll(definitions);
    ChestCavity.LOGGER.info("[GuScript] Loaded {} FX definitions", DEFINITIONS.size());
  }

  public static synchronized Optional<FxDefinition> definition(ResourceLocation id) {
    return Optional.ofNullable(DEFINITIONS.get(id));
  }

  public static synchronized Map<ResourceLocation, FxDefinition> snapshot() {
    return Collections.unmodifiableMap(new HashMap<>(DEFINITIONS));
  }

  public record FxContext(
      Vec3 origin,
      Vec3 fallbackDirection,
      Vec3 look,
      Vec3 target,
      float intensity,
      int performerId,
      int targetId) {
    public FxContext {
      origin = origin == null ? Vec3.ZERO : origin;
      fallbackDirection = fallbackDirection == null ? Vec3.ZERO : fallbackDirection;
      look = look == null ? Vec3.ZERO : look;
      intensity = Float.isNaN(intensity) || intensity <= 0.0F ? 1.0F : intensity;
    }

    public Vec3 resolveDirection() {
      if (target != null) {
        return target.subtract(origin);
      }
      if (fallbackDirection.lengthSqr() > 1.0E-4) {
        return fallbackDirection;
      }
      return look;
    }
  }
}
