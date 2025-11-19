package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.HunDaoCooldownOps;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.HunDaoDaohenOps;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage.HunDaoSoulState;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Runtime context for hun-dao operations on an entity.
 *
 * <p>This class provides unified access to all hun-dao runtime components:
 *
 * <ul>
 *   <li>{@link HunDaoResourceOps} - resource operations (hunpo)
 *   <li>{@link HunDaoFxOps} - special effects (soul flame)
 *   <li>{@link HunDaoNotificationOps} - notifications and maintenance
 *   <li>{@link HunDaoStateMachine} - state machine for soul beast transformations
 *   <li>{@link HunDaoDaohenOps} - effective scar computation hook (Phase 9 stub)
 *   <li>{@link HunDaoCooldownOps} - liupai-based cooldown scaling hook (Phase 9 stub)
 *   <li>{@link HunDaoSoulState} - persistent soul state storage
 * </ul>
 *
 * <p>This context is the primary interface for behavior classes to interact with hun-dao systems,
 * ensuring proper decoupling and testability.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Get context for a player
 * HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);
 *
 * // Access operations
 * context.getResourceOps().consumeHunpo(player, 10.0);
 * context.getStateMachine().activateSoulBeast();
 * context.getFxOps().applySoulFlame(player, target, 5.0, 3);
 * }</pre>
 */
public final class HunDaoRuntimeContext {

  private final LivingEntity entity;
  private final HunDaoResourceOps resourceOps;
  private final HunDaoFxOps fxOps;
  private final HunDaoNotificationOps notificationOps;
  private final HunDaoStateMachine stateMachine;
  private final HunDaoDaohenOps scarOps;
  private final HunDaoCooldownOps cooldownOps;

  private HunDaoRuntimeContext(
      LivingEntity entity,
      HunDaoResourceOps resourceOps,
      HunDaoFxOps fxOps,
      HunDaoNotificationOps notificationOps,
      HunDaoDaohenOps scarOps,
      HunDaoCooldownOps cooldownOps) {
    this.entity = entity;
    this.resourceOps = resourceOps;
    this.fxOps = fxOps;
    this.notificationOps = notificationOps;
    this.stateMachine = new HunDaoStateMachine(entity);
    this.scarOps = scarOps;
    this.cooldownOps = cooldownOps;
  }

  // ===== Factory Methods =====

  /**
   * Get or create a runtime context for an entity.
   *
   * <p>This method uses the default adapter implementation for all operations.
   *
   * @param entity the entity
   * @return runtime context
   */
  public static HunDaoRuntimeContext get(LivingEntity entity) {
    return builder()
        .entity(entity)
        .resourceOps(HunDaoOpsAdapter.INSTANCE)
        .fxOps(HunDaoOpsAdapter.INSTANCE)
        .notificationOps(HunDaoOpsAdapter.INSTANCE)
        .scarOps(HunDaoDaohenOps.INSTANCE)
        .cooldownOps(HunDaoCooldownOps.INSTANCE)
        .build();
  }

  /**
   * Create a builder for custom runtime contexts.
   *
   * <p>This is primarily useful for testing with mock implementations.
   *
   * @return builder
   */
  public static Builder builder() {
    return new Builder();
  }

  // ===== Accessors =====

  /**
   * Get the entity this context is associated with.
   *
   * @return the entity
   */
  public LivingEntity getEntity() {
    return entity;
  }

  /**
   * Get the resource operations interface.
   *
   * @return resource ops
   */
  public HunDaoResourceOps getResourceOps() {
    return resourceOps;
  }

  /**
   * Get the FX operations interface.
   *
   * @return FX ops
   */
  public HunDaoFxOps getFxOps() {
    return fxOps;
  }

  /**
   * Get the notification operations interface.
   *
   * @return notification ops
   */
  public HunDaoNotificationOps getNotificationOps() {
    return notificationOps;
  }

  /**
   * Get the state machine.
   *
   * @return state machine
   */
  public HunDaoStateMachine getStateMachine() {
    return stateMachine;
  }

  /**
   * Get the dao-hen (scar) operation helper.
   *
   * <p>Phase 9 returns the stub implementation that proxies to the Guzhenren resource bridge.
   */
  public HunDaoDaohenOps getScarOps() {
    return scarOps;
  }

