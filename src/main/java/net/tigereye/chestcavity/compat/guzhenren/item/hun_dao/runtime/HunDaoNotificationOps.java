package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for hun-dao notification and upkeep operations.
 *
 * <p>Decouples behavior classes from direct middleware dependencies by providing an abstraction
 * for entity maintenance (saturation, notifications, etc.).
 */
public interface HunDaoNotificationOps {

  /**
   * Handle player upkeep (saturation maintenance, etc.).
   *
   * @param player the player to handle
   */
  void handlePlayer(Player player);

  /**
   * Handle non-player entity upkeep (placeholder for future functionality).
   *
   * @param entity the entity to handle
   */
  void handleNonPlayer(LivingEntity entity);
}
