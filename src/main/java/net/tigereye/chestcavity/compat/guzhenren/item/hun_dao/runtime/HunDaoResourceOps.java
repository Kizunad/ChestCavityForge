package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import net.minecraft.world.entity.player.Player;

/**
 * Interface for hun-dao resource operations.
 *
 * <p>Decouples behavior classes from direct middleware dependencies by providing an abstraction
 * for hunpo resource management.
 */
public interface HunDaoResourceOps {

  /**
   * Leak hunpo from a player at a certain rate per second.
   *
   * @param player the player to leak hunpo from
   * @param amount the amount of hunpo to leak per second
   */
  void leakHunpoPerSecond(Player player, double amount);

  /**
   * Attempt to consume hunpo from a player.
   *
   * @param player the player to consume hunpo from
   * @param amount the amount of hunpo to consume
   * @return true if the hunpo was successfully consumed, false otherwise
   */
  boolean consumeHunpo(Player player, double amount);
}
