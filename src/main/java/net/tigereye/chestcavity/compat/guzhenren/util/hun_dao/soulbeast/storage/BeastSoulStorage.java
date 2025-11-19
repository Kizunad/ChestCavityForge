package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage;

import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Abstraction for persisting beast souls inside Hun Dao organs.
 *
 * <p>Implementations decide how to:
 *
 * <ul>
 *   <li>Validate capture targets
 *   <li>Serialize to carrier-specific storage
 *   <li>Read, consume, or clear existing souls
 * </ul>
 */
public interface BeastSoulStorage {

  /** Returns {@code true} if the organ already holds a stored beast soul. */
  boolean hasStoredSoul(ItemStack organ);

  /**
   * Performs lightweight validation before attempting to store a soul.
   *
   * @return {@code true} when the organ/entity pair is eligible
   */
  default boolean canStore(ItemStack organ, LivingEntity entity) {
    return organ != null
        && !organ.isEmpty()
        && entity != null
        && !(entity instanceof net.minecraft.world.entity.player.Player)
        && !hasStoredSoul(organ);
  }

  /**
   * Serializes the entity and persists it inside the organ.
   *
   * @param organ Carrier item stack
   * @param entity Target entity
   * @param storedGameTime Game time when the capture happened
   * @return Beast soul snapshot if storage succeeded
   */
  Optional<BeastSoulRecord> store(ItemStack organ, LivingEntity entity, long storedGameTime);

  /** Returns the stored soul snapshot without mutating the organ. */
  Optional<BeastSoulRecord> peek(ItemStack organ);

  /** Removes and returns the stored soul so callers can recreate the entity elsewhere. */
  Optional<BeastSoulRecord> consume(ItemStack organ);

  /** Clears the stored soul, if any. */
  void clear(ItemStack organ);
}
