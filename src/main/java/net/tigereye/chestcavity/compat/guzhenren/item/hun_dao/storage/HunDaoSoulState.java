package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * Persistent state container for hun-dao soul-related data.
 *
 * <p>This class stores runtime state that is not managed by the resource system (e.g., hunpo
 * values), such as:
 *
 * <ul>
 *   <li>DOT effect tracking (soul flame duration, intensity)
 *   <li>Soul beast transformation history/statistics
 *   <li>Temporary state flags
 * </ul>
 *
 * <p>This state is attached to entities via NeoForge attachments and persisted via NBT
 * serialization.
 */
public final class HunDaoSoulState {

  // NBT keys
  private static final String KEY_SOUL_FLAME_REMAINING_TICKS = "soul_flame_remaining_ticks";
  private static final String KEY_SOUL_FLAME_DPS = "soul_flame_dps";
  private static final String KEY_SOUL_BEAST_TOTAL_DURATION_TICKS =
      "soul_beast_total_duration_ticks";
  private static final String KEY_SOUL_BEAST_ACTIVATION_COUNT = "soul_beast_activation_count";
  private static final String KEY_LAST_HUNPO_LEAK_TICK = "last_hunpo_leak_tick";

  // DOT tracking
  private int soulFlameRemainingTicks;
  private double soulFlameDps;

  // Soul beast statistics
  private long soulBeastTotalDurationTicks;
  private int soulBeastActivationCount;

  // Scheduler state
  private long lastHunpoLeakTick;

  /** Creates a new empty soul state with all counters reset to zero. */
  public HunDaoSoulState() {
    this.soulFlameRemainingTicks = 0;
    this.soulFlameDps = 0.0;
    this.soulBeastTotalDurationTicks = 0L;
    this.soulBeastActivationCount = 0;
    this.lastHunpoLeakTick = 0L;
  }

  // ===== Soul Flame DOT =====

  /** Returns the remaining soul flame duration in ticks. */
  public int getSoulFlameRemainingTicks() {
    return soulFlameRemainingTicks;
  }

  /**
   * Sets the remaining soul flame duration in ticks.
   *
   * @param ticks remaining duration in ticks
   */
  public void setSoulFlameRemainingTicks(int ticks) {
    this.soulFlameRemainingTicks = Math.max(0, ticks);
  }

  /** Returns the soul flame damage per second. */
  public double getSoulFlameDps() {
    return soulFlameDps;
  }

  /**
   * Sets the soul flame damage per second.
   *
   * @param dps damage per second
   */
  public void setSoulFlameDps(double dps) {
    this.soulFlameDps = Math.max(0.0, dps);
  }

  /** Returns whether the entity currently has an active soul flame effect. */
  public boolean hasSoulFlame() {
    return soulFlameRemainingTicks > 0;
  }

  /** Clears all soul flame state. */
  public void clearSoulFlame() {
    this.soulFlameRemainingTicks = 0;
    this.soulFlameDps = 0.0;
  }

  // ===== Soul Beast Statistics =====

  /** Returns the total accumulated soul beast duration in ticks. */
  public long getSoulBeastTotalDurationTicks() {
    return soulBeastTotalDurationTicks;
  }

  /**
   * Sets the total accumulated soul beast duration in ticks.
   *
   * @param ticks total duration in ticks
   */
  public void setSoulBeastTotalDurationTicks(long ticks) {
    this.soulBeastTotalDurationTicks = Math.max(0L, ticks);
  }

  /**
   * Adds the given duration to the total soul beast duration.
   *
   * @param ticks duration in ticks to add
   */
  public void addSoulBeastDuration(long ticks) {
    this.soulBeastTotalDurationTicks += ticks;
  }

  /** Returns how many times soul beast has been activated. */
  public int getSoulBeastActivationCount() {
    return soulBeastActivationCount;
  }

