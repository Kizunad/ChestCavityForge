package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Mutable state container representing whether an entity is currently a soul beast.
 *
 * <p>This class is intentionally lightweight so it can be attached as an entity attachment and
 * synchronised across the network. All mutation happens through {@link SoulBeastStateManager}.
 */
public final class SoulBeastState {

  private static final String KEY_ACTIVE = "active";
  private static final String KEY_PERMANENT = "permanent";
  private static final String KEY_ENABLED = "enabled"; // server-side gate
  private static final String KEY_LAST_TICK = "last_tick";
  private static final String KEY_STARTED_TICK = "started_tick";
  private static final String KEY_SOURCE = "source";

  private boolean active;
  private boolean permanent;
  private boolean enabled;
  private long lastTick;
  private long startedTick;
  @Nullable private ResourceLocation source;

  /** Creates an inactive soul beast state placeholder. */
  public SoulBeastState() {
    this.active = false;
    this.permanent = false;
    this.enabled = false;
    this.lastTick = 0L;
    this.startedTick = 0L;
    this.source = null;
  }

  /**
   * Checks whether the state is currently active.
   *
   * @return {@code true} if soul beast mode is active
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Updates the active flag if necessary.
   *
   * @param active new active value
   * @return {@code true} if the flag changed
   */
  public boolean setActive(boolean active) {
    if (this.active == active) {
      return false;
    }
    this.active = active;
    return true;
  }

  /**
   * Checks if the state has been made permanent.
   *
   * @return {@code true} if the state persists forever
   */
  public boolean isPermanent() {
    return permanent;
  }

  /**
   * Updates the permanent flag if necessary.
   *
   * @param permanent new permanent flag value
   * @return {@code true} if the flag changed
   */
  public boolean setPermanent(boolean permanent) {
    if (this.permanent == permanent) {
      return false;
    }
    this.permanent = permanent;
    return true;
  }

  /**
   * Returns the last tick when the state was touched.
   *
   * @return last touched tick
   */
  public long getLastTick() {
    return lastTick;
  }

  /**
   * Updates the last touched tick.
   *
   * @param lastTick world tick timestamp
   */
  public void setLastTick(long lastTick) {
    this.lastTick = Math.max(0L, lastTick);
  }

  /**
   * Checks whether the state is administratively enabled.
   *
   * @return {@code true} if behavior is enabled server-side
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Updates the enabled flag if necessary.
   *
   * @param enabled new enabled value
   * @return {@code true} if the flag changed
   */
  public boolean setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return false;
    }
    this.enabled = enabled;
    return true;
  }

  /**
   * Returns the tick when the state first became active.
   *
   * @return origin tick
   */
  public long getStartedTick() {
    return startedTick;
  }

  /**
   * Updates the start tick.
   *
   * @param startedTick baseline tick
   */
  public void setStartedTick(long startedTick) {
    this.startedTick = Math.max(0L, startedTick);
  }

  /**
   * Returns the identifier for the current soul beast source if present.
   *
   * @return optional source identifier
   */
  public Optional<ResourceLocation> getSource() {
    return Optional.ofNullable(source);
  }

  /**
   * Updates the source identifier if it differs.
   *
   * @param source new origin identifier
   * @return {@code true} if the identifier changed
   */
  public boolean setSource(@Nullable ResourceLocation source) {
    if (Objects.equals(this.source, source)) {
      return false;
    }
    this.source = source;
    return true;
  }

  /**
   * Saves the soul beast state to a compound tag.
   *
   * @return The compound tag.
   */
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putBoolean(KEY_ACTIVE, active);
    tag.putBoolean(KEY_PERMANENT, permanent);
    tag.putBoolean(KEY_ENABLED, enabled);
    tag.putLong(KEY_LAST_TICK, lastTick);
    tag.putLong(KEY_STARTED_TICK, startedTick);
    if (source != null) {
      tag.putString(KEY_SOURCE, source.toString());
    }
    return tag;
  }

  /**
   * Loads the soul beast state from a compound tag.
   *
   * @param tag The compound tag.
   */
  public void load(CompoundTag tag) {
    if (tag == null || tag.isEmpty()) {
      return;
    }
    active = tag.getBoolean(KEY_ACTIVE);
    permanent = tag.getBoolean(KEY_PERMANENT);
    enabled = tag.getBoolean(KEY_ENABLED);
    lastTick = tag.getLong(KEY_LAST_TICK);
    startedTick = tag.getLong(KEY_STARTED_TICK);
    if (tag.contains(KEY_SOURCE)) {
      try {
        source = ResourceLocation.parse(tag.getString(KEY_SOURCE));
      } catch (IllegalArgumentException ex) {
        source = null;
      }
    } else {
      source = null;
    }
  }

  @Override
  public String toString() {
    return String.format(
        Locale.ROOT,
        "SoulBeastState{active=%s, permanent=%s, lastTick=%d, source=%s}",
        active,
        permanent,
        lastTick,
        source);
  }
}