  /**
   * Get the cooldown helper for Hun Dao liupai scaling.
   *
   * <p>Phase 9 returns a placeholder that simply logs usage; later phases can inject customized
   * cooldown strategies here.
   */
  public HunDaoCooldownOps getCooldownOps() {
    return cooldownOps;
  }

  /**
   * Get the soul state storage.
   *
   * <p>This retrieves the persistent soul state from the entity's attachments.
   *
   * @return optional soul state (empty if not yet created)
   */
  public Optional<HunDaoSoulState> getSoulState() {
    return CCAttachments.getExistingHunDaoSoulState(entity);
  }

  /**
   * Get or create the soul state storage.
   *
   * @return soul state (never null)
   */
  public HunDaoSoulState getOrCreateSoulState() {
    return CCAttachments.getHunDaoSoulState(entity);
  }

  // ===== Convenience Methods =====

  /**
   * Check if the entity is a player.
   *
   * @return true if entity is a Player
   */
  public boolean isPlayer() {
    return entity instanceof Player;
  }

  /**
   * Get the entity as a player (if it is one).
   *
   * @return optional player
   */
  public Optional<Player> asPlayer() {
    return entity instanceof Player p ? Optional.of(p) : Optional.empty();
  }

  // ===== Builder =====

  /** Builder for constructing custom runtime contexts. */
  public static final class Builder {
    private LivingEntity entity;
    private HunDaoResourceOps resourceOps;
    private HunDaoFxOps fxOps;
    private HunDaoNotificationOps notificationOps;
    private HunDaoDaohenOps scarOps;
    private HunDaoCooldownOps cooldownOps;

    private Builder() {}

    /**
     * Set the entity for the context.
     *
     * @param entity the entity
     * @return this builder
     */
    public Builder entity(@Nullable LivingEntity entity) {
      this.entity = entity;
      return this;
    }

    /**
     * Set the resource operations implementation.
     *
     * @param resourceOps the resource ops
     * @return this builder
     */
    public Builder resourceOps(@Nullable HunDaoResourceOps resourceOps) {
      this.resourceOps = resourceOps;
      return this;
    }

    /**
     * Set the FX operations implementation.
     *
     * @param fxOps the FX ops
     * @return this builder
     */
    public Builder fxOps(@Nullable HunDaoFxOps fxOps) {
      this.fxOps = fxOps;
      return this;
    }

    /**
     * Set the notification operations implementation.
     *
     * @param notificationOps the notification ops
     * @return this builder
     */
    public Builder notificationOps(@Nullable HunDaoNotificationOps notificationOps) {
      this.notificationOps = notificationOps;
      return this;
    }

    /**
     * Set the scar (dao-hen) operations helper.
     *
     * @param scarOps scar ops implementation
     * @return this builder
     */
    public Builder scarOps(@Nullable HunDaoDaohenOps scarOps) {
      this.scarOps = scarOps;
      return this;
    }

    /**
     * Set the cooldown operations helper.
     *
     * @param cooldownOps cooldown ops implementation
     * @return this builder
     */
    public Builder cooldownOps(@Nullable HunDaoCooldownOps cooldownOps) {
      this.cooldownOps = cooldownOps;
      return this;
    }

    /**
     * Build the runtime context.
     *
     * @return the new runtime context
     * @throws IllegalStateException if a required field is missing
     */
    public HunDaoRuntimeContext build() {
      if (entity == null) {
        throw new IllegalStateException("Entity is required");
      }
      if (resourceOps == null) {
        throw new IllegalStateException("ResourceOps is required");
      }
      if (fxOps == null) {
        throw new IllegalStateException("FxOps is required");
      }
      if (notificationOps == null) {
        throw new IllegalStateException("NotificationOps is required");
      }
      if (scarOps == null) {
        throw new IllegalStateException("ScarOps is required");
      }
      if (cooldownOps == null) {
        throw new IllegalStateException("CooldownOps is required");
      }
      return new HunDaoRuntimeContext(
          entity, resourceOps, fxOps, notificationOps, scarOps, cooldownOps);
    }
  }
}
