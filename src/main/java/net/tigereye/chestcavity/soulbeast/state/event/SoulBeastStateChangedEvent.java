package net.tigereye.chestcavity.soulbeast.state.event;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent}
 */
@Deprecated(forRemoval = true)
public class SoulBeastStateChangedEvent extends Event {

  private final LivingEntity entity;
  private final Snapshot previous;
  private final Snapshot current;

  public SoulBeastStateChangedEvent(LivingEntity entity, Snapshot previous, Snapshot current) {
    this.entity = entity;
    this.previous = previous;
    this.current = current;
  }

  public LivingEntity entity() {
    return entity;
  }

  public Snapshot previous() {
    return previous;
  }

  public Snapshot current() {
    return current;
  }

  /** Immutable view over the soul beast state flags at a moment in time. */
  public record Snapshot(boolean active, boolean enabled, boolean permanent) {

    public static final Snapshot EMPTY = new Snapshot(false, false, false);

    public boolean isSoulBeast() {
      return active || enabled || permanent;
    }
  }
}
