package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * State machine for managing hun-dao entity states.
 *
 * <p>This class encapsulates all state transitions for hun-dao behaviors, including:
 *
 * <ul>
 *   <li>Soul beast transformation activation/deactivation
 *   <li>Permanent soul beast mode
 *   <li>Hunpo draining state
 * </ul>
 *
 * <p>The state machine integrates with {@link SoulBeastState} as the underlying storage layer and
 * ensures all transitions are valid and properly synchronized.
 */
public final class HunDaoStateMachine {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final LivingEntity entity;

  /**
   * Create a new state machine for an entity.
   *
   * @param entity the entity
   */
  public HunDaoStateMachine(LivingEntity entity) {
    this.entity = entity;
  }

  // ===== State Queries =====

  /**
   * Get the current Hun Dao state.
   *
   * @return current state enum
   */
  public HunDaoState getCurrentState() {
    SoulBeastState beastState = SoulBeastStateManager.getOrCreate(entity);
    if (beastState.isPermanent()) {
      return HunDaoState.SOUL_BEAST_PERMANENT;
    }
    if (beastState.isActive() || beastState.isEnabled()) {
      return HunDaoState.SOUL_BEAST_ACTIVE;
    }
    return HunDaoState.NORMAL;
  }

  /**
   * Check if the entity is in soul beast mode (active or permanent).
   *
   * @return true if in any soul beast state
   */
  public boolean isSoulBeastMode() {
    HunDaoState state = getCurrentState();
    return state == HunDaoState.SOUL_BEAST_ACTIVE || state == HunDaoState.SOUL_BEAST_PERMANENT;
  }

  /**
   * Check if the entity is permanently a soul beast.
   *
   * @return true if permanent soul beast
   */
  public boolean isPermanentSoulBeast() {
    return getCurrentState() == HunDaoState.SOUL_BEAST_PERMANENT;
  }

  /**
   * Check if hunpo is currently draining.
   *
   * @return true if in soul beast mode and draining is active
   */
  public boolean isDraining() {
    return isSoulBeastMode() && !isPermanentSoulBeast();
  }

  // ===== State Transitions =====

  /**
   * Activate soul beast transformation.
   *
   * @return true if transition succeeded
   */
  public boolean activateSoulBeast() {
    HunDaoState current = getCurrentState();
    if (!canTransition(current, HunDaoState.SOUL_BEAST_ACTIVE)) {
      LOGGER.debug("[hun_dao][state] Cannot activate soul beast from state {}", current);
      return false;
    }
    SoulBeastStateManager.setEnabled(entity, true);
    SoulBeastStateManager.setActive(entity, true);
    logTransition(current, HunDaoState.SOUL_BEAST_ACTIVE);
    return true;
  }

  /**
   * Deactivate soul beast transformation.
   *
   * @return true if transition succeeded
   */
  public boolean deactivateSoulBeast() {
    HunDaoState current = getCurrentState();
    if (current == HunDaoState.SOUL_BEAST_PERMANENT) {
      LOGGER.debug("[hun_dao][state] Cannot deactivate permanent soul beast");
      return false;
    }
    if (current == HunDaoState.NORMAL) {
      return true; // Already normal
    }
    SoulBeastStateManager.setEnabled(entity, false);
    SoulBeastStateManager.setActive(entity, false);
    logTransition(current, HunDaoState.NORMAL);
    return true;
  }

  /**
   * Make soul beast transformation permanent.
   *
   * @return true if transition succeeded
   */
  public boolean makePermanent() {
    HunDaoState current = getCurrentState();
    if (!canTransition(current, HunDaoState.SOUL_BEAST_PERMANENT)) {
      LOGGER.debug("[hun_dao][state] Cannot make soul beast permanent from state {}", current);
      return false;
    }
    SoulBeastStateManager.setPermanent(entity, true);
    SoulBeastStateManager.setEnabled(entity, true);
    SoulBeastStateManager.setActive(entity, true);
    logTransition(current, HunDaoState.SOUL_BEAST_PERMANENT);
    return true;
  }

  /**
   * Remove permanent soul beast status (admin command).
   *
   * @return true if transition succeeded
   */
  public boolean removePermanent() {
    HunDaoState current = getCurrentState();
    if (current != HunDaoState.SOUL_BEAST_PERMANENT) {
      return false;
    }
    SoulBeastStateManager.setPermanent(entity, false);
    logTransition(current, HunDaoState.SOUL_BEAST_ACTIVE);
    return true;
  }

  // ===== Transition Validation =====

  private boolean canTransition(HunDaoState from, HunDaoState to) {
    return switch (from) {
      case NORMAL -> to == HunDaoState.SOUL_BEAST_ACTIVE || to == HunDaoState.SOUL_BEAST_PERMANENT;
      case SOUL_BEAST_ACTIVE -> to == HunDaoState.NORMAL || to == HunDaoState.SOUL_BEAST_PERMANENT;
      case SOUL_BEAST_PERMANENT -> to == HunDaoState.SOUL_BEAST_ACTIVE; // Admin only
    };
  }

  // ===== Synchronization =====

  /** Synchronize state to client (if entity is a server player). */
  public void syncToClient() {
    if (entity instanceof ServerPlayer player) {
      SoulBeastStateManager.syncToClient(player);
    }
  }

  // ===== Utilities =====

  /**
   * Get the underlying SoulBeastState.
   *
   * @return optional SoulBeastState
   */
  public Optional<SoulBeastState> getSoulBeastState() {
    return SoulBeastStateManager.getExisting(entity);
  }

  private void logTransition(HunDaoState from, HunDaoState to) {
    LOGGER.debug("[hun_dao][state] {} transitioned {} -> {}", describe(entity), from, to);
  }

  private String describe(@Nullable LivingEntity entity) {
    if (entity == null) {
      return "<null>";
    }
    return String.format(Locale.ROOT, "%s(%s)", entity.getName().getString(), entity.getUUID());
  }

  // ===== State Enum =====

  /** Hun Dao state enumeration. */
  public enum HunDaoState {
    /** Normal state - no soul beast transformation. */
    NORMAL,
    /** Soul beast transformation active - hunpo draining. */
    SOUL_BEAST_ACTIVE,
    /** Permanent soul beast - no hunpo drain. */
    SOUL_BEAST_PERMANENT
  }
}