  /**
   * Sets the total number of soul beast activations.
   *
   * @param count activation count
   */
  public void setSoulBeastActivationCount(int count) {
    this.soulBeastActivationCount = Math.max(0, count);
  }

  /** Increments the soul beast activation count by one. */
  public void incrementSoulBeastActivationCount() {
    this.soulBeastActivationCount++;
  }

  // ===== Scheduler State =====

  /** Returns the last game tick when hunpo leak was processed. */
  public long getLastHunpoLeakTick() {
    return lastHunpoLeakTick;
  }

  /**
   * Sets the last game tick when hunpo leak was processed.
   *
   * @param tick game tick value
   */
  public void setLastHunpoLeakTick(long tick) {
    this.lastHunpoLeakTick = Math.max(0L, tick);
  }

  // ===== Persistence =====

  /**
   * Save this state to NBT.
   *
   * @return NBT tag containing all state data
   */
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putInt(KEY_SOUL_FLAME_REMAINING_TICKS, soulFlameRemainingTicks);
    tag.putDouble(KEY_SOUL_FLAME_DPS, soulFlameDps);
    tag.putLong(KEY_SOUL_BEAST_TOTAL_DURATION_TICKS, soulBeastTotalDurationTicks);
    tag.putInt(KEY_SOUL_BEAST_ACTIVATION_COUNT, soulBeastActivationCount);
    tag.putLong(KEY_LAST_HUNPO_LEAK_TICK, lastHunpoLeakTick);
    return tag;
  }

  /**
   * Load this state from NBT.
   *
   * @param tag NBT tag to load from (may be null or empty)
   */
  public void load(@Nullable CompoundTag tag) {
    if (tag == null || tag.isEmpty()) {
      return;
    }
    soulFlameRemainingTicks = tag.getInt(KEY_SOUL_FLAME_REMAINING_TICKS);
    soulFlameDps = tag.getDouble(KEY_SOUL_FLAME_DPS);
    soulBeastTotalDurationTicks = tag.getLong(KEY_SOUL_BEAST_TOTAL_DURATION_TICKS);
    soulBeastActivationCount = tag.getInt(KEY_SOUL_BEAST_ACTIVATION_COUNT);
    lastHunpoLeakTick = tag.getLong(KEY_LAST_HUNPO_LEAK_TICK);
  }

  /**
   * Create a copy of this state.
   *
   * @return a new HunDaoSoulState with the same values
   */
  public HunDaoSoulState copy() {
    HunDaoSoulState copy = new HunDaoSoulState();
    copy.soulFlameRemainingTicks = this.soulFlameRemainingTicks;
    copy.soulFlameDps = this.soulFlameDps;
    copy.soulBeastTotalDurationTicks = this.soulBeastTotalDurationTicks;
    copy.soulBeastActivationCount = this.soulBeastActivationCount;
    copy.lastHunpoLeakTick = this.lastHunpoLeakTick;
    return copy;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof HunDaoSoulState other)) {
      return false;
    }
    return soulFlameRemainingTicks == other.soulFlameRemainingTicks
        && Double.compare(soulFlameDps, other.soulFlameDps) == 0
        && soulBeastTotalDurationTicks == other.soulBeastTotalDurationTicks
        && soulBeastActivationCount == other.soulBeastActivationCount
        && lastHunpoLeakTick == other.lastHunpoLeakTick;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        soulFlameRemainingTicks,
        soulFlameDps,
        soulBeastTotalDurationTicks,
        soulBeastActivationCount,
        lastHunpoLeakTick);
  }

  @Override
  public String toString() {
    return String.format(
        Locale.ROOT,
        "HunDaoSoulState{soulFlame=%dt/%.2fdps, beastStats=%d activations/%dt total, leak=t%d}",
        soulFlameRemainingTicks,
        soulFlameDps,
        soulBeastActivationCount,
        soulBeastTotalDurationTicks,
        lastHunpoLeakTick);
  }
}
