package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side state cache for Hun Dao mechanics.
 *
 * <p>Stores client-specific state like soul flame DoT stacks, soul beast transformation timers,
 * and hun po levels for smooth HUD rendering and FX playback. Updated via network sync from server.
 *
 * <p>Phase 5: Client-side state management for HUD and FX systems.
 */
public final class HunDaoClientState {

  private static final HunDaoClientState INSTANCE = new HunDaoClientState();

  private HunDaoClientState() {}

  public static HunDaoClientState instance() {
    return INSTANCE;
  }

  // ===== State Storage =====

  /** Soul flame DoT stacks per entity (entity UUID → stack count). */
  private final Map<UUID, Integer> soulFlameStacks = new HashMap<>();

  /** Soul flame DoT remaining ticks per entity (entity UUID → remaining ticks). */
  private final Map<UUID, Integer> soulFlameDuration = new HashMap<>();

  /** Soul beast active state per player (player UUID → active flag). */
  private final Map<UUID, Boolean> soulBeastActive = new HashMap<>();

  /** Soul beast remaining ticks per player (player UUID → remaining ticks). */
  private final Map<UUID, Integer> soulBeastDuration = new HashMap<>();

  /** Hun po current value per player (player UUID → current hun po). */
  private final Map<UUID, Double> hunpoCurrent = new HashMap<>();

  /** Hun po maximum value per player (player UUID → max hun po). */
  private final Map<UUID, Double> hunpoMax = new HashMap<>();

  /** Gui wu active state per player (player UUID → active flag). */
  private final Map<UUID, Boolean> guiWuActive = new HashMap<>();

  /** Gui wu remaining ticks per player (player UUID → remaining ticks). */
  private final Map<UUID, Integer> guiWuDuration = new HashMap<>();

  // ===== Soul Flame =====

  public void setSoulFlameStacks(UUID entityId, int stacks) {
    if (stacks <= 0) {
      soulFlameStacks.remove(entityId);
    } else {
      soulFlameStacks.put(entityId, stacks);
    }
  }

  public int getSoulFlameStacks(UUID entityId) {
    return soulFlameStacks.getOrDefault(entityId, 0);
  }

  public void setSoulFlameDuration(UUID entityId, int ticks) {
    if (ticks <= 0) {
      soulFlameDuration.remove(entityId);
    } else {
      soulFlameDuration.put(entityId, ticks);
    }
  }

  public int getSoulFlameDuration(UUID entityId) {
    return soulFlameDuration.getOrDefault(entityId, 0);
  }

  // ===== Soul Beast =====

  public void setSoulBeastActive(UUID playerId, boolean active) {
    if (active) {
      soulBeastActive.put(playerId, true);
    } else {
      soulBeastActive.remove(playerId);
    }
  }

  public boolean isSoulBeastActive(UUID playerId) {
    return soulBeastActive.getOrDefault(playerId, false);
  }

  public void setSoulBeastDuration(UUID playerId, int ticks) {
    if (ticks <= 0) {
      soulBeastDuration.remove(playerId);
    } else {
      soulBeastDuration.put(playerId, ticks);
    }
  }

  public int getSoulBeastDuration(UUID playerId) {
    return soulBeastDuration.getOrDefault(playerId, 0);
  }

  // ===== Hun Po =====

  public void setHunPo(UUID playerId, double current, double max) {
    hunpoCurrent.put(playerId, current);
    hunpoMax.put(playerId, max);
  }

  public double getHunPoCurrent(UUID playerId) {
    return hunpoCurrent.getOrDefault(playerId, 0.0);
  }

  public double getHunPoMax(UUID playerId) {
    return hunpoMax.getOrDefault(playerId, 0.0);
  }

  public double getHunPoPercentage(UUID playerId) {
    double current = getHunPoCurrent(playerId);
    double max = getHunPoMax(playerId);
    if (max <= 0) {
      return 0.0;
    }
    return current / max;
  }

  // ===== Gui Wu =====

  public void setGuiWuActive(UUID playerId, boolean active) {
    if (active) {
      guiWuActive.put(playerId, true);
    } else {
      guiWuActive.remove(playerId);
    }
  }

  public boolean isGuiWuActive(UUID playerId) {
    return guiWuActive.getOrDefault(playerId, false);
  }

  public void setGuiWuDuration(UUID playerId, int ticks) {
    if (ticks <= 0) {
      guiWuDuration.remove(playerId);
    } else {
      guiWuDuration.put(playerId, ticks);
    }
  }

  public int getGuiWuDuration(UUID playerId) {
    return guiWuDuration.getOrDefault(playerId, 0);
  }

  // ===== Cleanup =====

  /**
   * Clears all state for a specific entity/player.
   *
   * @param entityId the entity UUID
   */
  public void clear(UUID entityId) {
    soulFlameStacks.remove(entityId);
    soulFlameDuration.remove(entityId);
    soulBeastActive.remove(entityId);
    soulBeastDuration.remove(entityId);
    hunpoCurrent.remove(entityId);
    hunpoMax.remove(entityId);
    guiWuActive.remove(entityId);
    guiWuDuration.remove(entityId);
  }

  /**
   * Clears all cached state.
   *
   * <p>Called on world unload or dimension change.
   */
  public void clearAll() {
    soulFlameStacks.clear();
    soulFlameDuration.clear();
    soulBeastActive.clear();
    soulBeastDuration.clear();
    hunpoCurrent.clear();
    hunpoMax.clear();
    guiWuActive.clear();
    guiWuDuration.clear();
  }

  // ===== Client Tick =====

  /**
   * Called every client tick to decay timers.
   *
   * <p>Decrements duration counters for soul flame, soul beast, and gui wu effects.
   */
  public void tick() {
    // Decay soul flame durations
    soulFlameDuration.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
    soulFlameDuration.entrySet().removeIf(entry -> entry.getValue() <= 0);

    // Decay soul beast durations
    soulBeastDuration.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
    soulBeastDuration.entrySet().removeIf(entry -> entry.getValue() <= 0);

    // Decay gui wu durations
    guiWuDuration.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
    guiWuDuration.entrySet().removeIf(entry -> entry.getValue() <= 0);

    // Auto-deactivate effects when duration expires
    soulBeastActive.entrySet().removeIf(entry -> !soulBeastDuration.containsKey(entry.getKey()));
    guiWuActive.entrySet().removeIf(entry -> !guiWuDuration.containsKey(entry.getKey()));
  }
}
