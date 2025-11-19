package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * Fired whenever a {@link
 * net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState} attached
 * to an entity changes its activation flags.
 */
public final class SoulBeastStateChangedEvent extends Event {

  private final LivingEntity entity;
  private final Snapshot previous;
  private final Snapshot current;

  public SoulBeastStateChangedEvent(LivingEntity entity, Snapshot previous, Snapshot current) {
    this.entity = entity;
    this.previous = previous;
    this.current = current;
  }

  /**
   * Returns the entity whose soul beast state changed.
   *
   * @return The entity.
   */
  public LivingEntity entity() {
    return entity;
  }

  /**
   * Returns the previous soul beast state snapshot.
   *
   * @return The previous snapshot.
   */
  public Snapshot previous() {
    return previous;
  }

  /**
   * Returns the current soul beast state snapshot.
   *
   * @return The current snapshot.
   */
  public Snapshot current() {
    return current;
  }

  /** Immutable view over the soul beast state flags at a moment in time. */
  public record Snapshot(boolean active, boolean enabled, boolean permanent) {

    public static final Snapshot EMPTY = new Snapshot(false, false, false);

    /**
     * Returns whether the snapshot represents an active soul beast state.
     *
     * @return {@code true} if the snapshot represents an active soul beast state, {@code false}
     *     otherwise.
     */
    public boolean isSoulBeast() {
      return active || enabled || permanent;
    }
  }
}
