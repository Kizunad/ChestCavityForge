package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client-side state cache for Hun Dao mechanics.
 *
 * <p>Stores client-specific state like soul flame DoT stacks, soul beast transformation timers,
 * and hun po levels for smooth HUD rendering and FX playback. Updated via network sync from server.
 *
 * <p>Phase 5: Client-side state management for HUD and FX systems.
 *
 * <p>Phase 7: Extended with soul state, level, rarity, and attributes for Modern UI panel.
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

  // ===== Phase 7: Soul Panel Data =====

  /** Soul state per player (player UUID → soul state). */
  private final Map<UUID, SoulState> soulState = new HashMap<>();

  /** Soul level per player (player UUID → soul level). */
  private final Map<UUID, Integer> soulLevel = new HashMap<>();

  /** Soul rarity per player (player UUID → soul rarity). */
  private final Map<UUID, SoulRarity> soulRarity = new HashMap<>();

  /** Soul attributes per player (player UUID → attribute map). */
  private final Map<UUID, Map<String, Object>> soulAttributes = new HashMap<>();

  /** Whether soul system is active per player (player UUID → active flag). */
  private final Map<UUID, Boolean> soulSystemActive = new HashMap<>();

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

  // ===== Phase 7: Soul Panel Methods =====

  /**
   * Set the soul state for a player.
   *
   * @param playerId the player UUID
   * @param state the soul state (null to clear)
   */
  public void setSoulState(UUID playerId, SoulState state) {
    if (state == null) {
      soulState.remove(playerId);
    } else {
      soulState.put(playerId, state);
    }
  }

  /**
   * Get the soul state for a player.
   *
   * @param playerId the player UUID
   * @return optional soul state
   */
  public Optional<SoulState> getSoulState(UUID playerId) {
    return Optional.ofNullable(soulState.get(playerId));
  }

  /**
   * Set the soul level for a player.
   *
   * @param playerId the player UUID
   * @param level the soul level (0 or negative to clear)
   */
  public void setSoulLevel(UUID playerId, int level) {
    if (level <= 0) {
      soulLevel.remove(playerId);
    } else {
      soulLevel.put(playerId, level);
    }
  }

  /**
   * Get the soul level for a player.
   *
   * @param playerId the player UUID
   * @return the soul level (0 if not set)
   */
  public int getSoulLevel(UUID playerId) {
    return soulLevel.getOrDefault(playerId, 0);
  }

  /**
   * Set the soul rarity for a player.
   *
   * @param playerId the player UUID
   * @param rarity the soul rarity (null to clear)
   */
  public void setSoulRarity(UUID playerId, SoulRarity rarity) {
    if (rarity == null) {
      soulRarity.remove(playerId);
    } else {
      soulRarity.put(playerId, rarity);
    }
  }

  /**
   * Get the soul rarity for a player.
   *
   * @param playerId the player UUID
   * @return optional soul rarity
   */
  public Optional<SoulRarity> getSoulRarity(UUID playerId) {
    return Optional.ofNullable(soulRarity.get(playerId));
  }

  /**
   * Set the soul attributes for a player.
   *
   * @param playerId the player UUID
   * @param attributes the attribute map (null or empty to clear)
   */
  public void setSoulAttributes(UUID playerId, Map<String, Object> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      soulAttributes.remove(playerId);
    } else {
      soulAttributes.put(playerId, new LinkedHashMap<>(attributes));
    }
  }

  /**
   * Get the soul attributes for a player.
   *
   * @param playerId the player UUID
   * @return immutable map of soul attributes (empty if not set)
   */
  public Map<String, Object> getSoulAttributes(UUID playerId) {
    Map<String, Object> attrs = soulAttributes.get(playerId);
    return attrs != null ? Collections.unmodifiableMap(attrs) : Collections.emptyMap();
  }

  /**
   * Set whether the soul system is active for a player.
   *
   * @param playerId the player UUID
   * @param active true if the player has hun dao organs
   */
  public void setSoulSystemActive(UUID playerId, boolean active) {
    if (active) {
      soulSystemActive.put(playerId, true);
    } else {
      soulSystemActive.remove(playerId);
    }
  }

  /**
   * Check if the soul system is active for a player.
   *
   * @param playerId the player UUID
   * @return true if the player has hun dao organs
   */
  public boolean isSoulSystemActive(UUID playerId) {
    return soulSystemActive.getOrDefault(playerId, false);
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
    // Phase 7: Clear soul panel data
    soulState.remove(entityId);
    soulLevel.remove(entityId);
    soulRarity.remove(entityId);
    soulAttributes.remove(entityId);
    soulSystemActive.remove(entityId);
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
    // Phase 7: Clear soul panel data
    soulState.clear();
    soulLevel.clear();
    soulRarity.clear();
    soulAttributes.clear();
    soulSystemActive.clear();
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
