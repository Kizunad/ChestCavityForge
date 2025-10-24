package net.tigereye.chestcavity.listeners;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Registry for organ score side-effects. This allows compat modules to react when specific organ
 * score values change without patching {@link OrganUpdateListeners} directly.
 */
public final class OrganScoreEffects {

  @FunctionalInterface
  public interface Effect {
    void apply(
        LivingEntity entity,
        ChestCavityInstance chestCavity,
        float previousValue,
        float currentValue);
  }

  private static final Map<ResourceLocation, Effect> EFFECTS = new LinkedHashMap<>();

  private OrganScoreEffects() {}

  /**
   * Registers an effect that will be invoked whenever the organ score identified by {@code id}
   * changes during a chest cavity evaluation pass.
   */
  public static synchronized void register(ResourceLocation id, Effect effect) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(effect, "effect");
    Effect previous = EFFECTS.put(id, effect);
    if (previous != null && previous != effect && ChestCavity.LOGGER.isWarnEnabled()) {
      ChestCavity.LOGGER.warn(
          "Organ score effect for {} replaced: {} -> {}",
          id,
          previous.getClass().getName(),
          effect.getClass().getName());
    }
  }

  /**
   * Applies all registered effects whose tracked organ score changed during the current update
   * pass.
   */
  public static void applyAll(LivingEntity entity, ChestCavityInstance chestCavity) {
    if (entity == null || chestCavity == null) {
      return;
    }
    Map<ResourceLocation, Effect> snapshot;
    synchronized (OrganScoreEffects.class) {
      if (EFFECTS.isEmpty()) {
        return;
      }
      snapshot = new LinkedHashMap<>(EFFECTS);
    }
    for (Map.Entry<ResourceLocation, Effect> entry : snapshot.entrySet()) {
      ResourceLocation id = entry.getKey();
      Effect effect = entry.getValue();
      float previous = chestCavity.getOldOrganScore(id);
      float current = chestCavity.getOrganScore(id);
      if (Float.compare(previous, current) == 0) {
        continue;
      }
      try {
        effect.apply(entity, chestCavity, previous, current);
      } catch (Exception exception) {
        ChestCavity.LOGGER.error("Error applying organ score effect for {}", id, exception);
      }
    }
  }

  /**
   * Returns an immutable snapshot of the currently registered effects. Primarily intended for
   * tests.
   */
  public static synchronized Map<ResourceLocation, Effect> snapshot() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(EFFECTS));
  }
}
