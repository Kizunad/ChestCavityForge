package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Interface for hun-dao resource operations.
 *
 * <p>Decouples behavior classes from direct middleware and resource bridge dependencies by
 * providing an abstraction for hunpo resource management.
 *
 * <p>This interface covers all resource operations needed by hun-dao behaviors, ensuring complete
 * decoupling from GuzhenrenResourceBridge and ResourceOps.
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

  /**
   * Open a resource handle for the player.
   *
   * @param player the player
   * @return optional resource handle
   */
  Optional<GuzhenrenResourceBridge.ResourceHandle> openHandle(Player player);

  /**
   * Read the current hunpo value for a player.
   *
   * @param player the player
   * @return the current hunpo value, or 0.0 if unavailable
   */
  double readHunpo(Player player);

  /**
   * Read the maximum hunpo value for a player.
   *
   * @param player the player
   * @return the maximum hunpo value, or 0.0 if unavailable
   */
  double readMaxHunpo(Player player);

  /**
   * Read a double resource field by name.
   *
   * @param player the player
   * @param field the field name (e.g., "hunpo_kangxing")
   * @return the field value, or 0.0 if unavailable
   */
  double readDouble(Player player, String field);

  /**
   * Adjust a double resource field with optional clamping.
   *
   * @param player the player
   * @param field the field name (e.g., "hunpo")
   * @param amount the adjustment amount
   * @param clamp whether to clamp to max
   * @param maxField the max field name (e.g., "zuida_hunpo")
   * @return optional result value
   */
  OptionalDouble adjustDouble(
      Player player, String field, double amount, boolean clamp, String maxField);
}
