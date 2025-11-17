package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for hun-dao visual/audio effect operations.
 *
 * <p>Decouples behavior classes from direct middleware dependencies by providing an abstraction
 * for special effects like soul flame DoT.
 */
public interface HunDaoFxOps {

  /**
   * Apply soul flame DoT effect to a target.
   *
   * @param source the player applying the effect
   * @param target the target entity
   * @param perSecondDamage damage per second
   * @param seconds duration in seconds
   */
  void applySoulFlame(Player source, LivingEntity target, double perSecondDamage, int seconds);
}
